/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProblemDescriptorBase extends CommonProblemDescriptorBase implements ProblemDescriptor {
  private static final Logger LOG = Logger.getInstance(ProblemDescriptorBase.class);

  @Nonnull
  private final SmartPsiElementPointer myStartSmartPointer;
  @Nullable
  private final SmartPsiElementPointer myEndSmartPointer;

  private final ProblemHighlightType myHighlightType;
  private Navigatable myNavigatable;
  private final boolean myAfterEndOfLine;
  private final TextRange myTextRangeInElement;
  private final boolean myShowTooltip;
  private TextAttributesKey myEnforcedTextAttributes;
  private int myLineNumber = -1;
  private ProblemGroup myProblemGroup;

  public ProblemDescriptorBase(@Nonnull PsiElement startElement,
                               @Nonnull PsiElement endElement,
                               @Nonnull String descriptionTemplate,
                               LocalQuickFix[] fixes,
                               @Nonnull ProblemHighlightType highlightType,
                               boolean isAfterEndOfLine,
                               @Nullable TextRange rangeInElement,
                               boolean tooltip,
                               boolean onTheFly) {
    super(fixes, descriptionTemplate);
    myShowTooltip = tooltip;
    PsiFile startContainingFile = startElement.getContainingFile();
    LOG.assertTrue(startContainingFile != null && startContainingFile.isValid() || startElement.isValid(), startElement);
    PsiFile endContainingFile = startElement == endElement ? startContainingFile : endElement.getContainingFile();
    LOG.assertTrue(startElement == endElement || endContainingFile != null && endContainingFile.isValid() || endElement.isValid(), endElement);
    assertPhysical(startElement);
    if (startElement != endElement) assertPhysical(endElement);

    TextRange startElementRange = startElement.getTextRange();
    LOG.assertTrue(startElementRange != null, startElement);
    TextRange endElementRange = endElement.getTextRange();
    LOG.assertTrue(endElementRange != null, endElement);
    if (startElementRange.getStartOffset() >= endElementRange.getEndOffset()) {
      if (!(startElement instanceof PsiFile && endElement instanceof PsiFile)) {
        LOG.error("Empty PSI elements should not be passed to createDescriptor. Start: " + startElement + ", end: " + endElement);
      }
    }

    myHighlightType = highlightType;
    Project project = startContainingFile == null ? startElement.getProject() : startContainingFile.getProject();
    SmartPointerManager manager = SmartPointerManager.getInstance(project);
    myStartSmartPointer = manager.createSmartPsiElementPointer(startElement, startContainingFile);
    myEndSmartPointer = startElement == endElement ? null : manager.createSmartPsiElementPointer(endElement, endContainingFile);

    myAfterEndOfLine = isAfterEndOfLine;
    myTextRangeInElement = rangeInElement;
  }

  protected void assertPhysical(PsiElement element) {
    if (!element.isPhysical()) {
      LOG.error("Non-physical PsiElement. Physical element is required to be able to anchor the problem in the source tree: " + element + "; file: " + element.getContainingFile());
    }
  }

  @RequiredReadAction
  @Override
  public PsiElement getPsiElement() {
    PsiElement startElement = getStartElement();
    if (myEndSmartPointer == null) {
      return startElement;
    }
    PsiElement endElement = getEndElement();
    if (startElement == endElement) {
      return startElement;
    }
    if (startElement == null || endElement == null) return null;
    return PsiTreeUtil.findCommonParent(startElement, endElement);
  }

  @Override
  @Nullable
  public TextRange getTextRangeInElement() {
    return myTextRangeInElement;
  }

  @RequiredReadAction
  @Override
  public PsiElement getStartElement() {
    return myStartSmartPointer.getElement();
  }

  @RequiredReadAction
  @Override
  public PsiElement getEndElement() {
    return myEndSmartPointer == null ? getStartElement() : myEndSmartPointer.getElement();
  }

  @Override
  public int getLineNumber() {
    if (myLineNumber == -1) {
      PsiElement psiElement = getPsiElement();
      if (psiElement == null) return -1;
      if (!psiElement.isValid()) return -1;
      LOG.assertTrue(psiElement.isPhysical());
      InjectedLanguageManager manager = InjectedLanguageManager.getInstance(psiElement.getProject());
      PsiFile containingFile = manager.getTopLevelFile(psiElement);
      Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(containingFile);
      if (document == null) return -1;
      TextRange textRange = getTextRange();
      if (textRange == null) return -1;
      textRange = manager.injectedToHost(psiElement, textRange);
      int startOffset = textRange.getStartOffset();
      int textLength = document.getTextLength();
      LOG.assertTrue(startOffset <= textLength, getDescriptionTemplate() + " at " + startOffset + ", " + textLength);
      myLineNumber = document.getLineNumber(startOffset) + 1;
    }
    return myLineNumber;
  }

  @Nonnull
  @Override
  public ProblemHighlightType getHighlightType() {
    return myHighlightType;
  }

  @Override
  public boolean isAfterEndOfLine() {
    return myAfterEndOfLine;
  }

  @Override
  public void setTextAttributes(TextAttributesKey key) {
    myEnforcedTextAttributes = key;
  }

  public TextAttributesKey getEnforcedTextAttributes() {
    return myEnforcedTextAttributes;
  }

  public TextRange getTextRangeForNavigation() {
    TextRange textRange = getTextRange();
    if (textRange == null) return null;
    PsiElement element = getPsiElement();
    return InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, textRange);
  }

  public TextRange getTextRange() {
    PsiElement startElement = getStartElement();
    PsiElement endElement = myEndSmartPointer == null ? startElement : getEndElement();
    if (startElement == null || endElement == null) {
      return null;
    }

    TextRange textRange = startElement.getTextRange();
    if (startElement == endElement) {
      if (isAfterEndOfLine()) return new TextRange(textRange.getEndOffset(), textRange.getEndOffset());
      if (myTextRangeInElement != null) {
        return new TextRange(textRange.getStartOffset() + myTextRangeInElement.getStartOffset(), textRange.getStartOffset() + myTextRangeInElement.getEndOffset());
      }
      return textRange;
    }
    return new TextRange(textRange.getStartOffset(), endElement.getTextRange().getEndOffset());
  }

  public Navigatable getNavigatable() {
    return myNavigatable;
  }

  public void setNavigatable(Navigatable navigatable) {
    myNavigatable = navigatable;
  }

  @Override
  @Nullable
  public ProblemGroup getProblemGroup() {
    return myProblemGroup;
  }

  @Override
  public void setProblemGroup(@Nullable ProblemGroup problemGroup) {
    myProblemGroup = problemGroup;
  }

  @Override
  public boolean showTooltip() {
    return myShowTooltip;
  }

  @Override
  public String toString() {
    PsiElement element = getPsiElement();
    return ProblemDescriptorUtil.renderDescriptionMessage(this, element);
  }
}
