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

import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;

/**
 * Allows to inject additional references into an element that supports reference contributors.
 * Register it via {@link PsiReferenceContributor} or {@link PsiReferenceProviderByPattern}
 * <p>
 * Note that, if you're implementing a custom language, it won't by default support references registered through PsiReferenceContributor.
 * If you want to support that, you need to call
 * {@link ReferenceProvidersRegistry#getReferencesFromProviders(PsiElement)} from your implementation
 * of PsiElement.getReferences().
 *
 * @author ik
 */
public abstract class PsiReferenceProvider {
  public static final PsiReferenceProvider[] EMPTY_ARRAY = new PsiReferenceProvider[0];

  @Nonnull
  public abstract PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context);

  public boolean acceptsHints(@Nonnull PsiElement element, @Nonnull PsiReferenceService.Hints hints) {
    PsiElement target = hints.target;
    return target == null || acceptsTarget(target);
  }

  public boolean acceptsTarget(@Nonnull PsiElement target) {
    return true;
  }
}
