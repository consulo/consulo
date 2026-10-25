// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.versionControlSystem.review.diff.DiffLineLocation;

public record DiffViewerLineScrollRequest(DiffLineLocation location) implements DiffViewerScrollRequest {
}
