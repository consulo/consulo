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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoImplementationAction;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoSuperAction;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoTypeDeclarationAction;
import consulo.ide.impl.idea.ide.hierarchy.actions.BrowseCallHierarchyGroup;
import consulo.ide.impl.idea.ide.navigationToolbar.ShowNavBarAction;
import consulo.ide.impl.idea.testIntegration.GotoTestOrCodeAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-07-31
 */
@ActionImpl(
    id = "GoToCodeGroup",
    children = {
        @ActionRef(type = SelectInAction.class),
        @ActionRef(type = ShowNavBarAction.class),
        @ActionRef(type = GotoDeclarationAction.class),
        @ActionRef(type = GotoImplementationAction.class),
        @ActionRef(type = GotoTypeDeclarationAction.class),
        @ActionRef(type = GotoSuperAction.class),
        @ActionRef(type = GotoTestOrCodeAction.class),
        @ActionRef(type = GotoRelatedFileAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ViewStructureAction.class),
        @ActionRef(type = ShowFilePathAction.class),
        @ActionRef(type = BrowseCallHierarchyGroup.class),
        @ActionRef(type = AnSeparator.class)
    }
)
public class GoToCodeGroup extends DefaultActionGroup implements DumbAware {
    public GoToCodeGroup() {
        super(ActionLocalize.groupGotocodegroupText(), false);
    }
}
