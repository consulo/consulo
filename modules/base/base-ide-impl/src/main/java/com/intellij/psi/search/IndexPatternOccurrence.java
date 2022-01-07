// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Represents the occurrence of an index pattern in the comments of a source code file.
 *
 * @author yole
 * @see com.intellij.psi.search.searches.IndexPatternSearch
 * @see IndexPatternProvider
 */
public interface IndexPatternOccurrence {
  /**
   * Returns the file in which the occurrence was found.
   *
   * @return the file in which the occurrence was found.
   */
  @Nonnull
  PsiFile getFile();

  /**
   * Returns the text range which was matched by the pattern.
   *
   * @return the text range which was matched by the pattern.
   */
  @Nonnull
  TextRange getTextRange();

  /**
   * Additional ranges associated with matched range (e.g. for multi-line matching)
   */
  @Nonnull
  default List<TextRange> getAdditionalTextRanges() {
    return Collections.emptyList();
  }

  /**
   * Returns the instance of the pattern which was matched.
   *
   * @return the instance of the pattern which was matched.
   */
  @Nonnull
  IndexPattern getPattern();
}
