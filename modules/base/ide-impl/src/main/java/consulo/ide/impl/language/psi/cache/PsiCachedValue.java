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

package consulo.ide.impl.language.psi.cache;

import consulo.application.impl.internal.util.CachedValueBase;
import consulo.application.impl.internal.util.CachedValuesFactory;
import consulo.application.util.CachedValueProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.project.content.ProjectRootModificationTracker;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 */
public abstract class PsiCachedValue<T> extends CachedValueBase<T> {
    private static final Key<?> PSI_MOD_COUNT_OPTIMIZATION = Key.create("PSI_MOD_COUNT_OPTIMIZATION");
    private final PsiManager myManager;

    PsiCachedValue(@Nonnull PsiManager manager, boolean trackValue, CachedValuesFactory factory) {
        super(trackValue, factory);
        myManager = manager;
    }

    @Nonnull
    @Override
    protected Object[] normalizeDependencies(@Nonnull CachedValueProvider.Result<T> result) {
        Object[] dependencies = super.normalizeDependencies(result);
        if (dependencies.length > 0 && ContainerUtil.and(dependencies, this::anyChangeImpliesPsiCounterChange)) {
            return ArrayUtil.prepend(PSI_MOD_COUNT_OPTIMIZATION, dependencies);
        }
        return dependencies;
    }

    @SuppressWarnings("deprecation")
    private boolean anyChangeImpliesPsiCounterChange(@Nonnull Object dependency) {
        return dependency instanceof PsiElement element && isVeryPhysical(element)
            || dependency instanceof ProjectRootModificationTracker
            || dependency instanceof PsiModificationTracker
            || dependency == PsiModificationTracker.MODIFICATION_COUNT
            || dependency == PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
            || dependency == PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT;
    }

    private boolean isVeryPhysical(@Nonnull PsiElement dependency) {
        if (!dependency.isValid()) {
            return false;
        }
        if (!dependency.isPhysical()) {
            return false;
        }
        // injected files are physical but can sometimes (look at you, completion)
        // be inexplicably injected into non-physical element, in which case PSI_MODIFICATION_COUNT doesn't change and thus can't be relied upon
        InjectedLanguageManager manager = InjectedLanguageManager.getInstance(myManager.getProject());
        PsiFile topLevelFile = manager.getTopLevelFile(dependency);
        return topLevelFile != null && topLevelFile.isPhysical();
    }

    @Override
    protected boolean isUpToDate(@Nonnull Data<T> data) {
        if (myManager.isDisposed()) {
            return false;
        }

        Object[] dependencies = data.getDependencies();
        //noinspection SimplifiableIfStatement
        if (dependencies.length > 0
            && dependencies[0] == PSI_MOD_COUNT_OPTIMIZATION
            && data.getTimeStamps()[0] == myManager.getModificationTracker().getModificationCount()) {
            return true;
        }

        return super.isUpToDate(data);
    }

    @Override
    protected boolean isDependencyOutOfDate(@Nonnull Object dependency, long oldTimeStamp) {
        //noinspection SimplifiableIfStatement
        if (dependency == PSI_MOD_COUNT_OPTIMIZATION) {
            return false;
        }
        return super.isDependencyOutOfDate(dependency, oldTimeStamp);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected long getTimeStamp(@Nonnull Object dependency) {
        if (dependency instanceof PsiDirectory) {
            return myManager.getModificationTracker().getOutOfCodeBlockModificationCount();
        }

        if (dependency instanceof PsiElement element) {
            if (!element.isValid()) {
                return -1;
            }
            PsiFile containingFile = element.getContainingFile();
            if (containingFile != null) {
                return containingFile.getModificationStamp();
            }
        }

        if (dependency == PsiModificationTracker.MODIFICATION_COUNT || dependency == PSI_MOD_COUNT_OPTIMIZATION) {
            return myManager.getModificationTracker().getModificationCount();
        }
        if (dependency == PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) {
            return myManager.getModificationTracker().getOutOfCodeBlockModificationCount();
        }
        if (dependency == PsiModificationTracker.JAVA_STRUCTURE_MODIFICATION_COUNT) {
            return myManager.getModificationTracker().getJavaStructureModificationCount();
        }

        return super.getTimeStamp(dependency);
    }

    @Override
    public boolean isFromMyProject(@Nonnull Project project) {
        return myManager.getProject() == project;
    }
}
