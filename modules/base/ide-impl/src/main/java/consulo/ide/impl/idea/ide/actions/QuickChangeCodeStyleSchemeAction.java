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

import consulo.codeEditor.EditorFactory;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class QuickChangeCodeStyleSchemeAction extends QuickSwitchSchemeAction {
    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
        CodeStyleSettingsManager manager = CodeStyleSettingsManager.getInstance(project);
        if (manager.PER_PROJECT_SETTINGS != null) {
            //noinspection HardCodedStringLiteral
            group.add(new AnAction(
                "<project>",
                "",
                manager.USE_PER_PROJECT_SETTINGS ? ourCurrentAction : ourNotCurrentAction
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    manager.USE_PER_PROJECT_SETTINGS = true;
                }
            });
        }

        CodeStyleScheme currentScheme = CodeStyleSchemes.getInstance().getCurrentScheme();
        for (CodeStyleScheme scheme : CodeStyleSchemes.getInstance().getSchemes()) {
            addScheme(group, manager, currentScheme, scheme, false);
        }
    }

    private static void addScheme(
        DefaultActionGroup group,
        CodeStyleSettingsManager manager,
        CodeStyleScheme currentScheme,
        CodeStyleScheme scheme,
        boolean addScheme
    ) {
        group.add(new AnAction(
            scheme.getName(),
            "",
            scheme == currentScheme && !manager.USE_PER_PROJECT_SETTINGS ? ourCurrentAction : ourNotCurrentAction
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                if (addScheme) {
                    CodeStyleSchemes.getInstance().addScheme(scheme);
                }
                CodeStyleSchemes.getInstance().setCurrentScheme(scheme);
                manager.USE_PER_PROJECT_SETTINGS = false;
                manager.PREFERRED_PROJECT_CODE_STYLE = scheme.getName();
                EditorFactory.getInstance().refreshAllEditors();
            }
        });
    }

    @Override
    protected boolean isEnabled() {
        return CodeStyleSchemes.getInstance().getSchemes().length > 1;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(e.getDataContext().getData(Project.KEY) != null);
    }
}
