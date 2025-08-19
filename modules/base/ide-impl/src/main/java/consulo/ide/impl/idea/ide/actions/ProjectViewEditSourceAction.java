/*
 * Copyright 2013-2021 consulo.io
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

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigatableWithText;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Objects;

// from kotlin
@ActionImpl(id = "ProjectViewEditSource", shortcutFrom = @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE))
public class ProjectViewEditSourceAction extends BaseNavigateToSourceAction {
    public ProjectViewEditSourceAction() {
        super(true);
        getTemplatePresentation().setTextValue(ActionLocalize.actionProjectvieweditsourceText());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        Presentation presentation = e.getPresentation();
        if (!presentation.isVisible() || !presentation.isEnabled()) {
            return;
        }

        Navigatable[] navigatables = getNavigatables(e.getDataContext());
        if (navigatables == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        boolean find = Arrays.stream(navigatables)
            .map(it -> it instanceof NavigatableWithText navWithText ? navWithText.getNavigateActionText(true) : null)
            .filter(Objects::nonNull)
            .findAny()
            .isPresent();
        e.getPresentation().setEnabledAndVisible(find);
    }
}
