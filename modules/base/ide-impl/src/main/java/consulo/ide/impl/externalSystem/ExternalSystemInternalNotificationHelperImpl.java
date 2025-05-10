/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.externalSystem;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposer;
import consulo.externalSystem.internal.ExternalSystemInternalNotificationHelper;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.notification.NotificationCategory;
import consulo.externalSystem.service.notification.NotificationData;
import consulo.externalSystem.service.notification.NotificationSource;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.ide.errorTreeView.GroupingElement;
import consulo.ide.impl.idea.ide.errorTreeView.NavigatableMessageElement;
import consulo.ide.impl.idea.ide.errorTreeView.NewEditableErrorTreeViewPanel;
import consulo.ide.impl.idea.ide.errorTreeView.NewErrorTreeViewPanelImpl;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.view.MessageView;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-04-10
 */
@ServiceImpl
@Singleton
public class ExternalSystemInternalNotificationHelperImpl implements ExternalSystemInternalNotificationHelper {
    private final Project myProject;

    @Inject
    public ExternalSystemInternalNotificationHelperImpl(Project project) {
        myProject = project;
    }

    public void addMessage(
        VirtualFile virtualFile,
        String groupName,
        @Nonnull Notification notification,
        @Nonnull ProjectSystemId externalSystemId,
        @Nonnull NotificationData notificationData
    ) {


        int line = notificationData.getLine() - 1;
        int column = notificationData.getColumn() - 1;
        if (virtualFile == null) {
            line = column = -1;
        }
        final int guiLine = line < 0 ? -1 : line + 1;
        final int guiColumn = column < 0 ? 0 : column + 1;

        final Navigatable navigatable = notificationData.getNavigatable() != null
            ? notificationData.getNavigatable()
            : virtualFile != null ? new OpenFileDescriptorImpl(myProject, virtualFile, line, column) : null;

        final ErrorTreeElementKind kind =
            ErrorTreeElementKind.convertMessageFromCompilerErrorType(notificationData.getNotificationCategory().getMessageCategory());
        final String[] message = notificationData.getMessage().split("\n");
        final String exportPrefix = NewErrorTreeViewPanelImpl.createExportPrefix(guiLine);
        final String rendererPrefix = NewErrorTreeViewPanelImpl.createRendererPrefix(guiLine, guiColumn);

        UIUtil.invokeLaterIfNeeded(() -> {
            boolean activate =
                notificationData.getNotificationCategory() == NotificationCategory.ERROR ||
                    notificationData.getNotificationCategory() == NotificationCategory.WARNING;
            final NewErrorTreeViewPanelImpl errorTreeView =
                prepareMessagesView(externalSystemId, notificationData.getNotificationSource(), activate);
            final GroupingElement groupingElement = errorTreeView.getErrorViewStructure().getGroupingElement(groupName, null, virtualFile);
            final NavigatableMessageElement navigatableMessageElement;
            if (notificationData.hasLinks()) {
                navigatableMessageElement = new EditableNotificationMessageElement(
                    notification,
                    kind,
                    groupingElement,
                    message,
                    navigatable,
                    exportPrefix,
                    rendererPrefix
                );
            }
            else {
                navigatableMessageElement = new NotificationMessageElement(
                    kind,
                    groupingElement,
                    message,
                    navigatable,
                    exportPrefix,
                    rendererPrefix
                );
            }

            errorTreeView.getErrorViewStructure().addNavigatableMessage(groupName, navigatableMessageElement);
            errorTreeView.updateTree();
        });
    }

    @Nonnull
    @RequiredUIAccess
    public NewErrorTreeViewPanelImpl prepareMessagesView(
        @Nonnull final ProjectSystemId externalSystemId,
        @Nonnull final NotificationSource notificationSource,
        boolean activateView
    ) {
        UIAccess.assertIsUIThread();

        final NewErrorTreeViewPanelImpl errorTreeView;
        final String contentDisplayName = getContentDisplayName(notificationSource, externalSystemId);
        final Pair<NotificationSource, ProjectSystemId> contentIdPair = Pair.create(notificationSource, externalSystemId);
        Content targetContent = findContent(contentIdPair, contentDisplayName);

        final MessageView messageView = ServiceManager.getService(myProject, MessageView.class);
        if (targetContent == null || !contentIdPair.equals(targetContent.getUserData(CONTENT_ID_KEY))) {
            errorTreeView = new NewEditableErrorTreeViewPanel(myProject, null, true, true, null);
            targetContent = ContentFactory.getInstance().createContent(errorTreeView, contentDisplayName, true);
            targetContent.putUserData(CONTENT_ID_KEY, contentIdPair);

            messageView.getContentManager().addContent(targetContent);
            Disposer.register(targetContent, errorTreeView);
        }
        else {
            assert targetContent.getComponent() instanceof NewEditableErrorTreeViewPanel;
            errorTreeView = (NewEditableErrorTreeViewPanel) targetContent.getComponent();
        }

        messageView.getContentManager().setSelectedContent(targetContent);
        final ToolWindow tw = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (activateView && tw != null && !tw.isActive()) {
            tw.activate(null, false);
        }
        return errorTreeView;
    }

    @Nullable
    private Content findContent(@Nonnull Pair<NotificationSource, ProjectSystemId> contentIdPair, @Nonnull String contentDisplayName) {
        Content targetContent = null;
        final MessageView messageView = myProject.getInstance(MessageView.class);
        for (Content content : messageView.getContentManager().getContents()) {
            if (contentIdPair.equals(content.getUserData(CONTENT_ID_KEY))
                && StringUtil.equals(content.getDisplayName(), contentDisplayName) && !content.isPinned()) {
                targetContent = content;
            }
        }
        return targetContent;
    }

    @Nonnull
    public static String getContentDisplayName(
        @Nonnull final NotificationSource notificationSource,
        @Nonnull final ProjectSystemId externalSystemId
    ) {
        if (notificationSource != NotificationSource.PROJECT_SYNC) {
            throw new AssertionError("unsupported notification source found: " + notificationSource);
        }
        return ExternalSystemLocalize.notificationMessagesProjectSyncTabName(externalSystemId.getReadableName()).get();
    }
}
