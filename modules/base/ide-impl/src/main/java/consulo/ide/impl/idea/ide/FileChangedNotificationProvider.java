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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.ide.IdeBundle;
import consulo.logging.Logger;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.ide.impl.idea.ui.EditorNotifications;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.codeEditor.EditorNotificationProvider;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class FileChangedNotificationProvider implements EditorNotificationProvider<EditorNotificationPanel>, DumbAware {
  private static final Logger LOG = Logger.getInstance(FileChangedNotificationProvider.class);

  private final Project myProject;

  @Inject
  public FileChangedNotificationProvider(@Nonnull Project project, @Nonnull FrameStateManager frameStateManager) {
    myProject = project;

    frameStateManager.addListener(new FrameStateListener() {
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
    connection.subscribe(BulkFileListener.class, new BulkFileListener() {
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
          if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("%s: (%s,%s) -> %s", file, file.getTimeStamp(), file.getLength(), attributes));
          }
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
