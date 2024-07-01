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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.FrameStateManager;
import consulo.application.ui.event.FrameStateListener;
import consulo.component.messagebus.MessageBusConnection;
import consulo.fileEditor.*;
import consulo.logging.Logger;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.util.io.FileAttributes;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@ExtensionImpl
public class FileChangedNotificationProvider implements EditorNotificationProvider, DumbAware {
  private static final Logger LOG = Logger.getInstance(FileChangedNotificationProvider.class);

  private final Project myProject;
  private final GeneralSettings myGeneralSettings;
  private final EditorNotifications myEditorNotifications;

  @Inject
  public FileChangedNotificationProvider(@Nonnull Project project,
                                         @Nonnull FrameStateManager frameStateManager,
                                         @Nonnull GeneralSettings generalSettings,
                                         @Nonnull FileEditorManager fileEditorManager,
                                         @Nonnull EditorNotifications editorNotifications) {
    myProject = project;
    myGeneralSettings = generalSettings;
    myEditorNotifications = editorNotifications;

    frameStateManager.addListener(new FrameStateListener() {
      @Override
      public void onFrameActivated() {
        if (!myProject.isDisposed() && !generalSettings.isSyncOnFrameActivation()) {
          for (VirtualFile file : fileEditorManager.getSelectedFiles()) {
            editorNotifications.updateNotifications(file);
          }
        }
      }
    }, project);

    MessageBusConnection connection = project.getApplication().getMessageBus().connect(myProject);
    connection.subscribe(BulkFileListener.class, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        if (!myProject.isDisposed() && !generalSettings.isSyncOnFrameActivation()) {
          Set<VirtualFile> openFiles = Set.of(fileEditorManager.getSelectedFiles());
          for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && openFiles.contains(file)) {
              editorNotifications.updateNotifications(file);
            }
          }
        }
      }
    });
  }

  @Nonnull
  @Override
  public String getId() {
    return "file-changed-externally";
  }

  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@Nonnull VirtualFile file,
                                                     @Nonnull FileEditor fileEditor,
                                                     @Nonnull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!myProject.isDisposed() && !myGeneralSettings.isSyncOnFrameActivation()) {
      VirtualFileSystem fs = file.getFileSystem();
      if (fs instanceof LocalFileSystem) {
        FileAttributes attributes = ((LocalFileSystem)fs).getAttributes(file);
        if (attributes == null || file.getTimeStamp() != attributes.lastModified || file.getLength() != attributes.length) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("%s: (%s,%s) -> %s", file, file.getTimeStamp(), file.getLength(), attributes));
          }
          return build(file, builderFactory.get());
        }
      }
    }

    return null;
  }

  private EditorNotificationBuilder build(@Nonnull final VirtualFile file, EditorNotificationBuilder builder) {
    builder.withText(IdeLocalize.fileChangedExternallyMessage());
    builder.withAction(IdeLocalize.fileChangedExternallyReload(), (i) -> {
      if (!myProject.isDisposed()) {
        file.refresh(false, false);
        myEditorNotifications.updateNotifications(file);
      }
    });
    return builder;
  }
}
