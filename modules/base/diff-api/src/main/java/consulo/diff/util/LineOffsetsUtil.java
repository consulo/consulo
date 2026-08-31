/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.diff.util;

import consulo.document.Document;

public final class LineOffsetsUtil {
    public static LineOffsets create(Document document) {
        return new LineOffsetsDocumentWrapper(document);
    }

    /**
     * NB: Does not support CRLF separators, use {@link consulo.util.lang.StringUtil#convertLineSeparators}.
     */
    public static LineOffsets create(CharSequence text) {
        return LineOffsetsImpl.create(text);
    }
}
