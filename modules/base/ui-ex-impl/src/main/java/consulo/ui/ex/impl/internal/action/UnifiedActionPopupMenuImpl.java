/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.ex.impl.internal.action;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.DelayedAction;
import consulo.ui.PopupMenu;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.PresentationFactory;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
public class UnifiedActionPopupMenuImpl implements ActionPopupMenu {
    private static final Logger LOG = Logger.getInstance(UnifiedActionPopupMenuImpl.class);

    private final String myPlace;

    private final ActionGroup myGroup;
    private final @Nullable PresentationFactory myPresentationFactory;

    private @Nullable Supplier<DataContext> myDataContextProvider;
    private @Nullable PopupMenu myPopupMenu;
    private @Nullable DelayedAction myDelayedAction;
    private @Nullable ProgressIndicator myUpdateIndicator;

    public UnifiedActionPopupMenuImpl(String place, ActionGroup group, @Nullable PresentationFactory factory) {
        myPlace = place;
        myGroup = group;
        myPresentationFactory = factory;
    }

    @Override
    public String getPlace() {
        return myPlace;
    }

    @Override
    public ActionGroup getActionGroup() {
        return myGroup;
    }

    @Override
    public void setTargetComponent(Component component) {
        myDataContextProvider = () -> DataManager.getInstance().getDataContext(component);
    }

    @Override
    @RequiredUIAccess
    public void show(Component component, int x, int y) {
        DataManager dataManager = DataManager.getInstance();

        Supplier<DataContext> provider = myDataContextProvider;
        DataContext context = dataManager.createAsyncDataContext(provider == null ? dataManager.getDataContext(component) : provider.get());

        PresentationFactory presentationFactory = myPresentationFactory == null ? new MenuItemPresentationFactory() : myPresentationFactory;

        hide();

        PopupMenu popupMenu = PopupMenu.create(component);
        myPopupMenu = popupMenu;
        myDelayedAction = DelayedAction.start(component, x, y);

        ProgressIndicator indicator = new EmptyProgressIndicator();
        myUpdateIndicator = indicator;

        UIAccess uiAccess = UIAccess.current();
        UnifiedActionMenuExpander.expandAsync(myGroup, context, myPlace, presentationFactory, uiAccess, indicator, false)
            .whenCompleteAsync((nodes, throwable) -> {
                if (myPopupMenu != popupMenu) {
                    return;
                }

                stopDelayedAction();
                myUpdateIndicator = null;

                if (throwable != null) {
                    if (!UnifiedActionMenuExpander.isProcessCanceled(throwable)) {
                        LOG.error("Failed to expand action group of " + myPlace, throwable);
                    }
                    myPopupMenu = null;
                    return;
                }

                if (nodes.isEmpty()) {
                    myPopupMenu = null;
                    return;
                }

                for (UnifiedActionMenuExpander.MenuNode node : nodes) {
                    popupMenu.add(UnifiedActionMenuExpander.createMenuItem(node, () -> context, myPlace, presentationFactory));
                }

                popupMenu.show(x, y);
            }, uiAccess);
    }

    @Override
    @RequiredUIAccess
    public void hide() {
        stopDelayedAction();

        ProgressIndicator indicator = myUpdateIndicator;
        myUpdateIndicator = null;
        if (indicator != null) {
            indicator.cancel();
        }

        PopupMenu popupMenu = myPopupMenu;
        myPopupMenu = null;

        if (popupMenu != null) {
            popupMenu.hide();
        }
    }

    @RequiredUIAccess
    private void stopDelayedAction() {
        DelayedAction delayedAction = myDelayedAction;
        myDelayedAction = null;

        if (delayedAction != null) {
            delayedAction.stop();
        }
    }
}
