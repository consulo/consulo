// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection;

public enum ProblemHighlightType {

  /**
   * Underlying highlighting with color depending on the inspection {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel}.
   */
  GENERIC_ERROR_OR_WARNING,

  /**
   * Changes font color depending on the inspection {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel}.
   */
  LIKE_UNKNOWN_SYMBOL,

  LIKE_DEPRECATED,

  LIKE_UNUSED_SYMBOL,

  /**
   * The same as {@link #LIKE_UNKNOWN_SYMBOL} with enforced {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#ERROR} severity level.
   */
  ERROR,

  /**
   * Enforces {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#WARNING} severity level.
   */
  WARNING,

  /**
   * The same as {@link #GENERIC_ERROR_OR_WARNING} with enforced {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#ERROR} severity level.
   */
  GENERIC_ERROR,

  /**
   * Enforces {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#INFO} severity level.
   *
   * @deprecated use {@link #WEAK_WARNING} instead
   */
  @Deprecated INFO,

  /**
   * Enforces {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#WEAK_WARNING} severity level.
   */
  WEAK_WARNING,

  /**
   * Enforces {@link consulo.ide.impl.idea.codeHighlighting.HighlightDisplayLevel#DO_NOT_SHOW} severity level.
   * Please ensure that if used from inspection explicitly, corresponding problem is added in {@code onTheFly} mode only.
   */
  INFORMATION,

  /**
   * JEP 277 enhanced deprecation.
   */
  LIKE_MARKED_FOR_REMOVAL
}
