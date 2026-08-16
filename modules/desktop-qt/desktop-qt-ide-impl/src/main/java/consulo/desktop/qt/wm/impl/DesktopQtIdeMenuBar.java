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
package consulo.desktop.qt.wm.impl;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.qt.ui.impl.DesktopQtMenuBar;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.impl.internal.action.MenuItemPresentationFactory;
import consulo.ui.ex.impl.internal.action.UnifiedActionMenuExpander;
import io.qt.widgets.QApplication;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Qt analog of {@code consulo.desktop.awt.wm.impl.DefaultIdeMenuBar}. Builds main menu from
 * {@link IdeActions#GROUP_MAIN_MENU} using async action expansion.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtIdeMenuBar {
    private static final Logger LOG = Logger.getInstance(DesktopQtIdeMenuBar.class);

    private final Component myContextComponent;
    private final DesktopQtMenuBar myMenuBar = new DesktopQtMenuBar();
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private String mySignature = "";
    private @Nullable ProgressIndicator myUpdateIndicator;

    @RequiredUIAccess
    public DesktopQtIdeMenuBar(Component contextComponent) {
        myContextComponent = contextComponent;

        // qt has no 'menu about to open' hook which could wait for an expansion, so presentations are refreshed
        // when a bar entry is highlighted - which happens on hover, one open ahead of any menu being shown
        myMenuBar.build().hovered.connect(action -> updateMenuActions());
    }

    public MenuBar getMenuBar() {
        return myMenuBar;
    }

    /**
     * Forgets that the current items were ever built. The signature exists to skip rebuilding a menu which is
     * already on screen unchanged.
     */
    @RequiredUIAccess
    public void reset() {
        mySignature = "";
        myMenuBar.clear();
    }

    @RequiredUIAccess
    public void updateMenuActions() {
        UIAccess uiAccess = UIAccess.current();

        ProgressIndicator previousIndicator = myUpdateIndicator;
        if (previousIndicator != null) {
            previousIndicator.cancel();
        }

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myUpdateIndicator = indicator;

        ActionGroup mainMenuGroup = findMainMenuGroup();
        if (mainMenuGroup == null) {
            myUpdateIndicator = null;
            return;
        }

        UnifiedActionMenuExpander
            .expandAsync(mainMenuGroup, createDataContext(), ActionPlaces.MAIN_MENU, myPresentationFactory, uiAccess, indicator, false)
            .whenCompleteAsync((nodes, throwable) -> {
                if (myUpdateIndicator != indicator) {
                    return;
                }

                myUpdateIndicator = null;

                if (throwable != null) {
                    if (!UnifiedActionMenuExpander.isProcessCanceled(throwable)) {
                        LOG.warn("Failed to expand main menu", throwable);
                    }
                    return;
                }

                applyNodes(nodes);
            }, uiAccess);
    }

    private static @Nullable ActionGroup findMainMenuGroup() {
        AnAction mainMenuAction = CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);

        return mainMenuAction instanceof ActionGroup mainMenuGroup ? mainMenuGroup : null;
    }

    private DataContext createDataContext() {
        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext(myContextComponent));
    }

    @RequiredUIAccess
    private void applyNodes(List<UnifiedActionMenuExpander.MenuNode> nodes) {
        // a rebuild takes down the very menu a user may hold open, and qt is drawing it out of the widget which
        // would be disposed - whatever is open keeps the items it was opened with, the next open is rebuilt
        if (QApplication.activePopupWidget() != null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        UnifiedActionMenuExpander.appendSignature(nodes, builder);
        String signature = builder.toString();

        if (signature.equals(mySignature)) {
            return;
        }

        mySignature = signature;

        myMenuBar.clear();

        for (UnifiedActionMenuExpander.MenuNode node : nodes) {
            if (node.children() == null) {
                continue;
            }

            MenuItem item =
                UnifiedActionMenuExpander.createMenuItem(node, this::createDataContext, ActionPlaces.MAIN_MENU, myPresentationFactory);

            myMenuBar.add(item);
        }
    }
}
