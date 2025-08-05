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
import consulo.codeEditor.impl.internal.action.EditCodeActionsGroup;
import consulo.codeEditor.impl.internal.action.ToggleColumnModeAction;
import consulo.ide.impl.idea.codeInsight.completion.actions.TemplateParametersNavigationGroup;
import consulo.ide.impl.idea.find.actions.FindMenuGroup;
import consulo.ide.impl.idea.ide.actionMacro.actions.MacrosPopupGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-08-05
 */
@ActionImpl(
    id = "EditMenu",
    children = {
        @ActionRef(type = UndoAction.class),
        @ActionRef(type = RedoAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CutCopyPasteGroup.class),
        @ActionRef(type = EditCreateDeleteGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = FindMenuGroup.class),
        @ActionRef(type = MacrosPopupGroup.class),
        @ActionRef(type = ToggleColumnModeAction.class),
        @ActionRef(type = EditorSelectActionsGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = EditCodeActionsGroup.class),
        @ActionRef(type = TemplateParametersNavigationGroup.class)
    }
)
public class EditMenuGroup extends DefaultActionGroup implements DumbAware {
    public EditMenuGroup() {
        super(ActionLocalize.groupEditmenuText(), true);
    }
}
