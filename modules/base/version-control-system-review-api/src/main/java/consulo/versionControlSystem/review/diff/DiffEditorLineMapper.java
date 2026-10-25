// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import org.jspecify.annotations.Nullable;

/**
 * The lines of one editor of a diff viewer against the lines of the diff sides.
 */
public interface DiffEditorLineMapper {
    /**
     * @return the editor line showing {@code location}, {@code null} when the editor does not show it
     */
    @Nullable Integer locationToLine(DiffLineLocation location);

    /**
     * @return the side line an editor line shows, {@code null} for a line that cannot be commented
     */
    @Nullable DiffLineLocation lineToLocation(int line);

    /**
     * @return the left and the right line of an editor line, {@code -1} for a side it does not show
     */
    int[] lineToUnified(int line);
}
