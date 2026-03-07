// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nonnull;

import java.util.List;

public interface GraphQLPagedResponseDataDTO<T> {
    @Nonnull
    GraphQLCursorPageInfoDTO getPageInfo();

    @Nonnull
    List<T> getNodes();
}
