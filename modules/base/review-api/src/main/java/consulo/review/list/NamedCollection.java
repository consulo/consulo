// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class NamedCollection<T> {
    private final LocalizeValue myNamePlural;
    private final List<T> myItems;

    private NamedCollection(LocalizeValue namePlural, List<T> items) {
        myNamePlural = namePlural;
        myItems = items;
    }

    public static <T> @Nullable NamedCollection<T> create(LocalizeValue namePlural, List<T> items) {
        if (items.isEmpty()) {
            return null;
        }
        return new NamedCollection<>(namePlural, items);
    }

    public LocalizeValue getNamePlural() {
        return myNamePlural;
    }

    public List<T> getItems() {
        return myItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NamedCollection<?> that)) {
            return false;
        }
        return myNamePlural.equals(that.myNamePlural) && myItems.equals(that.myItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myNamePlural, myItems);
    }
}
