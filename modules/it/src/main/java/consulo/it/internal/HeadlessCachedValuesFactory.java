/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.util.CachedValueImpl;
import consulo.application.impl.internal.util.CachedValuesFactory;
import consulo.application.impl.internal.util.ParameterizedCachedValueImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.ParameterizedCachedValue;
import consulo.application.util.ParameterizedCachedValueProvider;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Cached-value factory for the headless container — the production
 * {@code PsiCachedValuesFactory} lives in ide-impl which is excluded here. Mirrors its
 * PSI-aware dependency handling ({@code PsiModificationTracker.MODIFICATION_COUNT},
 * PSI elements and directories as dependencies), which the stub/AST reconciliation
 * machinery relies on.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessCachedValuesFactory implements CachedValuesFactory {
    private final Project myProject;

    @Inject
    public HeadlessCachedValuesFactory(Project project) {
        myProject = project;
    }

    @Override
    public <T> CachedValue<T> createCachedValue(CachedValueProvider<T> provider, boolean trackValue) {
        return new CachedValueImpl<>(provider, trackValue, this) {
            @Override
            protected long getTimeStamp(Object dependency) {
                return psiAwareTimeStamp(dependency, () -> super.getTimeStamp(dependency));
            }
        };
    }

    @Override
    public <T, P> ParameterizedCachedValue<T, P> createParameterizedCachedValue(
        ParameterizedCachedValueProvider<T, P> provider,
        boolean trackValue
    ) {
        return new ParameterizedCachedValueImpl<>(myProject, provider, trackValue, this) {
            @Override
            protected long getTimeStamp(Object dependency) {
                return psiAwareTimeStamp(dependency, () -> super.getTimeStamp(dependency));
            }
        };
    }

    private long psiAwareTimeStamp(Object dependency, java.util.function.LongSupplier fallback) {
        PsiModificationTracker tracker = PsiManager.getInstance(myProject).getModificationTracker();
        if (dependency instanceof PsiDirectory) {
            return tracker.getModificationCount();
        }
        if (dependency instanceof PsiElement element) {
            if (!element.isValid()) {
                return -1;
            }
            PsiFile containingFile = element.getContainingFile();
            if (containingFile != null) {
                return containingFile.getModificationStamp();
            }
            return tracker.getModificationCount();
        }
        if (dependency == PsiModificationTracker.MODIFICATION_COUNT) {
            return tracker.getModificationCount();
        }
        return fallback.getAsLong();
    }
}
