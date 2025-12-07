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
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.editorActions.smartEnter.SmartEnterAction;
import consulo.ide.impl.idea.codeInsight.lookup.impl.actions.LookupActionsGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-09-23
 */
@ActionImpl(
    id = "CodeInsightEditorActions",
    children = {
        @ActionRef(type = LookupActionsGroup.class),
        @ActionRef(type = EmacsStyleIndentAction.class),
        @ActionRef(type = CodeBlockStartAction.class),
        @ActionRef(type = CodeBlockEndAction.class),
        @ActionRef(type = MatchBraceAction.class),
        @ActionRef(type = CodeBlockStartWithSelectionAction.class),
        @ActionRef(type = CodeBlockEndWithSelectionAction.class),
        @ActionRef(type = SmartEnterAction.class)
    },
    parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR))
)
public class CodeInsightEditorActionsGroup extends DefaultActionGroup implements DumbAware {
    public CodeInsightEditorActionsGroup() {
        super(LocalizeValue.absent(), false);
    }
}
