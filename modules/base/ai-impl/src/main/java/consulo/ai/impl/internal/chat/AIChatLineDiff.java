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
 * The change of a file a tool call made, as the lines of a unified diff with a few lines of context around each
 * change - enough to read in a chat, not a patch to apply.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
record AIChatLineDiff(List<String> lines, int added, int removed) {
    private static final int CONTEXT = 3;
    /**
     * Longer texts are compared only by their size - the table of the longest common subsequence grows with the
     * product of the line counts.
     */
    private static final long MAX_CELLS = 4_000_000;

    static AIChatLineDiff of(@Nullable String oldText, String newText) {
        String[] before = lines(oldText);
        String[] after = lines(newText);
        if ((long) before.length * after.length > MAX_CELLS) {
            List<String> lines = new ArrayList<>();
            for (String line : after) {
                lines.add("+" + line);
            }
            return new AIChatLineDiff(lines, after.length, before.length);
        }

        int[][] common = new int[before.length + 1][after.length + 1];
        for (int i = before.length - 1; i >= 0; i--) {
            for (int j = after.length - 1; j >= 0; j--) {
                common[i][j] = before[i].equals(after[j]) ? common[i + 1][j + 1] + 1 : Math.max(common[i + 1][j], common[i][j + 1]);
            }
        }

        // every line marked: ' ' kept, '-' removed, '+' added
        List<String> marked = new ArrayList<>();
        int i = 0;
        int j = 0;
        int added = 0;
        int removed = 0;
        while (i < before.length || j < after.length) {
            if (i < before.length && j < after.length && before[i].equals(after[j])) {
                marked.add(" " + before[i]);
                i++;
                j++;
            }
            else if (j < after.length && (i == before.length || common[i][j + 1] >= common[i + 1][j])) {
                marked.add("+" + after[j]);
                j++;
                added++;
            }
            else {
                marked.add("-" + before[i]);
                i++;
                removed++;
            }
        }

        List<String> lines = new ArrayList<>();
        int lastShown = -1;
        for (int index = 0; index < marked.size(); index++) {
            if (marked.get(index).charAt(0) == ' ') {
                continue;
            }
            int from = Math.max(index - CONTEXT, lastShown + 1);
            if (lastShown >= 0 && from > lastShown + 1) {
                lines.add("@@");
            }
            int to = index;
            while (to + 1 < marked.size() && (marked.get(to + 1).charAt(0) != ' ' || hasChangeWithin(marked, to + 1, CONTEXT * 2))) {
                to++;
            }
            to = Math.min(marked.size() - 1, to + CONTEXT);
            for (int line = from; line <= to; line++) {
                lines.add(marked.get(line));
            }
            lastShown = to;
            index = to;
        }
        return new AIChatLineDiff(lines, added, removed);
    }

    /**
     * The lines of the text - the line break which ends the last line starts no line of its own.
     */
    private static String[] lines(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return (text.endsWith("\n") ? text.substring(0, text.length() - 1) : text).split("\n", -1);
    }

    private static boolean hasChangeWithin(List<String> marked, int from, int count) {
        for (int index = from; index < Math.min(marked.size(), from + count); index++) {
            if (marked.get(index).charAt(0) != ' ') {
                return true;
            }
        }
        return false;
    }
}
