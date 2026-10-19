// Copyright 2000-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor;

import org.jspecify.annotations.Nullable;

import java.util.function.IntUnaryOperator;

/**
 * Adapter for migrating from obsolete API
 * {@link EditorGutterComponentEx#setLineNumberConvertor(IntUnaryOperator)} to
 * {@link EditorGutterComponentEx#setLineNumberConverter(LineNumberConverter)} and
 * {@link EditorGutterComponentEx#setLineNumberConverter(LineNumberConverter, LineNumberConverter)}
 */
public final class LineNumberConverterAdapter implements LineNumberConverter {
    private final IntUnaryOperator myFunction;

    public LineNumberConverterAdapter(IntUnaryOperator function) {
        myFunction = function;
    }

    @Override
    public @Nullable Integer convert(Editor editor, int lineNumber) {
        int result = myFunction.applyAsInt(lineNumber - 1);
        return result < 0 ? null : result + 1;
    }

    @Override
    public @Nullable Integer getMaxLineNumber(Editor editor) {
        for (int i = editor.getDocument().getLineCount(); i > 0; i--) {
            int number = myFunction.applyAsInt(i - 1);
            if (number >= 0) {
                return number + 1;
            }
        }
        return 0;
    }
}
