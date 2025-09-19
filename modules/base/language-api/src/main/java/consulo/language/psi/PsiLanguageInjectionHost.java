// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Marks PSI element as (potentially) containing text in other language.
 * Injected language PSI does not embed into the PSI tree of the hosting element,
 * but is used by the IDE for highlighting, completion and other code insight actions.
 * <p>
 * In order to do the injection, you have to
 * <ul>
 * <li>Implement {@link consulo.language.inject.LanguageInjector} to describe exact place where injection should occur.</li>
 * <li>Register injection in {@link com.intellij.psi.LanguageInjector#EXTENSION_POINT_NAME} extension point.</li>
 * </ul>
 * Currently, language can be injected into string literals, XML tag contents and XML attributes.
 * <p>
 * You don't have to implement {@code PsiLanguageInjectionHost} by yourself, unless you want to inject something into your own custom PSI.
 * For all returned injected PSI elements, {@link InjectedLanguageManager#getInjectionHost(PsiElement)} returns {@code PsiLanguageInjectionHost} they were injected into.
 */
public interface PsiLanguageInjectionHost extends PsiElement {
  /**
   * @return {@code true} if this instance can accept injections, {@code false} otherwise
   */
  boolean isValidHost();

  /**
   * Update the host element using the provided text of the injected file. It may be required to escape characters from {@code text}
   * in accordance with the host language syntax. The implementation may delegate to {@link ElementManipulators#handleContentChange(PsiElement, String)}
   * if {@link ElementManipulator} implementation is registered for this element class.
   *
   * @param text text of the injected file
   * @return the updated instance
   */
  PsiLanguageInjectionHost updateText(@Nonnull String text);

  /**
   * @return {@link LiteralTextEscaper} instance which will be used to convert the content of this host element to the content of injected file
   */
  @Nonnull
  LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper();


  @FunctionalInterface
  interface InjectedPsiVisitor {
    @RequiredReadAction
    void visit(@Nonnull PsiFile injectedPsi, @Nonnull List<Shred> places);
  }

  interface Place extends List<Shred> {
    @Nonnull
    SmartPsiElementPointer<PsiLanguageInjectionHost> getHostPointer();
  }

  interface Shred {
    /**
     * @return returns null when the host document marker is invalid
     */
    @Nullable
    Segment getHostRangeMarker();

    @Nonnull
    TextRange getRangeInsideHost();

    boolean isValid();

    void dispose();

    @Nullable
    PsiLanguageInjectionHost getHost();

    /**
     * @return range in decoded PSI
     */
    @Nonnull
    TextRange getRange();

    @Nonnull
    String getPrefix();

    @Nonnull
    String getSuffix();
  }
}
