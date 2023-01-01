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
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.intention.SuppressIntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;

public class SuppressIntentionActionFromFix extends SuppressIntentionAction {
  private final SuppressQuickFix myFix;

  private SuppressIntentionActionFromFix(@Nonnull SuppressQuickFix fix) {
    myFix = fix;
  }

  @Nonnull
  public static SuppressIntentionAction convertBatchToSuppressIntentionAction(@Nonnull final SuppressQuickFix fix) {
    return new SuppressIntentionActionFromFix(fix);
  }

  @Nonnull
  public static SuppressIntentionAction[] convertBatchToSuppressIntentionActions(@Nonnull SuppressQuickFix[] actions) {
    return ContainerUtil.map2Array(actions, SuppressIntentionAction.class, fix -> convertBatchToSuppressIntentionAction(fix));
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException {
    PsiElement container = getContainer(element);
    boolean caretWasBeforeStatement = editor != null && container != null && editor.getCaretModel().getOffset() == container.getTextRange().getStartOffset();
    InspectionManager inspectionManager = InspectionManager.getInstance(project);
    ProblemDescriptor descriptor = inspectionManager.createProblemDescriptor(element, element, "", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false);
    myFix.applyFix(project, descriptor);

    if (caretWasBeforeStatement) {
      editor.getCaretModel().moveToOffset(container.getTextRange().getStartOffset());
    }
  }

  public ThreeState isShouldBeAppliedToInjectionHost() {
    return myFix instanceof InjectionAwareSuppressQuickFix
           ? ((InjectionAwareSuppressQuickFix)myFix).isShouldBeAppliedToInjectionHost()
           : ThreeState.UNSURE;
  }

  public PsiElement getContainer(PsiElement element) {
    return myFix instanceof AbstractBatchSuppressByNoInspectionCommentFix
           ? ((AbstractBatchSuppressByNoInspectionCommentFix )myFix).getContainer(element) : null;
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
    return myFix.isAvailable(project, element);
  }

  @Nonnull
  @Override
  public String getText() {
    return myFix.getName();
  }
}
