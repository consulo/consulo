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
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2024-12-31
 */
public abstract class ActionToolbarEngine {
    @Nonnull
    private final String myPlace;

    @Nonnull
    private final ActionGroup myActionGroup;

    @Nonnull
    private final ActionToolbar myActionToolbar;
    @Nonnull
    private final ActionManager myActionManager;

    private final ToolbarNotifier myUpdater;

    private final PresentationFactory myPresentationFactory = new BasePresentationFactory();

    private boolean myAlreadyUpdated;

    private List<? extends AnAction> myVisibleActions = List.of();

    @Nonnull
    private CompletableFuture<List<? extends AnAction>> myLastUpdate = CompletableFuture.completedFuture(List.of());

    public ActionToolbarEngine(@Nonnull String place,
                               @Nonnull ActionGroup actionGroup,
                               @Nonnull ActionToolbar actionToolbar,
                               @Nonnull Application application,
                               @Nonnull KeymapManager keymapManager,
                               @Nonnull ActionManager actionManager,
                               @Nonnull JComponent component) {
        myPlace = place;
        myActionGroup = actionGroup;
        myActionToolbar = actionToolbar;
        myActionManager = actionManager;

        myUpdater = new ToolbarNotifier(application, keymapManager, actionManager, component, new ActionUpdateProxy() {
            @Override
            public void update() {
                updateActionsImpl();
            }

            @Override
            public CompletableFuture<List<? extends AnAction>> updateAsync(@Nonnull UIAccess uiAccess) {
                return ActionToolbarEngine.this.updateAsync(uiAccess);
            }
        });
    }

    protected abstract DataContext getDataContext();

    protected abstract void fillToolBar(List<? extends AnAction> visibleActions, boolean shouldRebuildUI);

    protected abstract boolean isShowing();

    protected abstract void removeAll();

    @Nonnull
    public String getPlace() {
        return myPlace;
    }

    @Nonnull
    private CompletableFuture<List<? extends AnAction>> updateAsync(@Nonnull UIAccess uiAccess) {
        DataContext dataContext = getDataContext();
        boolean async = myAlreadyUpdated && ActionToolbarsHolder.contains(myActionToolbar) && isShowing();
        ActionUpdater updater = new ActionUpdater(myActionManager,
            myPresentationFactory,
            async ? DataManager.getInstance().createAsyncDataContext(dataContext) : dataContext,
            myPlace,
            false,
            true
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

    @Deprecated
    private void updateActionsImpl() {
        DataContext dataContext = getDataContext();
        boolean async = myAlreadyUpdated && Registry.is("actionSystem.update.actions.asynchronously") && ActionToolbarsHolder.contains(myActionToolbar) && isShowing();
        ActionUpdater updater =
            new ActionUpdater(myActionManager,
                myPresentationFactory,
                async ? DataManager.getInstance().createAsyncDataContext(dataContext) : dataContext,
                myPlace,
                false,
                true);
        if (async) {
            myLastUpdate.cancel(false);

            myLastUpdate = updater.expandActionGroupAsync(myActionGroup, false);
            myLastUpdate.whenComplete((result, throwable) -> {
                if (result != null) {
                    actionsUpdated(result);
                }
            });
        }
        else {
            actionsUpdated(updater.expandActionGroupWithTimeout(myActionGroup, false));
            myAlreadyUpdated = true;
        }
    }

    private void actionsUpdated(@Nonnull List<? extends AnAction> newVisibleActions) {
        if (!newVisibleActions.equals(myVisibleActions)) {
            boolean shouldRebuildUI = newVisibleActions.isEmpty() || myVisibleActions.isEmpty();
            myVisibleActions = newVisibleActions;

            fillToolBar(myVisibleActions, shouldRebuildUI);
        }
    }

    public Presentation getPresentation(@Nonnull AnAction action) {
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

    @Nonnull
    public CompletableFuture<List<? extends AnAction>> updateActionsAsync(@Nonnull UIAccess uiAccess) {
        return myUpdater.updateActionsAsync(uiAccess);
    }

    @RequiredUIAccess
    public void addNotify() {
        ActionToolbarsHolder.add(myActionToolbar);

        // should update action right on the showing, otherwise toolbar may not be displayed at all,
        // since by default all updates are postponed until frame gets focused.
        updateActionsAsync(UIAccess.current());
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

    @Nonnull
    public ActionGroup getActionGroup() {
        return myActionGroup;
    }

    @Nonnull
    public List<? extends AnAction> getVisibleActions() {
        return myVisibleActions;
    }

    @Nonnull
    public List<AnAction> getActions() {
        AnAction[] kids = myActionGroup.getChildren(null);
        return List.of(kids);
    }

    public boolean hasVisibleActions() {
        return !myVisibleActions.isEmpty();
    }
}
