// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.language.editor.ui.navigation;

import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.usage.Usage;
import consulo.usage.UsageInfo;
import consulo.usage.UsageInfo2UsageAdapter;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Streams already-presented rows into an open target popup. Replaces {@link BackgroundUpdaterTask},
 * which yielded raw PSI and left the popup to render it.
 */
public abstract class TargetUpdaterTask<T extends PsiElement> extends BackgroundUpdaterTaskBase<ItemWithPresentation<T>> {
    private final TargetPresentationProvider<? super T> myPresentationProvider;

    public TargetUpdaterTask(
        @Nullable Project project,
        LocalizeValue title,
        TargetPresentationProvider<? super T> presentationProvider,
        @Nullable Comparator<ItemWithPresentation<T>> comparator
    ) {
        super(project, title, comparator);
        myPresentationProvider = presentationProvider;
    }

    public TargetUpdaterTask(@Nullable Project project, LocalizeValue title, TargetPresentationProvider<? super T> presentationProvider) {
        this(project, title, presentationProvider, presentationOrder());
    }

    /**
     * Presents the element and feeds it to the open popup. Called from search callbacks, so it takes the
     * read lock itself.
     */
    public boolean updateElement(T element) {
        return updateComponent(ReadAction.compute(() -> new ItemWithPresentation<>(element, myPresentationProvider)));
    }

    /**
     * Rows sorted the way the popup shows them - by the text of the presentation they were built with,
     * ties broken by position so that two distinct elements never collapse into one row.
     */
    public static <T extends PsiElement> Comparator<ItemWithPresentation<T>> presentationOrder() {
        return (o1, o2) -> {
            int diff = o1.getPresentation().getPresentableText().get().compareTo(o2.getPresentation().getPresentableText().get());
            return diff != 0 ? diff : ReadAction.compute(() -> comparePosition(o1, o2));
        };
    }

    private static <T extends PsiElement> int comparePosition(ItemWithPresentation<T> o1, ItemWithPresentation<T> o2) {
        T e1 = o1.dereference();
        T e2 = o2.dereference();
        if (e1 == null || e2 == null) {
            return e1 == e2 ? 0 : e1 == null ? 1 : -1;
        }
        return PsiUtilCore.compareElementsByPosition(e1, e2);
    }

    @Override
    protected @Nullable Usage createUsage(ItemWithPresentation<T> item) {
        return ReadAction.compute(() -> {
            T element = item.dereference();
            return element == null ? null : new UsageInfo2UsageAdapter(new UsageInfo(element));
        });
    }
}
