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

import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.analysis.AnalysisUIOptions;
import consulo.ide.impl.idea.analysis.BaseAnalysisActionDialog;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerEx;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.impl.internal.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;

/**
 * User: anna
 * Date: 21-Feb-2006
 */
public class RunInspectionIntention implements IntentionAction, HighPriorityAction, SyntheticIntentionAction {
  private final String myShortName;

  public RunInspectionIntention(@Nonnull InspectionToolWrapper toolWrapper) {
    myShortName = toolWrapper.getShortName();
  }

  public RunInspectionIntention(final HighlightDisplayKey key) {
    myShortName = key.toString();
  }

  @Override
  @Nonnull
  public String getText() {
    return InspectionsBundle.message("run.inspection.on.file.intention.text");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return LocalInspectionToolWrapper.findTool2RunInBatch(project, file, myShortName) != null;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final InspectionManagerEx managerEx = (InspectionManagerEx)InspectionManager.getInstance(project);
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    AnalysisScope analysisScope = new AnalysisScope(file);
    final VirtualFile virtualFile = file.getVirtualFile();
    if (file.isPhysical() || virtualFile == null || !virtualFile.isInLocalFileSystem()) {
      analysisScope = new AnalysisScope(project);
    }
    final BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(AnalysisScopeBundle.message("specify.analysis.scope", InspectionsBundle.message("inspection.action.title")),
                                                                      AnalysisScopeBundle.message("analysis.scope.title", InspectionsBundle.message("inspection.action.noun")),
                                                                      project,
                                                                      analysisScope,
                                                                      module != null ? module.getName() : null,
                                                                      true, AnalysisUIOptions.getInstance(project), file);
    dlg.show();
    if (!dlg.isOK()) return;
    final AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
    analysisScope = dlg.getScope(uiOptions, analysisScope, project, module);
    rerunInspection(LocalInspectionToolWrapper.findTool2RunInBatch(project, file, myShortName), managerEx, analysisScope, file);
  }

  public static void rerunInspection(@Nonnull InspectionToolWrapper toolWrapper,
                                     @Nonnull InspectionManagerEx managerEx,
                                     @Nonnull AnalysisScope scope,
                                     PsiElement psiElement) {
    GlobalInspectionContextImpl inspectionContext = createContext(toolWrapper, managerEx, psiElement);
    inspectionContext.doInspections(scope);
  }

  public static GlobalInspectionContextImpl createContext(@Nonnull InspectionToolWrapper toolWrapper,
                                                          @Nonnull InspectionManagerEx managerEx,
                                                          PsiElement psiElement) {
    final InspectionProfileImpl rootProfile = (InspectionProfileImpl)InspectionProfileManager.getInstance().getRootProfile();
    LinkedHashSet<InspectionToolWrapper> allWrappers = new LinkedHashSet<InspectionToolWrapper>();
    allWrappers.add(toolWrapper);
    rootProfile.collectDependentInspections(toolWrapper, allWrappers, managerEx.getProject());
    InspectionToolWrapper[] toolWrappers = allWrappers.toArray(new InspectionToolWrapper[allWrappers.size()]);
    final InspectionProfileImpl model = InspectionProfileImpl.createSimple(toolWrapper.getDisplayName(), managerEx.getProject(), toolWrappers);
    try {
      Element element = new Element("toCopy");
      for (InspectionToolWrapper wrapper : toolWrappers) {
        wrapper.writeExternal(element);
        InspectionToolWrapper tw = psiElement == null ? model.getInspectionTool(wrapper.getShortName(), managerEx.getProject())
                                                      : model.getInspectionTool(wrapper.getShortName(), psiElement);
        tw.readExternal(element);
      }
    }
    catch (WriteExternalException | InvalidDataException ignored) {
    }
    model.setEditable(toolWrapper.getDisplayName());
    final GlobalInspectionContextImpl inspectionContext = managerEx.createNewGlobalContext(false);
    inspectionContext.setExternalProfile(model);
    return inspectionContext;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
