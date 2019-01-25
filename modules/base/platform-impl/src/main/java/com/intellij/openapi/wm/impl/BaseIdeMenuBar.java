/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.util.Disposer;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseIdeMenuBar {
  public static final int COLLAPSED_HEIGHT = 2;

  public enum State {
    EXPANDED,
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    TEMPORARY_EXPANDED;

    boolean isInProgress() {
      return this == COLLAPSING || this == EXPANDING;
    }
  }

  protected List<AnAction> myVisibleActions;
  protected List<AnAction> myNewVisibleActions;
  protected final MenuItemPresentationFactory myPresentationFactory;
  protected final DataManager myDataManager;
  protected final ActionManager myActionManager;
  protected final Disposable myDisposable = Disposer.newDisposable();
  protected boolean myDisabled = false;

  @Nonnull
  protected State myState = State.EXPANDED;
  protected double myProgress = 0;
  protected boolean myActivated = false;

  public BaseIdeMenuBar(ActionManager actionManager, DataManager dataManager) {
    myActionManager = actionManager;
    myVisibleActions = new ArrayList<>();
    myNewVisibleActions = new ArrayList<>();
    myPresentationFactory = new MenuItemPresentationFactory();
    myDataManager = dataManager;
  }

  protected abstract void updateMenuActions();

  @RequiredUIAccess
  protected void expandActionGroup(final DataContext context, final List<AnAction> newVisibleActions, ActionManager actionManager) {
    final ActionGroup mainActionGroup = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);
    if (mainActionGroup == null) return;
    final AnAction[] children = mainActionGroup.getChildren(null);
    for (final AnAction action : children) {
      if (!(action instanceof ActionGroup)) {
        continue;
      }
      final Presentation presentation = myPresentationFactory.getPresentation(action);
      final AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
      e.setInjectedContext(action.isInInjectedContext());
      action.update(e);
      if (presentation.isVisible()) { // add only visible items
        newVisibleActions.add(action);
      }
    }
  }

  public void disableUpdates() {
    myDisabled = true;
    updateMenuActions();
  }

  public void enableUpdates() {
    myDisabled = false;
    updateMenuActions();
  }
}
