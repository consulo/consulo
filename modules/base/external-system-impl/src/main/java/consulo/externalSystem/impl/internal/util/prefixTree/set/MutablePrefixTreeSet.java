// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.set;

public interface MutablePrefixTreeSet<Key> extends PrefixTreeSet<Key> {

    @Override
    boolean add(Key element);

    @Override
    boolean remove(Object element);

    default void addAll(Iterable<? extends Key> elements) {
        for (Key element : elements) {
            add(element);
        }
    }
}
