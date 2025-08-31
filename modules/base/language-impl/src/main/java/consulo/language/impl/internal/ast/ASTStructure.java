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

package consulo.language.impl.internal.ast;

import consulo.language.ast.ASTNode;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class ASTStructure implements FlyweightCapableTreeStructure<ASTNode> {
  private final ASTNode myRoot;

  public ASTStructure(@Nonnull ASTNode root) {
    myRoot = root;
  }

  @Override
  @Nonnull
  public ASTNode getRoot() {
    return myRoot;
  }

  @Override
  public ASTNode getParent(@Nonnull ASTNode node) {
    return node.getTreeParent();
  }

  @Override
  @Nonnull
  public ASTNode prepareForGetChildren(@Nonnull ASTNode astNode) {
    return astNode;
  }

  @Override
  public int getChildren(@Nonnull ASTNode astNode, @Nonnull SimpleReference<ASTNode[]> into) {
    ASTNode child = astNode.getFirstChildNode();
    if (child == null) return 0;

    ASTNode[] store = into.get();
    if (store == null) {
      store = new ASTNode[10];
      into.set(store);
    }

    int count = 0;
    while (child != null) {
      if (count >= store.length) {
        ASTNode[] newStore = new ASTNode[count * 3 / 2];
        System.arraycopy(store, 0, newStore, 0, count);
        into.set(newStore);
        store = newStore;
      }
      store[count++] = child;
      child = child.getTreeNext();
    }

    return count;
  }

  @Override
  public void disposeChildren(ASTNode[] nodes, int count) {
  }

  @Nonnull
  @Override
  public CharSequence toString(@Nonnull ASTNode node) {
    return node.getChars();
  }

  @Override
  public int getStartOffset(@Nonnull ASTNode node) {
    return node.getStartOffset();
  }

  @Override
  public int getEndOffset(@Nonnull ASTNode node) {
    return node.getStartOffset() + node.getTextLength();
  }
}
