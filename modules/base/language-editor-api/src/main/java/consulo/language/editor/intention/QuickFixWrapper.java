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
package consulo.language.editor.intention;

import consulo.codeEditor.Editor;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.QuickFix;
import consulo.language.editor.util.LanguageUndoUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class QuickFixWrapper implements IntentionAction, SyntheticIntentionAction {
  private static final Logger LOG = Logger.getInstance(QuickFixWrapper.class);

  private final ProblemDescriptor myDescriptor;
  private final int myFixNumber;


  @Nonnull
  public static IntentionAction wrap(@Nonnull ProblemDescriptor descriptor, int fixNumber) {
    LOG.assertTrue(fixNumber >= 0, fixNumber);
    QuickFix[] fixes = descriptor.getFixes();
    LOG.assertTrue(fixes != null && fixes.length > fixNumber);

    QuickFix fix = fixes[fixNumber];
    return fix instanceof IntentionAction ? (IntentionAction)fix : new QuickFixWrapper(descriptor, fixNumber);
  }

  private QuickFixWrapper(@Nonnull ProblemDescriptor descriptor, int fixNumber) {
    myDescriptor = descriptor;
    myFixNumber = fixNumber;
  }

  @Override
  @Nonnull
  public LocalizeValue getText() {
    return myDescriptor.getFixes()[myFixNumber].getName();
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    PsiElement psiElement = myDescriptor.getPsiElement();
    if (psiElement == null || !psiElement.isValid()) return false;
    LocalQuickFix fix = getFix();
    return !(fix instanceof IntentionAction) || ((IntentionAction)fix).isAvailable(project, editor, file);
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    //if (!CodeInsightUtil.prepareFileForWrite(file)) return;
    // consider all local quick fixes do it themselves

    PsiElement element = myDescriptor.getPsiElement();
    PsiFile fileForUndo = element == null ? null : element.getContainingFile();
    LocalQuickFix fix = getFix();
    fix.applyFix(project, myDescriptor);
    DaemonCodeAnalyzer.getInstance(project).restart();
    if (fileForUndo != null && !fileForUndo.equals(file)) {
      LanguageUndoUtil.markPsiFileForUndo(fileForUndo);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return getFix().startInWriteAction();
  }

  public LocalQuickFix getFix() {
    return (LocalQuickFix)myDescriptor.getFixes()[myFixNumber];
  }

  @Override
  public String toString() {
    return getText().get();
  }
}
