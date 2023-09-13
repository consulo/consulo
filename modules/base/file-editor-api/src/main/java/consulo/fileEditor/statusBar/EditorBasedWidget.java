// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.statusBar;

import consulo.application.Application;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHolder;
import consulo.codeEditor.internal.InternalEditorKeys;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.WindowManager;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;


public abstract class EditorBasedWidget implements StatusBarWidget, FileEditorManagerListener {
  private static final Logger LOG = Logger.getInstance(EditorBasedWidget.class);

  @Nonnull
  protected final Project myProject;

  protected StatusBar myStatusBar;
  protected MessageBusConnection myConnection;
  private volatile boolean myDisposed;

  protected EditorBasedWidget(@Nonnull Project project) {
    myProject = project;
  }

  @Nullable
  protected final Editor getEditor() {
    final Project project = getProject();
    if (project.isDisposed()) return null;

    FileEditor fileEditor = StatusBarUtil.getCurrentFileEditor(myStatusBar);
    Editor result = null;
    if (fileEditor instanceof TextEditor) {
      Editor editor = ((TextEditor)fileEditor).getEditor();
      if (ensureValidEditorFile(editor)) {
        result = editor;
      }
    }

    if (result == null) {
      final FileEditorManager manager = FileEditorManager.getInstance(project);
      Editor editor = manager.getSelectedTextEditor();
      if (editor != null && WindowManager.getInstance().getStatusBar(editor.getComponent(), project) == myStatusBar && ensureValidEditorFile(editor)) {
        result = editor;
      }
    }

    return result;
  }

  private static boolean ensureValidEditorFile(Editor editor) {
    Document document = editor.getDocument();
    VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null && !file.isValid()) {
      Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(file);
      Project project = editor.getProject();
      Boolean fileIsOpen = project == null ? null : ArrayUtil.contains(file, FileEditorManager.getInstance(project).getOpenFiles());
      LOG.error("Returned editor for invalid file: " + editor +
                "; disposed=" + editor.isDisposed() +
                "; file " + file.getClass() +
                "; cached document exists: " + (cachedDocument != null) +
                "; same as document: " + (cachedDocument == document) +
                "; file is open: " + fileIsOpen);
      return false;
    }
    return true;
  }

  protected boolean isOurEditor(Editor editor) {
    if(!Application.get().isSwingApplication()) {
      return true;
    }
    return editor != null &&
           editor.isShowing() &&
           !Boolean.TRUE.equals(editor.getUserData(InternalEditorKeys.SUPPLEMENTARY_KEY)) &&
           WindowManager.getInstance().getStatusBar(editor.getComponent(), editor.getProject()) == myStatusBar;
  }

  protected Component getFocusedComponent() {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner == null) {
      IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(myProject);
      FocusableFrame frame = focusManager.getLastFocusedFrame();
      if (frame != null) {
        focusOwner = focusManager.getLastFocusedFor(frame);
      }
    }
    return focusOwner;
  }

  @Nullable
  protected Editor getFocusedEditor() {
    Component component = getFocusedComponent();
    Editor editor = component instanceof EditorHolder ? ((EditorHolder)component).getEditor() : getEditor();
    return editor != null && !editor.isDisposed() ? editor : null;
  }

  @Nullable
  protected VirtualFile getSelectedFile() {
    final Editor editor = getEditor();
    if (editor == null) return null;
    Document document = editor.getDocument();
    return FileDocumentManager.getInstance().getFile(document);
  }

  @Nonnull
  protected final Project getProject() {
    return myProject;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    assert statusBar.getProject() == null || statusBar.getProject().equals(myProject) : "Cannot install widget from one project on status bar of another project";

    myStatusBar = statusBar;
    Disposer.register(myStatusBar, this);
    myConnection = myProject.getMessageBus().connect(this);
    myConnection.subscribe(FileEditorManagerListener.class, this);
  }

  @Override
  public void dispose() {
    myDisposed = true;
    myStatusBar = null;
    myConnection = null;
  }

  protected final boolean isDisposed() {
    return myDisposed;
  }
}
