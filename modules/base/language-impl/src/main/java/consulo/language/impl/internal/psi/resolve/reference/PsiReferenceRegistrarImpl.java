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
package consulo.language.impl.internal.psi.resolve.reference;

import consulo.application.util.ConcurrentFactoryMap;
import consulo.language.pattern.*;
import consulo.language.psi.*;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.util.ProcessingContext;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Maps;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Dmitry Avdeev
 */
public class PsiReferenceRegistrarImpl extends PsiReferenceRegistrar {
  private final ConcurrentMap<Class, SimpleProviderBinding<PsiReferenceProvider>> myBindingsMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<Class, NamedObjectProviderBinding<PsiReferenceProvider>> myNamedBindingsMap = new ConcurrentHashMap<>();
  private final ConcurrentMap<Class, Class[]> myKnownSupers = ConcurrentFactoryMap.createMap(key -> {
    Set<Class> result = new LinkedHashSet<Class>();
    for (Class candidate : myBindingsMap.keySet()) {
      if (candidate.isAssignableFrom(key)) {
        result.add(candidate);
      }
    }
    for (Class candidate : myNamedBindingsMap.keySet()) {
      if (candidate.isAssignableFrom(key)) {
        result.add(candidate);
      }
    }
    if (result.isEmpty()) {
      return ArrayUtil.EMPTY_CLASS_ARRAY;
    }
    return result.toArray(new Class[result.size()]);
  });

  @Override
  public <T extends PsiElement> void registerReferenceProvider(@Nonnull ElementPattern<T> pattern,
                                                               @Nonnull PsiReferenceProvider provider,
                                                               double priority) {
    myKnownSupers.clear(); // we should clear the cache
    Class scope = pattern.getCondition().getInitialCondition().getAcceptedClass();
    List<PatternCondition<? super T>> conditions = pattern.getCondition().getConditions();
    for (PatternCondition<? super T> _condition : conditions) {
      if (!(_condition instanceof PsiNamePatternCondition)) {
        continue;
      }
      PsiNamePatternCondition<?> nameCondition = (PsiNamePatternCondition)_condition;
      List<PatternCondition<? super String>> conditions1 = nameCondition.getNamePattern().getCondition().getConditions();
      for (PatternCondition<? super String> condition1 : conditions1) {
        if (condition1 instanceof ValuePatternCondition) {
          Collection<String> strings = ((ValuePatternCondition)condition1).getValues();
          registerNamedReferenceProvider(ArrayUtil.toStringArray(strings), nameCondition, scope, true, provider, priority, pattern);
          return;
        }
        if (condition1 instanceof CaseInsensitiveValuePatternCondition) {
          String[] strings = ((CaseInsensitiveValuePatternCondition)condition1).getValues();
          registerNamedReferenceProvider(strings, nameCondition, scope, false, provider, priority, pattern);
          return;
        }
      }
      break;
    }

    while (true) {
      SimpleProviderBinding<PsiReferenceProvider> providerBinding = myBindingsMap.get(scope);
      if (providerBinding != null) {
        providerBinding.registerProvider(provider, pattern, priority);
        return;
      }

      SimpleProviderBinding<PsiReferenceProvider> binding = new SimpleProviderBinding<PsiReferenceProvider>();
      binding.registerProvider(provider, pattern, priority);
      if (myBindingsMap.putIfAbsent(scope, binding) == null) break;
    }
  }

  private void registerNamedReferenceProvider(@Nonnull String[] names,
                                              final PsiNamePatternCondition<?> nameCondition,
                                              @Nonnull Class scopeClass,
                                              boolean caseSensitive,
                                              @Nonnull PsiReferenceProvider provider,
                                              double priority,
                                              @Nonnull ElementPattern pattern) {
    NamedObjectProviderBinding<PsiReferenceProvider> providerBinding = myNamedBindingsMap.get(scopeClass);

    if (providerBinding == null) {
      providerBinding = Maps.cacheOrGet(myNamedBindingsMap, scopeClass, new NamedObjectProviderBinding<PsiReferenceProvider>() {
        @Override
        protected String getName(PsiElement position) {
          return nameCondition.getPropertyValue(position);
        }
      });
    }

    providerBinding.registerProvider(names, pattern, caseSensitive, provider, priority);
  }

  @Nonnull
  List<ProviderBinding.ProviderInfo<PsiReferenceProvider,ProcessingContext>> getPairsByElement(@Nonnull PsiElement element,
                                                                                               @Nonnull PsiReferenceService.Hints hints) {
    Class<? extends PsiElement> clazz = element.getClass();
    List<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> ret = null;

    for (Class aClass : myKnownSupers.get(clazz)) {
      SimpleProviderBinding<PsiReferenceProvider> simpleBinding = myBindingsMap.get(aClass);
      NamedObjectProviderBinding<PsiReferenceProvider> namedBinding = myNamedBindingsMap.get(aClass);
      if (simpleBinding == null && namedBinding == null) continue;

      if (ret == null) ret = new SmartList<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>>();
      if (simpleBinding != null) {
        simpleBinding.addAcceptableReferenceProviders(element, ret, hints);
      }
      if (namedBinding != null) {
        namedBinding.addAcceptableReferenceProviders(element, ret, hints);
      }
    }
    return ret == null ? Collections.<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>>emptyList() : ret;
  }
}
