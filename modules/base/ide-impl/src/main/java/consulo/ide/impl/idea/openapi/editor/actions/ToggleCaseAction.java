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

import consulo.annotation.access.RequiredWriteAction;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorWriteActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nullable;

import java.util.Locale;

import static consulo.language.ast.StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN;

/**
 * @author max
 * @since 2002-05-20
 */
public class ToggleCaseAction extends TextComponentEditorAction {
    public ToggleCaseAction() {
        super(new Handler());
    }

    private static class Handler extends EditorWriteActionHandler {
        @RequiredWriteAction
        @Override
        public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            SimpleReference<Boolean> toLowerCase = new SimpleReference<>(Boolean.FALSE);
            runForCaret(
                editor,
                caret,
                eachCaret -> {
                    if (!eachCaret.hasSelection()) {
                        eachCaret.selectWordAtCaret(true);
                    }
                    int selectionStartOffset = eachCaret.getSelectionStart();
                    int selectionEndOffset = eachCaret.getSelectionEnd();
                    String originalText = editor.getDocument().getText(new TextRange(selectionStartOffset, selectionEndOffset));
                    if (!originalText.equals(toCase(editor, selectionStartOffset, selectionEndOffset, true))) {
                        toLowerCase.set(Boolean.TRUE);
                    }
                }
            );
            runForCaret(
                editor,
                caret,
                eachCaret -> {
                    VisualPosition caretPosition = eachCaret.getVisualPosition();
                    int selectionStartOffset = eachCaret.getSelectionStart();
                    int selectionEndOffset = eachCaret.getSelectionEnd();
                    VisualPosition selectionStartPosition = eachCaret.getSelectionStartPosition();
                    VisualPosition selectionEndPosition = eachCaret.getSelectionEndPosition();
                    eachCaret.removeSelection();
                    editor.getDocument().replaceString(
                        selectionStartOffset,
                        selectionEndOffset,
                        toCase(editor, selectionStartOffset, selectionEndOffset, toLowerCase.get())
                    );
                    eachCaret.moveToVisualPosition(caretPosition);
                    eachCaret.setSelection(selectionStartPosition, selectionStartOffset, selectionEndPosition, selectionEndOffset);
                }
            );
        }

        private static void runForCaret(Editor editor, Caret caret, CaretAction action) {
            if (caret == null) {
                editor.getCaretModel().runForEachCaret(action);
            }
            else {
                action.perform(caret);
            }
        }

        private static String toCase(Editor editor, int startOffset, int endOffset, boolean lower) {
            CharSequence text = editor.getDocument().getImmutableCharSequence();
            EditorHighlighter highlighter = editor.getHighlighter();
            HighlighterIterator iterator = highlighter.createIterator(startOffset);
            StringBuilder builder = new StringBuilder(endOffset - startOffset);
            while (!iterator.atEnd()) {
                int start = trim(iterator.getStart(), startOffset, endOffset);
                int end = trim(iterator.getEnd(), startOffset, endOffset);
                CharSequence fragment = text.subSequence(start, end);

                builder.append(
                    iterator.getTokenType() == VALID_STRING_ESCAPE_TOKEN ? fragment
                        : lower ? fragment.toString().toLowerCase(Locale.getDefault())
                        : fragment.toString().toUpperCase(Locale.getDefault())
                );

                if (end == endOffset) {
                    break;
                }
                iterator.advance();
            }
            return builder.toString();
        }

        private static int trim(int value, int lowerLimit, int upperLimit) {
            return Math.min(upperLimit, Math.max(lowerLimit, value));
        }
    }
}
