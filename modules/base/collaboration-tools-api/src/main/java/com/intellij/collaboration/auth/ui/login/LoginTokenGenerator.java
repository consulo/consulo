// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth.ui.login;

import jakarta.annotation.Nonnull;

public interface LoginTokenGenerator {
    boolean canGenerateToken(@Nonnull String serverUri);

    void generateToken(@Nonnull String serverUri);
}
