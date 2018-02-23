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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.util.Condition;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.WizardPopup;
import javax.annotation.Nonnull;

public class FlatSpeedSearchPopup extends PopupFactoryImpl.ActionGroupPopup {

  public FlatSpeedSearchPopup(String title,
                              @Nonnull DefaultActionGroup actionGroup,
                              @Nonnull DataContext dataContext,
                              @javax.annotation.Nullable Condition<AnAction> preselectActionCondition, boolean showDisableActions) {
    super(title, actionGroup, dataContext, false, false, showDisableActions, false,
          null, -1, preselectActionCondition, null);
  }

  protected FlatSpeedSearchPopup(@javax.annotation.Nullable WizardPopup parent,
                                 @Nonnull ListPopupStep step,
                                 @Nonnull DataContext dataContext,
                                 @javax.annotation.Nullable Object value) {
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

  public interface SpeedsearchAction {
  }

  private static class MySpeedSearchAction extends EmptyAction.MyDelegatingAction implements SpeedsearchAction {

    public MySpeedSearchAction(@Nonnull AnAction action) {
      super(action);
    }
  }

  private static class MySpeedSearchActionGroup extends EmptyAction.MyDelegatingActionGroup implements SpeedsearchAction {
    public MySpeedSearchActionGroup(@Nonnull ActionGroup actionGroup) {
      super(actionGroup);
    }
  }
}
