// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Getter;

import javax.annotation.Nonnull;

/**
 * {@link RangeMarkerTree} with intervals which are not collected when no one holds a reference to them.
 *
 * @see RangeMarkerWithGetterImpl
 */
public class HardReferencingRangeMarkerTree<T extends RangeMarkerWithGetterImpl> extends RangeMarkerTree<T> {
  public HardReferencingRangeMarkerTree(@Nonnull Document document) {
    super(document);
  }

  @Nonnull
  @Override
  protected Node<T> createNewNode(@Nonnull T key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight, int layer) {
    return new Node<>(this, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
  }

  public static class Node<T extends RangeMarkerWithGetterImpl> extends RMNode<T> {
    public Node(@Nonnull RangeMarkerTree<T> rangeMarkerTree, @Nonnull T key, int start, int end, boolean greedyToLeft, boolean greedyToRight, boolean stickingToRight) {
      super(rangeMarkerTree, key, start, end, greedyToLeft, greedyToRight, stickingToRight);
    }

    @Override
    protected Getter<T> createGetter(@Nonnull T interval) {
      //noinspection unchecked
      return (Getter<T>)interval;
    }
  }
}
