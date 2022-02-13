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

package com.intellij.codeInspection.actions;

import consulo.language.editor.scope.AnalysisScope;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.openapi.actionSystem.*;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

public class CodeInspectionOnEditorAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null){
      return;
    }
    PsiFile psiFile = dataContext.getData(CommonDataKeys.PSI_FILE);
    if (psiFile != null){
      analyze(project, psiFile);
    }
  }

  protected static void analyze(Project project, PsiFile psiFile) {
    FileDocumentManager.getInstance().saveAllDocuments();
    final InspectionManagerEx inspectionManagerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final AnalysisScope scope = new AnalysisScope(psiFile);
    final GlobalInspectionContextImpl inspectionContext = inspectionManagerEx.createNewGlobalContext(false);
    inspectionContext.setCurrentScope(scope);
    final InspectionProfile inspectionProfile =
      InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    inspectionContext.setExternalProfile(inspectionProfile);
    inspectionContext.doInspections(scope);
  }

  @Override
  public void update(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final PsiFile psiFile = dataContext.getData(CommonDataKeys.PSI_FILE);
    e.getPresentation().setEnabled(project != null && psiFile != null  && DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(psiFile));
  }
}
