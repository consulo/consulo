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

import consulo.application.Application;
import consulo.content.scope.SearchScope;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.analysis.AnalysisUIOptions;
import consulo.ide.impl.idea.analysis.BaseAnalysisActionDialog;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerEx;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameFilter;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNamePopup;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.psi.*;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author Konstantin Bulenkov
 */
public class RunInspectionAction extends GotoActionBase {
  private static final Logger LOGGER = Logger.getInstance(RunInspectionAction.class);

  public RunInspectionAction() {
    getTemplatePresentation().setTextValue(IdeLocalize.gotoInspectionActionText());
  }

  @Override
  protected void gotoActionPerformed(final AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null) return;

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final PsiElement psiElement = e.getDataContext().getData(PsiElement.KEY);
    final PsiFile psiFile = e.getDataContext().getData(PsiFile.KEY);
    final VirtualFile virtualFile = e.getDataContext().getData(VirtualFile.KEY);

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.inspection");

    final GotoInspectionModel model = new GotoInspectionModel(project);
    showNavigationPopup(e, model, new GotoActionCallback<>() {
      @Override
      protected ChooseByNameFilter<Object> createFilter(@Nonnull ChooseByNamePopup popup) {
        popup.setSearchInAnyPlace(true);
        return super.createFilter(popup);
      }

      @Override
      public void elementChosen(ChooseByNamePopup popup, final Object element) {
        Application.get().invokeLater(
          () -> runInspection(project, ((InspectionToolWrapper)element).getShortName(), virtualFile, psiElement, psiFile)
        );
      }
    }, false);
  }

  private static void runInspection(
    @Nonnull Project project,
    @Nonnull String shortName,
    @Nullable VirtualFile virtualFile,
    PsiElement psiElement,
    PsiFile psiFile
  ) {
    final InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final Module module = virtualFile != null ? ModuleUtilCore.findModuleForFile(virtualFile, project) : null;

    AnalysisScope analysisScope = null;
    if (psiFile != null) {
      analysisScope = new AnalysisScope(psiFile);
    }
    else {
      if (virtualFile != null && virtualFile.isDirectory()) {
        final PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile);
        if (psiDirectory != null) {
          analysisScope = new AnalysisScope(psiDirectory);
        }
      }
      if (analysisScope == null && virtualFile != null) {
        analysisScope = new AnalysisScope(project, Arrays.asList(virtualFile));
      }
      if (analysisScope == null) {
        analysisScope = new AnalysisScope(project);
      }
    }

    final FileFilterPanel fileFilterPanel = new FileFilterPanel();
    fileFilterPanel.init();

    final BaseAnalysisActionDialog dialog = new BaseAnalysisActionDialog(
      AnalysisScopeLocalize.specifyAnalysisScope(InspectionLocalize.inspectionActionTitle()).get(),
      AnalysisScopeLocalize.analysisScopeTitle(InspectionLocalize.inspectionActionNoun()).get(),
      project,
      analysisScope,
      module != null ? module.getName() : null,
      true,
      AnalysisUIOptions.getInstance(project), psiElement
    ) {

      @Override
      protected JComponent getAdditionalActionSettings(Project project) {
        return fileFilterPanel.getPanel();
      }

      @Nonnull
      @Override
      public AnalysisScope getScope(
        @Nonnull AnalysisUIOptions uiOptions,
        @Nonnull AnalysisScope defaultScope,
        @Nonnull Project project,
        Module module
      ) {
        final AnalysisScope scope = super.getScope(uiOptions, defaultScope, project, module);
        final SearchScope filterScope = fileFilterPanel.getSearchScope();
        if (filterScope == null) {
          return scope;
        }
        final SearchScope filteredScope = filterScope.intersectWith(scope.toSearchScope());
        return new AnalysisScope(filteredScope, project);
      }
    };

    if (!dialog.showAndGet()) {
      return;
    }
    final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    AnalysisScope scope = dialog.getScope(uiOptions, analysisScope, project, module);
    PsiElement element = psiFile == null ? psiElement : psiFile;
    final InspectionProfile currentProfile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    final InspectionToolWrapper toolWrapper = currentProfile.getInspectionTool(shortName, project);
    LOGGER.assertTrue(toolWrapper != null, "Missed inspection: " + shortName);
    RunInspectionIntention.rerunInspection(toolWrapper, managerEx, scope, element);
  }
}
