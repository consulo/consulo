/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.impl.internal.psi.resolve.reference;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.psi.*;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Arrays;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
@Singleton
@ServiceImpl
public class PsiReferenceServiceImpl extends PsiReferenceService {
  private final ReferenceProvidersRegistry myReferenceProvidersRegistry;

  @Inject
  public PsiReferenceServiceImpl(ReferenceProvidersRegistry referenceProvidersRegistry) {
    myReferenceProvidersRegistry = referenceProvidersRegistry;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public List<PsiReference> getReferences(@Nonnull PsiElement element, @Nonnull Hints hints) {
    if (element instanceof ContributedReferenceHost) {
      return Arrays.asList(getReferencesFromProviders(element, hints));
    }
    if (element instanceof HintedReferenceHost) {
      return Arrays.asList(((HintedReferenceHost)element).getReferences(hints));
    }
    return Arrays.asList(element.getReferences());
  }

  private PsiReference[] getReferencesFromProviders(PsiElement context, @Nonnull PsiReferenceService.Hints hints) {
    ProgressIndicatorProvider.checkCanceled();
    assert context.isValid() : "Invalid context: " + context;
    return myReferenceProvidersRegistry.doGetReferencesFromProviders(context, hints);
  }
}
