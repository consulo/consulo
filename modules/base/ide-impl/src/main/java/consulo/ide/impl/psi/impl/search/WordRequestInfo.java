// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.search;

import consulo.content.scope.SearchScope;
import consulo.language.psi.search.SearchSession;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
