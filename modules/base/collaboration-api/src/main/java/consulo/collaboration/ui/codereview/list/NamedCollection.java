// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.list;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class NamedCollection<T> {
    private final String myNamePlural;
    private final List<T> myItems;

    private NamedCollection(String namePlural, List<T> items) {
        myNamePlural = namePlural;
        myItems = List.copyOf(items);
    }

    public String getNamePlural() {
        return myNamePlural;
    }

    public List<T> getItems() {
        return myItems;
    }

    public static <T> @Nullable NamedCollection<T> create(String namePlural, List<T> items) {
        if (items.isEmpty()) {
            return null;
        }
        return new NamedCollection<>(namePlural, items);
    }
}
