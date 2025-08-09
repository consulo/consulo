/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInspection.actions;

import consulo.codeEditor.Editor;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class CleanupIntention implements SyntheticIntentionAction, LowPriorityAction {

  protected CleanupIntention() {}

  @Override
  @Nonnull
  public String getText() {
    return InspectionsBundle.message("cleanup.in.scope");
  }

  @Override
  public void invoke(@Nonnull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    if (!FileModificationService.getInstance().preparePsiElementForWrite(file)) return;
    final InspectionManager managerEx = InspectionManager.getInstance(project);
    final GlobalInspectionContextBase globalContext = (GlobalInspectionContextBase)managerEx.createNewGlobalContext(false);
    final AnalysisScope scope = getScope(project, file);
    if (scope != null) {
      final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
      globalContext.codeCleanup(project, scope, profile, getText(), null, false);
    }
  }

  @Nullable
  protected abstract AnalysisScope getScope(Project project, PsiFile file);

  @Override
  public boolean isAvailable(@Nonnull final Project project, final Editor editor, final PsiFile file) {
    return true;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
