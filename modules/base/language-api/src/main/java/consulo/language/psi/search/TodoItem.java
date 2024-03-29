// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public interface TodoItem {
  @Nonnull
  PsiFile getFile();

  @Nonnull
  TextRange getTextRange();

  @Nonnull
  TodoPattern getPattern();

  @Nonnull
  default List<TextRange> getAdditionalTextRanges() {
    return Collections.emptyList();
  }
}
