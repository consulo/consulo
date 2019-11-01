// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.util.treeView.TreeAnchorizer;
import javax.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author peter
 */
class AnchoredSet {
  private final Set<Object> myAnchors;

  AnchoredSet(@Nonnull Set<Object> elements) {
    myAnchors = new LinkedHashSet<>(TreeAnchorizer.anchorizeList(elements));
  }

  @Nonnull
  Set<Object> getElements() {
    return new LinkedHashSet<>(TreeAnchorizer.retrieveList(myAnchors));
  }
}
