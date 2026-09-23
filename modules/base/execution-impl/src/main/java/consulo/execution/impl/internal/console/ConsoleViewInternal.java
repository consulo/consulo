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
import consulo.colorScheme.TextAttributes;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * A console as the platform builds it: what {@link ConsoleView} promises, plus what only the frontend drawing it
 * can answer. The engine owns the text and asks the view for the rest, so a frontend implements rather than
 * extends.
 *
 * @author VISTALL
 * @since 2026-09-22
 */
public interface ConsoleViewInternal extends ConsoleView {
    EditorEx createConsoleEditor();

    void releaseConsoleEditor(EditorEx editor);

    void consoleEditorCreated(EditorEx editor);

    void scrollToEnd(EditorEx editor);

    boolean isStickToEndCancelled();

    void resetStickToEndCancelled();

    void heavyFilterStarted(LocalizeValue message);

    void heavyFilterFinished();

    /**
     * Hyperlinks are drawn and followed by whoever owns the editor - a frontend without them answers that it has
     * none rather than refusing to run.
     */
    void createManualHyperlink(int startOffset, int endOffset, HyperlinkInfo info);

    @Nullable
    HyperlinkInfo getHyperlinkInfoByLineAndCol(int line, int column);

    void highlightHyperlinks(Filter filter, int startLine, int endLine);

    void addHyperlinkHighlighter(int startOffset, int endOffset, TextAttributes attributes);

    void clearHyperlinks();
}
