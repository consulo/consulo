/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.*;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author max
 * @since 2002-05-14
 */
public class ToggleColumnModeAction extends ToggleAction implements DumbAware {
  public ToggleColumnModeAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    final EditorEx editor = getEditor(e);
    final SelectionModel selectionModel = editor.getSelectionModel();
    final CaretModel caretModel = editor.getCaretModel();
    if (state) {
      caretModel.removeSecondaryCarets();
      boolean hasSelection = selectionModel.hasSelection();
      int selStart = selectionModel.getSelectionStart();
      int selEnd = selectionModel.getSelectionEnd();
      LogicalPosition blockStart, blockEnd;
      if (caretModel.supportsMultipleCarets()) {
        LogicalPosition logicalSelStart = editor.offsetToLogicalPosition(selStart);
        LogicalPosition logicalSelEnd = editor.offsetToLogicalPosition(selEnd);
        int caretOffset = caretModel.getOffset();
        blockStart = selStart == caretOffset ? logicalSelEnd : logicalSelStart;
        blockEnd = selStart == caretOffset ? logicalSelStart : logicalSelEnd;
      }
      else {
        blockStart = selStart == caretModel.getOffset()
                     ? caretModel.getLogicalPosition()
                     : editor.offsetToLogicalPosition(selStart);
        blockEnd = selEnd == caretModel.getOffset()
                   ? caretModel.getLogicalPosition()
                   : editor.offsetToLogicalPosition(selEnd);
      }
      editor.setColumnMode(true);
      if (hasSelection) {
        selectionModel.setBlockSelection(blockStart, blockEnd);
      }
      else {
        selectionModel.removeSelection();
      }
    }
    else {
      boolean hasSelection = false;
      int selStart = 0;
      int selEnd = 0;

      if (caretModel.supportsMultipleCarets()) {
        hasSelection = true;
        List<Caret> allCarets = caretModel.getAllCarets();
        Caret fromCaret = allCarets.get(0);
        Caret toCaret = allCarets.get(allCarets.size() - 1);
        if (fromCaret == caretModel.getPrimaryCaret()) {
          Caret tmp = fromCaret;
          fromCaret = toCaret;
          toCaret = tmp;
        }
        selStart = fromCaret.getLeadSelectionOffset();
        selEnd = toCaret.getSelectionStart() == toCaret.getLeadSelectionOffset() ? toCaret.getSelectionEnd() : toCaret.getSelectionStart();
      }

      editor.setColumnMode(false);
      caretModel.removeSecondaryCarets();
      if (hasSelection) {
        selectionModel.setSelection(selStart, selEnd);
      }
      else {
        selectionModel.removeSelection();
      }
    }
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    final EditorEx ex = getEditor(e);
    return ex != null && ex.isColumnMode();
  }

  private static EditorEx getEditor(AnActionEvent e) {
    return (EditorEx) e.getData(Editor.KEY);
  }

  @Override
  public void update(@Nonnull AnActionEvent e){
    EditorEx editor = getEditor(e);
    if (editor == null || editor.isOneLineMode()) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setVisible(false);
    } else {
      e.getPresentation().setEnabled(true);
      e.getPresentation().setVisible(true);
      super.update(e);
    }
  }
}
