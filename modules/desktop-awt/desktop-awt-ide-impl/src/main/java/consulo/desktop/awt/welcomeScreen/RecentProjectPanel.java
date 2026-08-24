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
package consulo.desktop.awt.welcomeScreen;

import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.ide.impl.welcomeScreen.RecentProjectItemRender;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ComponentItemRender;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author max
 */
public class RecentProjectPanel {
    public static final String RECENT_PROJECTS_LABEL = "Recent Projects";

    protected static final String WELCOME_SCREEN_RECENT_PROJECT_ACTION_GROUP = "WelcomeScreenRecentProjectActionGroup";

    private @Nullable ActionPopupMenu myMenu;

    protected AnAction removeRecentProjectAction;

    protected final DockLayout myRootLayout;
    protected final ScrollableLayout myScrollLayout;
    protected final ListBox<AnAction> myListBox;
    protected final MutableFlatDataModel<AnAction> myModel;

    protected final JPanel myRootPanel;
    protected final JBList<AnAction> myList;
    protected final JScrollPane myScrollPane;

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public RecentProjectPanel(Disposable parentDisposable) {
        AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false, isUseGroups());

        Collection<String> pathsToCheck = new HashSet<>();
        for (AnAction action : recentProjectActions) {
            if (action instanceof ReopenProjectAction item) {
                pathsToCheck.add(item.getProjectPath());
            }
        }

        myModel = FlatDataModel.of(Arrays.asList(recentProjectActions));

        myListBox = ListBox.create(myModel);
        myListBox.setRender(createItemRender());
        myListBox.setSpeedSearchConverter(RecentProjectPanel::getSearchText);
        myListBox.setItemHeightGetter(action -> RecentProjectItemRender.ROW_HEIGHT);

        myListBox.setSelectOnHover(true);
        myListBox.setPlaceholder(ProjectLocalize.recentProjectsNoProjectOpenYet());

        myListBox.setAccessibleName(LocalizeValue.of(RECENT_PROJECTS_LABEL));

        myList = (JBList<AnAction>) TargetAWT.to(myListBox);

        Runnable checkerCallback = () -> {
            if (myList.isShowing()) {
                myList.revalidate();
                myList.repaint();
            }
        };

        RecentProjectsChecker checker = RecentProjectsChecker.getInstance();
        checker.addCallback(checkerCallback, pathsToCheck);
        Disposer.register(parentDisposable, () -> checker.removeCallback(checkerCallback));

        myList.registerKeyboardAction(
            e -> {
                AnAction selected = myListBox.getValue();
                if (selected != null) {
                    performSelectedAction(null, selected);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        removeRecentProjectAction = new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                AnAction selection = myListBox.getValue();
                if (selection == null) {
                    return;
                }

                int rc = Messages.showOkCancelDialog(
                    myRootPanel,
                    "Remove '" + selection.getTemplatePresentation().getText() + "' from recent projects list?",
                    "Remove Recent Project",
                    UIUtil.getQuestionIcon()
                );
                if (rc == Messages.OK) {
                    removeRecentProjectElement(selection);
                    myModel.remove(selection);
                }
            }
        };

        removeRecentProjectAction.registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), myList, parentDisposable);

        if (recentProjectActions.length != 0) {
            myListBox.setValueByIndex(0);
        }

        myScrollLayout = ScrollableLayout.create(myListBox);

        myRootLayout = DockLayout.create();
        myRootLayout.center(myScrollLayout);

        myRootPanel = (JPanel) TargetAWT.to(myRootLayout);
        myScrollPane = (JScrollPane) TargetAWT.to(myScrollLayout);

        ClientProperty.put(myList, UiDataProvider.KEY, sink -> {
            sink.set(RecentProjectsWelcomeScreenActionBase.RECENT_PROJECTS_LIST, myListBox);
        });
    }

    public JPanel getRootPanel() {
        return myRootPanel;
    }

    public JBList<AnAction> getList() {
        return myList;
    }

    public ListBox<AnAction> getListBox() {
        return myListBox;
    }

    private static String getSearchText(AnAction action) {
        if (action instanceof ReopenProjectAction item) {
            String home = Platform.current().user().homePath().toString();
            String path = item.getProjectPath();
            if (FileUtil.startsWith(path, home)) {
                path = path.substring(home.length());
            }
            return item.getProjectName() + " " + path;
        }

        if (action instanceof PopupProjectGroupActionGroup actionGroup) {
            return actionGroup.getGroup().getName();
        }

        return action.toString();
    }

    @RequiredUIAccess
    private void performSelectedAction(@Nullable InputDetails inputDetails, AnAction selection) {
        String actionPlace =
            UIUtil.uiParents(myList, true).filter(FlatWelcomeFrame.class).isEmpty() ? ActionPlaces.POPUP : ActionPlaces.WELCOME_SCREEN;

        AnActionEvent actionEvent = AnActionEvent.createFromAnAction(
            selection,
            null,
            actionPlace,
            DataManager.getInstance().getDataContext(myList),
            inputDetails
        );

        ActionImplUtil.performActionDumbAwareWithCallbacks(selection, actionEvent, actionEvent.getDataContext());
    }

    protected static void removeRecentProjectElement(Object element) {
        RecentProjectsManager manager = RecentProjectsManager.getInstance();
        if (element instanceof ReopenProjectAction reopenProjectAction) {
            manager.removePath(reopenProjectAction.getProjectPath());
        }
        else if (element instanceof PopupProjectGroupActionGroup actionGroup) {
            ProjectGroup group = actionGroup.getGroup();
            for (String path : group.getProjects()) {
                manager.removePath(path);
            }
            manager.removeGroup(group);
        }
    }

    protected boolean isUseGroups() {
        return false;
    }

    protected ComponentItemRender<AnAction> createItemRender() {
        return new RecentProjectItemRender(
            RecentProjectsManager.getInstance(),
            RecentProjectsChecker.getInstance(),
            this::onActivate,
            this::onMenuClick
        );
    }

    @RequiredUIAccess
    protected void onActivate(AnAction action, ComponentEvent<consulo.ui.Component> event) {
        performSelectedAction(event.getInputDetails(), action);

        if (action instanceof ReopenProjectAction reopenProjectAction && reopenProjectAction.isRemoved()) {
            myModel.remove(action);
        }
    }

    @RequiredUIAccess
    protected void onMenuClick(AnAction action, ComponentEvent<consulo.ui.Component> event) {
        if (!(ActionManager.getInstance().getAction(WELCOME_SCREEN_RECENT_PROJECT_ACTION_GROUP) instanceof ActionGroup group)) {
            return;
        }

        myListBox.setValue(action, false);

        closeMenu();

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.WELCOME_SCREEN, group);
        menu.setTargetComponent(myListBox);

        myMenu = menu;
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @RequiredUIAccess
    private void closeMenu() {
        ActionPopupMenu menu = myMenu;
        myMenu = null;

        if (menu != null) {
            menu.hide();
        }
    }
}
