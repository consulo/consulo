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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.language.editor.parameterInfo.SignatureStyle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class ParameterInfoLines {
    private static final String SPACE = " ";

    private ParameterInfoLines() {
    }

    public static List<List<ParameterInfoRun>> breakLines(ParameterInfoSignature signature, ToIntFunction<String> width, int maxWidth) {
        Set<SignatureStyle> signatureStyles = signatureStyles(signature);

        List<List<ParameterInfoRun>> chunks = new ArrayList<>();
        List<ParameterInfoRun> chunk = new ArrayList<>();
        for (ParameterInfoSegment segment : signature.segments()) {
            Set<SignatureStyle> styles = union(segment.styles(), signatureStyles);
            if (segment.kind() == ParameterInfoSegmentKind.SEPARATOR) {
                if (!segment.text().isEmpty()) {
                    chunk.add(new ParameterInfoRun(segment.text(), styles));
                }
                chunks.add(chunk);
                chunk = new ArrayList<>();
            }
            else if (!segment.text().isEmpty()) {
                chunk.add(new ParameterInfoRun(segment.text(), styles));
            }
        }
        chunks.add(chunk);

        List<List<ParameterInfoRun>> lines = new ArrayList<>();
        List<ParameterInfoRun> line = new ArrayList<>();
        int lineWidth = 0;
        boolean afterSeparator = false;
        for (List<ParameterInfoRun> next : chunks) {
            int nextWidth = width(next, width);
            if (afterSeparator) {
                int spaceWidth = width.applyAsInt(SPACE);
                if (!line.isEmpty() && lineWidth + spaceWidth + nextWidth > maxWidth) {
                    lines.add(line);
                    line = new ArrayList<>();
                    lineWidth = 0;
                }
                else {
                    line.add(new ParameterInfoRun(SPACE, signatureStyles));
                    lineWidth += spaceWidth;
                }
            }

            line.addAll(next);
            lineWidth += nextWidth;
            afterSeparator = true;
        }

        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    public static String toPlainText(ParameterInfoSignature signature) {
        StringBuilder builder = new StringBuilder();
        for (List<ParameterInfoRun> line : breakLines(signature, String::length, Integer.MAX_VALUE)) {
            for (ParameterInfoRun run : line) {
                builder.append(run.text());
            }
        }
        return builder.toString();
    }

    private static Set<SignatureStyle> signatureStyles(ParameterInfoSignature signature) {
        Set<SignatureStyle> styles = EnumSet.noneOf(SignatureStyle.class);
        if (signature.disabled()) {
            styles.add(SignatureStyle.DISABLED);
        }
        if (signature.deprecated()) {
            styles.add(SignatureStyle.STRIKEOUT);
        }
        return styles;
    }

    private static Set<SignatureStyle> union(Set<SignatureStyle> first, Set<SignatureStyle> second) {
        Set<SignatureStyle> styles = EnumSet.noneOf(SignatureStyle.class);
        styles.addAll(first);
        styles.addAll(second);
        return styles;
    }

    private static int width(List<ParameterInfoRun> runs, ToIntFunction<String> width) {
        int result = 0;
        for (ParameterInfoRun run : runs) {
            result += width.applyAsInt(run.text());
        }
        return result;
    }
}
