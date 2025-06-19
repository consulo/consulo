// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.usageView.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.usage.UsageView;
import consulo.usage.UsageViewContentManager;
import consulo.usage.UsageViewSettings;
import consulo.usage.localize.UsageLocalize;
import consulo.usage.rule.UsageFilteringRuleListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Singleton
@ServiceImpl
public class UsageViewContentManagerImpl extends UsageViewContentManager {
    private final Key<Boolean> REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.REUSABLE_CONTENT_KEY");
    //todo[myakovlev] dont use it
    private final Key<Boolean> NOT_REUSABLE_CONTENT_KEY = Key.create("UsageTreeManager.NOT_REUSABLE_CONTENT_KEY");
    private final Key<UsageView> NEW_USAGE_VIEW_KEY = Key.create("NEW_USAGE_VIEW_KEY");
    private final ContentManager myFindContentManager;

    @Inject
    @RequiredUIAccess
    public UsageViewContentManagerImpl(@Nonnull Project project, @Nonnull ToolWindowManager toolWindowManager) {
        ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.FIND, true, ToolWindowAnchor.BOTTOM, project, true);
        //toolWindow.setHelpId(UsageViewImpl.HELP_ID);
        toolWindow.setDisplayName(FindLocalize.toolwindowFindDisplayName());
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setIcon(PlatformIconGroup.toolwindowsToolwindowfind());

        DumbAwareToggleAction toggleNewTabAction = new DumbAwareToggleAction(FindLocalize.findOpenInNewTabAction()) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return FindSettings.getInstance().isShowResultsInSeparateView();
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                FindSettings.getInstance().setShowResultsInSeparateView(state);
            }
        };

        DumbAwareToggleAction toggleSortAction =
            new DumbAwareToggleAction(
                UsageLocalize.sortAlphabeticallyActionText(),
                LocalizeValue.empty(),
                PlatformIconGroup.objectbrowserSorted()
            ) {
                @Override
                public boolean isSelected(@Nonnull AnActionEvent e) {
                    return UsageViewSettings.getInstance().isSortAlphabetically();
                }

                @Override
                public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                    UsageViewSettings.getInstance().setSortAlphabetically(state);
                    project.getMessageBus().syncPublisher(UsageFilteringRuleListener.class).rulesChanged();
                }
            };

        DumbAwareToggleAction toggleAutoscrollAction = new DumbAwareToggleAction(
            UILocalize.autoscrollToSourceActionName(),
            UILocalize.autoscrollToSourceActionDescription(),
            PlatformIconGroup.generalAutoscrolltosource()
        ) {
            @Override
            public boolean isSelected(@Nonnull AnActionEvent e) {
                return UsageViewSettings.getInstance().isAutoScrollToSource();
            }

            @Override
            public void setSelected(@Nonnull AnActionEvent e, boolean state) {
                UsageViewSettings.getInstance().setAutoScrollToSource(state);
            }
        };

        DefaultActionGroup gearActions = new DefaultActionGroup(IdeLocalize.groupViewOptions(), true);
        gearActions.addAll(toggleAutoscrollAction, toggleSortAction, toggleNewTabAction);
        toolWindow.setAdditionalGearActions(gearActions);

        myFindContentManager = toolWindow.getContentManager();
        myFindContentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            @RequiredUIAccess
            public void contentRemoved(@Nonnull ContentManagerEvent event) {
                event.getContent().release();
            }
        });
        ContentManagerWatcher.watchContentManager(toolWindow, myFindContentManager);
    }

    @Nonnull
    @Override
    public Content addContent(
        @Nonnull String contentName,
        boolean reusable,
        @Nonnull JComponent component,
        boolean toOpenInNewTab,
        boolean isLockable
    ) {
        return addContent(contentName, null, null, reusable, component, toOpenInNewTab, isLockable);
    }

    @Nonnull
    @Override
    public Content addContent(
        @Nonnull String contentName,
        String tabName,
        String toolwindowTitle,
        boolean reusable,
        @Nonnull JComponent component,
        boolean toOpenInNewTab,
        boolean isLockable
    ) {
        Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
        Content selectedContent = getSelectedContent();
        toOpenInNewTab |= selectedContent != null && selectedContent.isPinned();

        Content contentToDelete = null;
        int indexToAdd = -1;
        if (!toOpenInNewTab && reusable) {
            List<Content> contents = ContainerUtil.newArrayList(myFindContentManager.getContents());
            if (selectedContent != null) {
                contents.remove(selectedContent);
                contents.add(selectedContent);// Selected content has to be the last (and the best) candidate to be deleted
            }

            for (Content content : contents) {
                if (!content.isPinned() && content.getUserData(contentKey) != null) {
                    UsageView usageView = content.getUserData(NEW_USAGE_VIEW_KEY);
                    if (usageView == null || !usageView.isSearchInProgress()) {
                        contentToDelete = content;
                        indexToAdd = myFindContentManager.getIndexOfContent(contentToDelete);
                    }
                }
            }
        }
        Content content = ContentFactory.getInstance().createContent(component, contentName, isLockable);
        content.setTabName(tabName);
        content.setToolwindowTitle(toolwindowTitle);
        content.putUserData(contentKey, Boolean.TRUE);
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);

        myFindContentManager.addContent(content, indexToAdd);
        if (contentToDelete != null) {
            myFindContentManager.removeContent(contentToDelete, true);
        }
        myFindContentManager.setSelectedContent(content);

        return content;
    }

    @Override
    public int getReusableContentsCount() {
        return getContentCount(true);
    }

    private int getContentCount(boolean reusable) {
        Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
        Content[] contents = myFindContentManager.getContents();
        return (int) Arrays.stream(contents).filter(content -> content.getUserData(contentKey) != null).count();
    }

    @Override
    public Content getSelectedContent(boolean reusable) {
        Key<Boolean> contentKey = reusable ? REUSABLE_CONTENT_KEY : NOT_REUSABLE_CONTENT_KEY;
        Content selectedContent = myFindContentManager.getSelectedContent();
        return selectedContent == null || selectedContent.getUserData(contentKey) == null ? null : selectedContent;
    }

    @Override
    public Content getSelectedContent() {
        return myFindContentManager == null ? null : myFindContentManager.getSelectedContent();
    }

    @Override
    public void closeContent(@Nonnull Content content) {
        myFindContentManager.removeContent(content, true);
        content.release();
    }
}
