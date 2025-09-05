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
package consulo.ui.ex.action;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigatableWithText;
import consulo.navigation.internal.NavigateWithDelegate;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class BaseNavigateToSourceAction extends AnAction implements DumbAware {
    private final boolean myFocusEditor;

    protected BaseNavigateToSourceAction(boolean focusEditor) {
        myFocusEditor = focusEditor;
    }

    protected BaseNavigateToSourceAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, boolean focusEditor) {
        super(text, description);
        myFocusEditor = focusEditor;
    }

    protected BaseNavigateToSourceAction(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon,
        boolean focusEditor
    ) {
        super(text, description, icon);
        myFocusEditor = focusEditor;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        OpenSourceUtil.navigate(myFocusEditor, getNavigatables(dataContext));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean inPopup = ActionPlaces.isPopupPlace(e.getPlace());
        Navigatable target = findTargetForUpdate(e.getDataContext());
        boolean enabled = target != null;
//        FIXME [VISTALL] we need this?
//        if (inPopup && !(this instanceof OpenModuleSettingsAction) && OpenModuleSettingsAction.isModuleInProjectViewPopup(e)) {
//            e.getPresentation().setVisible(false);
//            return;
//        }
        //as myFocusEditor is always ignored - Main Menu|View always contains 2 actions with the same name and actually same behaviour
        e.getPresentation().setVisible((enabled || !inPopup) && (myFocusEditor || !(target instanceof NavigatableWithText)));
        e.getPresentation().setEnabled(enabled);

        LocalizeValue navigateActionText = myFocusEditor && target instanceof NavigatableWithText navigatableWithText
            ? navigatableWithText.getNavigateActionText(true)
            : LocalizeValue.empty();
        e.getPresentation().setTextValue(
            navigateActionText == LocalizeValue.empty()
                ? getTemplatePresentation().getTextValue()
                : navigateActionText
        );
    }

    @Nullable
    private Navigatable findTargetForUpdate(@Nonnull DataContext dataContext) {
        Navigatable[] navigatables = getNavigatables(dataContext);
        if (navigatables == null) {
            return null;
        }

        for (Navigatable navigatable : navigatables) {
            if (navigatable.canNavigate()) {
                return navigatable instanceof NavigateWithDelegate delegate ? delegate.getDelegate() : navigatable;
            }
        }
        return null;
    }

    @Nullable
    protected Navigatable[] getNavigatables(DataContext dataContext) {
        return dataContext.getData(Navigatable.KEY_OF_ARRAY);
    }
}
