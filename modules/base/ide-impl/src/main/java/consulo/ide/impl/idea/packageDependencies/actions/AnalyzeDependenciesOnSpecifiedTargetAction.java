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
package consulo.ide.impl.idea.packageDependencies.actions;

import consulo.language.editor.LangDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.dataholder.Key;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class AnalyzeDependenciesOnSpecifiedTargetAction extends AnAction {
  public static final Key<GlobalSearchScope> TARGET_SCOPE_KEY = Key.create("MODULE_DEPENDENCIES_TARGET_SCOPE");

  public AnalyzeDependenciesOnSpecifiedTargetAction() {
    super(LocalizeValue.localizeTODO("Analyze Dependencies on Specified Target"));
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    final GlobalSearchScope targetScope = e.getData(TARGET_SCOPE_KEY);
    if (module == null || targetScope == null) return;

    new AnalyzeDependenciesOnSpecifiedTargetHandler(module.getProject(), new AnalysisScope(module), targetScope).analyze();
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    final GlobalSearchScope scope = e.getData(TARGET_SCOPE_KEY);
    final Presentation presentation = e.getPresentation();
    if (module != null && scope != null) {
      presentation.setVisible(true);
      presentation.setText("Analyze Dependencies of Module '" + module.getName() + "' on " + scope.getDisplayName());
    }
    else {
      presentation.setVisible(false);
    }
  }
}
