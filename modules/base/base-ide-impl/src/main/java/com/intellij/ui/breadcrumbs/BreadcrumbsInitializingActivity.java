// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.breadcrumbs;

import com.intellij.codeInsight.breadcrumbs.FileBreadcrumbsCollector;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import consulo.disposer.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;

final class BreadcrumbsInitializingActivity implements StartupActivity.DumbAware {
  @Override
  public void runActivity(@Nonnull Project project) {
    if (project.isDefault() || ApplicationManager.getApplication().isUnitTestMode() || project.isDisposed()) {
      return;
    }

    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new MyFileEditorManagerListener());
    connection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent event) {
        reinitBreadcrumbsInAllEditors(project);
      }
    });

    VirtualFileManager.getInstance().addVirtualFileListener(new MyVirtualFileListener(project), project);
    connection.subscribe(UISettingsListener.TOPIC, uiSettings -> reinitBreadcrumbsInAllEditors(project));

    UIUtil.invokeLaterIfNeeded(() -> reinitBreadcrumbsInAllEditors(project));
  }

  private static final class MyFileEditorManagerListener implements FileEditorManagerListener {
    @Override
    public void fileOpened(@Nonnull final FileEditorManager source, @Nonnull final VirtualFile file) {
      reinitBreadcrumbsComponent(source, file);
    }
  }

  private static class MyVirtualFileListener implements VirtualFileListener {
    private final Project myProject;

    MyVirtualFileListener(@Nonnull Project project) {
      myProject = project;
    }

    @Override
    public void propertyChanged(@Nonnull VirtualFilePropertyEvent event) {
      if (VirtualFile.PROP_NAME.equals(event.getPropertyName()) && !myProject.isDisposed()) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        VirtualFile file = event.getFile();
        if (fileEditorManager.isFileOpen(file)) {
          reinitBreadcrumbsComponent(fileEditorManager, file);
        }
      }
    }
  }

  private static void reinitBreadcrumbsInAllEditors(@Nonnull Project project) {
    if (project.isDisposed()) return;
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    for (VirtualFile virtualFile : fileEditorManager.getOpenFiles()) {
      reinitBreadcrumbsComponent(fileEditorManager, virtualFile);
    }
  }

  private static void reinitBreadcrumbsComponent(@Nonnull final FileEditorManager fileEditorManager, @Nonnull VirtualFile file) {
    boolean above = EditorSettingsExternalizable.getInstance().isBreadcrumbsAbove();
    for (FileEditor fileEditor : fileEditorManager.getAllEditors(file)) {
      if (fileEditor instanceof TextEditor) {
        TextEditor textEditor = (TextEditor)fileEditor;
        Editor editor = textEditor.getEditor();
        BreadcrumbsWrapper wrapper = BreadcrumbsWrapper.getBreadcrumbsComponent(editor);
        if (isSuitable(textEditor, file)) {
          if (wrapper != null) {
            if (wrapper.breadcrumbs.above != above) {
              remove(fileEditorManager, fileEditor, wrapper);
              wrapper.breadcrumbs.above = above;
              add(fileEditorManager, fileEditor, wrapper);
            }
            wrapper.queueUpdate();
          }
          else {
            registerWrapper(fileEditorManager, fileEditor, new BreadcrumbsWrapper(editor));
          }
        }
        else if (wrapper != null) {
          disposeWrapper(fileEditorManager, fileEditor, wrapper);
        }
      }
    }
  }

  private static boolean isSuitable(@Nonnull TextEditor editor, @Nonnull VirtualFile file) {
    if (file instanceof HttpVirtualFile || !editor.isValid()) {
      return false;
    }

    for (FileBreadcrumbsCollector collector : FileBreadcrumbsCollector.EP_NAME.getExtensionList(editor.getEditor().getProject())) {
      if (collector.handlesFile(file) && collector.isShownForFile(editor.getEditor(), file)) {
        return true;
      }
    }
    return false;
  }

  private static void add(@Nonnull FileEditorManager manager, @Nonnull FileEditor editor, @Nonnull BreadcrumbsWrapper wrapper) {
    if (wrapper.breadcrumbs.above) {
      manager.addTopComponent(editor, wrapper);
    }
    else {
      manager.addBottomComponent(editor, wrapper);
    }
  }

  private static void remove(@Nonnull FileEditorManager manager, @Nonnull FileEditor editor, @Nonnull BreadcrumbsWrapper wrapper) {
    if (wrapper.breadcrumbs.above) {
      manager.removeTopComponent(editor, wrapper);
    }
    else {
      manager.removeBottomComponent(editor, wrapper);
    }
  }

  private static void registerWrapper(@Nonnull FileEditorManager fileEditorManager, @Nonnull FileEditor fileEditor, @Nonnull BreadcrumbsWrapper wrapper) {
    add(fileEditorManager, fileEditor, wrapper);
    Disposer.register(fileEditor, () -> disposeWrapper(fileEditorManager, fileEditor, wrapper));
  }

  private static void disposeWrapper(@Nonnull FileEditorManager fileEditorManager, @Nonnull FileEditor fileEditor, @Nonnull BreadcrumbsWrapper wrapper) {
    remove(fileEditorManager, fileEditor, wrapper);
    Disposer.dispose(wrapper);
  }
}