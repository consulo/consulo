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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.Language;
import consulo.language.util.ProcessingContext;

import jakarta.annotation.Nonnull;

/**
 * @author ik
 * @since 2003-03-27
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ReferenceProvidersRegistry {
  public final static PsiReferenceProvider NULL_REFERENCE_PROVIDER = new PsiReferenceProvider() {
      @Nonnull
      @Override
      public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        return PsiReference.EMPTY_ARRAY;
      }
    };

  public static ReferenceProvidersRegistry getInstance() {
    return Application.get().getInstance(ReferenceProvidersRegistry.class);
  }

  public abstract PsiReferenceRegistrar getRegistrar(Language language);

  /**
   * @see #getReferencesFromProviders(PsiElement)
   */
  @Deprecated
  public static PsiReference[] getReferencesFromProviders(PsiElement context, @Nonnull Class clazz) {
    return getReferencesFromProviders(context, PsiReferenceService.Hints.NO_HINTS);
  }

  public static PsiReference[] getReferencesFromProviders(PsiElement context) {
    return getReferencesFromProviders(context, PsiReferenceService.Hints.NO_HINTS);
  }

  public static PsiReference[] getReferencesFromProviders(PsiElement context, @Nonnull PsiReferenceService.Hints hints) {
    ProgressIndicatorProvider.checkCanceled();
    assert context.isValid() : "Invalid context: " + context;

    ReferenceProvidersRegistry registry = getInstance();
    return registry.doGetReferencesFromProviders(context, hints);
  }

  public abstract PsiReference[] doGetReferencesFromProviders(PsiElement context, PsiReferenceService.Hints hints);
}
