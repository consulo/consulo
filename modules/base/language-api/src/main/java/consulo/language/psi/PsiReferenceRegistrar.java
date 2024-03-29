/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.psi;

import consulo.language.pattern.ElementPattern;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;

/**
 * Allows to register reference providers for specific locations. The locations are described by
 * {@link consulo.ide.impl.idea.patterns.ElementPattern}s. If a pattern matches some PSI element, then the corresponding
 * {@link PsiReferenceProvider#getReferencesByElement(PsiElement, ProcessingContext)} is executed, from
 * which one can return the references whose {@link PsiReference#getElement()} is the same as the first parameter of
 * {@link PsiReferenceProvider#getReferencesByElement(PsiElement, ProcessingContext)}.
 *
 * @author peter
 */
public abstract class PsiReferenceRegistrar {
  public static final double DEFAULT_PRIORITY = 0.0;
  public static final double HIGHER_PRIORITY = 100.0;
  public static final double LOWER_PRIORITY = -100.0;

  /**
   * Register reference provider with default priority ({@link #DEFAULT_PRIORITY})
   * @param pattern reference place description. See {@link consulo.ide.impl.idea.patterns.StandardPatterns}, {@link consulo.ide.impl.idea.patterns.PlatformPatterns} and their extenders
   * @param provider provider to be registered
   */
  public void registerReferenceProvider(@Nonnull ElementPattern<? extends PsiElement> pattern, @Nonnull PsiReferenceProvider provider) {
    registerReferenceProvider(pattern, provider, DEFAULT_PRIORITY);
  }


  /**
   * Register reference provider
   * @param pattern reference place description. See {@link consulo.ide.impl.idea.patterns.StandardPatterns}, {@link consulo.ide.impl.idea.patterns.PlatformPatterns} and their extenders
   * @param provider provider to be registered
   * @param priority @see DEFAULT_PRIORITY, HIGHER_PRIORITY, LOWER_PRIORITY
   */
  public abstract <T extends PsiElement> void registerReferenceProvider(@Nonnull ElementPattern<T> pattern, @Nonnull PsiReferenceProvider provider, double priority);
}
