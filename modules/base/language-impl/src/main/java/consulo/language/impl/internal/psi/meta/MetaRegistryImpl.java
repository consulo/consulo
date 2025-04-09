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

package consulo.language.impl.internal.psi.meta;

import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.UserDataCache;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.filter.position.PatternFilter;
import consulo.language.psi.meta.MetaDataContributor;
import consulo.language.psi.meta.MetaDataRegistrar;
import consulo.language.psi.meta.MetaDataService;
import consulo.language.psi.meta.PsiMetaData;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * User: ik
 * Date: 07.05.2003
 * Time: 3:31:09
 */
@Singleton
@ServiceImpl
public class MetaRegistryImpl implements MetaDataService, MetaDataRegistrar {
    private final List<MyBinding> myBindings = Lists.newLockFreeCopyOnWriteList();

    private static volatile boolean ourContributorsLoaded = false;

    private static final Key<CachedValue<PsiMetaData>> META_DATA_KEY = Key.create("META DATA KEY");

    @Override
    public void bindDataToElement(PsiElement element, PsiMetaData data) {
        CachedValue<PsiMetaData> value = CachedValuesManager.getManager(element.getProject()).createCachedValue(() -> {
            data.init(element);
            return new CachedValueProvider.Result<>(data, data.getDependences());
        });
        element.putUserData(META_DATA_KEY, value);
    }

    @Override
    public PsiMetaData getMeta(PsiElement element) {
        PsiMetaData base = getMetaBase(element);
        return base != null ? base : null;
    }

    private final UserDataCache<CachedValue<PsiMetaData>, PsiElement, Object> myCachedMetaCache = new UserDataCache<>() {
        @Override
        protected CachedValue<PsiMetaData> compute(PsiElement element, Object p) {
            return CachedValuesManager.getManager(element.getProject()).createCachedValue(
                () -> {
                    ensureContributorsLoaded();
                    for (MyBinding binding : myBindings) {
                        if (binding.myFilter.isClassAcceptable(element.getClass())
                            && binding.myFilter.isAcceptable(element, element.getParent())) {
                            PsiMetaData data = binding.myFactory.get();
                            data.init(element);
                            return new CachedValueProvider.Result<>(data, data.getDependences());
                        }
                    }
                    return new CachedValueProvider.Result<>(null, element);
                },
                false
            );
        }
    };

    private void ensureContributorsLoaded() {
        if (!ourContributorsLoaded) {
            synchronized (myBindings) {
                if (!ourContributorsLoaded) {
                    for (MetaDataContributor contributor : MetaDataContributor.EP_NAME.getExtensionList()) {
                        contributor.contributeMetaData(this);
                    }
                    ourContributorsLoaded = true;
                }
            }
        }
    }

    @Nullable
    public PsiMetaData getMetaBase(PsiElement element) {
        ProgressIndicatorProvider.checkCanceled();
        return myCachedMetaCache.get(META_DATA_KEY, element, null).getValue();
    }

    public <T extends PsiMetaData> void addMetadataBinding(ElementFilter filter, Supplier<T> aMetadataClass) {
        addBinding(new MyBinding(filter, aMetadataClass));
    }

    private void addBinding(MyBinding binding) {
        myBindings.add(0, binding);
    }

    @Override
    public <T extends PsiMetaData> void registerMetaData(ElementFilter filter, Supplier<T> metadataDescriptorFactory) {
        addMetadataBinding(filter, metadataDescriptorFactory);
    }

    @Override
    public <T extends PsiMetaData> void registerMetaData(ElementPattern<?> pattern, Supplier<T> metadataDescriptorFactory) {
        addMetadataBinding(new PatternFilter(pattern), metadataDescriptorFactory);
    }

    private static class MyBinding {
        private final ElementFilter myFilter;
        private final Supplier<? extends PsiMetaData> myFactory;

        public MyBinding(@Nonnull ElementFilter filter, @Nonnull Supplier<? extends PsiMetaData> factory) {
            myFilter = filter;
            myFactory = factory;
        }
    }
}
