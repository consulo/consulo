// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inspection;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoType;

import jakarta.annotation.Nonnull;

public enum ProblemHighlightType {

  /**
   * Underlying highlighting with color depending on the inspection {@link HighlightDisplayLevel}.
   */
  GENERIC_ERROR_OR_WARNING,

  /**
   * Changes font color depending on the inspection {@link HighlightDisplayLevel}.
   */
  LIKE_UNKNOWN_SYMBOL,

  LIKE_DEPRECATED,

  LIKE_UNUSED_SYMBOL,

  /**
   * The same as {@link #LIKE_UNKNOWN_SYMBOL} with enforced {@link HighlightDisplayLevel#ERROR} severity level.
   */
  ERROR,

  /**
   * Enforces {@link HighlightDisplayLevel#WARNING} severity level.
   */
  WARNING,

  /**
   * The same as {@link #GENERIC_ERROR_OR_WARNING} with enforced {@link HighlightDisplayLevel#ERROR} severity level.
   */
  GENERIC_ERROR,

  /**
   * Enforces {@link HighlightDisplayLevel#INFO} severity level.
   *
   * @deprecated use {@link #WEAK_WARNING} instead
   */
  @Deprecated INFO,

  /**
   * Enforces {@link HighlightDisplayLevel#WEAK_WARNING} severity level.
   */
  WEAK_WARNING,

  /**
   * Enforces {@link HighlightDisplayLevel#DO_NOT_SHOW} severity level.
   * Please ensure that if used from inspection explicitly, corresponding problem is added in {@code onTheFly} mode only.
   */
  INFORMATION,

  /**
   * JEP 277 enhanced deprecation.
   */
  LIKE_MARKED_FOR_REMOVAL;

  @Nonnull
  public static ProblemHighlightType from(@Nonnull HighlightInfoType infoType) {
    if (infoType == HighlightInfoType.ERROR || infoType == HighlightInfoType.WRONG_REF) return ProblemHighlightType.ERROR;
    if (infoType == HighlightInfoType.WARNING) return ProblemHighlightType.WARNING;
    if (infoType == HighlightInfoType.INFORMATION) return ProblemHighlightType.INFORMATION;
    return ProblemHighlightType.WEAK_WARNING;
  }

  @Nonnull
  @SuppressWarnings("deprecation")
  public static ProblemHighlightType from(@Nonnull HighlightSeverity severity) {
    if (severity == HighlightSeverity.ERROR) {
      return ProblemHighlightType.ERROR;
    }
    else if (severity == HighlightSeverity.WARNING) {
      return ProblemHighlightType.WARNING;
    }
    else if (severity == HighlightSeverity.INFO) {
      return ProblemHighlightType.INFO;
    }
    else if (severity == HighlightSeverity.WEAK_WARNING) {
      return ProblemHighlightType.WEAK_WARNING;
    }
    else {
      return ProblemHighlightType.INFORMATION;
    }
  }
}
