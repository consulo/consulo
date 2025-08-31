// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.internal;

import consulo.document.util.TextRange;
import consulo.language.codeStyle.*;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class FormatterEx implements Formatter {
  public abstract void format(FormattingModel model, CodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions indentOptions, FormatTextRanges affectedRanges)
          throws IncorrectOperationException;

  public abstract int adjustLineIndent(FormattingModel psiBasedFormattingModel,
                                       CodeStyleSettings settings,
                                       CommonCodeStyleSettings.IndentOptions indentOptions,
                                       int offset,
                                       TextRange affectedRange) throws IncorrectOperationException;

  @Nullable
  public abstract String getLineIndent(FormattingModel psiBasedFormattingModel,
                                       CodeStyleSettings settings,
                                       CommonCodeStyleSettings.IndentOptions indentOptions,
                                       int offset,
                                       TextRange affectedRange);

  public abstract boolean isDisabled();


  public abstract void adjustLineIndentsForRange(FormattingModel model,
                                                 CodeStyleSettings settings,
                                                 CommonCodeStyleSettings.IndentOptions indentOptions,
                                                 TextRange rangeToAdjust);

  public abstract void formatAroundRange(FormattingModel model, CodeStyleSettings settings, PsiFile file, TextRange textRange);

  public abstract void setProgressTask(@Nonnull FormattingProgressCallback progressIndicator);

  /**
   * Calculates minimum spacing, allowed by formatting model (in columns) for a block starting at given offset,
   * relative to its previous sibling block.
   * Returns {@code -1}, if required block cannot be found at provided offset,
   * or spacing cannot be calculated due to some other reason.
   */
  public abstract int getSpacingForBlockAtOffset(FormattingModel model, int offset);

  /**
   * Calculates minimum number of line feeds that should precede block starting at given offset, as dictated by formatting model.
   * Returns {@code -1}, if required block cannot be found at provided offset,
   * or spacing cannot be calculated due to some other reason.
   */
  public abstract int getMinLineFeedsBeforeBlockAtOffset(FormattingModel model, int offset);


  public static FormatterEx getInstanceEx() {
    return (FormatterEx)Formatter.getInstance();
  }
}
