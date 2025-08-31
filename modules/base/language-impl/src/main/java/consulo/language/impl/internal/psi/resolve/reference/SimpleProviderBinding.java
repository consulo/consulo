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

package consulo.language.impl.internal.psi.resolve.reference;

import consulo.application.dumb.IndexNotReadyException;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceService;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.Lists;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ik
 * @since 2003-04-01
 */
public class SimpleProviderBinding<Provider> implements ProviderBinding<Provider> {
  private final List<ProviderInfo<Provider, ElementPattern>> myProviderPairs = Lists.newLockFreeCopyOnWriteList();

  public void registerProvider(Provider provider, ElementPattern pattern, double priority) {
    myProviderPairs.add(new ProviderInfo<>(provider, pattern, priority));
  }

  @Override
  public void addAcceptableReferenceProviders(@Nonnull PsiElement position,
                                              @Nonnull List<ProviderInfo<Provider, ProcessingContext>> list,
                                              @Nonnull PsiReferenceService.Hints hints) {
    for (ProviderInfo<Provider, ElementPattern> trinity : myProviderPairs) {
      if (hints != PsiReferenceService.Hints.NO_HINTS && !((PsiReferenceProvider)trinity.provider).acceptsHints(position, hints)) {
        continue;
      }

      ProcessingContext context = new ProcessingContext();
      if (hints != PsiReferenceService.Hints.NO_HINTS) {
        context.put(PsiReferenceService.HINTS, hints);
      }
      boolean suitable = false;
      try {
        suitable = trinity.processingContext.accepts(position, context);
      }
      catch (IndexNotReadyException ignored) {
      }
      if (suitable) {
        list.add(new ProviderInfo<>(trinity.provider, context, trinity.priority));
      }
    }
  }

  @Override
  public void unregisterProvider(@Nonnull Provider provider) {
    for (ProviderInfo<Provider, ElementPattern> trinity : new ArrayList<>(myProviderPairs)) {
      if (trinity.provider.equals(provider)) {
        myProviderPairs.remove(trinity);
      }
    }
  }
}
