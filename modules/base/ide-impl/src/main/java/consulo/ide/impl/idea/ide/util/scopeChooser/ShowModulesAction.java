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
package consulo.ide.impl.idea.ide.util.scopeChooser;

import consulo.ide.impl.idea.packageDependencies.DependencyUISettings;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2008-01-16
 */
public final class ShowModulesAction extends ToggleAction {
    private final Runnable myUpdate;

    public ShowModulesAction(Runnable update) {
        super(
            IdeLocalize.actionShowModules(),
            IdeLocalize.actionDescriptionShowModules(),
            PlatformIconGroup.actionsGroupbymodule()
        );
        myUpdate = update;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return DependencyUISettings.getInstance().UI_SHOW_MODULES;
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean flag) {
        DependencyUISettings.getInstance().UI_SHOW_MODULES = flag;
        myUpdate.run();
    }
}
