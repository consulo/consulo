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

package consulo.ide.impl.idea.packageDependencies.actions;

import consulo.ide.impl.idea.analysis.BaseAnalysisAction;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

public class AnalyzeDependenciesAction extends BaseAnalysisAction {
  private CheckBox myTransitiveCB;
  private IntBox myDeepField;

  public AnalyzeDependenciesAction() {
    super(AnalysisScopeBundle.message("action.forward.dependency.analysis"), AnalysisScopeBundle.message("action.analysis.noun"));
  }

  @Override
  protected void analyze(@Nonnull final Project project, @Nonnull AnalysisScope scope) {
    new AnalyzeDependenciesHandler(project,
                                   scope,
                                   myTransitiveCB.getValue() ? myDeepField.getValueOrError() : 0).analyze();

    clear();
  }

  @RequiredUIAccess
  @Override
  protected void extendMainLayout(BaseAnalysisActionDialog dialog, VerticalLayout layout, Project project) {
    DockLayout dockLayout = DockLayout.create();
    layout.add(dockLayout);

    dockLayout.left(
      myTransitiveCB = CheckBox.create(LocalizeValue.localizeTODO("Show transitive dependencies. Do not travel deeper than")));

    dockLayout.right(myDeepField = IntBox.create(5));

    myDeepField.setEnabled(false);
    myTransitiveCB.addValueListener(event -> myDeepField.setEnabled(event.getValue()));
  }

  @Override
  protected void canceled() {
    super.canceled();
    clear();
  }

  private void clear() {
    myTransitiveCB = null;
    myDeepField = null;
  }
}
