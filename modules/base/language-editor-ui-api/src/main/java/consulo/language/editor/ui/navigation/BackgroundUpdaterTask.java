/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.editor.ui.navigation;

import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.usage.Usage;
import consulo.usage.UsageInfo;
import consulo.usage.UsageInfo2UsageAdapter;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;

public abstract class BackgroundUpdaterTask extends BackgroundUpdaterTaskBase<PsiElement> {
    public BackgroundUpdaterTask(@Nullable Project project, @Nonnull String title, @Nullable Comparator<PsiElement> comparator) {
        super(project, title, comparator);
    }

    protected static Comparator<PsiElement> createComparatorWrapper(@Nonnull Comparator<? super PsiElement> comparator) {
        return (o1, o2) -> {
            int diff = comparator.compare(o1, o2);
            if (diff == 0) {
                return ReadAction.compute(() -> PsiUtilCore.compareElementsByPosition(o1, o2));
            }
            return diff;
        };
    }

    @Override
    protected Usage createUsage(PsiElement element) {
        return new UsageInfo2UsageAdapter(new UsageInfo(element));
    }

    @Override
    public boolean updateComponent(@Nonnull PsiElement element, @Nullable Comparator comparator) {
        //Ensures that method with signature `updateComponent(PsiElement, Comparator)` is present in bytecode,
        //which is necessary for binary compatibility with some external plugins.
        return super.updateComponent(element, comparator);
    }

    @Override
    public boolean updateComponent(@Nonnull PsiElement element) {
        //Ensures that method with signature `updateComponent(PsiElement)` is present in bytecode,
        //which is necessary for binary compatibility with some external plugins.
        return super.updateComponent(element);
    }
}
