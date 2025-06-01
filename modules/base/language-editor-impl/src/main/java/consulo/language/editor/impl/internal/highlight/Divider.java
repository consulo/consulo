/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.highlight;

import consulo.application.progress.ProgressManager;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.util.collection.Stack;
import consulo.util.collection.primitive.ints.IntStack;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;

import jakarta.annotation.Nonnull;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Divider {
  private static final int STARTING_TREE_HEIGHT = 10;

  public static class DividedElements {
    private final long modificationStamp;
    @Nonnull
    public final PsiFile root;
    @Nonnull
    private final TextRange restrictRange;
    @Nonnull
    private final TextRange priorityRange;
    public final List<PsiElement> inside = new ArrayList<>();
    final List<ProperTextRange> insideRanges = new ArrayList<>();
    public final List<PsiElement> outside = new ArrayList<>();
    final List<ProperTextRange> outsideRanges = new ArrayList<>();
    public final List<PsiElement> parents = new ArrayList<>();
    final List<ProperTextRange> parentRanges = new ArrayList<>();

    private DividedElements(long modificationStamp, @Nonnull PsiFile root, @Nonnull TextRange restrictRange, @Nonnull TextRange priorityRange) {
      this.modificationStamp = modificationStamp;
      this.root = root;
      this.restrictRange = restrictRange;
      this.priorityRange = priorityRange;
    }
  }

  private static final Key<Reference<DividedElements>> DIVIDED_ELEMENTS_KEY = Key.create("DIVIDED_ELEMENTS");

  public static void divideInsideAndOutsideAllRoots(@Nonnull PsiFile file,
                                                    @Nonnull TextRange restrictRange,
                                                    @Nonnull TextRange priorityRange,
                                                    @Nonnull Predicate<PsiFile> rootFilter,
                                                    @Nonnull Consumer<DividedElements> processor) {
    final FileViewProvider viewProvider = file.getViewProvider();
    for (Language language : viewProvider.getLanguages()) {
      final PsiFile root = viewProvider.getPsi(language);
      if (!rootFilter.test(root)) {
        continue;
      }
      divideInsideAndOutsideInOneRoot(root, restrictRange, priorityRange, processor);
    }
  }

  public static void divideInsideAndOutsideInOneRoot(@Nonnull PsiFile root, @Nonnull TextRange restrictRange, @Nonnull TextRange priorityRange, @Nonnull Consumer<DividedElements> processor) {
    long modificationStamp = root.getModificationStamp();
    DividedElements cached = SoftReference.dereference(root.getUserData(DIVIDED_ELEMENTS_KEY));
    DividedElements elements;
    if (cached == null || cached.modificationStamp != modificationStamp || !cached.restrictRange.equals(restrictRange) || !cached.priorityRange.contains(priorityRange)) {
      elements = new DividedElements(modificationStamp, root, restrictRange, priorityRange);
      divideInsideAndOutsideInOneRoot(root, restrictRange, priorityRange, elements.inside, elements.insideRanges, elements.outside, elements.outsideRanges, elements.parents, elements.parentRanges,
                                      true);
      root.putUserData(DIVIDED_ELEMENTS_KEY, new java.lang.ref.SoftReference<>(elements));
    }
    else {
      elements = cached;
    }
    processor.accept(elements);
  }

  private static final PsiElement HAVE_TO_GET_CHILDREN = PsiUtilCore.NULL_PSI_ELEMENT;

  private static void divideInsideAndOutsideInOneRoot(@Nonnull PsiFile root,
                                                      @Nonnull TextRange restrictRange,
                                                      @Nonnull TextRange priorityRange,
                                                      @Nonnull List<PsiElement> inside,
                                                      @Nonnull List<ProperTextRange> insideRanges,
                                                      @Nonnull List<PsiElement> outside,
                                                      @Nonnull List<ProperTextRange> outsideRanges,
                                                      @Nonnull List<PsiElement> outParents,
                                                      @Nonnull List<ProperTextRange> outParentRanges,
                                                      boolean includeParents) {
    int startOffset = restrictRange.getStartOffset();
    int endOffset = restrictRange.getEndOffset();

    final IntStack starts = new IntStack(STARTING_TREE_HEIGHT);
    starts.push(startOffset);
    final Stack<PsiElement> elements = new Stack<>(STARTING_TREE_HEIGHT);
    final Stack<PsiElement> children = new Stack<>(STARTING_TREE_HEIGHT);
    PsiElement element = root;

    PsiElement child = HAVE_TO_GET_CHILDREN;
    int offset = 0;
    while (true) {
      ProgressManager.checkCanceled();

      boolean startChildrenVisiting;
      if (child == HAVE_TO_GET_CHILDREN) {
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

        int start = starts.pop();
        if (startOffset <= start && offset <= endOffset) {
          if (priorityRange.containsRange(start, offset)) {
            inside.add(element);
            insideRanges.add(new ProperTextRange(start, offset));
          }
          else {
            outside.add(element);
            outsideRanges.add(new ProperTextRange(start, offset));
          }
        }

        if (elements.isEmpty()) break;
        element = elements.pop();
        child = children.pop();
      }
      else {
        // composite element
        if (offset > endOffset) break;
        children.push(child.getNextSibling());
        starts.push(offset);
        elements.push(element);
        element = child;
        child = HAVE_TO_GET_CHILDREN;
      }
    }

    if (includeParents) {
      PsiElement parent =
              !outside.isEmpty() ? outside.get(outside.size() - 1) : !inside.isEmpty() ? inside.get(inside.size() - 1) : CollectHighlightsUtil.findCommonParent(root, startOffset, endOffset);
      while (parent != null && !(parent instanceof PsiFile)) {
        parent = parent.getParent();
        if (parent != null) {
          outParents.add(parent);
          TextRange textRange = parent.getTextRange();
          assert textRange != null : "Text range for " + parent + " is null. " + parent.getClass() + "; root: " + root + ": " + root.getVirtualFile();
          outParentRanges.add(ProperTextRange.create(textRange));
        }
      }
    }

    assert inside.size() == insideRanges.size();
    assert outside.size() == outsideRanges.size();
    assert outParents.size() == outParentRanges.size();
  }
}
