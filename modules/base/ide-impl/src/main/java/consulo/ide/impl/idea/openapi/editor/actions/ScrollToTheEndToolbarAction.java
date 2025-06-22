/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.application.AllIcons;
import consulo.application.ReadAction;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author oleg
 */
public class ScrollToTheEndToolbarAction extends ToggleAction implements DumbAware {
  private final Editor myEditor;

  public ScrollToTheEndToolbarAction(@Nonnull final Editor editor) {
    super();
    myEditor = editor;
    final LocalizeValue message = ActionLocalize.actionEditorconsolescrolltotheendText();
    getTemplatePresentation().setDescriptionValue(message);
    getTemplatePresentation().setTextValue(message);
    getTemplatePresentation().setIcon(AllIcons.RunConfigurations.Scroll_down);
  }

  @Override
  public boolean isSelected(@Nonnull AnActionEvent e) {
    Document document = myEditor.getDocument();
      if (document.getLineCount() == 0) {
          return true;
      }

      int offset = ReadAction.compute(() -> myEditor.getCaretModel().getOffset());

      if (document.getLineNumber(offset) == document.getLineCount() - 1) {
          return true;
      }
      return false;
  }

  @Override
  public void setSelected(@Nonnull AnActionEvent e, boolean state) {
    if (state) {
      EditorUtil.scrollToTheEnd(myEditor);
    }
    else {
      int lastLine = Math.max(0, myEditor.getDocument().getLineCount() - 1);
      LogicalPosition currentPosition = myEditor.getCaretModel().getLogicalPosition();
      LogicalPosition position = new LogicalPosition(Math.max(0, Math.min(currentPosition.line, lastLine - 1)), currentPosition.column);
      myEditor.getCaretModel().moveToLogicalPosition(position);
    }
  }
}
