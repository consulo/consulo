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
package consulo.language.editor.parameterInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class LegacySignaturePresentation {
    private static final int NO_SEPARATOR = -1;

    private LegacySignaturePresentation() {
    }

    static String apply(
        SignatureBuilder builder,
        String text,
        int highlightStartOffset,
        int highlightEndOffset,
        boolean isDisabled,
        boolean strikeout,
        boolean isDisabledBeforeHighlight
    ) {
        boolean hasHighlight = highlightStartOffset >= 0 && highlightEndOffset > highlightStartOffset;

        int[] levels = levels(text);
        int separatorLevel = separatorLevel(text, levels);

        List<int[]> pieces = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (separatorLevel != NO_SEPARATOR && text.charAt(i) == ',' && levels[i] == separatorLevel) {
                pieces.add(new int[]{start, i});
                start = i + 1;
            }
        }
        pieces.add(new int[]{start, text.length()});

        StringBuilder plainText = new StringBuilder();
        for (int p = 0; p < pieces.size(); p++) {
            int pieceStart = pieces.get(p)[0];
            int pieceEnd = pieces.get(p)[1];

            if (p > 0) {
                while (pieceStart < pieceEnd && Character.isWhitespace(text.charAt(pieceStart))) {
                    pieceStart++;
                }
            }

            if (p < pieces.size() - 1) {
                while (pieceEnd > pieceStart && Character.isWhitespace(text.charAt(pieceEnd - 1))) {
                    pieceEnd--;
                }
            }

            if (p > 0) {
                builder.comma();
                plainText.append(", ");
            }

            boolean parameterStarted = false;
            int runStart = pieceStart;
            while (runStart < pieceEnd) {
                boolean parameter = separatorLevel == NO_SEPARATOR || levels[runStart] >= separatorLevel;
                SignatureStyle style = styleAt(runStart, hasHighlight, highlightStartOffset, highlightEndOffset, isDisabledBeforeHighlight);

                int runEnd = runStart + 1;
                while (runEnd < pieceEnd
                    && (separatorLevel == NO_SEPARATOR || levels[runEnd] >= separatorLevel) == parameter
                    && styleAt(runEnd, hasHighlight, highlightStartOffset, highlightEndOffset, isDisabledBeforeHighlight) == style) {
                    runEnd++;
                }

                String runText = text.substring(runStart, runEnd);
                SignatureStyle[] styles = style == null ? new SignatureStyle[0] : new SignatureStyle[]{style};
                if (parameter && !parameterStarted) {
                    builder.parameter(runText, styles);
                    parameterStarted = true;
                }
                else {
                    builder.text(runText, styles);
                }

                plainText.append(runText);
                runStart = runEnd;
            }
        }

        if (isDisabled) {
            builder.disabled();
        }

        if (strikeout) {
            builder.deprecated();
        }

        builder.apply();

        return plainText.toString();
    }

    private static SignatureStyle styleAt(
        int offset,
        boolean hasHighlight,
        int highlightStartOffset,
        int highlightEndOffset,
        boolean isDisabledBeforeHighlight
    ) {
        if (hasHighlight && offset >= highlightStartOffset && offset < highlightEndOffset) {
            return SignatureStyle.HIGHLIGHT;
        }

        if (isDisabledBeforeHighlight && (!hasHighlight || offset < highlightStartOffset)) {
            return SignatureStyle.DISABLED;
        }
        return null;
    }

    private static int[] levels(String text) {
        int[] levels = new int[text.length()];
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '(', '[', '{', '<' -> {
                    levels[i] = depth;
                    depth++;
                }
                case ')', ']', '}', '>' -> {
                    depth = Math.max(0, depth - 1);
                    levels[i] = depth;
                }
                default -> levels[i] = depth;
            }
        }
        return levels;
    }

    private static int separatorLevel(String text, int[] levels) {
        int level = NO_SEPARATOR;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',' && (level == NO_SEPARATOR || levels[i] < level)) {
                level = levels[i];
            }
        }
        return level;
    }
}
