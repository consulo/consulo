/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor;

import consulo.language.ast.ASTNode;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author ven
 */
public class PsiEquivalenceUtil {
  private static final Logger LOG = Logger.getInstance(PsiEquivalenceUtil.class);

  public static boolean areElementsEquivalent(@Nonnull PsiElement element1, @Nonnull PsiElement element2, @Nullable Comparator<PsiElement> resolvedElementsComparator, boolean areCommentsSignificant) {
    return areElementsEquivalent(element1, element2, resolvedElementsComparator, null, null, areCommentsSignificant);
  }

  public static boolean areElementsEquivalent(@Nonnull PsiElement element1,
                                              @Nonnull PsiElement element2,
                                              @Nullable Comparator<PsiElement> resolvedElementsComparator,
                                              @Nullable Comparator<PsiElement> leafElementsComparator,
                                              @Nullable Predicate<PsiElement> isElementSignificantCondition,
                                              boolean areCommentsSignificant) {
    if (element1 == element2) return true;
    ASTNode node1 = element1.getNode();
    ASTNode node2 = element2.getNode();
    if (node1 == null || node2 == null) return false;
    if (node1.getElementType() != node2.getElementType()) return false;

    PsiElement[] children1 = getFilteredChildren(element1, isElementSignificantCondition, areCommentsSignificant);
    PsiElement[] children2 = getFilteredChildren(element2, isElementSignificantCondition, areCommentsSignificant);
    if (children1.length != children2.length) return false;

    for (int i = 0; i < children1.length; i++) {
      PsiElement child1 = children1[i];
      PsiElement child2 = children2[i];
      if (!areElementsEquivalent(child1, child2, resolvedElementsComparator, leafElementsComparator, isElementSignificantCondition, areCommentsSignificant)) return false;
    }

    if (children1.length == 0) {
      if (leafElementsComparator != null) {
        if (leafElementsComparator.compare(element1, element2) != 0) return false;
      }
      else {
        if (!element1.textMatches(element2)) return false;
      }
    }

    PsiReference ref1 = element1.getReference();
    if (ref1 != null) {
      PsiReference ref2 = element2.getReference();
      if (ref2 == null) return false;
      PsiElement resolved1 = ref1.resolve();
      PsiElement resolved2 = ref2.resolve();
      if (!Comparing.equal(resolved1, resolved2) && (resolvedElementsComparator == null || resolvedElementsComparator.compare(resolved1, resolved2) != 0)) return false;
    }
    return true;

  }

  public static boolean areElementsEquivalent(@Nonnull PsiElement element1, @Nonnull PsiElement element2) {
    return areElementsEquivalent(element1, element2, null, false);
  }

  public static PsiElement[] getFilteredChildren(@Nonnull PsiElement element, @Nullable Predicate<PsiElement> isElementSignificantCondition, boolean areCommentsSignificant) {
    ASTNode[] children1 = element.getNode().getChildren(null);
    ArrayList<PsiElement> array = new ArrayList<>();
    for (ASTNode node : children1) {
      PsiElement child = node.getPsi();
      if (!(child instanceof PsiWhiteSpace) && (areCommentsSignificant || !(child instanceof PsiComment)) && (isElementSignificantCondition == null || isElementSignificantCondition.test(child))) {
        array.add(child);
      }
    }
    return PsiUtilCore.toPsiElementArray(array);
  }

  public static void findChildRangeDuplicates(PsiElement first, PsiElement last, List<Pair<PsiElement, PsiElement>> result, PsiElement scope) {
    findChildRangeDuplicates(first, last, scope, (start, end) -> result.add(new Pair<>(start, end)));
  }

  public static void findChildRangeDuplicates(PsiElement first, PsiElement last, PsiElement scope, BiConsumer<PsiElement, PsiElement> consumer) {
    LOG.assertTrue(first.getParent() == last.getParent());
    LOG.assertTrue(!(first instanceof PsiWhiteSpace) && !(last instanceof PsiWhiteSpace));
    addRangeDuplicates(scope, first, last, consumer);
  }

  private static void addRangeDuplicates(PsiElement scope, PsiElement first, PsiElement last, BiConsumer<PsiElement, PsiElement> result) {
    PsiElement[] children = getFilteredChildren(scope, null, true);
    NextChild:
    for (int i = 0; i < children.length; ) {
      PsiElement child = children[i];
      if (child != first) {
        int j = i;
        PsiElement next = first;
        do {
          if (!areElementsEquivalent(children[j], next)) break;
          j++;
          if (next == last) {
            result.accept(child, children[j - 1]);
            i = j + 1;
            continue NextChild;
          }
          next = PsiTreeUtil.skipSiblingsForward(next, PsiWhiteSpace.class);
        }
        while (true);

        if (i == j) {
          addRangeDuplicates(child, first, last, result);
        }
      }

      i++;
    }
  }
}
