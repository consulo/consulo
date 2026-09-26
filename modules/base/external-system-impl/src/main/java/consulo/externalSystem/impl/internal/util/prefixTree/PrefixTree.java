// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import consulo.externalSystem.impl.internal.util.prefixTree.map.PrefixTreeMap;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface PrefixTree<Key, Value extends @Nullable Object> extends PrefixTreeMap<List<Key>, Value> {
}
