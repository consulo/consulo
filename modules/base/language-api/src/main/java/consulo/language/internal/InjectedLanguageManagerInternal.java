/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-09-19
 */
public interface InjectedLanguageManagerInternal extends InjectedLanguageManager {
    DocumentWindow getDocumentWindow(@Nonnull PsiElement element);

    void processInjectableElements(@Nonnull Collection<? extends PsiElement> in, @Nonnull Predicate<? super PsiElement> processor);

    void injectLanguagesFromConcatenationAdapter(@Nonnull MultiHostRegistrar registrar,
                                                 @Nonnull PsiElement context,
                                                 @Nonnull Function<PsiElement, Pair<PsiElement, PsiElement[]>> computeAnchorAndOperandsFunc);

    int hostToInjectedUnescaped(DocumentWindow window, int hostOffset);

    /**
     * Start injecting the reference in {@code language} in this place.
     * Unlike {@link MultiHostRegistrar#startInjecting(Language)} this method doesn't inject the full blown file in the other language.
     * Instead, it just marks some range as a reference in some language.
     * For example, you can inject file reference into string literal.
     * After that, it won't be highlighted as an injected fragment but still can be subject to e.g. "Goto declaraion" action.
     */
    void injectReference(@Nonnull MultiHostRegistrar registrar,
                         @Nonnull Language language,
                         @Nonnull String prefix,
                         @Nonnull String suffix,
                         @Nonnull PsiLanguageInjectionHost host,
                         @Nonnull TextRange rangeInsideHost);

    <T> void putInjectedFileUserData(@Nonnull PsiElement element, @Nonnull Language language, @Nonnull Key<T> key, @Nullable T value);

    // null means failed to reparse
    BooleanSupplier reparse(@Nonnull PsiFile injectedPsiFile,
                            @Nonnull DocumentWindow injectedDocument,
                            @Nonnull PsiFile hostPsiFile,
                            @Nonnull Document hostDocument,
                            @Nonnull FileViewProvider hostViewProvider,
                            @Nonnull ProgressIndicator indicator,
                            @Nonnull ASTNode oldRoot,
                            @Nonnull ASTNode newRoot);

    void disposeInvalidEditors();

    List<InjectedHighlightTokenInfo> getHighlightTokens(@Nonnull PsiFile file);
}
