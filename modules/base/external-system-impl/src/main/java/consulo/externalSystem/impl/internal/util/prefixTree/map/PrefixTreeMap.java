// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree.map;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see consulo.externalSystem.impl.internal.util.prefixTree.set.PrefixTreeSet
 */
public interface PrefixTreeMap<Key, Value extends @Nullable Object> extends Map<Key, Value> {

    /**
     * Returns descendant keys for {@code key}.
     * <p>
     * For example, we have a map of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then descendant keys for {@code key=[a,b]} are {@code [a,b,c]}, {@code [a,b,c,d]} and {@code [a,b,c,e]}.
     */
    Set<Key> getDescendantKeys(Key key);

    List<Value> getDescendantValues(Key key);

    Set<Map.Entry<Key, Value>> getDescendantEntries(Key key);

    /**
     * Returns ancestor elements for {@code key}.
     * <p>
     * For example, we have a map of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then ancestor keys for {@code key=[a,b,c,d,e]} are {@code [a,b,c]} and {@code [a,b,c,d]}.
     */
    Set<Key> getAncestorKeys(Key key);

    List<Value> getAncestorValues(Key key);

    Set<Map.Entry<Key, Value>> getAncestorEntries(Key key);

    /**
     * Returns root keys in this map.
     * <p>
     * For example, we have a map of {@code [a,b,c]}, {@code [a,b,c,d]}, {@code [a,b,c,e]} and {@code [a,f,g]}.
     * Then root keys are {@code [a,b,c]} and {@code [a,f,g]}.
     */
    Set<Key> getRootKeys();

    List<Value> getRootValues();

    Set<Map.Entry<Key, Value>> getRootEntries();
}
