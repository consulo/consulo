/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.dataContext.DataContext;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.popup.ListPopup;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import jakarta.annotation.Nonnull;

public class BranchLogSpeedSearchPopup {
    public static ListPopup createSpeedSearchPopup(@Nonnull ActionGroup actionGroup, @Nonnull DataContext context) {
        return FlatSpeedSearchPopupFactory.getInstance()
            .createFlatSpeedSearchPopup(
                null,
                ActionGroup.of(actionGroup, createSpeedSearchActionGroup(actionGroup)),
                context,
                null,
                false,
                (action, holdingFilter) -> !holdingFilter || !(action instanceof ActionGroup));
    }

    @Nonnull
    public static ActionGroup createSpeedSearchActionGroup(@Nonnull ActionGroup actionGroup) {
        ActionGroup.Builder speedSearchActions = ActionGroup.newImmutableBuilder();
        createSpeedSearchActions(actionGroup, speedSearchActions, true);
        return speedSearchActions.build();
    }

    private static void createSpeedSearchActions(@Nonnull ActionGroup actionGroup,
                                                 @Nonnull ActionGroup.Builder speedSearchActions,
                                                 boolean isFirstLevel) {
        if (!isFirstLevel) {
            speedSearchActions.addSeparator(actionGroup.getTemplatePresentation().getTextValue());
        }

        for (AnAction child : actionGroup.getChildren(null)) {
            if (!isFirstLevel && !(child instanceof ActionGroup || child instanceof AnSeparator || child instanceof FlatSpeedSearchPopupFactory.SpeedsearchAction)) {
                speedSearchActions.add(FlatSpeedSearchPopupFactory.createSpeedSearchWrapper(child));
            }
            else if (child instanceof ActionGroup) {
                createSpeedSearchActions((ActionGroup) child, speedSearchActions, isFirstLevel && !((ActionGroup) child).isPopup());
            }
        }
    }
}
