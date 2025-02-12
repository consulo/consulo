// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.ui.ex.tree.TreeAnchorizer;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author peter
 */
class AnchoredSet {
    private final Set<Object> myAnchors;
    private final TreeAnchorizer myTreeAnchorizer;

    AnchoredSet(@Nonnull Set<Object> elements) {
        myTreeAnchorizer = TreeAnchorizer.getService();
        myAnchors = new LinkedHashSet<>(TreeAnchorizer.anchorizeList(myTreeAnchorizer, elements));
    }

    @Nonnull
    Set<Object> getElements() {
        return new LinkedHashSet<>(TreeAnchorizer.retrieveList(myTreeAnchorizer, myAnchors));
    }
}
