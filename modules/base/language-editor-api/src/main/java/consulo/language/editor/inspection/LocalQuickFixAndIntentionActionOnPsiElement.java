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

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class LocalQuickFixAndIntentionActionOnPsiElement extends LocalQuickFixOnPsiElement implements IntentionAction, SyntheticIntentionAction {
  protected LocalQuickFixAndIntentionActionOnPsiElement(@Nullable PsiElement element) {
    this(element, element);
  }
  protected LocalQuickFixAndIntentionActionOnPsiElement(@Nullable PsiElement startElement, @Nullable PsiElement endElement) {
    super(startElement, endElement);
  }

  @Override
  public final void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (file == null||myStartElement==null) return;
    final PsiElement startElement = myStartElement.getElement();
    final PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
    if (startElement == null || endElement == null) return;
    invoke(project, file, editor, startElement, endElement);
  }

  @Override
  public final boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (myStartElement == null) return false;
    final PsiElement startElement = myStartElement.getElement();
    final PsiElement endElement = myEndElement == null ? startElement : myEndElement.getElement();
    return startElement != null &&
           endElement != null &&
           startElement.isValid() &&
           (endElement == startElement || endElement.isValid()) &&
           file != null &&
           isAvailable(project, file, startElement, endElement);
  }

  /**
   * @param project
   * @param file
   * @param editor is null when called from inspection
   * @param startElement
   * @param endElement
   */
  public abstract void invoke(@Nonnull Project project,
                              @Nonnull PsiFile file,
                              @Nullable Editor editor,
                              @Nonnull PsiElement startElement,
                              @Nonnull PsiElement endElement);

  @Override
  public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
    invoke(project, file, null, startElement, endElement);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
