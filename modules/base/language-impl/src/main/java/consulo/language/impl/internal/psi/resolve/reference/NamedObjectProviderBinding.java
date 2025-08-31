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
import consulo.util.collection.Maps;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author maxim
 */
public abstract class NamedObjectProviderBinding<Provider> implements ProviderBinding<Provider> {
  private final ConcurrentMap<String, List<ProviderInfo<Provider, ElementPattern>>> myNamesToProvidersMap = new ConcurrentHashMap<>(5);
  private final ConcurrentMap<String, List<ProviderInfo<Provider, ElementPattern>>> myNamesToProvidersMapInsensitive =
    new ConcurrentHashMap<>(5);

  public void registerProvider(@Nonnull String[] names,
                               @Nonnull ElementPattern filter,
                               boolean caseSensitive,
                               @Nonnull Provider provider,
                               double priority) {
    ConcurrentMap<String, List<ProviderInfo<Provider, ElementPattern>>> map =
      caseSensitive ? myNamesToProvidersMap : myNamesToProvidersMapInsensitive;

    for (String attributeName : names) {
      List<ProviderInfo<Provider, ElementPattern>> psiReferenceProviders = map.get(attributeName);

      if (psiReferenceProviders == null) {
        String key = caseSensitive ? attributeName : attributeName.toLowerCase();
        psiReferenceProviders = Maps.cacheOrGet(map, key, Lists.newLockFreeCopyOnWriteList());
      }

      psiReferenceProviders.add(new ProviderInfo<>(provider, filter, priority));
    }
  }

  @Override
  public void addAcceptableReferenceProviders(@Nonnull PsiElement position,
                                              @Nonnull List<ProviderInfo<Provider, ProcessingContext>> list,
                                              @Nonnull PsiReferenceService.Hints hints) {
    String name = getName(position);
    if (name != null) {
      addMatchingProviders(position, myNamesToProvidersMap.get(name), list, hints);
      addMatchingProviders(position, myNamesToProvidersMapInsensitive.get(name.toLowerCase()), list, hints);
    }
  }

  @Override
  public void unregisterProvider(@Nonnull Provider provider) {
    for (List<ProviderInfo<Provider, ElementPattern>> list : myNamesToProvidersMap.values()) {
      for (ProviderInfo<Provider, ElementPattern> trinity : new ArrayList<>(list)) {
        if (trinity.provider.equals(provider)) {
          list.remove(trinity);
        }
      }
    }
    for (List<ProviderInfo<Provider, ElementPattern>> list : myNamesToProvidersMapInsensitive.values()) {
      for (ProviderInfo<Provider, ElementPattern> trinity : new ArrayList<>(list)) {
        if (trinity.provider.equals(provider)) {
          list.remove(trinity);
        }
      }
    }
  }

  @Nullable
  protected abstract String getName(PsiElement position);

  private void addMatchingProviders(PsiElement position,
                                    @Nullable List<ProviderInfo<Provider, ElementPattern>> providerList,
                                    @Nonnull List<ProviderInfo<Provider, ProcessingContext>> ret,
                                    PsiReferenceService.Hints hints) {
    if (providerList == null) return;

    for (ProviderInfo<Provider, ElementPattern> trinity : providerList) {
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
        ret.add(new ProviderInfo<>(trinity.provider, context, trinity.priority));
      }
    }
  }
}
