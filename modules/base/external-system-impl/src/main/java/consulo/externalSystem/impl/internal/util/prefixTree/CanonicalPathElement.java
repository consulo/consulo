// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.util.prefixTree;

import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

public final class CanonicalPathElement {
    private final String myKeyElement;

    public CanonicalPathElement(String keyElement) {
        myKeyElement = keyElement;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return FileUtil.pathsEqual(myKeyElement, other instanceof CanonicalPathElement element ? element.myKeyElement : null);
    }

    @Override
    public int hashCode() {
        return FileUtil.pathHashCode(myKeyElement);
    }
}
