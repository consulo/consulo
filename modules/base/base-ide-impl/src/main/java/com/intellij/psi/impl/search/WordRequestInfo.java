// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.search;

import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.SearchSession;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

interface WordRequestInfo {

  @Nonnull
  String getWord();

  @Nonnull
  SearchScope getSearchScope();

  @Nullable
  String getContainerName();

  short getSearchContext();

  boolean isCaseSensitive();

  @Nonnull
  SearchSession getSearchSession();
}
