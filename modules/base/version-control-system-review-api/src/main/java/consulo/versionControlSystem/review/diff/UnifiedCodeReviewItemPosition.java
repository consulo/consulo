// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.versionControlSystem.change.RefComparisonChange;

/**
 * This class indicates the position of an object inside a diff as a single ordered number.
 * In practice, the line number is always the right-side mapping of the location.
 * If a left-side location has been removed, the right-side mapping is estimated to be the first line above the changed section.
 * <p>
 * Line numbers are supposed to be 0-indexed.
 */
public record UnifiedCodeReviewItemPosition(RefComparisonChange change, int leftLine, int rightLine) {
}
