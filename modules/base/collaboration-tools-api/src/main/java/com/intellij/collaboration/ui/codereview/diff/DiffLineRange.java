// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import kotlin.Pair;
import jakarta.annotation.Nonnull;

/**
 * Represents a range in a diff identified by start and end {@link DiffLineLocation}s.
 * Replacement for the Kotlin typealias {@code DiffLineRange = Pair<DiffLineLocation, DiffLineLocation>}.
 */
public final class DiffLineRange extends Pair<@Nonnull DiffLineLocation, @Nonnull DiffLineLocation> {

  public DiffLineRange(@Nonnull DiffLineLocation start, @Nonnull DiffLineLocation end) {
    super(start, end);
  }

  public @Nonnull DiffLineLocation getStart() {
    return getFirst();
  }

  public @Nonnull DiffLineLocation getEnd() {
    return getSecond();
  }
}
