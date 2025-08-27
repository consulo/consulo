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
package consulo.ide.impl.idea.codeInspection.actions;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerImpl;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "CodeInspection.OnEditor")
public class CodeInspectionOnEditorAction extends AnAction {
    public CodeInspectionOnEditorAction() {
        super(ActionLocalize.actionCodeinspectionOneditorText(), ActionLocalize.actionCodeinspectionOneditorDescription());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiFile psiFile = dataContext.getData(PsiFile.KEY);
        if (psiFile != null) {
            analyze(project, psiFile);
        }
    }

    protected static void analyze(Project project, PsiFile psiFile) {
        FileDocumentManager.getInstance().saveAllDocuments();
        InspectionManagerImpl inspectionManagerEx = (InspectionManagerImpl) InspectionManager.getInstance(project);
        AnalysisScope scope = new AnalysisScope(psiFile);
        GlobalInspectionContextImpl inspectionContext = inspectionManagerEx.createNewGlobalContext(false);
        inspectionContext.setCurrentScope(scope);
        InspectionProfile inspectionProfile =
            InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
        inspectionContext.setExternalProfile(inspectionProfile);
        inspectionContext.doInspections(scope);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        PsiFile psiFile = e.getData(PsiFile.KEY);
        e.getPresentation().setEnabled(
            project != null && psiFile != null && DaemonCodeAnalyzer.getInstance(project).isHighlightingAvailable(psiFile)
        );
    }
}
