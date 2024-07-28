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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.ide.impl.idea.codeInspection.actions.CleanupIntention;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

/**
 * Created by anna on 5/13/2014.
 */
public class CleanupOnScopeIntention extends CleanupIntention {
  public static final CleanupOnScopeIntention INSTANCE = new CleanupOnScopeIntention();

  private CleanupOnScopeIntention() {}

  @Nullable
  @Override                               
  @RequiredUIAccess
  protected AnalysisScope getScope(final Project project, final PsiFile file) {
    final Module module = file.getModule();
    AnalysisScope analysisScope = new AnalysisScope(file);
    final VirtualFile virtualFile = file.getVirtualFile();
    if (file.isPhysical() || virtualFile == null || !virtualFile.isInLocalFileSystem()) {
      analysisScope = new AnalysisScope(project);
    }
    final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
      AnalysisScopeLocalize.specifyAnalysisScope(InspectionLocalize.inspectionActionTitle()).get(),
      AnalysisScopeLocalize.analysisScopeTitle(InspectionLocalize.inspectionActionNoun()).get(),
      project,
      analysisScope,
      module != null ? module.getName() : null,
      true,
      AnalysisUIOptions.getInstance(project),
      file
    );
    dlg.show();
    if (!dlg.isOK()) return null;
    final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    return dlg.getScope(uiOptions, analysisScope, project, module);
  }
}
