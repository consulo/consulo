// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.graphql;

import jakarta.annotation.Nonnull;

import java.io.FileNotFoundException;

public final class GraphQLFileNotFoundException extends FileNotFoundException {
    public GraphQLFileNotFoundException(@Nonnull String message) {
        super(message);
    }
}
