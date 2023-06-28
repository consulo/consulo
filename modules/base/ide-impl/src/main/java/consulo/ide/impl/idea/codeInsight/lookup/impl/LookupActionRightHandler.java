/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretAction;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.completion.CompletionProgressIndicator;
import consulo.ide.impl.idea.codeInsight.completion.impl.CompletionServiceImpl;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class LookupActionRightHandler extends LookupActionHandler {
  @Override
  protected void executeInLookup(LookupEx lookup, DataContext context, final Caret caret) {
    final Editor editor = lookup.getEditor();
    final int offset = editor.getCaretModel().getOffset();
    final CharSequence seq = editor.getDocument().getCharsSequence();
    if (seq.length() <= offset || !lookup.isCompletion()) {
      myOriginalHandler.execute(editor, caret, context);
      return;
    }

    char c = seq.charAt(offset);
    CharFilter.Result lookupAction = LookupTypedHandler.getLookupAction(c, lookup);

    if (lookupAction != CharFilter.Result.ADD_TO_PREFIX || Character.isWhitespace(c)) {
      myOriginalHandler.execute(editor, caret, context);
      return;
    }

    if (!lookup.performGuardedChange(() -> {
      CaretAction action = lookupCaret -> {
        lookupCaret.removeSelection();
        int caretOffset = lookupCaret.getOffset();
        if (caretOffset < seq.length()) {
          lookupCaret.moveToOffset(caretOffset + 1);
        }
      };
      if (caret == null) {
        editor.getCaretModel().runForEachCaret(action);
      }
      else {
        action.perform(caret);
      }
    })) {
      return;
    }

    lookup.fireBeforeAppendPrefix(c);
    lookup.appendPrefix(c);
    final CompletionProgressIndicator completion = CompletionServiceImpl.getCurrentCompletionProgressIndicator();
    if (completion != null) {
      completion.prefixUpdated();
    }
  }

  @Nonnull
  @Override
  public String getActionId() {
    return IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT;
  }
}
