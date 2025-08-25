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
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-07-27
 */
@ActionImpl(
    id = "EditSmartGroup",
    children = {
        @ActionRef(id = IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT),
        @ActionRef(type = JoinLinesAction.class),
        @ActionRef(id = "FillParagraph"),
        @ActionRef(type = DuplicateAction.class),
        @ActionRef(id = "EditorIndentSelection"),
        @ActionRef(type = UnindentSelectionAction.class),
        @ActionRef(type = ToggleCaseAction.class),
        @ActionRef(id = "ConvertIndentsGroup")
    }
)
public class EditCodeActionsGroup extends DefaultActionGroup implements DumbAware {
    public EditCodeActionsGroup() {
        super(ActionLocalize.groupEditsmartgroupText(), false);
    }
}
