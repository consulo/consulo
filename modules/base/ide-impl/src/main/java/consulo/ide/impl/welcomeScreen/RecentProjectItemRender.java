/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.welcomeScreen;

import consulo.application.util.UserHomeFileUtil;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.Label;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;

import java.util.function.BiConsumer;

/**
 * A recent project as {@code [icon] name [branch]} over its path, with a menu handle on the right. Shared by every
 * welcome screen so a row looks the same wherever the list is shown.
 *
 * @author VISTALL
 * @since 2026-08-23
 */
public class RecentProjectItemRender implements ComponentItemRender<AnAction> {
    private static final int GROUP_INDENT = 22;
    private static final int GAP = 8;
    private static final int VERTICAL_GAP = 4;

    public static final Length ROW_HEIGHT = Length.ofFont(2.5f).plusPixel(2 * VERTICAL_GAP);

    /**
     * The recent projects are the same list on every welcome screen, so how much room it asks for is decided once
     * here rather than by each frontend picking its own pixel count.
     */
    public static final Length LIST_WIDTH = Length.ofFont(18);
    public static final Length LIST_HEIGHT = Length.ofFont(23);

    private final RecentProjectsManager myRecentProjectsManager;
    private final RecentProjectsChecker myRecentProjectsChecker;
    private final BiConsumer<AnAction, ComponentEvent<Component>> myActivateHandler;
    private final BiConsumer<AnAction, ComponentEvent<Component>> myMenuHandler;

    public RecentProjectItemRender(
        RecentProjectsManager recentProjectsManager,
        RecentProjectsChecker recentProjectsChecker,
        @RequiredUIAccess BiConsumer<AnAction, ComponentEvent<Component>> activateHandler,
        @RequiredUIAccess BiConsumer<AnAction, ComponentEvent<Component>> menuHandler
    ) {
        myRecentProjectsManager = recentProjectsManager;
        myRecentProjectsChecker = recentProjectsChecker;
        myActivateHandler = activateHandler;
        myMenuHandler = menuHandler;
    }

    @Override
    @RequiredUIAccess
    public Component render(RenderItem<AnAction> item) {
        item.allowMouseEvents();

        AnAction value = item.getValue();

        DockLayout row = DockLayout.create();
        row.addClickListener(event -> myActivateHandler.accept(value, event));
        row.addContextMenuListener(event -> myMenuHandler.accept(value, event));
        row.right(createMenuHandle(value));
        row.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, GAP);
        row.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, VERTICAL_GAP);
        row.addBorder(BorderPosition.BOTTOM, BorderStyle.EMPTY, VERTICAL_GAP);

        if (value instanceof PopupProjectGroupActionGroup groupAction) {
            ProjectGroup group = groupAction.getGroup();

            Label name = Label.create(LocalizeValue.of(group.getName()));
            name.setImage(group.isExpanded() ? PlatformIconGroup.nodesFolderopened() : PlatformIconGroup.nodesFolder());
            row.center(name);
            row.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, GAP);
            return row;
        }

        ReopenProjectAction action = (ReopenProjectAction) value;

        Label icon = Label.create();
        icon.setImage(action.getExtensionIcon());
        icon.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, GAP);
        row.left(icon);

        row.center(VerticalLayout.create().add(createTitle(action)).add(createPath(action)));
        row.setAccessibleName(LocalizeValue.of(action.getProjectName() + " - " + action.getProjectPath()));

        row.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, isInsideGroup(action) ? GROUP_INDENT + GAP : GAP);

        return row;
    }

    @RequiredUIAccess
    private Component createMenuHandle(AnAction value) {
        Label menu = Label.create();
        menu.setImage(PlatformIconGroup.actionsMorevertical());
        menu.setToolTipText(ProjectLocalize.recentProjectRemoveFromListTooltip());
        menu.addClickListener(event -> myMenuHandler.accept(value, event));
        return menu;
    }

    @RequiredUIAccess
    private Component createTitle(ReopenProjectAction action) {
        HorizontalLayout title = HorizontalLayout.create();
        title.add(Label.create(action.isOpened()
            ? ProjectLocalize.recentProject0OpenedActionText(LocalizeValue.of(action.getProjectName()))
            : LocalizeValue.of(action.getProjectName())));

        String branch = myRecentProjectsChecker.getBranch(action.getProjectPath());
        if (branch != null && !branch.isEmpty()) {
            Label branchLabel = Label.create(LocalizeValue.of(branch));
            branchLabel.setImage(PlatformIconGroup.vcsBranch());
            branchLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            branchLabel.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, GAP);
            title.add(branchLabel);
        }

        return title;
    }

    @RequiredUIAccess
    private Component createPath(ReopenProjectAction action) {
        String projectPath = action.getProjectPath();

        Label path = Label.create(LocalizeValue.of(UserHomeFileUtil.getLocationRelativeToUserHome(projectPath, false)));
        path.setForegroundColor(myRecentProjectsChecker.isValid(projectPath) ? ComponentColors.DISABLED_TEXT : StandardColors.RED);
        return path;
    }


    private boolean isInsideGroup(ReopenProjectAction action) {
        for (ProjectGroup group : myRecentProjectsManager.getGroups()) {
            if (group.getProjects().contains(action.getProjectPath())) {
                return true;
            }
        }
        return false;
    }
}
