/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.actions;

import consulo.project.ui.action.QuickSwitchSchemeAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RefactoringQuickListPopupAction extends QuickSwitchSchemeAction {
  @Override
  @RequiredUIAccess
  protected void fillActions(
    @Nullable final Project project,
    @Nonnull final DefaultActionGroup group,
    @Nonnull final DataContext dataContext
  ) {
    if (project == null) {
      return;
    }

    final ActionManager actionManager = ActionManager.getInstance();
    final AnAction action = actionManager.getAction(IdeActions.GROUP_REFACTOR);
    collectEnabledChildren(action, group, dataContext, actionManager, false);
  }

  @RequiredUIAccess
  private static void collectEnabledChildren(
    AnAction action,
    @Nonnull DefaultActionGroup destinationGroup,
    @Nonnull DataContext dataContext,
    @Nonnull ActionManager actionManager,
    boolean popup
  ) {
    if (action instanceof DefaultActionGroup) {
      final AnAction[] children = ((DefaultActionGroup)action).getChildren(null);
      for (AnAction child : children) {
        if (child instanceof DefaultActionGroup) {
          final boolean isPopup = ((DefaultActionGroup)child).isPopup();
          if (isPopup) {
            destinationGroup.add(new AnSeparator(child.getTemplatePresentation().getText()));
          }
          collectEnabledChildren(child, destinationGroup, dataContext, actionManager, isPopup || popup);
          if (isPopup) {
            destinationGroup.add(AnSeparator.getInstance());
          }
        } else if (child instanceof AnSeparator && !popup) {
          destinationGroup.add(child);
        }
        else {
          if (child instanceof BaseRefactoringAction && ((BaseRefactoringAction)child).hasAvailableHandler(dataContext)) {
            final Presentation presentation = new Presentation();
            final AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, presentation, actionManager, 0);
            child.update(event);
            if (presentation.isEnabled() && presentation.isVisible()) {
              destinationGroup.add(child);
            }
          }
        }
      }
    }
  }


  @Override
  protected void showPopup(AnActionEvent e, ListPopup popup) {
    final Editor editor = e.getData(Editor.KEY);
    if (editor != null) {
      editor.showPopupInBestPositionFor(popup);
    } else {
      super.showPopup(e, popup);
    }
  }

  @Override
  protected boolean isEnabled() {
    return true;
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setVisible(
      e.getPlace() == ActionPlaces.MAIN_MENU || e.getPlace() == ActionPlaces.ACTION_PLACE_QUICK_LIST_POPUP_ACTION
    );
  }

  @Override
  protected String getPopupTitle(AnActionEvent e) {
    return "Refactor This";
  }
}
