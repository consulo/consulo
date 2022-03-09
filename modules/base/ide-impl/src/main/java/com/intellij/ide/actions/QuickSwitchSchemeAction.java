/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.actions;

import consulo.application.AllIcons;
import com.intellij.openapi.actionSystem.*;
import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author max
 */
public abstract class QuickSwitchSchemeAction extends AnAction implements DumbAware {
  protected static final Image ourCurrentAction = AllIcons.Actions.Forward;
  protected static final Image ourNotCurrentAction = Image.empty(Image.DEFAULT_ICON_SIZE);
  @Nonnull
  protected String myActionPlace = ActionPlaces.UNKNOWN;

  private final boolean myShowPopupWithNoActions;

  protected QuickSwitchSchemeAction() {
    this(false);
  }

  protected QuickSwitchSchemeAction(boolean showPopupWithNoActions) {
    myShowPopupWithNoActions = showPopupWithNoActions;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    DefaultActionGroup group = new DefaultActionGroup();
    fillActions(project, group, e.getDataContext());
    showPopup(e, group);
  }

  protected abstract void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext);

  private void showPopup(AnActionEvent e, DefaultActionGroup group) {
    if (!myShowPopupWithNoActions && group.getChildrenCount() == 0) return;
    final ListPopup popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(getPopupTitle(e),
                              group,
                              e.getDataContext(), getAidMethod(),
                              true, myActionPlace);

    showPopup(e, popup);
  }

  protected void showPopup(AnActionEvent e, ListPopup popup) {
    popup.showCenteredInCurrentWindow(e.getData(CommonDataKeys.PROJECT));
  }

  protected JBPopupFactory.ActionSelectionAid getAidMethod() {
    return JBPopupFactory.ActionSelectionAid.NUMBERING;
  }

  protected String getPopupTitle(AnActionEvent e) {
    return e.getPresentation().getText();
  }

  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null && isEnabled());
  }

  protected abstract boolean isEnabled();
}
