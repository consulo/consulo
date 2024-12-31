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
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdater;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.concurrent.CancellablePromise;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

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

    private final ToolbarUpdater myUpdater;

    private final PresentationFactory myPresentationFactory = new BasePresentationFactory();

    private boolean myAlreadyUpdated;

    private List<? extends AnAction> myVisibleActions = List.of();

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

        myUpdater = new ToolbarUpdater(keymapManager, actionManager, component) {
            @Override
            protected void updateActionsImpl(boolean transparentOnly, boolean forced) {
                if (!application.isDisposedOrDisposeInProgress()) {
                    ActionToolbarEngine.this.updateActionsImpl(transparentOnly, forced);
                }
            }
        };

        myUpdater.updateActions(false, false);
    }

    protected abstract DataContext getDataContext();

    protected abstract void fillToolBar(List<? extends AnAction> visibleActions, boolean shouldRebuildUI);

    protected abstract boolean isShowing();

    protected abstract void removeAll();

    @Nonnull
    public String getPlace() {
        return myPlace;
    }

    private void updateActionsImpl(boolean transparentOnly, boolean forced) {
        DataContext dataContext = getDataContext();
        boolean async =
            myAlreadyUpdated && Registry.is("actionSystem.update.actions.asynchronously") && ActionToolbarsHolder.contains(myActionToolbar) && isShowing();
        ActionUpdater updater =
            new ActionUpdater(myActionManager,
                LaterInvocator.isInModalContext(),
                myPresentationFactory,
                async ? DataManager.getInstance().createAsyncDataContext(dataContext) : dataContext,
                myPlace,
                false,
                true);
        if (async) {
            if (myLastUpdate != null) {
                myLastUpdate.cancel();
            }

            myLastUpdate = updater.expandActionGroupAsync(myActionGroup, false);
            myLastUpdate.onSuccess(actions -> actionsUpdated(forced, actions)).onProcessed(__ -> myLastUpdate = null);
        }
        else {
            actionsUpdated(forced, updater.expandActionGroupWithTimeout(myActionGroup, false));
            myAlreadyUpdated = true;
        }
    }

    private CancellablePromise<java.util.List<AnAction>> myLastUpdate;

    private void actionsUpdated(boolean forced, @Nonnull List<? extends AnAction> newVisibleActions) {
        if (forced || !newVisibleActions.equals(myVisibleActions)) {
            boolean shouldRebuildUI = newVisibleActions.isEmpty() || myVisibleActions.isEmpty();
            myVisibleActions = newVisibleActions;

            fillToolBar(myVisibleActions, shouldRebuildUI);
        }
    }

    @RequiredUIAccess
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
        removeAll();
    }

    @RequiredUIAccess
    public void updateActionsImmediately() {
        UIAccess.assertIsUIThread();
        myUpdater.updateActions(true, false);
    }

    private void cancelCurrentUpdate() {
        CancellablePromise<List<AnAction>> lastUpdate = myLastUpdate;
        myLastUpdate = null;
        if (lastUpdate != null) {
            lastUpdate.cancel();
        }
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
