/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.DesktopAsyncEditorLoader;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import consulo.annotations.RequiredWriteAction;
import consulo.application.AccessRule;
import consulo.editor.notifications.EditorNotificationProvider;
import consulo.editor.notifications.EditorNotificationProviders;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author peter
 */
public class EditorNotificationsImpl extends EditorNotifications {
  private final MergingUpdateQueue myUpdateMerger;
  private final Project myProject;

  private final NotNullLazyValue<List<EditorNotificationProvider<?>>> myProvidersValue = new NotNullLazyValue<List<EditorNotificationProvider<?>>>() {
    @Nonnull
    @Override
    protected List<EditorNotificationProvider<?>> compute() {
      return EditorNotificationProviders.createProviders(myProject);
    }
  };

  @Inject
  public EditorNotificationsImpl(Project project) {
    myProject = project;
    myUpdateMerger = new MergingUpdateQueue("EditorNotifications update merger", 100, true, null, project);
    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerAdapter() {
      @Override
      public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        updateNotifications(file);
      }
    });
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @RequiredWriteAction
      @Override
      public void enteredDumbMode(@Nonnull UIAccess uiAccess) {
        uiAccess.give(() -> updateAllNotifications());
      }

      @RequiredWriteAction
      @Override
      public void exitDumbMode(@Nonnull UIAccess uiAccess) {
        uiAccess.give(() -> updateAllNotifications());
      }
    });

  }

  @RequiredUIAccess
  @Override
  public void updateNotifications(@Nonnull final VirtualFile file) {
    List<FileEditor> editors = ContainerUtil.filter(FileEditorManager.getInstance(myProject).getAllEditors(file),
                                                    editor -> !(editor instanceof TextEditor) || DesktopAsyncEditorLoader.isEditorLoaded(((TextEditor)editor).getEditor()));

    AccessRule.readAsync(() -> {
      final List<EditorNotificationProvider<?>> providers = DumbService.getInstance(myProject).filterByDumbAwareness(myProvidersValue.getValue());

      final List<Runnable> updates = ContainerUtil.newArrayList();
      for (final FileEditor editor : editors) {
        for (final EditorNotificationProvider<?> provider : providers) {
          final JComponent component = provider.createNotificationPanel(file, editor);
          updates.add(() -> updateNotification(editor, provider.getKey(), component));
        }
      }
    });
  }

  private void updateNotification(@Nonnull FileEditor editor, @Nonnull Key<? extends JComponent> key, @Nullable JComponent component) {
    JComponent old = editor.getUserData(key);
    if (old != null) {
      FileEditorManager.getInstance(myProject).removeTopComponent(editor, old);
    }
    if (component != null) {
      FileEditorManager.getInstance(myProject).addTopComponent(editor, component);
      @SuppressWarnings("unchecked") Key<JComponent> _key = (Key<JComponent>)key;
      editor.putUserData(_key, component);
    }
    else {
      editor.putUserData(key, null);
    }
  }

  @Override
  public void updateAllNotifications() {
    myUpdateMerger.queue(new Update("update") {
      @Override
      public void run() {
        for (VirtualFile file : FileEditorManager.getInstance(myProject).getOpenFiles()) {
          updateNotifications(file);
        }
      }
    });
  }
}
