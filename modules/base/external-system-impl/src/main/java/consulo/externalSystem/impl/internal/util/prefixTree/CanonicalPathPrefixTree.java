// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import java.util.ArrayList;
import java.util.List;

public final class CanonicalPathPrefixTree implements PrefixTreeFactory<String, CanonicalPathElement> {
    public static final CanonicalPathPrefixTree INSTANCE = new CanonicalPathPrefixTree();

    private CanonicalPathPrefixTree() {
    }

    @Override
    public List<CanonicalPathElement> convertToList(String element) {
        String path = element.endsWith("/") ? element.substring(0, element.length() - 1) : element;
        List<CanonicalPathElement> result = new ArrayList<>();
        for (String keyElement : path.split("/", -1)) {
            result.add(new CanonicalPathElement(keyElement));
        }
        return result;
    }
}
