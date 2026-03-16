/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.keymap.KeymapManager;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2024-12-31
 */
public abstract class ActionToolbarEngine {
    
    private final String myPlace;

    
    private final ActionGroup myActionGroup;

    
    private final ActionToolbar myActionToolbar;
    
    private final ActionManager myActionManager;

    private final ToolbarNotifier myUpdater;

    private final PresentationFactory myPresentationFactory = new BasePresentationFactory();

    private List<? extends AnAction> myVisibleActions = List.of();

    
    private CompletableFuture<List<? extends AnAction>> myLastUpdate = CompletableFuture.completedFuture(List.of());

    public ActionToolbarEngine(String place,
                               ActionGroup actionGroup,
                               ActionToolbar actionToolbar,
                               Application application,
                               KeymapManager keymapManager,
                               ActionManager actionManager,
                               JComponent component) {
        myPlace = place;
        myActionGroup = actionGroup;
        myActionToolbar = actionToolbar;
        myActionManager = actionManager;

        myUpdater = new ToolbarNotifier(application, keymapManager, actionManager, component, ActionToolbarEngine.this::updateAsync);
    }

    protected abstract DataContext getDataContext();

    protected abstract void fillToolBar(List<? extends AnAction> visibleActions, boolean shouldRebuildUI);

    protected abstract boolean isShowing();

    protected abstract void removeAll();

    
    public String getPlace() {
        return myPlace;
    }

    
    @RequiredUIAccess
    private CompletableFuture<List<? extends AnAction>> updateAsync() {
        DataContext dataContext = getDataContext();
        ActionUpdater updater = new ActionUpdater(
            myActionManager,
            myPresentationFactory,
            DataManager.getInstance().createAsyncDataContext(dataContext),
            myPlace,
            false,
            true,
            UIAccess.current()
        );

        myLastUpdate.cancel(false);

        myLastUpdate = updater.expandActionGroupAsync(myActionGroup, false);
        myLastUpdate.whenComplete((result, throwable) -> {
            if (result != null) {
                actionsUpdated(result);
            }
        });
        return myLastUpdate;
    }

    private void actionsUpdated(List<? extends AnAction> newVisibleActions) {
        if (!newVisibleActions.equals(myVisibleActions)) {
            boolean shouldRebuildUI = newVisibleActions.isEmpty() || myVisibleActions.isEmpty();
            myVisibleActions = newVisibleActions;

            fillToolBar(myVisibleActions, shouldRebuildUI);
        }
    }

    public Presentation getPresentation(AnAction action) {
        return myPresentationFactory.getPresentation(action);
    }

    /**
     * Clear internal caches.
     * <p>
     * This method can be called after updating {@link SimpleActionToolbarImpl#myActionGroup}
     * to make sure toolbar does not reference old {@link AnAction} instances.
     */
    @RequiredUIAccess
    public void reset() {
        cancelCurrentUpdate();

        myPresentationFactory.reset();
        myVisibleActions = List.of();
        myLastUpdate = CompletableFuture.completedFuture(List.of());
        removeAll();
    }

    @RequiredUIAccess
    @Deprecated
    public void updateActionsImmediately() {
        UIAccess.assertIsUIThread();
        myUpdater.updateActions(true);
    }

    
    @RequiredUIAccess
    public CompletableFuture<List<? extends AnAction>> updateActionsAsync() {
        return myUpdater.updateActionsAsync();
    }

    @RequiredUIAccess
    public void addNotify() {
        ActionToolbarsHolder.add(myActionToolbar);

        // should update action right on the showing, otherwise toolbar may not be displayed at all,
        // since by default all updates are postponed until frame gets focused.
        updateActionsAsync();
    }

    @RequiredUIAccess
    public void removeNotify() {
        ActionToolbarsHolder.remove(myActionToolbar);

        CompletableFuture<List<? extends AnAction>> lastUpdate = myLastUpdate;
        lastUpdate.cancel(false);

        myLastUpdate = CompletableFuture.completedFuture(List.of());
    }

    private void cancelCurrentUpdate() {
        CompletableFuture<List<? extends AnAction>> lastUpdate = myLastUpdate;
        lastUpdate.cancel(false);

        myLastUpdate = CompletableFuture.completedFuture(myVisibleActions);
    }

    
    public ActionGroup getActionGroup() {
        return myActionGroup;
    }

    
    public List<? extends AnAction> getVisibleActions() {
        return myVisibleActions;
    }

    
    public List<AnAction> getActions() {
        AnAction[] kids = myActionGroup.getChildren(null);
        return List.of(kids);
    }
}
