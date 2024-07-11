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

import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.analysis.BaseAnalysisAction;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.ide.impl.idea.codeInspection.ex.InspectionManagerEx;
import consulo.ide.impl.idea.openapi.options.ex.SingleConfigurableEditor;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.ide.impl.idea.profile.codeInspection.ui.ErrorsConfigurable;
import consulo.ide.impl.idea.profile.codeInspection.ui.IDEInspectionToolsConfigurable;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.inspection.InspectionsBundle;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.*;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.MutableListModel;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

public class CodeInspectionAction extends BaseAnalysisAction {
  private GlobalInspectionContextImpl myGlobalInspectionContext = null;
  protected InspectionProfile myExternalProfile = null;

  public CodeInspectionAction() {
    super(InspectionsBundle.message("inspection.action.title"), InspectionsBundle.message("inspection.action.noun"));
  }

  public CodeInspectionAction(String title, String analysisNoon) {
    super(title, analysisNoon);
  }

  @Override
  protected void analyze(@Nonnull Project project, @Nonnull AnalysisScope scope) {
    try {
      runInspections(project, scope);
    }
    finally {
      myGlobalInspectionContext = null;
      myExternalProfile = null;
    }
  }

  protected void runInspections(Project project, AnalysisScope scope) {
    scope.setSearchInLibraries(false);
    FileDocumentManager.getInstance().saveAllDocuments();
    final GlobalInspectionContextImpl inspectionContext = getGlobalInspectionContext(project);
    inspectionContext.setExternalProfile(myExternalProfile);
    inspectionContext.setCurrentScope(scope);
    inspectionContext.doInspections(scope);
  }


  private GlobalInspectionContextImpl getGlobalInspectionContext(Project project) {
    if (myGlobalInspectionContext == null) {
      myGlobalInspectionContext = ((InspectionManagerEx)InspectionManager.getInstance(project)).createNewGlobalContext(false);
    }
    return myGlobalInspectionContext;
  }

  @Override
  @NonNls
  protected String getHelpTopic() {
    return "reference.dialogs.inspection.scope";
  }

  @Override
  protected void canceled() {
    super.canceled();
    myGlobalInspectionContext = null;
  }

  @RequiredUIAccess
  @Override
  protected void extendMainLayout(BaseAnalysisActionDialog dialog, VerticalLayout layout, Project project) {
    dialog.setAnalyzeInjectedCode(true);

    MutableListModel<InspectionProfile> model = MutableListModel.of(List.of());
    ComboBox<InspectionProfile> profiles = ComboBox.create(model);
    profiles.setRender((render, index, profile) -> {
      if (profile != null) {
        render.append(profile.getName());
        render.withIcon(PlatformIconGroup.generalGearplain());
      }
    });

    DockLayout dockLayout = DockLayout.create();
    dockLayout.left(Label.create(InspectionLocalize.inspectionProfileLabel()));
    dockLayout.center(profiles);
    layout.add(dockLayout);

    final InspectionManagerEx manager = (InspectionManagerEx)InspectionManager.getInstance(project);
    final InspectionProfileManager profileManager = InspectionProfileManager.getInstance();
    final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
    reloadProfiles(profiles, model, profileManager, projectProfileManager, manager);

    Hyperlink hyperlink = Hyperlink.create(IdeLocalize.buttonConfigureE().get(), event -> {
      final IDEInspectionToolsConfigurable errorConfigurable = createConfigurable(projectProfileManager, profileManager);
      final MySingleConfigurableEditor editor = new MySingleConfigurableEditor(project, errorConfigurable, manager);
      errorConfigurable.selectProfile(profiles.getValue());
      if (editor.showAndGet()) {
        reloadProfiles(profiles, model, profileManager, projectProfileManager, manager);
      }
      else {
        //if profile was disabled and cancel after apply was pressed
        final InspectionProfile profile = (InspectionProfile)profiles.getValue();
        final boolean canExecute = profile != null && profile.isExecutable(project);
        dialog.setOKActionEnabled(canExecute);
      }
    });

    dockLayout.right(hyperlink);

    profiles.addValueListener(event -> {
      myExternalProfile = profiles.getValue();
      final boolean canExecute = myExternalProfile != null && myExternalProfile.isExecutable(project);
      dialog.setOKActionEnabled(canExecute);
      if (canExecute) {
        manager.setProfile(myExternalProfile.getName());
      }
    });

    final InspectionProfile profile = profiles.getValue();
    dialog.setOKActionEnabled(profile != null && profile.isExecutable(project));
  }

  protected IDEInspectionToolsConfigurable createConfigurable(InspectionProjectProfileManager projectProfileManager,
                                                              InspectionProfileManager profileManager) {
    return new IDEInspectionToolsConfigurable(projectProfileManager, profileManager);
  }

  @RequiredUIAccess
  private void reloadProfiles(ComboBox<InspectionProfile> profiles,
                              MutableListModel<InspectionProfile> model,
                              InspectionProfileManager inspectionProfileManager,
                              InspectionProjectProfileManager inspectionProjectProfileManager,
                              InspectionManagerEx inspectionManager) {
    final InspectionProfile selectedProfile = getGlobalInspectionContext(inspectionManager.getProject()).getCurrentProfile();

    List<InspectionProfile> resultItems = new ArrayList<>();
    fillModel(inspectionProfileManager, resultItems);
    fillModel(inspectionProjectProfileManager, resultItems);
    model.replaceAll(resultItems);
    profiles.setValue(selectedProfile);
  }

  private static void fillModel(final ProfileManager inspectionProfileManager, List<InspectionProfile> model) {
    for (Profile profile : inspectionProfileManager.getProfiles()) {
      model.add((InspectionProfile)profile);
    }
  }

  private static class MySingleConfigurableEditor extends SingleConfigurableEditor {
    private final InspectionManagerEx myManager;

    public MySingleConfigurableEditor(final Project project, final ErrorsConfigurable configurable, InspectionManagerEx manager) {
      super(project, configurable, createDimensionKey(configurable));
      myManager = manager;
      setTitle(configurable.getDisplayName());
    }


    @RequiredUIAccess
    @Override
    protected void doOKAction() {
      final Object o = ((ErrorsConfigurable)getConfigurable()).getSelectedObject();
      if (o instanceof InspectionProfile) {
        myManager.setProfile(((InspectionProfile)o).getName());
      }
      super.doOKAction();
    }
  }
}
