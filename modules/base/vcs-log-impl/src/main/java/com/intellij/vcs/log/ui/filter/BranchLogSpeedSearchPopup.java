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
package com.intellij.vcs.log.ui.filter;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.vcs.ui.FlatSpeedSearchPopup;
import javax.annotation.Nonnull;

public class BranchLogSpeedSearchPopup extends FlatSpeedSearchPopup {
  public BranchLogSpeedSearchPopup(@Nonnull ActionGroup actionGroup, @Nonnull DataContext dataContext) {
    super(null, new DefaultActionGroup(actionGroup, createSpeedSearchActionGroup(actionGroup)), dataContext, null, false);
  }

  @Override
  protected boolean shouldBeShowing(@Nonnull AnAction action) {
    if (!super.shouldBeShowing(action)) return false;
    return !getSpeedSearch().isHoldingFilter() || !(action instanceof ActionGroup);
  }

  @Nonnull
  public static ActionGroup createSpeedSearchActionGroup(@Nonnull ActionGroup actionGroup) {
    DefaultActionGroup speedSearchActions = new DefaultActionGroup();
    createSpeedSearchActions(actionGroup, speedSearchActions, true);
    return speedSearchActions;
  }

  private static void createSpeedSearchActions(@Nonnull ActionGroup actionGroup,
                                               @Nonnull DefaultActionGroup speedSearchActions,
                                               boolean isFirstLevel) {
    if (!isFirstLevel) speedSearchActions.addSeparator(actionGroup.getTemplatePresentation().getText());

    for (AnAction child : actionGroup.getChildren(null)) {
      if (!isFirstLevel && !(child instanceof ActionGroup || child instanceof AnSeparator || child instanceof SpeedsearchAction)) {
        speedSearchActions.add(createSpeedSearchWrapper(child));
      }
      else if (child instanceof ActionGroup) {
        createSpeedSearchActions((ActionGroup)child, speedSearchActions, isFirstLevel && !((ActionGroup)child).isPopup());
      }
    }
  }
}
