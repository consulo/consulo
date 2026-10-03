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
package consulo.ui.ex.impl.internal.action;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionPopupMenuFactory;
import consulo.ui.ex.action.PresentationFactory;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@Singleton
public class UnifiedActionPopupMenuFactory implements ActionPopupMenuFactory {
    @Override
    public ActionPopupMenu createActionPopupMenu(String place, ActionGroup group) {
        return new UnifiedActionPopupMenuImpl(place, group, null);
    }

    @Override
    public ActionPopupMenu createActionPopupMenu(String place,
                                                 ActionGroup group,
                                                 @Nullable PresentationFactory presentationFactory) {
        return new UnifiedActionPopupMenuImpl(place, group, presentationFactory);
    }

    @Override
    public ActionPopupMenu createActionPopupMenuForceHide(String place, ActionGroup group) {
        return new UnifiedActionPopupMenuImpl(place, group, new MenuItemPresentationFactory(true));
    }
}
