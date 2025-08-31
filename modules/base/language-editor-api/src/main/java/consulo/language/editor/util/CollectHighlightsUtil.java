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

package consulo.language.editor.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.collection.Stack;
import consulo.util.collection.primitive.ints.IntStack;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CollectHighlightsUtil {
  private static final Logger LOG = Logger.getInstance(CollectHighlightsUtil.class);

  private CollectHighlightsUtil() {
  }

  @Nonnull
  @RequiredReadAction
  public static List<PsiElement> getElementsInRange(@Nonnull PsiElement root, int startOffset, int endOffset) {
    return getElementsInRange(root, startOffset, endOffset, false);
  }

  @Nonnull
  @RequiredReadAction
  public static List<PsiElement> getElementsInRange(@Nonnull PsiElement root, int startOffset, int endOffset, boolean includeAllParents) {
    PsiElement commonParent = findCommonParent(root, startOffset, endOffset);
    if (commonParent == null) return new ArrayList<>();
    List<PsiElement> list = getElementsToHighlight(commonParent, startOffset, endOffset);

    PsiElement parent = commonParent;
    while (parent != null && parent != root) {
      list.add(parent);
      parent = includeAllParents ? parent.getParent() : null;
    }

    list.add(root);

    return list;
  }

  private static final int STARTING_TREE_HEIGHT = 100;

  @Nonnull
  @RequiredReadAction
  private static List<PsiElement> getElementsToHighlight(@Nonnull PsiElement commonParent, int startOffset, int endOffset) {
    List<PsiElement> result = new ArrayList<>();

    int offset = commonParent.getTextRange().getStartOffset();

    IntStack starts = new IntStack(STARTING_TREE_HEIGHT);
    Stack<PsiElement> elements = new Stack<>(STARTING_TREE_HEIGHT);
    Stack<PsiElement> children = new Stack<>(STARTING_TREE_HEIGHT);
    PsiElement element = commonParent;

    PsiElement child = PsiUtilCore.NULL_PSI_ELEMENT;
    while (true) {
      ProgressIndicatorProvider.checkCanceled();

      boolean startChildrenVisiting;
      if (child == PsiUtilCore.NULL_PSI_ELEMENT) {
        startChildrenVisiting = true;
        child = element.getFirstChild();
      }
      else {
        startChildrenVisiting = false;
      }

      if (child == null) {
        if (startChildrenVisiting) {
          // leaf element
          offset += element.getTextLength();
        }

        if (elements.isEmpty()) break;
        int start = starts.pop();
        if (startOffset <= start && offset <= endOffset) {
          assert element != null;
          result.add(element);
        }

        element = elements.pop();
        child = children.pop();
      }
      else {
        // composite element
        if (offset > endOffset) break;
        children.push(child.getNextSibling());
        starts.push(offset);
        assert element != null;
        elements.push(element);
        element = child;
        child = PsiUtilCore.NULL_PSI_ELEMENT;
      }
    }

    return result;
  }


  @Nullable
  public static PsiElement findCommonParent(PsiElement root, int startOffset, int endOffset) {
    if (startOffset == endOffset) return null;
    PsiElement left = findElementAtInRoot(root, startOffset);
    PsiElement right = findElementAtInRoot(root, endOffset - 1);
    if (left == null || right == null) return null;

    PsiElement commonParent = PsiTreeUtil.findCommonParent(left, right);
    if (commonParent == null) {
      LOG.error("No common parent for " + left + " and " + right + "; root: " + root + "; startOffset: " + startOffset + "; endOffset: " + endOffset);
    }
    LOG.assertTrue(commonParent.getTextRange() != null, commonParent);

    PsiElement parent = commonParent.getParent();
    while (parent != null && commonParent.getTextRange().equals(parent.getTextRange())) {
      commonParent = parent;
      parent = parent.getParent();
    }
    return commonParent;
  }

  @Nullable
  private static PsiElement findElementAtInRoot(PsiElement root, int offset) {
    if (root instanceof PsiFile) {
      return ((PsiFile)root).getViewProvider().findElementAt(offset, root.getLanguage());
    }
    return root.findElementAt(offset);
  }
}
