// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import jakarta.annotation.Nonnull;

public interface ReviewListQuickFilter<S extends ReviewListSearchValue> {
    @Nonnull
    S getFilter();
}
