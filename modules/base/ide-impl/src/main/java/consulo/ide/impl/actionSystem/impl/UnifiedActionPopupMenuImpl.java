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
package consulo.ide.impl.actionSystem.impl;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ui.Component;
import consulo.ui.PopupMenu;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.BasePresentationFactory;
import consulo.ui.ex.action.PresentationFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17/08/2021
 * <p>
 * Simple ActionPopupMenu implementation
 */
public class UnifiedActionPopupMenuImpl implements ActionPopupMenu {
    private final String myPlace;
    @Nonnull
    private final ActionGroup myGroup;
    private final ActionManagerImpl myManager;
    @Nullable
    private final PresentationFactory myPresentationFactory;

    @Nullable
    private Supplier<DataContext> myDataContextProvider;

    public UnifiedActionPopupMenuImpl(String place, @Nonnull ActionGroup group, ActionManagerImpl actionManager, @Nullable PresentationFactory factory) {
        myPlace = place;
        myGroup = group;
        myManager = actionManager;
        myPresentationFactory = factory;
    }

    @Nonnull
    @Override
    public String getPlace() {
        return myPlace;
    }

    @Nonnull
    @Override
    public ActionGroup getActionGroup() {
        return myGroup;
    }

    @Override
    public void setTargetComponent(@Nonnull Component component) {
        myDataContextProvider = () -> DataManager.getInstance().getDataContext(component);
    }

    @Override
    public void show(Component component, int x, int y) {
        DataContext context = myDataContextProvider == null ? DataManager.getInstance().getDataContext(component) : myDataContextProvider.get();

        PresentationFactory presentationFactory = myPresentationFactory == null ? new BasePresentationFactory() : myPresentationFactory;

        PopupMenu popupMenu = PopupMenu.create(component);

        UnifiedActionUtil.expandActionGroup(myGroup, context, myManager, presentationFactory, popupMenu::add);

        myManager.addActionPopup(this);

        popupMenu.show(x, y);
    }
}
