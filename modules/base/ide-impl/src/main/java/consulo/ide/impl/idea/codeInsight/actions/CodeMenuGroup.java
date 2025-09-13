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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.completion.actions.CodeCompletionGroup;
import consulo.ide.impl.idea.codeInsight.editorActions.moveLeftRight.MoveElementLeftAction;
import consulo.ide.impl.idea.codeInsight.editorActions.moveLeftRight.MoveElementRightAction;
import consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown.MoveLineDownAction;
import consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown.MoveLineUpAction;
import consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown.MoveStatementDownAction;
import consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown.MoveStatementUpAction;
import consulo.ide.impl.idea.codeInsight.folding.impl.actions.FoldingGroup;
import consulo.ide.impl.idea.codeInsight.generation.actions.CommentGroup;
import consulo.ide.impl.idea.codeInsight.generation.actions.SurroundWithAction;
import consulo.ide.impl.idea.codeInsight.template.impl.actions.ListTemplatesAction;
import consulo.ide.impl.idea.codeInsight.template.impl.actions.SurroundWithTemplateAction;
import consulo.ide.impl.idea.codeInsight.unwrap.UnwrapAction;
import consulo.ide.impl.idea.codeInspection.actions.AnalyzeMenuGroup;
import consulo.ide.impl.idea.codeInspection.actions.CodeInspectionAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-08-06
 */
@ActionImpl(
    id = IdeActions.ACTION_CODE_MENU,
    children = {
        @ActionRef(id = "OverrideMethods"),
        @ActionRef(id = "ImplementMethods"),
        @ActionRef(id = "DelegateMethods"),
        @ActionRef(id = "Generate"),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = SurroundWithAction.class),
        @ActionRef(type = UnwrapAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CodeCompletionGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CodeInspectionAction.class),
        @ActionRef(type = AnalyzeMenuGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = FoldingGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = ListTemplatesAction.class),
        @ActionRef(type = SurroundWithTemplateAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = CommentGroup.class),
        @ActionRef(type = CodeFormatGroup.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = MoveStatementDownAction.class),
        @ActionRef(type = MoveStatementUpAction.class),
        @ActionRef(type = MoveElementLeftAction.class),
        @ActionRef(type = MoveElementRightAction.class),
        @ActionRef(type = MoveLineDownAction.class),
        @ActionRef(type = MoveLineUpAction.class),
        @ActionRef(type = AnSeparator.class)
    }
)
public class CodeMenuGroup extends DefaultActionGroup implements DumbAware {
    public CodeMenuGroup() {
        super(ActionLocalize.groupCodemenuText(), true);
    }
}
