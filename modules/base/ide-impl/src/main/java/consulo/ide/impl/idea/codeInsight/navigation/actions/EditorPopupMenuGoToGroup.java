/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.navigationToolbar.ShowNavBarAction;
import consulo.ide.impl.idea.testIntegration.GotoTestOrCodeAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ActionImpl(
    id = "EditorPopupMenu.GoTo",
    children = {
        @ActionRef(type = ShowNavBarAction.class),
        @ActionRef(type = GotoDeclarationAction.class),
        @ActionRef(type = GotoImplementationAction.class),
        @ActionRef(type = GotoTypeDeclarationAction.class),
        @ActionRef(type = GotoSuperAction.class),
        @ActionRef(type = GotoTestOrCodeAction.class)
    }
)
public class EditorPopupMenuGoToGroup extends DefaultActionGroup implements DumbAware {
    public EditorPopupMenuGoToGroup() {
        super(ActionLocalize.groupEditorpopupmenuGotoText(), true);
        getTemplatePresentation().setDescription(ActionLocalize.groupEditorpopupmenuGotoDescription());
    }
}
