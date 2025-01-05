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
package consulo.ide.impl.actionSystem.impl;

import consulo.dataContext.DataContext;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class UnifiedActionToolbarImpl implements ActionToolbar {
    private final String myPlace;

    private final ActionGroup myGroup;

    private int myLayoutPolicy;

    private Layout myComponent;

    private final Style myStyle;

    public UnifiedActionToolbarImpl(String place, ActionGroup group, Style style) {
        myPlace = place;
        myGroup = group;
        myStyle = style;

        rebuildUI();
    }

    private void rebuildUI() {
        myComponent = myStyle.isHorizontal() ? HorizontalLayout.create() : VerticalLayout.create();
    }

    public void setTargetComponent(final javax.swing.JComponent component) {
    }

    @Nonnull
    @Override
    public javax.swing.JComponent getComponent() {
        // FIXME [VISTALL] just stub - not throw on old ui
        return new JPanel();
    }

    @Nonnull
    @Override
    public Component getUIComponent() {
        return myComponent;
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

    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public CompletableFuture<?> updateActionsAsync() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean hasVisibleActions() {
        return false;
    }

    @Override
    public DataContext getToolbarDataContext() {
        return null;
    }

    @Nonnull
    @Override
    public List<AnAction> getActions() {
        return Collections.emptyList();
    }
}
