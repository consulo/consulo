/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.dataContext.DataContext;
import consulo.application.dumb.DumbAware;
import consulo.navigation.Navigatable;
import consulo.language.pom.NavigatableWithText;
import consulo.language.pom.PomTargetPsiElement;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class BaseNavigateToSourceAction extends AnAction implements DumbAware {
  private final boolean myFocusEditor;

  protected BaseNavigateToSourceAction(boolean focusEditor) {
    myFocusEditor = focusEditor;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    OpenSourceUtil.navigate(myFocusEditor, getNavigatables(dataContext));
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    boolean inPopup = ActionPlaces.isPopupPlace(e.getPlace());
    Navigatable target = findTargetForUpdate(e.getDataContext());
    boolean enabled = target != null;
    if (inPopup && !(this instanceof OpenModuleSettingsAction) && OpenModuleSettingsAction.isModuleInProjectViewPopup(e)) {
      e.getPresentation().setVisible(false);
      return;
    }
    //as myFocusEditor is always ignored - Main Menu|View always contains 2 actions with the same name and actually same behaviour
    e.getPresentation().setVisible((enabled || !inPopup) && (myFocusEditor || !(target instanceof NavigatableWithText)));
    e.getPresentation().setEnabled(enabled);

    String navigateActionText = myFocusEditor && target instanceof NavigatableWithText navigatableWithText
      ? navigatableWithText.getNavigateActionText(true) : null;
    e.getPresentation().setText(navigateActionText == null ? getTemplatePresentation().getText() : navigateActionText);
  }

  @Nullable
  private Navigatable findTargetForUpdate(@Nonnull DataContext dataContext) {
    Navigatable[] navigatables = getNavigatables(dataContext);
    if (navigatables == null) return null;

    for (Navigatable navigatable : navigatables) {
      if (navigatable.canNavigate()) {
        return navigatable instanceof PomTargetPsiElement ? ((PomTargetPsiElement)navigatable).getTarget() : navigatable;
      }
    }
    return null;
  }

  @Nullable
  protected Navigatable[] getNavigatables(final DataContext dataContext) {
    return dataContext.getData(Navigatable.KEY_OF_ARRAY);
  }
}
