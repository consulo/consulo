/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.ide.KillRingTransferable;
import consulo.annotations.RequiredWriteAction;

/**
 * Stands for emacs <a href="http://www.gnu.org/software/emacs/manual/html_node/emacs/Words.html#Words">backward-kill-word</a> command.
 * <p/>
 * Generally, it removes text from the previous word start up to the current cursor position and puts
 * it to the {@link KillRingTransferable kill ring}.
 * <p/>
 * Thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 4/19/11 4:18 PM
 */
public class KillToWordStartAction extends TextComponentEditorAction {

  public KillToWordStartAction() {
    super(new Handler());
  }
  
  private static class Handler extends EditorWriteActionHandler {
    @RequiredWriteAction
    @Override
    public void executeWriteAction(Editor editor, DataContext dataContext) {
      CaretModel caretModel = editor.getCaretModel();
      int caretOffset = caretModel.getOffset();
      Document document = editor.getDocument();
      if (caretOffset <= 0) {
        return;
      }

      CharSequence text = document.getCharsSequence();
      boolean camel = editor.getSettings().isCamelWords();
      for (int i = caretOffset - 1; i >= 0; i--) {
        if (EditorActionUtil.isWordStart(text, i, camel)) {
          KillRingUtil.cut(editor, i, caretOffset);
          return;
        }
      }

      KillRingUtil.cut(editor, 0, caretOffset);
    }
  }
}
