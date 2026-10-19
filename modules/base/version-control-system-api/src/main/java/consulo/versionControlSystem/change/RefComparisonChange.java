// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.change;

import consulo.util.dataholder.Key;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import org.jspecify.annotations.Nullable;

/**
 * Describes a file change between two revisions.
 */
public record RefComparisonChange(ShortVcsRevisionNumber revisionNumberBefore,
                                  @Nullable FilePath filePathBefore,
                                  ShortVcsRevisionNumber revisionNumberAfter,
                                  @Nullable FilePath filePathAfter) {
    public static final Key<RefComparisonChange> KEY = Key.create("RefComparisonChange");
}
