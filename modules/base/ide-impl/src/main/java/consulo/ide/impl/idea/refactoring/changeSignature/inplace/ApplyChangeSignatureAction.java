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
package consulo.ide.impl.idea.refactoring.changeSignature.inplace;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApplyChangeSignatureAction extends BaseRefactoringIntentionAction {
  public static final String CHANGE_SIGNATURE = "Apply signature change";
  private final String myMethodName;

  public ApplyChangeSignatureAction(String methodName) {
    myMethodName = methodName;
  }

  @Nonnull
  @Override
  public String getText() {
    return RefactoringBundle.message("changing.signature.of.0", myMethodName);
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return CHANGE_SIGNATURE;
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
    final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetector.forLanguage(element.getLanguage());
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
    final String initialSignature = signatureGestureDetector.getInitialSignature();
    final ChangeInfo currentInfo = signatureGestureDetector.getCurrentInfo();
    signatureGestureDetector.detach();

    final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetector.forLanguage(element.getLanguage());

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
