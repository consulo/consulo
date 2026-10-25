// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import org.jspecify.annotations.Nullable;

import java.util.EventListener;

public interface MutableDiffRequestProcessorUpdateListener extends EventListener {
    default void onReloadRequested() {
    }

    default void onUpdateRequested(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
    }
}
