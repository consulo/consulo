// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.OuterLanguageElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Inserts the {@link OuterLanguageElement} so that it isn't a first child of the parent (unless it's the very first element in the file).
 */
public abstract class MiddleTreePatcher implements TreePatcher {
  @Override
  public void insert(@Nonnull CompositeElement parent, TreeElement anchorBefore, @Nonnull OuterLanguageElement toInsert) {
    anchorBefore = findTopmostAnchor(anchorBefore);
    if (anchorBefore != null) {
      anchorBefore.rawInsertBeforeMe((TreeElement)toInsert);
    }
    else {
      parent.rawAddChildren((TreeElement)toInsert);
    }
  }

  private static TreeElement findTopmostAnchor(@Nullable TreeElement anchorBefore) {
    while (anchorBefore != null && anchorBefore.getTreePrev() == null) {
      CompositeElement parent = anchorBefore.getTreeParent();
      if (parent.getTreeParent() == null) break;
      anchorBefore = parent;
    }
    return anchorBefore;
  }

}
