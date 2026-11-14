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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatAttachment;
import consulo.ai.AIContent;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;

/**
 * The code a comment for the agent is about - the selection, or the line of the caret without one.
 *
 * @param name  the name of the file
 * @param where the file and the lines, as the chat shows them
 * @param code  the code as a resource of the file, with the lines in its address
 * @author VISTALL
 * @since 2026-11-14
 */
record AIEditorSelection(String name, int firstLine, int lastLine, LocalizeValue where, AIContent code) {
    private static final int MAX_LABEL_COMMENT = 40;

    @RequiredUIAccess
    static AIEditorSelection of(Editor editor) {
        Document document = editor.getDocument();
        SelectionModel selection = editor.getSelectionModel();
        int start;
        int end;
        if (selection.hasSelection()) {
            start = selection.getSelectionStart();
            end = selection.getSelectionEnd();
        }
        else {
            int line = document.getLineNumber(editor.getCaretModel().getOffset());
            start = document.getLineStartOffset(line);
            end = document.getLineEndOffset(line);
        }
        int firstLine = document.getLineNumber(start) + 1;
        int lastLine = document.getLineNumber(Math.max(start, end - 1)) + 1;
        String code = document.getText().substring(start, end);
        VirtualFile file = editor.getVirtualFile();
        String path = file == null ? "selection" : file.getPath();
        String name = file == null ? path : file.getName();
        return new AIEditorSelection(
            name,
            firstLine,
            lastLine,
            AIEditorLocalize.labelSelection(name, firstLine, lastLine),
            AIContent.resource("file://" + path + "#L" + firstLine + "-L" + lastLine, null, code)
        );
    }

    /**
     * The code alone - for a message which is the comment.
     */
    AIChatAttachment attachment() {
        return new AIChatAttachment(where, List.of(code));
    }

    /**
     * The code with the comment on it - for a chip which waits for the next message.
     */
    AIChatAttachment commented(String comment) {
        LocalizeValue label = LocalizeValue.join(where, LocalizeValue.of(" \u00b7 "), LocalizeValue.of(shorten(comment)));
        AIContent note = AIContent.text(AIEditorLocalize.promptCommentOn(name, firstLine, lastLine, comment).get());
        return new AIChatAttachment(label, List.of(note, code));
    }

    private static String shorten(String comment) {
        String line = comment.lines().findFirst().orElse("").strip();
        return line.length() > MAX_LABEL_COMMENT ? line.substring(0, MAX_LABEL_COMMENT) + "\u2026" : line;
    }
}
