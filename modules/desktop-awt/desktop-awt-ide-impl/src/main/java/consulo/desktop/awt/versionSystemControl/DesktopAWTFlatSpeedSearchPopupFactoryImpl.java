/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.versionSystemControl;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.ListPopup;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import consulo.versionControlSystem.internal.FlatSpeedSearchShouldBeShowingFilter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceImpl
@Singleton
public class DesktopAWTFlatSpeedSearchPopupFactoryImpl implements FlatSpeedSearchPopupFactory {
    @Override
    public ListPopup createFlatSpeedSearchPopup(String title,
                                                @Nonnull ActionGroup actionGroup,
                                                @Nonnull DataContext dataContext,
                                                @Nullable Predicate<AnAction> preselectActionCondition,
                                                boolean showDisableActions,
                                                @Nonnull FlatSpeedSearchShouldBeShowingFilter filter) {
        return new FlatSpeedSearchPopup(title, actionGroup, dataContext, preselectActionCondition, showDisableActions) {
            @Override
            protected boolean shouldBeShowing(@Nonnull AnAction action) {
                return super.shouldBeShowing(action) || filter.shouldBeShowing(action, getSpeedSearch().isHoldingFilter());
            }
        };
    }

    @Override
    public ListPopup createBranchPopup(@Nonnull String title, @Nonnull Project project, @Nonnull Predicate<AnAction> preselectActionCondition, @Nonnull ActionGroup actions, @Nullable String dimensionKey) {
        return new BranchActionGroupPopup(title, project, preselectActionCondition, actions, dimensionKey);
    }
}
