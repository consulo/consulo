/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.DeprecationInfo;
import org.mustbe.consulo.RequiredDispatchThread;
import org.mustbe.consulo.RequiredReadAction;
import org.mustbe.consulo.editor.notifications.EditorNotificationProvider;
import org.mustbe.consulo.editor.notifications.EditorNotificationProviders;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeManager;
import org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeTask;

import javax.swing.*;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class EditorNotifications extends AbstractProjectComponent {
  @Deprecated
  @DeprecationInfo(value = "Use org.mustbe.consulo.editor.notifications.EditorNotificationProvider", until = "2.0")
  public abstract static class Provider<T extends JComponent> implements EditorNotificationProvider<T> {
    @Override
    @NotNull
    public abstract Key<T> getKey();

    @Override
    @Nullable
    public abstract T createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor);
  }

  @NotNull
  public static EditorNotifications getInstance(Project project) {
    return project.getComponent(EditorNotifications.class);
  }

  private final NotNullLazyValue<List<EditorNotificationProvider<?>>> myProvidersValue = new NotNullLazyValue<List<EditorNotificationProvider<?>>>() {
    @NotNull
    @Override
    protected List<EditorNotificationProvider<?>> compute() {
      return EditorNotificationProviders.createProviders(myProject);
    }
  };

  private final FileEditorManager myFileEditorManager;

  public EditorNotifications(final Project project, FileEditorManager fileEditorManager) {
    super(project);
    myFileEditorManager = fileEditorManager;
    project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        updateNotifications(file);
      }
    });
    project.getMessageBus().connect(project).subscribe(BackgroundTaskByVfsChangeManager.TOPIC, new BackgroundTaskByVfsChangeManager.ListenerAdapter() {
      @Override
      @RequiredReadAction
      public void taskChanged(@NotNull BackgroundTaskByVfsChangeTask task) {
        for (VirtualFile virtualFile : task.getGeneratedFiles()) {
          updateNotifications(virtualFile);
        }
      }
    });
  }

  public void updateNotifications(final VirtualFile file) {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          @Override
          @RequiredDispatchThread
          public void run() {
            if (myProject.isDisposed()) return;
            FileEditor[] editors = myFileEditorManager.getAllEditors(file);
            for (FileEditor editor : editors) {
              for (EditorNotificationProvider<?> provider : myProvidersValue.getValue()) {
                JComponent component = provider.createNotificationPanel(file, editor);
                Key<? extends JComponent> key = provider.getKey();
                updateNotification(editor, key, component);
              }
            }
          }
        });
      }
    });
  }

  public void updateAllNotifications() {
    VirtualFile[] files = myFileEditorManager.getOpenFiles();
    for (VirtualFile file : files) {
      updateNotifications(file);
    }
  }

  private void updateNotification(@NotNull FileEditor editor, Key<? extends JComponent> key, @Nullable JComponent component) {
    JComponent old = editor.getUserData(key);
    if (old != null) {
      myFileEditorManager.removeTopComponent(editor, old);
    }
    if (component != null) {
      myFileEditorManager.addTopComponent(editor, component);
      @SuppressWarnings("unchecked") Key<JComponent> _key = (Key<JComponent>)key;
      editor.putUserData(_key, component);
    }
    else {
      editor.putUserData(key, null);
    }
  }

  public static void updateAll() {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      getInstance(project).updateAllNotifications();
    }
  }
}
