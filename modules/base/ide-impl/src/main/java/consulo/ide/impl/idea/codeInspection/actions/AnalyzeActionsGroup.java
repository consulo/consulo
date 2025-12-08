/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.moduleDependencies.ShowModuleDependenciesAction;
import consulo.ide.impl.idea.packageDependencies.actions.AnalyzeDependenciesAction;
import consulo.ide.impl.idea.packageDependencies.actions.AnalyzeDependenciesOnSpecifiedTargetAction;
import consulo.ide.impl.idea.packageDependencies.actions.BackwardDependenciesAction;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-06
 */
@ActionImpl(
    id = "AnalyzeActions",
    children = {
        @ActionRef(type = AnalyzeDependenciesAction.class),
        @ActionRef(type = BackwardDependenciesAction.class),
        @ActionRef(type = AnalyzeDependenciesOnSpecifiedTargetAction.class),
        @ActionRef(type = ShowModuleDependenciesAction.class)
    }
)
public class AnalyzeActionsGroup extends DefaultActionGroup implements DumbAware {
    public AnalyzeActionsGroup() {
        super(LocalizeValue.absent(), false);
    }
}
