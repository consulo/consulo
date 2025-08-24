/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;

@ActionImpl(id = IdeActions.ACTION_EDITOR_DELETE_TO_WORD_START)
public class DeleteToWordStartAction extends TextComponentEditorAction {
    public DeleteToWordStartAction() {
        super(ActionLocalize.actionEditordeletetowordstartText(), new DeleteToWordBoundaryHandler(true, false));
    }
}
