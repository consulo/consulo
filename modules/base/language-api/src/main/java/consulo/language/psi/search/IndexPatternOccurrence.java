// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;

import java.util.Collections;
import java.util.List;

/**
 * Represents the occurrence of an index pattern in the comments of a source code file.
 *
 * @author yole
 * @see IndexPatternSearch
 * @see IndexPatternProvider
 */
public interface IndexPatternOccurrence {
  /**
   * Returns the file in which the occurrence was found.
   *
   * @return the file in which the occurrence was found.
   */
  PsiFile getFile();

  /**
   * Returns the text range which was matched by the pattern.
   *
   * @return the text range which was matched by the pattern.
   */
  TextRange getTextRange();

  /**
   * Additional ranges associated with matched range (e.g. for multi-line matching)
   */
  default List<TextRange> getAdditionalTextRanges() {
    return Collections.emptyList();
  }

  /**
   * Returns the instance of the pattern which was matched.
   *
   * @return the instance of the pattern which was matched.
   */
  IndexPattern getPattern();
}
