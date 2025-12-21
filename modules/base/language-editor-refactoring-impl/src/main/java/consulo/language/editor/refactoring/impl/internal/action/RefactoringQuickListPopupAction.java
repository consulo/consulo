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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.action.QuickSwitchSchemeAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "Refactorings.QuickListPopupAction")
public class RefactoringQuickListPopupAction extends QuickSwitchSchemeAction {
    public RefactoringQuickListPopupAction() {
        super(
            ActionLocalize.actionRefactoringsQuicklistpopupactionText(),
            ActionLocalize.actionRefactoringsQuicklistpopupactionDescription()
        );
    }

    @Override
    @RequiredUIAccess
    protected void fillActions(@Nullable Project project,
                               @Nonnull ActionGroup.Builder group,
                               @Nonnull DataContext dataContext) {
        if (project == null) {
            return;
        }

        ActionManager actionManager = ActionManager.getInstance();
        AnAction action = actionManager.getAction(IdeActions.GROUP_REFACTOR);
        collectEnabledChildren(action, group, dataContext, actionManager, false);
    }

    @RequiredUIAccess
    private static void collectEnabledChildren(
        AnAction action,
        @Nonnull ActionGroup.Builder destinationGroup,
        @Nonnull DataContext dataContext,
        @Nonnull ActionManager actionManager,
        boolean popup
    ) {
        if (action instanceof DefaultActionGroup group) {
            for (AnAction child : group.getChildren(null)) {
                if (child instanceof DefaultActionGroup childGroup) {
                    boolean isPopup = childGroup.isPopup();
                    if (isPopup) {
                        destinationGroup.add(AnSeparator.create(child.getTemplatePresentation().getTextValue()));
                    }
                    collectEnabledChildren(child, destinationGroup, dataContext, actionManager, isPopup || popup);
                    if (isPopup) {
                        destinationGroup.add(AnSeparator.getInstance());
                    }
                }
                else if (child instanceof AnSeparator && !popup) {
                    destinationGroup.add(child);
                }
                else if (child instanceof BaseRefactoringAction baseRefactoringAction
                    && baseRefactoringAction.hasAvailableHandler(dataContext)) {
                    Presentation presentation = new Presentation();
                    AnActionEvent event = new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, presentation, actionManager, 0);
                    child.update(event);
                    if (presentation.isEnabled() && presentation.isVisible()) {
                        destinationGroup.add(child);
                    }
                }
            }
        }
    }

    @Override
    protected void showPopup(AnActionEvent e, ListPopup popup) {
        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            editor.showPopupInBestPositionFor(popup);
        }
        else {
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
    protected LocalizeValue getPopupTitle(AnActionEvent e) {
        return LocalizeValue.localizeTODO("Refactor This");
    }
}