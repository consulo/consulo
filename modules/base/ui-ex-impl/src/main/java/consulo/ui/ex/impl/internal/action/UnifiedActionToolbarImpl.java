/*
 * Copyright 2013-2020 consulo.io
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

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.PresentationFactory;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class UnifiedActionToolbarImpl implements ActionToolbar {
    private static final int PADDING = 4;

    private final ActionGroup myGroup;

    private final PresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private final UnifiedActionRow myRow;

    private int myLayoutPolicy = NOWRAP_LAYOUT_POLICY;

    private @Nullable Supplier<DataContext> myDataContextSupplier;

    @RequiredUIAccess
    public UnifiedActionToolbarImpl(String place, ActionGroup group, Style style) {
        myGroup = group;

        myRow = new UnifiedActionRow(
            () -> myGroup,
            this::getToolbarDataContext,
            place,
            place,
            myPresentationFactory,
            style
        );

        // a toolbar is a band of its own and must not sit flush against what stands around it, while an inplace
        // row is a part of the widget it belongs to - awt drops the border of its toolbar in the very same case
        if (style != Style.INPLACE) {
            myRow.getComponent().addBorders(BorderStyle.EMPTY, null, PADDING);
        }
    }

    /**
     * Points the toolbar at the context it has to update against. Needed by the frontends where the data context
     * cannot be derived from a component - the browser has no focus owner to walk up from, so the toolbar is given
     * the same supplier its owner reads its own state from.
     * <p/>
     * The group is expanded off the ui thread, the supplier has to hand over a context whose providers are already
     * snapshotted - see {@link consulo.dataContext.DataManager#createAsyncDataContext(DataContext)}.
     */
    public void setDataContextSupplier(Supplier<DataContext> dataContextSupplier) {
        myDataContextSupplier = dataContextSupplier;
    }

    @Override
    public void setTargetComponent(javax.swing.JComponent component) {
    }

    @Override
    public void setTargetUIComponent(Component component) {
        myDataContextSupplier = () -> {
            DataManager dataManager = DataManager.getInstance();

            return dataManager.createAsyncDataContext(dataManager.getDataContext(component));
        };
    }

    @Override
    public javax.swing.JComponent getComponent() {
        // FIXME [VISTALL] just stub - not throw on old ui
        return new JPanel();
    }

    @Override
    public Component getUIComponent() {
        return myRow.getComponent();
    }

    @Override
    public int getLayoutPolicy() {
        return myLayoutPolicy;
    }

    @Override
    public void setLayoutPolicy(int layoutPolicy) {
        myLayoutPolicy = layoutPolicy;
    }

    @RequiredUIAccess
    @Override
    public void updateActionsImmediately() {
        myRow.updateAsync();
    }

    @RequiredUIAccess
    @Override
    public CompletableFuture<List<? extends AnAction>> updateActionsAsync() {
        return myRow.updateAsync();
    }

    @Override
    public DataContext getToolbarDataContext() {
        Supplier<DataContext> supplier = myDataContextSupplier;
        if (supplier != null) {
            return supplier.get();
        }

        DataManager dataManager = DataManager.getInstance();

        // the group is expanded off the ui thread, the providers have to be snapshotted before that
        return dataManager.createAsyncDataContext(dataManager.getDataContext());
    }

    @Override
    public List<AnAction> getActions() {
        return myRow.getActions();
    }
}
