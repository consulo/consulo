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
package consulo.ide.impl.idea.codeInsight.lookup.impl.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.codeInsight.hint.actions.NextParameterAction;
import consulo.ide.impl.idea.codeInsight.hint.actions.PrevParameterAction;
import consulo.ide.impl.idea.codeInsight.template.impl.actions.PreviousVariableAction;
import consulo.ide.impl.idea.codeInsight.template.impl.editorActions.ExpandLiveTemplateByTabAction;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author UNV
 * @since 2025-09-23
 */
@ActionImpl(
    id = "LookupActions",
    children = {
        @ActionRef(type = FocusedOnlyChooseItemAction.class),
        @ActionRef(type = ChooseItemReplaceAction.class),
        @ActionRef(type = ChooseItemCompleteStatementAction.class),
        @ActionRef(type = ChooseItemWithDotAction.class),
        @ActionRef(type = ExpandLiveTemplateByTabAction.class),
        @ActionRef(type = PreviousVariableAction.class),
        @ActionRef(type = NextParameterAction.class),
        @ActionRef(type = PrevParameterAction.class)
    }
)
public class LookupActionsGroup extends DefaultActionGroup implements DumbAware {
    public LookupActionsGroup() {
        super(LocalizeValue.absent(), false);
    }
}
