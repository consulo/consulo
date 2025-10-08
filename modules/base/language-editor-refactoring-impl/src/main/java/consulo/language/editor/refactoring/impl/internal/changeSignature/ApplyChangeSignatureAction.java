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
package consulo.language.editor.refactoring.impl.internal.changeSignature;

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.inplace.InplaceChangeSignature;
import consulo.language.editor.refactoring.changeSignature.inplace.LanguageChangeSignatureDetector;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ApplyChangeSignatureAction extends BaseRefactoringIntentionAction implements SyntheticIntentionAction {
  private final String myMethodName;

  public ApplyChangeSignatureAction(String methodName) {
    myMethodName = methodName;
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return RefactoringLocalize.changingSignatureOf0(myMethodName);
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
    LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetector.forLanguage(element.getLanguage());
    if (detector != null) {
      InplaceChangeSignature changeSignature = InplaceChangeSignature.getCurrentRefactoring(editor);
      ChangeInfo currentInfo = changeSignature != null ? changeSignature.getCurrentInfo() : null;
      if (currentInfo != null && detector.isChangeSignatureAvailableOnElement(element, currentInfo)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
    InplaceChangeSignature signatureGestureDetector = InplaceChangeSignature.getCurrentRefactoring(editor);
    String initialSignature = signatureGestureDetector.getInitialSignature();
    ChangeInfo currentInfo = signatureGestureDetector.getCurrentInfo();
    signatureGestureDetector.detach();

    LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetector.forLanguage(element.getLanguage());

    detector.performChange(currentInfo, editor, initialSignature);
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@Nonnull PsiFile file) {
    return file;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
