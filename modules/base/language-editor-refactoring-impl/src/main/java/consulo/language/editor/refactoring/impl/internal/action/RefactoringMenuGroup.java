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
package consulo.language.editor.refactoring.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.refactoring.rename.RenameElementAction;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author UNV
 * @since 2025-08-06
 */
@ActionImpl(
    id = "RefactoringMenu",
    children = {
        @ActionRef(type = RefactoringQuickListPopupAction.class),
        @ActionRef(type = RenameElementAction.class),
        @ActionRef(type = ChangeSignatureAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = MoveAction.class),
        @ActionRef(id = "CopyElement"),
        @ActionRef(id = "CloneElement"),
        @ActionRef(type = SafeDeleteAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = IntroduceActionsGroup.class),
        @ActionRef(type = InlineAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = PullUpAction.class),
        @ActionRef(type = PushDownAction.class)
    },
    parents = @ActionParentRef(
        value = @ActionRef(id = IdeActions.GROUP_MAIN_MENU),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = IdeActions.ACTION_CODE_MENU)
    )
)
public class RefactoringMenuGroup extends DefaultActionGroup implements DumbAware {
    public RefactoringMenuGroup() {
        super(ActionLocalize.groupRefactoringmenuText(), true);
    }
}
