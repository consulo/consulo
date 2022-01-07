// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.structureView.customRegions;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.folding.CustomFoldingBuilder;
import com.intellij.lang.folding.CustomFoldingProvider;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileEx;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Rustam Vishnyakov
 */
public class CustomRegionStructureUtil {

  @RequiredReadAction
  public static Collection<StructureViewTreeElement> groupByCustomRegions(@Nonnull PsiElement rootElement, @Nonnull Collection<StructureViewTreeElement> originalElements) {
    if (rootElement instanceof PsiFileEx && !((PsiFileEx)rootElement).isContentsLoaded() || rootElement instanceof StubBasedPsiElement && ((StubBasedPsiElement)rootElement).getStub() != null) {
      return originalElements;
    }
    List<StructureViewTreeElement> physicalElements = ContainerUtil.filter(originalElements, element -> {
      Object value = element.getValue();
      return !(value instanceof StubBasedPsiElement) || ((StubBasedPsiElement)value).getStub() == null;
    });
    Set<TextRange> childrenRanges = ContainerUtil.map2SetNotNull(physicalElements, element -> {
      Object value = element.getValue();
      return value instanceof PsiElement ? getTextRange((PsiElement)value) : null;
    });
    Collection<CustomRegionTreeElement> customRegions = collectCustomRegions(rootElement, childrenRanges);
    if (customRegions.size() > 0) {
      List<StructureViewTreeElement> result = new ArrayList<>(customRegions);
      for (StructureViewTreeElement element : physicalElements) {
        ProgressManager.checkCanceled();
        boolean isInCustomRegion = false;
        for (CustomRegionTreeElement customRegion : customRegions) {
          if (customRegion.containsElement(element)) {
            customRegion.addChild(element);
            isInCustomRegion = true;
            break;
          }
        }
        if (!isInCustomRegion) result.add(element);
      }
      return result;
    }
    return originalElements;
  }

  /*
   * Fix cases when a line comment before an element (for example, method) gets inside it as a first child.
   */
  @RequiredReadAction
  private static TextRange getTextRange(@Nonnull PsiElement element) {
    PsiElement first = element.getFirstChild();
    if (!(element instanceof PsiFile) && first instanceof PsiComment && !first.textContains('\n')) {
      PsiElement next = first.getNextSibling();
      if (next instanceof PsiWhiteSpace) next = next.getNextSibling();
      if (next != null) {
        return new TextRange(next.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
      }
    }
    return element.getTextRange();
  }

  @RequiredReadAction
  private static Collection<CustomRegionTreeElement> collectCustomRegions(@Nonnull PsiElement rootElement, @Nonnull Set<? extends TextRange> ranges) {
    TextRange rootRange = getTextRange(rootElement);
    Iterator<PsiElement> iterator =
            SyntaxTraverser.psiTraverser(rootElement).filter(element -> isCustomRegionCommentCandidate(element) && rootRange.contains(element.getTextRange()) && !isInsideRanges(element, ranges)).iterator();

    List<CustomRegionTreeElement> customRegions = new SmartList<>();
    CustomRegionTreeElement currRegionElement = null;
    CustomFoldingProvider provider = null;
    while (iterator.hasNext()) {
      ProgressManager.checkCanceled();
      PsiElement child = iterator.next();
      if (provider == null) provider = getProvider(child);
      if (provider != null) {
        String commentText = child.getText();
        if (provider.isCustomRegionStart(commentText)) {
          if (currRegionElement == null) {
            currRegionElement = new CustomRegionTreeElement(child, provider);
            customRegions.add(currRegionElement);
          }
          else {
            currRegionElement = currRegionElement.createNestedRegion(child);
          }
        }
        else if (provider.isCustomRegionEnd(commentText) && currRegionElement != null) {
          currRegionElement = currRegionElement.endRegion(child);
        }
      }
    }
    return customRegions;
  }

  @Nullable
  static CustomFoldingProvider getProvider(@Nonnull PsiElement element) {
    ASTNode node = element.getNode();
    if (node != null) {
      for (CustomFoldingProvider provider : CustomFoldingProvider.EP_NAME.getExtensionList()) {
        if (provider.isCustomRegionStart(node.getText())) {
          return provider;
        }
      }
    }
    return null;
  }

  @RequiredReadAction
  private static boolean isInsideRanges(@Nonnull PsiElement element, @Nonnull Set<? extends TextRange> ranges) {
    for (TextRange range : ranges) {
      TextRange elementRange = element.getTextRange();
      if (range.contains(elementRange.getStartOffset()) || range.contains(elementRange.getEndOffset())) {
        return true;
      }
    }
    return false;
  }

  @RequiredReadAction
  private static boolean isCustomRegionCommentCandidate(@Nonnull PsiElement element) {
    Language language = element.getLanguage();
    if (!Language.ANY.is(language)) {
      for (FoldingBuilder builder : LanguageFolding.INSTANCE.allForLanguage(language)) {
        if (builder instanceof CustomFoldingBuilder) {
          return ((CustomFoldingBuilder)builder).isCustomFoldingCandidate(element);
        }
      }
    }
    return false;
  }
}
