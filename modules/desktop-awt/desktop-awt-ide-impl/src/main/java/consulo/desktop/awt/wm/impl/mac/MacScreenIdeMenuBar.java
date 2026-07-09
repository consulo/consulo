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
package consulo.desktop.awt.wm.impl.mac;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.mac.screenmenu.Menu;
import consulo.desktop.awt.ui.mac.screenmenu.MenuBar;
import consulo.desktop.awt.wm.impl.IdeMenuBar;
import consulo.desktop.awt.wm.impl.IdeRootPane;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionRunnerAsync;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Drives the native macOS main menu (screen menu bar) from the {@code GROUP_MAIN_MENU} action group.
 * Owns a {@link MenuBar} peer and re-fills it on a timer and on UI-settings changes, mirroring the Swing
 * {@code IdeMenuBar} update pipeline.
 *
 * @author VISTALL
 */
public final class MacScreenIdeMenuBar implements IdeMenuBar {
    private static boolean ourHelpMenuSearchDisabled = false;

    private final JFrame myFrame;
    private final ActionManager myActionManager;
    private final DataManager myDataManager;
    private final MenuItemPresentationFactory myPresentationFactory = new MenuItemPresentationFactory();
    private final MenuBar myPeer;
    private final Disposable myDisposable = Disposable.newDisposable();

    private List<AnAction> myVisibleActions = new ArrayList<>();

    public MacScreenIdeMenuBar(JFrame frame, ActionManager actionManager, DataManager dataManager) {
        myFrame = frame;
        myActionManager = actionManager;
        myDataManager = dataManager;
        myPeer = new MenuBar("MainMenu", frame);

        myActionManager.addTimerListener(1000, new WeakTimerListener(new MyTimerListener()));

        UISettingsListener uiSettingsListener = source -> {
            myPresentationFactory.reset();
            updateMenuActions();
        };
        UISettings.getInstance().addUISettingsListener(uiSettingsListener, myDisposable);
        Disposer.register(Application.get(), myDisposable);

        disableHelpMenuSearch();
    }

    /**
     * macOS injects a Spotlight search field into the menu it recognizes as the Help menu (by title, when
     * {@code NSApp.helpMenu} is unset). Pointing {@code helpMenu} at an empty, off-screen menu disables that
     * auto-detection, so no search field is added to our Help menu.
     */
    private static void disableHelpMenuSearch() {
        if (ourHelpMenuSearchDisabled) {
            return;
        }
        ourHelpMenuSearchDisabled = true;

        Foundation.executeOnMainThread(false, false, () -> {
            ID app = Foundation.invoke("NSApplication", "sharedApplication");
            // retained on purpose: NSApp keeps it as the (hidden) help menu for the search field
            ID emptyMenu = Foundation.invoke(Foundation.invoke("NSMenu", "alloc"), "init");
            Foundation.invoke(app, "setHelpMenu:", emptyMenu);
        });
    }

    public MenuBar getPeer() {
        return myPeer;
    }

    @Override
    public void install(IdeRootPane rootPane) {
        // the native screen menu has no Swing component to attach; just kick off the initial fill
        Application.get().invokeLater(this::updateMenuActions);
    }

    @Override
    public void dispose() {
        Disposer.dispose(myDisposable);
        myPeer.dispose();
    }

    @Override
    @RequiredUIAccess
    public void updateMenuActions() {
        UIAccess uiAccess = UIAccess.current();
        DataContext dataContext = myDataManager.getDataContext(myFrame);
        expandMainActionGroupAsync(dataContext).whenCompleteAsync((newVisibleActions, throwable) -> {
            if (newVisibleActions != null) {
                applyVisibleActions(newVisibleActions);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void applyVisibleActions(List<AnAction> newVisibleActions) {
        if (newVisibleActions.equals(myVisibleActions)) {
            return;
        }
        myVisibleActions = newVisibleActions;

        // macOS menus do not use mnemonics, and the native menu draws the plain text label
        myPeer.beginFill();
        for (AnAction action : newVisibleActions) {
            myPeer.add(MacNativeActionMenu.create(null, ActionPlaces.MAIN_MENU, (ActionGroup)action, myPresentationFactory, myFrame));
        }
        myPeer.endFill();

        Menu.renameAppMenuItems();
    }

    private CompletableFuture<List<AnAction>> expandMainActionGroupAsync(DataContext context) {
        ActionGroup mainActionGroup = (ActionGroup)CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);
        if (mainActionGroup == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        AnAction[] children = mainActionGroup.getChildren(null, myActionManager);
        List<AnAction> groups = new ArrayList<>();
        List<AnActionEvent> events = new ArrayList<>();
        List<CompletableFuture<?>> updates = new ArrayList<>();
        for (AnAction action : children) {
            if (!(action instanceof ActionGroup)) {
                continue;
            }
            Presentation presentation = myPresentationFactory.getPresentation(action);
            AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, myActionManager, 0);
            e.setInjectedContext(action.isInInjectedContext());
            groups.add(action);
            events.add(e);
            updates.add(ActionRunnerAsync.performDumbAwareUpdateAsync(action, e));
        }

        return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0])).thenApply(__ -> {
            List<AnAction> visible = new ArrayList<>();
            for (int i = 0; i < groups.size(); i++) {
                if (events.get(i).getPresentation().isVisible()) {
                    visible.add(groups.get(i));
                }
            }
            return visible;
        });
    }

    private final class MyTimerListener implements TimerListener {
        @Override
        public IdeaModalityState getModalityState() {
            return IdeaModalityState.stateForComponent(myFrame);
        }

        @Override
        @RequiredUIAccess
        public void run() {
            if (!myFrame.isShowing() || !myFrame.isActive()) {
                return;
            }

            Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (focusedWindow instanceof Dialog dialog && dialog.isModal()) {
                return;
            }

            updateMenuActions();
        }
    }
}
