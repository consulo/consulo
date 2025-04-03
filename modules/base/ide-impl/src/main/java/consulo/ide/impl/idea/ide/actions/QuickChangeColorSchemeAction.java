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
package consulo.ide.impl.idea.ide.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class QuickChangeColorSchemeAction extends QuickSwitchSchemeAction {
    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
        EditorColorsScheme current = EditorColorsManager.getInstance().getGlobalScheme();
        for (EditorColorsScheme scheme : EditorColorsManager.getInstance().getAllSchemes()) {
            addScheme(group, current, scheme, false);
        }
    }

    private static void addScheme(
        DefaultActionGroup group,
        EditorColorsScheme current,
        EditorColorsScheme scheme,
        boolean addScheme
    ) {
        group.add(new DumbAwareAction(scheme.getName(), "", scheme == current ? ourCurrentAction : ourNotCurrentAction) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nullable AnActionEvent e) {
                if (addScheme) {
                    EditorColorsManager.getInstance().addColorsScheme(scheme);
                }
                EditorColorsManager.getInstance().setGlobalScheme(scheme);
            }
        });
    }

    @Override
    protected boolean isEnabled() {
        return EditorColorsManager.getInstance().getAllSchemes().length > 1;
    }
}
