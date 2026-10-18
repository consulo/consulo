// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.details.data;

import org.jspecify.annotations.Nullable;

public record CodeReviewCIJob(String name, CodeReviewCIJobState status, boolean isRequired, @Nullable String detailsUrl) {
}
