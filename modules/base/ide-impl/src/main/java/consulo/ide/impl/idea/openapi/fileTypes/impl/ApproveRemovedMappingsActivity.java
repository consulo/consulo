// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextFileType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import javax.swing.event.HyperlinkEvent;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class ApproveRemovedMappingsActivity implements PostStartupActivity {
    public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("File type recognized");

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        RemovedMappingTracker removedMappings = ((FileTypeManagerImpl) FileTypeManager.getInstance()).getRemovedMappingTracker();
        List<RemovedMappingTracker.RemovedMapping> list = removedMappings.retrieveUnapprovedMappings();
        if (!list.isEmpty()) {
            UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                for (RemovedMappingTracker.RemovedMapping mapping : list) {
                    final FileNameMatcher matcher = mapping.getFileNameMatcher();
                    final FileType fileType = FileTypeManager.getInstance().findFileTypeByName(mapping.getFileTypeName());
                    NotificationService.getInstance()
                        .newWarn(GROUP)
                        .title(LocalizeValue.localizeTODO("File type recognized"))
                        .content(LocalizeValue.localizeTODO(
                            "File extension " + matcher.getPresentableString() + " was reassigned to " + fileType.getName() +
                                " <a href='revert'>Revert</a>"
                        ))
                        .hyperlinkListener(new NotificationListener.Adapter() {
                            @Override
                            @RequiredUIAccess
                            protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
                                Application.get().runWriteAction(() -> {
                                    FileTypeManager.getInstance().associate(PlainTextFileType.INSTANCE, matcher);
                                    removedMappings.add(matcher, fileType.getName(), true);
                                });
                                notification.expire();
                            }
                        })
                        .notify(project);
                    Application.get().runWriteAction(() -> FileTypeManager.getInstance().associate(fileType, matcher));
                }
            });
        }
    }
}
