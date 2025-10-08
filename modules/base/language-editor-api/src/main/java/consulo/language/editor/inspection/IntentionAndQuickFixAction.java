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

package consulo.language.editor.inspection;

import consulo.language.editor.intention.IntentionAction;
import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Gregory.Shrago
 */
public abstract class IntentionAndQuickFixAction implements LocalQuickFix, IntentionAction{
  public static IntentionAndQuickFixAction[] EMPTY_ARRAY = new IntentionAndQuickFixAction[0];

  @Override
  @Nonnull
  public abstract LocalizeValue getName();

  public abstract void applyFix(Project project, PsiFile file, @Nullable Editor editor);

  @Override
  @Nonnull
  public final LocalizeValue getText() {
    return getName();
  }

  @Override
  public final void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
    applyFix(project, descriptor.getPsiElement().getContainingFile(), null);
  }

  @Override
  public final void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    applyFix(project, file, editor);
  }

  /**
   *  In general case will be called if invoked as IntentionAction.
   */
  @Override
  public boolean isAvailable(@Nonnull Project project, @Nullable Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
