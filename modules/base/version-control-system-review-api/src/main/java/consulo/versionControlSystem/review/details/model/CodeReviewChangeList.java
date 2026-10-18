// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.details.model;

import consulo.versionControlSystem.change.RefComparisonChange;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record CodeReviewChangeList(@Nullable String commitSha, List<RefComparisonChange> changes) {
}
