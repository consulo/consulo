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

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import jakarta.annotation.Nonnull;

public abstract class LocalQuickFixOnPsiElement implements LocalQuickFix {
  protected static final Logger LOG = Logger.getInstance(LocalQuickFixOnPsiElement.class);
  protected final SmartPsiElementPointer<PsiElement> myStartElement;
  protected final SmartPsiElementPointer<PsiElement> myEndElement;

  protected LocalQuickFixOnPsiElement(@Nonnull PsiElement element) {
    this(element, element);
  }

  public LocalQuickFixOnPsiElement(PsiElement startElement, PsiElement endElement) {
    if (startElement == null || endElement == null) {
      myStartElement = myEndElement = null;
      return;
    }
    LOG.assertTrue(startElement.isValid());
    PsiFile startContainingFile = startElement.getContainingFile();
    PsiFile endContainingFile = startElement == endElement ? startContainingFile : endElement.getContainingFile();
    if (startElement != endElement) {
      LOG.assertTrue(endElement.isValid());
      LOG.assertTrue(startContainingFile == endContainingFile, "Both elements must be from the same file");
    }
    Project project = startContainingFile == null ? startElement.getProject() : startContainingFile.getProject(); // containingFile can be null for a directory
    myStartElement = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(startElement, startContainingFile);
    myEndElement = endElement == startElement ? null : SmartPointerManager.getInstance(project).createSmartPsiElementPointer(endElement, endContainingFile);
  }

  @Nonnull
  @Override
  public final String getName() {
    return getText();
  }

  public boolean isAvailable(@Nonnull Project project,
                             @Nonnull PsiFile file,
                             @Nonnull PsiElement startElement,
                             @Nonnull PsiElement endElement) {
    return true;
  }

  protected boolean isAvailable() {
    if (myStartElement == null) return false;
    PsiElement startElement = myStartElement.getElement();
    PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
    PsiFile file = myStartElement.getContainingFile();
    Project project = myStartElement.getProject();
    return startElement != null &&
           endElement != null &&
           startElement.isValid() &&
           (endElement == startElement || endElement.isValid()) &&
           file != null &&
           isAvailable(project, file, startElement, endElement);
  }

  public PsiElement getStartElement() {
    return myStartElement == null ? null : myStartElement.getElement();
  }

  public PsiElement getEndElement() {
    return myEndElement == null ? null : myEndElement.getElement();
  }

  @Nonnull
  public abstract String getText();

  @Override
  public final void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    applyFix();
  }

  public void applyFix() {
    if (myStartElement == null) return;
    PsiElement startElement = myStartElement.getElement();
    PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
    if (startElement == null || endElement == null) return;
    PsiFile file = startElement.getContainingFile();
    if (file == null) return;
    invoke(file.getProject(), file, startElement, endElement);
  }

  public abstract void invoke(@Nonnull Project project,
                              @Nonnull PsiFile file,
                              @Nonnull PsiElement startElement,
                              @Nonnull PsiElement endElement);

}
