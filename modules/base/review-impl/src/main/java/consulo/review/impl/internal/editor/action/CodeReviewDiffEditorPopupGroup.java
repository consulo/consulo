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
package consulo.review.impl.internal.editor.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * The review actions at the top of the context menu of a diff editor.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ActionImpl(
    id = "Code.Review.Diff.Editor.Popup",
    children = @ActionRef(type = AnSeparator.class),
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_DIFF_EDITOR_POPUP), anchor = ActionRefAnchor.FIRST)
)
public class CodeReviewDiffEditorPopupGroup extends DefaultActionGroup implements DumbAware {
}
