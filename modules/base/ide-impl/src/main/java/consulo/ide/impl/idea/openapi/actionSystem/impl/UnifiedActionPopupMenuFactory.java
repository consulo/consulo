/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.actionSystem.impl.UnifiedActionPopupMenuImpl;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class UnifiedActionPopupMenuFactory implements ActionPopupMenuFactory {
    private final ActionManagerImpl myActionManager;

    @Inject
    public UnifiedActionPopupMenuFactory(ActionManager actionManager) {
        myActionManager = (ActionManagerImpl) actionManager;
    }

    @Override
    public ActionPopupMenu createActionPopupMenu(String place, @Nonnull ActionGroup group) {
        return new UnifiedActionPopupMenuImpl(place, group, myActionManager, null);
    }

    @Override
    public ActionPopupMenu createActionPopupMenu(@Nonnull String place,
                                                 @Nonnull ActionGroup group,
                                                 @Nullable PresentationFactory presentationFactory) {
        return new UnifiedActionPopupMenuImpl(place, group, myActionManager, presentationFactory);
    }

    @Override
    public ActionPopupMenu createActionPopupMenuForceHide(String place, @Nonnull ActionGroup group) {
        return new UnifiedActionPopupMenuImpl(place, group, myActionManager, new MenuItemPresentationFactory(true));
    }
}
