// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api;

import jakarta.annotation.Nonnull;

import java.net.URI;

public interface ServerPath {
    @Nonnull
    URI toURI();

    @Override
    @Nonnull
    String toString();
}
