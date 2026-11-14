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
package consulo.ai.impl.internal.chat;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A piece of the markdown of an answer: prose, or a fenced code block with the language its fence names.
 *
 * @param code     whether the piece is a fenced code block - its text is the code without the fences
 * @param language the first word after the opening fence, null when there is none
 * @author VISTALL
 * @since 2026-11-14
 */
record AIChatTextBlock(boolean code, @Nullable String language, String text) {
    /**
     * Splits markdown at its fenced code blocks ({@code ```} or {@code ~~~}, indented by at most three spaces). A block
     * whose closing fence has not come yet - the answer still streams - runs to the end.
     */
    static List<AIChatTextBlock> split(String markdown) {
        List<AIChatTextBlock> blocks = new ArrayList<>();
        StringBuilder prose = new StringBuilder();
        StringBuilder code = new StringBuilder();
        String fence = null;
        String language = null;
        for (String line : markdown.split("\n", -1)) {
            if (fence == null) {
                String opening = fence(line);
                if (opening == null) {
                    prose.append(line).append('\n');
                    continue;
                }
                addProse(blocks, prose);
                fence = opening;
                String info = line.strip().substring(opening.length()).strip();
                int space = info.indexOf(' ');
                language = info.isEmpty() ? null : space < 0 ? info : info.substring(0, space);
                code.setLength(0);
            }
            else if (closes(line, fence)) {
                blocks.add(new AIChatTextBlock(true, language, trimLastBreak(code)));
                fence = null;
            }
            else {
                code.append(line).append('\n');
            }
        }
        if (fence != null) {
            blocks.add(new AIChatTextBlock(true, language, trimLastBreak(code)));
        }
        else {
            addProse(blocks, prose);
        }
        return blocks;
    }

    /**
     * Whether the markdown holds a line which opens a code block.
     */
    static boolean hasCode(CharSequence markdown) {
        for (String line : markdown.toString().split("\n", -1)) {
            if (fence(line) != null) {
                return true;
            }
        }
        return false;
    }

    private static void addProse(List<AIChatTextBlock> blocks, StringBuilder prose) {
        String text = trimLastBreak(prose);
        if (!text.isBlank()) {
            blocks.add(new AIChatTextBlock(false, null, text));
        }
        prose.setLength(0);
    }

    private static String trimLastBreak(StringBuilder text) {
        int length = text.length();
        return length > 0 && text.charAt(length - 1) == '\n' ? text.substring(0, length - 1) : text.toString();
    }

    /**
     * @return the fence a line opens with, null when it opens none
     */
    private static @Nullable String fence(String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        if (indent > 3 || indent >= line.length()) {
            return null;
        }
        char mark = line.charAt(indent);
        if (mark != '`' && mark != '~') {
            return null;
        }
        int end = indent;
        while (end < line.length() && line.charAt(end) == mark) {
            end++;
        }
        if (end - indent < 3) {
            return null;
        }
        // a backtick fence names no language with a backtick in it - that is inline code
        if (mark == '`' && line.indexOf('`', end) >= 0) {
            return null;
        }
        return line.substring(indent, end);
    }

    private static boolean closes(String line, String fence) {
        String stripped = line.strip();
        if (stripped.length() < fence.length() || line.length() - line.stripLeading().length() > 3) {
            return false;
        }
        char mark = fence.charAt(0);
        for (int i = 0; i < stripped.length(); i++) {
            if (stripped.charAt(i) != mark) {
                return false;
            }
        }
        return true;
    }
}
