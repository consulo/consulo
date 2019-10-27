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
package com.intellij.openapi.vcs.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.WizardPopup;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FlatSpeedSearchPopup extends PopupFactoryImpl.ActionGroupPopup {

  public FlatSpeedSearchPopup(String title,
                              @Nonnull ActionGroup actionGroup,
                              @Nonnull DataContext dataContext,
                              @Nullable Condition<AnAction> preselectActionCondition, boolean showDisableActions) {
    super(title, actionGroup, dataContext, false, false, showDisableActions, false, null, -1, preselectActionCondition, null);
  }

  protected FlatSpeedSearchPopup(@Nullable WizardPopup parent, @Nonnull ListPopupStep step, @Nonnull DataContext dataContext, @Nullable Object value) {
    super(parent, step, null, dataContext, null, -1);
    setParentValue(value);
  }

  @Override
  public final boolean shouldBeShowing(Object value) {
    if (!super.shouldBeShowing(value)) return false;
    if (!(value instanceof PopupFactoryImpl.ActionItem)) return true;
    return shouldBeShowing(((PopupFactoryImpl.ActionItem)value).getAction());
  }

  protected boolean shouldBeShowing(@Nonnull AnAction action) {
    return getSpeedSearch().isHoldingFilter() || !isSpeedsearchAction(action);
  }

  @Nonnull
  public static AnAction createSpeedSearchWrapper(@Nonnull AnAction child) {
    return new MySpeedSearchAction(child);
  }

  @Nonnull
  public static ActionGroup createSpeedSearchActionGroupWrapper(@Nonnull ActionGroup child) {
    return new MySpeedSearchActionGroup(child);
  }

  protected static boolean isSpeedsearchAction(@Nonnull AnAction action) {
    return action instanceof SpeedsearchAction;
  }

  protected static <T> T getSpecificAction(Object value, @Nonnull Class<T> clazz) {
    if (value instanceof PopupFactoryImpl.ActionItem) {
      AnAction action = ((PopupFactoryImpl.ActionItem)value).getAction();
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

  public interface SpeedsearchAction {
  }

  private static class MySpeedSearchAction extends EmptyAction.MyDelegatingAction implements SpeedsearchAction, DumbAware {

    MySpeedSearchAction(@Nonnull AnAction action) {
      super(action);
    }
  }

  private static class MySpeedSearchActionGroup extends EmptyAction.MyDelegatingActionGroup implements SpeedsearchAction, DumbAware {
    MySpeedSearchActionGroup(@Nonnull ActionGroup actionGroup) {
      super(actionGroup);
    }
  }
}
