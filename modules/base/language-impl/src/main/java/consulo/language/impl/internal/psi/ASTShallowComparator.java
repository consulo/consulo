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

package consulo.language.impl.internal.psi;

import consulo.application.progress.ProgressIndicator;
import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.*;
import consulo.language.impl.psi.ForeignLeafPsiElement;
import consulo.language.impl.internal.psi.diff.ShallowNodeComparator;
import consulo.language.psi.PsiErrorElement;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;
import java.util.Objects;

/**
 * @author max
 */
public class ASTShallowComparator implements ShallowNodeComparator<ASTNode, ASTNode> {
  private final ProgressIndicator myIndicator;

  public ASTShallowComparator(@Nonnull ProgressIndicator indicator) {
    myIndicator = indicator;
  }

  @Nonnull
  @Override
  public ThreeState deepEqual(@Nonnull ASTNode oldNode, @Nonnull ASTNode newNode) {
    return textMatches(oldNode, newNode);
  }

  private ThreeState textMatches(ASTNode oldNode, ASTNode newNode) {
    myIndicator.checkCanceled();
    String oldText = TreeUtil.isCollapsedChameleon(oldNode) ? oldNode.getText() : null;
    String newText = TreeUtil.isCollapsedChameleon(newNode) ? newNode.getText() : null;
    if (oldText != null && newText != null) return oldText.equals(newText) ? ThreeState.YES : ThreeState.UNSURE;

    if (oldText != null) {
      return compareTreeToText((TreeElement)newNode, oldText) ? ThreeState.YES : ThreeState.UNSURE;
    }
    if (newText != null) {
      return compareTreeToText((TreeElement)oldNode, newText) ? ThreeState.YES : ThreeState.UNSURE;
    }

    if (oldNode instanceof ForeignLeafPsiElement) {
      return newNode instanceof ForeignLeafPsiElement && oldNode.getText().equals(newNode.getText()) ? ThreeState.YES : ThreeState.NO;
    }

    if (newNode instanceof ForeignLeafPsiElement) return ThreeState.NO;

    if (oldNode instanceof LeafElement) {
      return ((LeafElement)oldNode).textMatches(newNode.getText()) ? ThreeState.YES : ThreeState.NO;
    }
    if (newNode instanceof LeafElement) {
      return ((LeafElement)newNode).textMatches(oldNode.getText()) ? ThreeState.YES : ThreeState.NO;
    }

    if (oldNode instanceof PsiErrorElement && newNode instanceof PsiErrorElement) {
      PsiErrorElement e1 = (PsiErrorElement)oldNode;
      PsiErrorElement e2 = (PsiErrorElement)newNode;
      if (!Objects.equals(e1.getErrorDescriptionValue(), e2.getErrorDescriptionValue())) return ThreeState.NO;
    }

    return ThreeState.UNSURE;
  }

  // have to perform tree walking by hand here to be able to interrupt ourselves
  private boolean compareTreeToText(@Nonnull TreeElement root, @Nonnull final String text) {
    final int[] curOffset = {0};
    root.acceptTree(new RecursiveTreeElementWalkingVisitor() {
      @Override
      public void visitLeaf(LeafElement leaf) {
        matchText(leaf);
      }

      private void matchText(TreeElement leaf) {
        curOffset[0] = leaf.textMatches(text, curOffset[0]);
        if (curOffset[0] < 0) {
          stopWalking();
        }
      }

      @Override
      public void visitComposite(CompositeElement composite) {
        myIndicator.checkCanceled();
        if (composite instanceof LazyParseableElement && !((LazyParseableElement)composite).isParsed()) {
          matchText(composite);
        }
        else {
          super.visitComposite(composite);
        }
      }
    });
    return curOffset[0] == text.length();
  }

  @Override
  public boolean typesEqual(@Nonnull ASTNode n1, @Nonnull ASTNode n2) {
    return n1.getElementType() == n2.getElementType();
  }

  @Override
  public boolean hashCodesEqual(@Nonnull ASTNode n1, @Nonnull ASTNode n2) {
    if (n1 instanceof LeafElement && n2 instanceof LeafElement) {
      return textMatches(n1, n2) == ThreeState.YES;
    }

    if (n1 instanceof PsiErrorElement && n2 instanceof PsiErrorElement) {
      PsiErrorElement e1 = (PsiErrorElement)n1;
      PsiErrorElement e2 = (PsiErrorElement)n2;
      if (!Objects.equals(e1.getErrorDescriptionValue(), e2.getErrorDescriptionValue())) return false;
    }

    return ((TreeElement)n1).hc() == ((TreeElement)n2).hc();
  }
}
