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

package com.intellij.codeInspection.actions;

import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.intention.IntentionAction;
import com.intellij.codeInsight.intention.LowPriorityAction;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.InspectionsBundle;
import com.intellij.codeInspection.ex.GlobalInspectionContextBase;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import javax.annotation.Nonnull;

public abstract class CleanupIntention implements IntentionAction, LowPriorityAction {

  protected CleanupIntention() {}

  @Override
  @Nonnull
  public String getText() {
    return getFamilyName();
  }

  @Override
  @Nonnull
  public String getFamilyName() {
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

  @javax.annotation.Nullable
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
