// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileTypes.impl;

import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.application.ApplicationManager;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import consulo.project.Project;
import consulo.project.startup.IdeaStartupActivity;
import consulo.application.util.registry.Registry;
import consulo.application.ui.awt.UIUtil;
import javax.annotation.Nonnull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ApproveRemovedMappingsActivity implements IdeaStartupActivity {
  @Override
  public void runActivity(@Nonnull final Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode() || !Registry.is("ide.restore.removed.mappings")) return;

    RemovedMappingTracker removedMappings = ((FileTypeManagerImpl)FileTypeManager.getInstance()).getRemovedMappingTracker();
    List<RemovedMappingTracker.RemovedMapping> list = removedMappings.retrieveUnapprovedMappings();
    if (!list.isEmpty()) {
      UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
        for (RemovedMappingTracker.RemovedMapping mapping : list) {
          final FileNameMatcher matcher = mapping.getFileNameMatcher();
          final FileType fileType = FileTypeManager.getInstance().findFileTypeByName(mapping.getFileTypeName());
          Notification notification = new Notification("File type recognized", "File type recognized",
                                                       "File extension " + matcher.getPresentableString() + " was reassigned to " + fileType.getName() + " <a href='revert'>Revert</a>",
                                                       NotificationType.WARNING, new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
              ApplicationManager.getApplication().runWriteAction(() -> {
                FileTypeManager.getInstance().associate(PlainTextFileType.INSTANCE, matcher);
                removedMappings.add(matcher, fileType.getName(), true);
              });
              notification.expire();
            }
          });
          Notifications.Bus.notify(notification, project);
          ApplicationManager.getApplication().runWriteAction(() -> FileTypeManager.getInstance().associate(fileType, matcher));
        }
      });
    }
  }
}
