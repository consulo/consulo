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

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.language.editor.impl.action.BaseAnalysisAction;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

@ActionImpl(
    id = IdeActions.ACTION_ANALYZE_DEPENDENCIES,
    parents = @ActionParentRef(value = @ActionRef(id = "ShowPackageDepsGroup"), anchor = ActionRefAnchor.FIRST)
)
public class AnalyzeDependenciesAction extends BaseAnalysisAction {
    private CheckBox myTransitiveCB;
    private IntBox myDeepField;

    public AnalyzeDependenciesAction() {
        super(
            ActionLocalize.actionShowpackagedepsText(),
            ActionLocalize.actionShowpackagedepsDescription(),
            AnalysisScopeLocalize.actionForwardDependencyAnalysis(),
            AnalysisScopeLocalize.actionAnalysisNoun()
        );
    }

    @Override
    protected void analyze(@Nonnull Project project, @Nonnull AnalysisScope scope) {
        new AnalyzeDependenciesHandler(
            project,
            scope,
            myTransitiveCB.getValue() ? myDeepField.getValueOrError() : 0
        ).analyze();

        clear();
    }

    @Override
    @RequiredUIAccess
    protected void extendMainLayout(BaseAnalysisActionDialog dialog, VerticalLayout layout, Project project) {
        DockLayout dockLayout = DockLayout.create();
        layout.add(dockLayout);

        myTransitiveCB = CheckBox.create(LocalizeValue.localizeTODO("Show transitive dependencies. Do not travel deeper than"));

        dockLayout.left(myTransitiveCB);
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
