// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl;

import consulo.document.Document;
import consulo.document.impl.RangeMarkerImpl;
import consulo.document.impl.RangeMarkerTree;
import jakarta.annotation.Nonnull;

/**
 * {@link RangeMarkerTree} with intervals which are not collected when no one holds a reference to them.
 */
public class HardReferencingRangeMarkerTree<T extends RangeMarkerImpl> extends RangeMarkerTree<T> {
    HardReferencingRangeMarkerTree(@Nonnull Document document) {
        super(document);
    }

    @Override
    protected boolean keepIntervalOnWeakReference(@Nonnull T interval) {
        return false;
    }
}
