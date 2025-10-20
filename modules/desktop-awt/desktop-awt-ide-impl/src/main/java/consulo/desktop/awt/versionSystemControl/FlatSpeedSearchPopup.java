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
package consulo.desktop.awt.versionSystemControl;

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ui.popup.actionPopup.ActionGroupPopup;
import consulo.ide.impl.idea.ui.popup.actionPopup.ActionPopupItem;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.EmptyAction;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.versionControlSystem.internal.FlatSpeedSearchPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

public class FlatSpeedSearchPopup extends ActionGroupPopup {
    public FlatSpeedSearchPopup(
        String title,
        @Nonnull ActionGroup actionGroup,
        @Nonnull DataContext dataContext,
        @Nullable Predicate<AnAction> preselectActionCondition,
        boolean showDisableActions
    ) {
        super(title, actionGroup, dataContext, false, false, showDisableActions, false, null, -1, preselectActionCondition, null, true);
    }

    protected FlatSpeedSearchPopup(
        @Nullable WizardPopup parent,
        @Nonnull ListPopupStep step,
        @Nonnull DataContext dataContext,
        @Nullable Object value
    ) {
        super(parent, step, null, dataContext, null, -1, true);
        setParentValue(value);
    }

    @Override
    public final boolean shouldBeShowing(Object value) {
        //noinspection SimplifiableIfStatement
        if (!super.shouldBeShowing(value)) {
            return false;
        }
        return !(value instanceof ActionPopupItem actionItem && !shouldBeShowing(actionItem.getAction()));
    }

    protected boolean shouldBeShowing(@Nonnull AnAction action) {
        return getSpeedSearch().isHoldingFilter() || !FlatSpeedSearchPopupFactory.isSpeedsearchAction(action);
    }


    protected static <T> T getSpecificAction(Object value, @Nonnull Class<T> clazz) {
        if (value instanceof ActionPopupItem) {
            AnAction action = ((ActionPopupItem)value).getAction();
            if (clazz.isInstance(action)) {
                return clazz.cast(action);
            }
            else if (action instanceof EmptyAction.MyDelegatingActionGroup) {
                ActionGroup group = ((EmptyAction.MyDelegatingActionGroup)action).getDelegate();
                return clazz.isInstance(group) ? clazz.cast(group) : null;
            }
        }
        return null;
    }

}
