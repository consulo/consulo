// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.searchEverywhere;

import org.jspecify.annotations.Nullable;

/**
 * Class containing info about found elements
 */
public class SearchEverywhereFoundElementInfo {
    public final int priority;
    public final Object element;
    public final SearchEverywhereContributor<?> contributor;

    private volatile @Nullable SearchEverywhereItemPresentation myPresentation;

    public SearchEverywhereFoundElementInfo(Object element, int priority, SearchEverywhereContributor<?> contributor) {
        this(element, priority, contributor, null);
    }

    public SearchEverywhereFoundElementInfo(
        Object element,
        int priority,
        SearchEverywhereContributor<?> contributor,
        @Nullable SearchEverywhereItemPresentation presentation
    ) {
        this.priority = priority;
        this.element = element;
        this.contributor = contributor;
        myPresentation = presentation;
    }

    public int getPriority() {
        return priority;
    }

    public Object getElement() {
        return element;
    }

    public SearchEverywhereContributor<?> getContributor() {
        return contributor;
    }

    public @Nullable SearchEverywhereItemPresentation getPresentation() {
        return myPresentation;
    }

    public void setPresentation(@Nullable SearchEverywhereItemPresentation presentation) {
        myPresentation = presentation;
    }
}
