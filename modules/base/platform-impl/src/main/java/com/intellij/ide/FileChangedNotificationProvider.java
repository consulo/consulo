/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.LogUtil;
import consulo.logging.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import consulo.annotation.access.RequiredReadAction;
import consulo.editor.notifications.EditorNotificationProvider;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class FileChangedNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private static final Logger LOG = Logger.getInstance(FileChangedNotificationProvider.class);

  private final Project myProject;

  @Inject
  public FileChangedNotificationProvider(@Nonnull Project project, @Nonnull FrameStateManager frameStateManager) {
    myProject = project;

    frameStateManager.addListener(new FrameStateListener.Adapter() {
      @Override
      public void onFrameActivated() {
        if (!myProject.isDisposed() && !GeneralSettings.getInstance().isSyncOnFrameActivation()) {
          EditorNotifications notifications = EditorNotifications.getInstance(myProject);
          for (VirtualFile file : FileEditorManager.getInstance(myProject).getSelectedFiles()) {
            notifications.updateNotifications(file);
          }
        }
      }
    }, project);

    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(myProject);
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        if (!myProject.isDisposed() && !GeneralSettings.getInstance().isSyncOnFrameActivation()) {
          Set<VirtualFile> openFiles = ContainerUtil.newHashSet(FileEditorManager.getInstance(myProject).getSelectedFiles());
          EditorNotifications notifications = EditorNotifications.getInstance(myProject);
          for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && openFiles.contains(file)) {
              notifications.updateNotifications(file);
            }
          }
        }
      }
    });
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@Nonnull VirtualFile file, @Nonnull FileEditor fileEditor) {
    if (!myProject.isDisposed() && !GeneralSettings.getInstance().isSyncOnFrameActivation()) {
      VirtualFileSystem fs = file.getFileSystem();
      if (fs instanceof LocalFileSystem) {
        FileAttributes attributes = ((LocalFileSystem)fs).getAttributes(file);
        if (attributes == null || file.getTimeStamp() != attributes.lastModified || file.getLength() != attributes.length) {
          LogUtil.debug(LOG, "%s: (%s,%s) -> %s", file, file.getTimeStamp(), file.getLength(), attributes);
          return createPanel(file);
        }
      }
    }

    return null;
  }

  private EditorNotificationPanel createPanel(@Nonnull final VirtualFile file) {
    EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(IdeBundle.message("file.changed.externally.message"));
    panel.createActionLabel(IdeBundle.message("file.changed.externally.reload"), () -> {
      if (!myProject.isDisposed()) {
        file.refresh(false, false);
        EditorNotifications.getInstance(myProject).updateNotifications(file);
      }
    });
    return panel;
  }
}
