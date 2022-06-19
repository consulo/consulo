// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.swing.event.HyperlinkEvent;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ApproveRemovedMappingsActivity implements PostStartupActivity {
  @Override
  public void runActivity(@Nonnull final Project project, UIAccess uiAccess) {
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
