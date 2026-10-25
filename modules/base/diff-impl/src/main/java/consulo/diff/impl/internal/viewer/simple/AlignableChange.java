// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.viewer.simple;

import consulo.codeEditor.Editor;
import consulo.diff.util.Side;
import consulo.diff.util.TextDiffType;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

public interface AlignableChange {
    TextDiffType getDiffType();

    int getStartLine(Side side);

    int getEndLine(Side side);

    static boolean isSame(AlignableChange change, AlignableChange other) {
        return change.getStartLine(Side.LEFT) == other.getStartLine(Side.LEFT) && change.getEndLine(Side.LEFT) == other.getEndLine(Side.LEFT)
            && change.getStartLine(Side.RIGHT) == other.getStartLine(Side.RIGHT) && change.getEndLine(Side.RIGHT) == other.getEndLine(Side.RIGHT);
    }

    static @Nullable ColorValue getAlignedChangeColor(TextDiffType type, Editor editor) {
        if (type == TextDiffType.MODIFIED) {
            return null;
        }
        return type.getColor(editor).withAlpha(200);
    }
}
