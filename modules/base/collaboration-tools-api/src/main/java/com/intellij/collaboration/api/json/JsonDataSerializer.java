// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.json;

import jakarta.annotation.Nonnull;

@ApiStatus.Experimental
public interface JsonDataSerializer {
    @Nonnull
    byte[] toJsonBytes(@Nonnull Object content);
}
