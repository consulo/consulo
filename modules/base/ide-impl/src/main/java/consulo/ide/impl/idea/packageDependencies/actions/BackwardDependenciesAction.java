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

import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.find.ui.ScopeChooserCombo;
import consulo.language.editor.impl.action.BaseAnalysisAction;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2005-01-16
 */
public class BackwardDependenciesAction extends BaseAnalysisAction {
  private ScopeChooserCombo myScopeChooserCombo;

  public BackwardDependenciesAction() {
    super(
      AnalysisScopeLocalize.actionBackwardDependencyAnalysis().get(),
      AnalysisScopeLocalize.actionAnalysisNoun().get()
    );
  }

  @Override
  protected void analyze(@Nonnull final Project project, final AnalysisScope scope) {
    scope.setSearchInLibraries(true); //find library usages in project
    final SearchScope selectedScope = myScopeChooserCombo.getSelectedScope();
    new BackwardDependenciesHandler(project, scope, selectedScope != null ? new AnalysisScope(selectedScope, project) : new AnalysisScope(project)).analyze();
    dispose();
  }

  @Override
  protected boolean acceptNonProjectDirectories() {
    return true;
  }

  @Override
  protected void canceled() {
    super.canceled();
    dispose();
  }

  @RequiredUIAccess
  @Override
  protected void extendMainLayout(BaseAnalysisActionDialog dialog, VerticalLayout layout, Project project) {
    DockLayout dockLayout = DockLayout.create();
    dockLayout.left(Label.create(LocalizeValue.localizeTODO("Scope to Analyze Usages in ")));

    myScopeChooserCombo = new ScopeChooserCombo();
    myScopeChooserCombo.init(project, null);

    dockLayout.right(TargetAWT.wrap(myScopeChooserCombo));
    layout.add(dockLayout);
  }

  private void dispose() {
    Disposer.dispose(myScopeChooserCombo);
    myScopeChooserCombo = null;
  }
}
