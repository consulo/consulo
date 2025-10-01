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
 * @since 2025-09-27
 */
@ActionImpl(
    id = IdeActions.GROUP_EDITOR,
    children = {
        @ActionRef(type = PageUpAction.class),
        @ActionRef(type = PageDownAction.class),
        @ActionRef(type = PageUpWithSelectionAction.class),
        @ActionRef(type = PageDownWithSelectionAction.class),
        @ActionRef(type = CopyAction.class),
        @ActionRef(type = CutAction.class),
        @ActionRef(type = PasteAction.class),
        @ActionRef(type = SimplePasteAction.class),
        @ActionRef(type = PasteFromX11Action.class),
        @ActionRef(type = DeleteAction.class),
        @ActionRef(type = BackspaceAction.class),
        @ActionRef(type = HungryBackspaceAction.class),
        @ActionRef(type = PageTopAction.class),
        @ActionRef(type = PageBottomAction.class),
        @ActionRef(type = PageTopWithSelectionAction.class),
        @ActionRef(type = PageBottomWithSelectionAction.class),
        @ActionRef(type = EnterAction.class),
        @ActionRef(type = EscapeAction.class),
        @ActionRef(type = MoveCaretUpAction.class),
        @ActionRef(type = MoveCaretDownAction.class),
        @ActionRef(type = MoveCaretLeftAction.class),
        @ActionRef(type = MoveCaretRightAction.class),
        @ActionRef(type = MoveCaretUpWithSelectionAction.class),
        @ActionRef(type = MoveCaretDownWithSelectionAction.class),
        @ActionRef(type = MoveCaretLeftWithSelectionAction.class),
        @ActionRef(type = MoveCaretRightWithSelectionAction.class),
        @ActionRef(type = UnindentSelectionAction.class),
        @ActionRef(type = TabAction.class),
        @ActionRef(type = ResetFontSizeAction.class),
        @ActionRef(type = ScrollUpAction.class),
        @ActionRef(type = ScrollDownAction.class),
        @ActionRef(type = ScrollUpAndMoveAction.class),
        @ActionRef(type = ScrollDownAndMoveAction.class),
        @ActionRef(type = ScrollRightAction.class),
        @ActionRef(type = ScrollLeftAction.class),
        @ActionRef(type = ScrollToTopAction.class),
        @ActionRef(type = ScrollToBottomAction.class),
        @ActionRef(type = MoveUpAndScrollAction.class),
        @ActionRef(type = MoveDownAndScrollAction.class),
        @ActionRef(type = MoveUpWithSelectionAndScrollAction.class),
        @ActionRef(type = MoveDownWithSelectionAndScrollAction.class),
        @ActionRef(id = IdeActions.ACTION_EDITOR_ADD_OR_REMOVE_CARET),
        @ActionRef(type = CloneCaretBelow.class),
        @ActionRef(type = CloneCaretAbove.class),
        @ActionRef(type = ToggleStickySelectionModeAction.class),
        @ActionRef(type = SwapSelectionBoundariesAction.class),
        @ActionRef(type = LineStartAction.class),
        @ActionRef(type = LineEndAction.class),
        @ActionRef(type = CutLineBackwardAction.class),
        @ActionRef(type = CutLineEndAction.class),
        @ActionRef(type = DeleteToLineStartAction.class),
        @ActionRef(type = DeleteToLineEndAction.class),
        @ActionRef(type = TextStartAction.class),
        @ActionRef(type = TextEndAction.class),
        @ActionRef(type = LineStartWithSelectionAction.class),
        @ActionRef(type = LineEndWithSelectionAction.class),
        @ActionRef(type = TextStartWithSelectionAction.class),
        @ActionRef(type = TextEndWithSelectionAction.class),
        @ActionRef(type = NextWordAction.class),
        @ActionRef(type = NextWordInDifferentHumpsModeAction.class),
        @ActionRef(type = NextWordWithSelectionAction.class),
        @ActionRef(type = NextWordInDifferentHumpsModeWithSelectionAction.class),
        @ActionRef(type = PreviousWordAction.class),
        @ActionRef(type = PreviousWordInDifferentHumpsModeAction.class),
        @ActionRef(type = PreviousWordWithSelectionAction.class),
        @ActionRef(type = PreviousWordInDifferentHumpsModeWithSelectionAction.class),
        @ActionRef(type = DeleteToWordStartAction.class),
        @ActionRef(type = DeleteToWordStartInDifferentHumpsModeAction.class),
        @ActionRef(type = DeleteToWordEndAction.class),
        @ActionRef(type = DeleteToWordEndInDifferentHumpsModeAction.class),
        @ActionRef(type = DeleteLineAction.class),
        @ActionRef(type = KillToWordStartAction.class),
        @ActionRef(type = KillToWordEndAction.class),
        @ActionRef(type = KillRegionAction.class),
        @ActionRef(type = KillRingSaveAction.class),
        @ActionRef(type = DuplicateAction.class),
        @ActionRef(type = DuplicateLinesAction.class),
        @ActionRef(type = ToggleInsertStateAction.class),
        @ActionRef(type = ToggleColumnModeAction.class),
        @ActionRef(type = IncreaseEditorFontSizeAction.class),
        @ActionRef(type = DecreaseEditorFontSizeAction.class),
        @ActionRef(type = ScrollToCenterAction.class),
        @ActionRef(type = ToggleCaseAction.class),
        @ActionRef(type = JoinLinesAction.class),
        @ActionRef(id = "FillParagraph"),
        @ActionRef(type = SelectLineAction.class),
        @ActionRef(type = SplitLineActionImpl.class),
        @ActionRef(type = StartNewLineAction.class),
        @ActionRef(type = StartNewLineBeforeAction.class)
    }
)
public class EditorActionsGroup extends DefaultActionGroup implements DumbAware {
    public EditorActionsGroup() {
        super(ActionLocalize.groupEditoractionsText(), false);
    }
}
