// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

public interface DiffMapped {
    ObservableValue<@Nullable DiffLineLocation> getLocation();

    ObservableValue<Boolean> isVisible();
}
