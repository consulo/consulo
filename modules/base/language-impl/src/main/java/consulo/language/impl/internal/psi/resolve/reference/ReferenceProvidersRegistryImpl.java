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

import consulo.annotation.component.ServiceImpl;
import consulo.application.dumb.IndexNotReadyException;
import consulo.language.Language;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.*;
import consulo.language.util.ProcessingContext;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@ServiceImpl
public class ReferenceProvidersRegistryImpl extends ReferenceProvidersRegistry {
    private static final Logger LOG = Logger.getInstance(ReferenceProvidersRegistryImpl.class);

    private static final Comparator<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> PRIORITY_COMPARATOR = (o1, o2) -> Comparing
        .compare(o2.priority, o1.priority);

    private final Map<Language, PsiReferenceRegistrarImpl> myRegistrars = FactoryMap.create(language -> {
        PsiReferenceRegistrarImpl registrar = new PsiReferenceRegistrarImpl();
        for (PsiReferenceContributor contributor : PsiReferenceContributor.forLanguage(language)) {
            try {
                contributor.registerReferenceProviders(registrar);
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        List<PsiReferenceProviderByPattern> referenceProviderBeans = PsiReferenceProviderByPattern.forLanguage(language);
        for (final PsiReferenceProviderByPattern provider : referenceProviderBeans) {
            try {
                final ElementPattern<PsiElement> pattern = provider.getElementPattern();
                registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
                    @Nonnull
                    @Override
                    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
                        return provider.getReferencesByElement(element, context);
                    }
                });
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
        return registrar;
    });

    @Override
    public synchronized PsiReferenceRegistrarImpl getRegistrar(Language language) {
        return myRegistrars.get(language);
    }

    @Override
    public PsiReference[] doGetReferencesFromProviders(PsiElement context, PsiReferenceService.Hints hints) {
        List<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> providersForContextLanguage = getRegistrar(context.getLanguage()).getPairsByElement(context, hints);

        List<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> providersForAllLanguages = getRegistrar(Language.ANY).getPairsByElement(context, hints);

        int providersCount = providersForContextLanguage.size() + providersForAllLanguages.size();

        if (providersCount == 0) {
            return PsiReference.EMPTY_ARRAY;
        }

        if (providersCount == 1) {
            final ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext> firstProvider = (providersForAllLanguages.isEmpty() ? providersForContextLanguage : providersForAllLanguages).get(0);
            return firstProvider.provider.getReferencesByElement(context, firstProvider.processingContext);
        }

        List<ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>> list = ContainerUtil.concat(providersForContextLanguage, providersForAllLanguages);
        @SuppressWarnings("unchecked") ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext>[] providers = list.toArray(new ProviderBinding.ProviderInfo[list.size()]);

        Arrays.sort(providers, PRIORITY_COMPARATOR);

        List<PsiReference> result = new ArrayList<PsiReference>();
        final double maxPriority = providers[0].priority;
        next:
        for (ProviderBinding.ProviderInfo<PsiReferenceProvider, ProcessingContext> trinity : providers) {
            final PsiReference[] refs;
            try {
                refs = trinity.provider.getReferencesByElement(context, trinity.processingContext);
            }
            catch (IndexNotReadyException ex) {
                continue;
            }
            if (trinity.priority != maxPriority) {
                for (PsiReference ref : refs) {
                    for (PsiReference reference : result) {
                        if (ref != null && ReferenceRange.containsRangeInElement(reference, ref.getRangeInElement())) {
                            continue next;
                        }
                    }
                }
            }
            for (PsiReference ref : refs) {
                if (ref != null) {
                    result.add(ref);
                }
            }
        }
        return result.isEmpty() ? PsiReference.EMPTY_ARRAY : result.toArray(PsiReference.ARRAY_FACTORY);
    }
}
