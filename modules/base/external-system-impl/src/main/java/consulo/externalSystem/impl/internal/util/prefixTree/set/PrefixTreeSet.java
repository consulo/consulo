// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.set;

import java.util.Set;

/**
 * Map for fast finding elements by their prefix.
 * {@code Key} is an element which can be represented as a sequence of objects.
 *
 * @see MutablePrefixTreeSet
 * @see consulo.externalSystem.impl.internal.util.prefixTree.PrefixTreeFactory
 * @see consulo.externalSystem.impl.internal.util.prefixTree.map.PrefixTreeMap
 */
public interface PrefixTreeSet<Key> extends Set<Key> {

    /**
     * Returns descendant elements for {@code element}.
     * <p>
     * For example, we have a set of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then ancestor elements for {@code element=[a,b]} are {@code [a,b,c]}, {@code [a,b,c,d]} and {@code [a,b,c,e]}.
     */
    Set<Key> getDescendants(Key element);

    /**
     * Returns ancestor elements for {@code element}.
     * <p>
     * For example, we have a set of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then descendant elements for {@code element=[a,b,c,d,e]} are {@code [a,b,c]} and {@code [a,b,c,d]}.
     */
    Set<Key> getAncestors(Key element);

    /**
     * Returns root elements in this set.
     * <p>
     * For example, we have a set of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then root elements are {@code [a,b,c]} and {@code [a,f,g]}.
     */
    Set<Key> getRoots();
}
