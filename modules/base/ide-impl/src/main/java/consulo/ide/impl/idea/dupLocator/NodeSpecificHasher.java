/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package consulo.ide.impl.idea.dupLocator;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class NodeSpecificHasher implements DuplocateVisitor {
  /**
   * getNodeHash is called on each node, which was returned by getNodeChildren method
   */
  public abstract int getNodeHash(PsiElement node);

  /**
   * Used in TreeComparator and TreeHashers
   * @return -1 if you want to ignore this node.
   * Note: only nodes with equal values can be considered as equal (areNodesEqual will be called further)
   * This cost will be used when calculating compound nodes. See DuplocatorSettings#LOWER_BOUND 
   */
  public abstract int getNodeCost(PsiElement node);

  /**
   * List all of the nodes to process within TreeComparator
   */
  public abstract List<PsiElement> getNodeChildren(PsiElement node);

  /**
   * This is dual function to getNodeCost, checks whether 2 nodes with same costs are really equal
   */
  public abstract boolean areNodesEqual(@Nonnull PsiElement node1, @Nonnull PsiElement node2);

  public boolean areTreesEqual(@Nonnull PsiElement root1, @Nonnull PsiElement root2, int discardCost) {
    return TreeComparator.areEqual(root1, root2, this, discardCost);
  }

  public abstract boolean checkDeep(PsiElement node1, PsiElement node2);
}
