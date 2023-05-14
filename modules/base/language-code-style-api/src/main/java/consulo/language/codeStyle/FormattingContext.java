// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a context of current formatting operation
 */
public class FormattingContext {
  @Nonnull
  private final PsiElement myPsiElement;
  @Nonnull
  private final TextRange myFormattingRange;
  @Nonnull
  private final CodeStyleSettings myCodeStyleSettings;
  @Nonnull
  private final FormattingMode myFormattingMode;

  private FormattingContext(@Nonnull PsiElement psiElement, @Nonnull TextRange formattingRange, @Nonnull CodeStyleSettings codeStyleSettings, @Nonnull FormattingMode formattingMode) {
    myPsiElement = psiElement;
    myFormattingRange = formattingRange;
    myCodeStyleSettings = codeStyleSettings;
    myFormattingMode = formattingMode;
  }

  public
  @Nonnull
  FormattingContext withPsiElement(@Nonnull PsiElement psiElement) {
    // fixme should we overwrite range here?
    return new FormattingContext(psiElement, myFormattingRange, myCodeStyleSettings, myFormattingMode);
  }

  public
  @Nonnull
  PsiFile getContainingFile() {
    return Objects.requireNonNull(myPsiElement.getContainingFile());
  }

  public
  @Nonnull
  ASTNode getNode() {
    return myPsiElement.getNode();
  }

  public
  @Nonnull
  Project getProject() {
    return myPsiElement.getProject();
  }

  /**
   * @return element being formatted
   */
  public
  @Nonnull
  PsiElement getPsiElement() {
    return myPsiElement;
  }

  /**
   * @return range being formatted. When text is selected in editor, or auto-formatting  performed after some Psi change, returns respective
   * range: selection or psi element. When this is an offset-based formatting, like indentation or spacing computation at offset, returns
   * empty range {@code (offset, offset)}
   * @apiNote returned range is relative to the containing {@link #getContainingFile() file}, not the {@link #getPsiElement() psiElement}
   */
  public
  @Nonnull
  TextRange getFormattingRange() {
    return myFormattingRange;
  }

  public
  @Nonnull
  CodeStyleSettings getCodeStyleSettings() {
    return myCodeStyleSettings;
  }

  /**
   * @return {@link FormattingMode type} of formatting operation performed
   */
  public
  @Nonnull
  FormattingMode getFormattingMode() {
    return myFormattingMode;
  }

  @Override
  public String toString() {
    return "FormattingContext{" +
           "myPsiElement=" +
           myPsiElement +
           ", myFormattingRange=" +
           myFormattingRange +
           ", myCodeStyleSettings=" +
           myCodeStyleSettings +
           ", myFormattingMode=" +
           myFormattingMode +
           '}';
  }

  public static
  @Nonnull
  FormattingContext create(@Nonnull PsiElement psiElement, @Nonnull TextRange formattingRange, @Nonnull CodeStyleSettings codeStyleSettings, @Nonnull FormattingMode formattingMode) {
    return new FormattingContext(psiElement, formattingRange, codeStyleSettings, formattingMode);
  }

  /**
   * @return formatting context for the full-range of {@code psiElement}
   */
  public static
  @Nonnull
  FormattingContext create(@Nonnull PsiElement psiElement, @Nonnull CodeStyleSettings codeStyleSettings, @Nonnull FormattingMode formattingMode) {
    return new FormattingContext(psiElement, psiElement.getTextRange(), codeStyleSettings, formattingMode);
  }

  /**
   * @return formatting context for {@link FormattingMode#REFORMAT re-formatting} of the full range of {@code psiElement}
   */
  public static
  @Nonnull
  FormattingContext create(@Nonnull PsiElement psiElement, @Nonnull CodeStyleSettings codeStyleSettings) {
    return new FormattingContext(psiElement, psiElement.getTextRange(), codeStyleSettings, FormattingMode.REFORMAT);
  }
}
