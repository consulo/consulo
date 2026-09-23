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
package consulo.execution.impl.internal.console;

import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.util.dataholder.Key;
import consulo.project.Project;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.document.RangeMarker;
import consulo.colorScheme.TextAttributesKey;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.execution.util.ConsoleBuffer;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import org.jspecify.annotations.Nullable;

import consulo.util.lang.StringUtil;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * What a console does with text before a frontend ever sees it. Output is taken on whichever thread produced it
 * and held until a flush carries it to the document, so the view is left with drawing rather than bookkeeping.
 *
 * @author VISTALL
 * @since 2026-09-22
 */
public final class ConsoleViewEngine {
    /**
     * What the caller of {@link #print} has to do about the text which was just taken.
     */
    public enum FlushHint {
        NONE,
        IMMEDIATELY,
        NOW,
        DELAYED
    }

    private static final Key<ConsoleViewContentType> CONTENT_TYPE = Key.create("ConsoleViewContentType");

    private static final char BACKSPACE = '\b';

    private final Object myLock = new Object();

    private final TokenBuffer myDeferredBuffer =
        new TokenBuffer(ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : Integer.MAX_VALUE);

    private final Supplier<EditorEx> myEditorSupplier;

    private boolean myOutputPaused;

    private boolean myLastStickingToEnd;

    public ConsoleViewEngine(Supplier<EditorEx> editorSupplier) {
        myEditorSupplier = editorSupplier;
    }

    /**
     * The decision of what to do next is taken under the same lock the text was taken under, so a console being
     * torn down cannot be found halfway.
     */
    public FlushHint print(String text, ConsoleViewContentType contentType, @Nullable HyperlinkInfo info) {
        synchronized (myLock) {
            myDeferredBuffer.print(text, contentType, info);

            if (contentType == ConsoleViewContentType.USER_INPUT) {
                return FlushHint.IMMEDIATELY;
            }

            if (myEditorSupplier.get() == null) {
                return FlushHint.NONE;
            }

            return myDeferredBuffer.length() >= myDeferredBuffer.getCycleBufferSize() ? FlushHint.NOW : FlushHint.DELAYED;
        }
    }

    public boolean hasDeferredOutput() {
        synchronized (myLock) {
            return myDeferredBuffer.length() > 0;
        }
    }

    public void clearDeferredOutput() {
        synchronized (myLock) {
            myDeferredBuffer.clear();
        }
    }

    /**
     * What a console holds is what a document already shows plus what is still waiting to reach it.
     */
    public int getContentSize() {
        synchronized (myLock) {
            EditorEx editor = myEditorSupplier.get();

            return (editor == null ? 0 : editor.getDocument().getTextLength()) + myDeferredBuffer.length();
        }
    }

    public boolean isOutputPaused() {
        synchronized (myLock) {
            return myOutputPaused;
        }
    }

    public void setOutputPaused(boolean outputPaused) {
        synchronized (myLock) {
            myOutputPaused = outputPaused;
        }
    }

    /**
     * Takes everything waiting, or nothing at all while the output is held.
     */
    public List<TokenBuffer.TokenInfo> drain() {
        synchronized (myLock) {
            return myOutputPaused ? List.of() : myDeferredBuffer.drain();
        }
    }

    public int getCycleBufferSize() {
        synchronized (myLock) {
            return myDeferredBuffer.getCycleBufferSize();
        }
    }

    public static int evaluateBackspacesInTokens(
        List<? extends TokenBuffer.TokenInfo> source,
        int sourceStartIndex,
        List<? super TokenBuffer.TokenInfo> dest
    ) {
        int backspacesFromNextToken = 0;
        for (int i = source.size() - 1; i >= sourceStartIndex; i--) {
            TokenBuffer.TokenInfo token = source.get(i);
            TokenBuffer.TokenInfo newToken;
            if (StringUtil.containsChar(token.getText(), BACKSPACE) || backspacesFromNextToken > 0) {
                StringBuilder tokenTextBuilder = new StringBuilder(token.getText().length() + backspacesFromNextToken);
                tokenTextBuilder.append(token.getText());
                for (int j = 0; j < backspacesFromNextToken; j++) {
                    tokenTextBuilder.append(BACKSPACE);
                }
                normalizeBackspaceCharacters(tokenTextBuilder);
                backspacesFromNextToken = getBackspacePrefixLength(tokenTextBuilder);
                String newText = tokenTextBuilder.substring(backspacesFromNextToken);
                newToken = new TokenBuffer.TokenInfo(token.contentType, newText, token.getHyperlinkInfo());
            }
            else {
                newToken = token;
            }
            dest.add(newToken);
        }
        Collections.reverse(dest);
        return backspacesFromNextToken;
    }

    private static int getBackspacePrefixLength(CharSequence text) {
        int prefix = 0;
        while (prefix < text.length() && text.charAt(prefix) == BACKSPACE) {
            prefix++;
        }
        return prefix;
    }

    // convert all "a\bc" sequences to "c", not crossing the line boundaries in the process
    private static void normalizeBackspaceCharacters(StringBuilder text) {
        int ind = StringUtil.indexOf(text, BACKSPACE);
        if (ind < 0) {
            return;
        }
        int guardLength = 0;
        int newLength = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            boolean append;
            if (ch == BACKSPACE) {
                assert guardLength <= newLength;
                if (guardLength == newLength) {
                    // Backspace is the first char in a new line:
                    // Keep backspace at the first line (guardLength == 0) as it might be in the middle of the actual line,
                    // handle it later (see getBackspacePrefixLength).
                    // Otherwise (for non-first lines), skip backspace as it can't be interpreted if located right after line ending.
                    append = guardLength == 0;
                }
                else {
                    append = text.charAt(newLength - 1) == BACKSPACE;
                    if (!append) {
                        newLength--; // interpret \b: delete prev char
                    }
                }
            }
            else {
                append = true;
            }
            if (append) {
                text.setCharAt(newLength, ch);
                newLength++;
                if (ch == '\r' || ch == '\n') {
                    guardLength = newLength;
                }
            }
        }
        text.setLength(newLength);
    }

    /**
     * Whether the caret still sits on the last line, which is what decides if new output should carry the view
     * along with it. Asked of the caret rather than of a scrollbar, so a frontend without one still answers.
     */
    public boolean isStickingToEnd() {
        EditorEx editor = myEditorSupplier.get();
        if (editor == null) {
            return myLastStickingToEnd;
        }

        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();

        myLastStickingToEnd = document.getLineNumber(caretOffset) >= document.getLineCount() - 1;

        return myLastStickingToEnd;
    }

    /**
     * Marks the run of text a token occupies with the colours of its content type, and remembers the type on the
     * marker so a later pass can tell what a stretch of the document was.
     */
    public void createTokenRangeHighlighter(Project project, ConsoleViewContentType contentType, int startOffset, int endOffset) {
        EditorEx editor = myEditorSupplier.get();
        if (editor == null) {
            return;
        }

        MarkupModelEx model = DocumentMarkupModel.forDocument(editor.getDocument(), project, true);
        // make custom filters able to draw their text attributes over the default ones
        int layer = HighlighterLayer.SYNTAX + 1;
        TextAttributesKey key = contentType.getAttributesKey();

        model.addRangeHighlighterAndChangeAttributes(
            key, startOffset, endOffset, layer, HighlighterTargetArea.EXACT_RANGE, false,
            rm -> {
                // fallback for contentTypes that provides only attributes
                if (key == null) {
                    rm.setTextAttributes(contentType.getAttributes());
                }
                saveTokenType(rm, contentType);
            });
    }

    public static @Nullable ConsoleViewContentType getTokenType(@Nullable RangeMarker m) {
        return m == null ? null : m.getUserData(CONTENT_TYPE);
    }

    public static void saveTokenType(RangeMarker m, ConsoleViewContentType contentType) {
        m.putUserData(CONTENT_TYPE, contentType);
    }
}
