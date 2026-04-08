// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.graphql;

import jakarta.annotation.Nonnull;

import java.io.IOException;

public interface GraphQLQueryLoader {
    @Nonnull
    String loadQuery(@Nonnull String queryPath) throws IOException;
}
