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
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.PresentationFactory;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class UnifiedActionToolbarImpl implements ActionToolbar {
    private static final Space PADDING = Space.SMALL;

    private final ActionGroup myGroup;

    private final PresentationFactory myPresentationFactory = new MenuItemPresentationFactory();

    private final UnifiedActionRow myRow;

    private int myLayoutPolicy = NOWRAP_LAYOUT_POLICY;

    private final ActionToolbarContextHolder myContextHolder;

    @RequiredUIAccess
    public UnifiedActionToolbarImpl(String place, ActionGroup group, Style style) {
        myGroup = group;
        myContextHolder = new ActionToolbarContextHolder(place);

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
            myRow.getComponent().paddingBuilder().allSet(PADDING).apply();
        }
    }

    @Override
    public void setDataContextProvider(Supplier<DataContext> dataContextProvider) {
        myContextHolder.setProvider(dataContextProvider);
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
        return myContextHolder.getDataContext();
    }

    @Override
    public List<AnAction> getActions() {
        return myRow.getActions();
    }

    public void addActionsUpdatedListener(Disposable parentDisposable, Runnable listener) {
        myRow.addActionsUpdatedListener(parentDisposable, listener);
    }
}
