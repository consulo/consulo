// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface AccountDetails {
    @Nonnull
    String getName();

    @Nullable
    String getAvatarUrl();
}
