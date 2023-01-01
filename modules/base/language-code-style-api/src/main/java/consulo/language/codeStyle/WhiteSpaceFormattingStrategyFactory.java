/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.document.Document;
import consulo.language.Language;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.ref.PatchedWeakReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains utility methods for working with {@link WhiteSpaceFormattingStrategy}.
 *
 * @author Denis Zhdanov
 * @since 10/1/10 3:31 PM
 */
public class WhiteSpaceFormattingStrategyFactory {

  private static final AtomicReference<PatchedWeakReference<Collection<WhiteSpaceFormattingStrategy>>> myCachedStrategies = new AtomicReference<>();

  private WhiteSpaceFormattingStrategyFactory() {
  }

  private static List<WhiteSpaceFormattingStrategy> getSharedStrategies(@Nonnull Language language) {
    return List.of(new StaticSymbolWhiteSpaceDefinitionStrategy(' ', '\t', '\n') {
      @Nonnull
      @Override
      public Language getLanguage() {
        return language;
      }
    });
  }

  /**
   * @return default language-agnostic white space strategy
   */
  public static WhiteSpaceFormattingStrategy getStrategy() {
    return new CompositeWhiteSpaceFormattingStrategy(Language.ANY, getSharedStrategies(Language.ANY));
  }

  /**
   * Tries to return white space strategy to use for the given language.
   *
   * @param language target language
   * @return white space strategy to use for the given language
   * @throws IllegalStateException if white space strategies configuration is invalid
   */
  public static WhiteSpaceFormattingStrategy getStrategy(@Nonnull Language language) throws IllegalStateException {
    CompositeWhiteSpaceFormattingStrategy result = new CompositeWhiteSpaceFormattingStrategy(language, getSharedStrategies(language));
    WhiteSpaceFormattingStrategy strategy = WhiteSpaceFormattingStrategy.forLanguage(language);
    if (strategy != null) {
      result.addStrategy(strategy);
    }
    return result;
  }

  /**
   * @return collection of all registered white space strategies
   */
  @Nonnull
  public static Collection<WhiteSpaceFormattingStrategy> getAllStrategies() {
    final WeakReference<Collection<WhiteSpaceFormattingStrategy>> reference = myCachedStrategies.get();
    if (reference != null) {
      final Collection<WhiteSpaceFormattingStrategy> strategies = reference.get();
      if (strategies != null) {
        return strategies;
      }
    }
    final Collection<Language> languages = Language.getRegisteredLanguages();

    Set<WhiteSpaceFormattingStrategy> result = new HashSet<>(getSharedStrategies(Language.ANY));
    for (Language language : languages) {
      final WhiteSpaceFormattingStrategy strategy = WhiteSpaceFormattingStrategy.forLanguage(language);
      if (strategy != null) {
        result.add(strategy);
      }
    }
    myCachedStrategies.set(new PatchedWeakReference<>(result));
    return result;
  }

  /**
   * Returns white space strategy to use for the document managed by the given editor.
   *
   * @param editor editor that manages target document
   * @return white space strategy for the document managed by the given editor
   * @throws IllegalStateException if white space strategies configuration is invalid
   */
  public static WhiteSpaceFormattingStrategy getStrategy(@Nullable Project project, @Nullable Document document) throws IllegalStateException {
    if (project != null) {
      PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(Objects.requireNonNull(document));
      if (psiFile != null) {
        return getStrategy(psiFile.getLanguage());
      }
    }
    return getStrategy();
  }
}
