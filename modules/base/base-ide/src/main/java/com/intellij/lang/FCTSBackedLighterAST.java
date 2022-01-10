/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.lang;

import com.intellij.util.CharTable;
import com.intellij.util.diff.FlyweightCapableTreeStructure;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import java.util.AbstractList;
import java.util.List;

public class FCTSBackedLighterAST extends LighterAST {
  @Nonnull
  private final FlyweightCapableTreeStructure<LighterASTNode> myTreeStructure;

  public FCTSBackedLighterAST(@Nonnull CharTable charTable, @Nonnull FlyweightCapableTreeStructure<LighterASTNode> treeStructure) {
    super(charTable);
    myTreeStructure = treeStructure;
  }

  @Nonnull
  @Override
  public LighterASTNode getRoot() {
    return myTreeStructure.getRoot();
  }

  @Override
  public LighterASTNode getParent(@Nonnull final LighterASTNode node) {
    return myTreeStructure.getParent(node);
  }

  @Nonnull
  @Override
  public List<LighterASTNode> getChildren(@Nonnull final LighterASTNode parent) {
    final SimpleReference<LighterASTNode[]> into = SimpleReference.create();
    final int numKids = myTreeStructure.getChildren(myTreeStructure.prepareForGetChildren(parent), into);
    if (numKids == 0) {
      return List.of();
    }
    LighterASTNode[] elements = into.get();
    assert elements != null : myTreeStructure + " (" + parent + ")";
    return new LighterASTNodeList(numKids, elements);
  }

  private static class LighterASTNodeList extends AbstractList<LighterASTNode> {
    private final int mySize;
    private final LighterASTNode[] myElements;

    public LighterASTNodeList(int size, LighterASTNode[] elements) {
      mySize = size;
      myElements = elements;
    }

    @Override
    public LighterASTNode get(final int index) {
      if (index < 0 || index >= mySize) throw new IndexOutOfBoundsException("index:" + index + " size:" + mySize);
      return myElements[index];
    }

    @Override
    public int size() {
      return mySize;
    }
  }
}
