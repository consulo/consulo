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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.artifacts.ArtifactsStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.*;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.util.AsyncResult;
import consulo.util.dataholder.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.storage.HeavyProcessLatch;
import consulo.options.ProjectStructureSelector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class ProjectStructureConfigurable implements SearchableConfigurable, Configurable.HoldPreferredFocusedComponent, Place.Navigator, ProjectStructureSelector {

  public static final Key<ProjectStructureConfigurable> KEY = Key.create("ProjectStructureConfiguration");

  protected final UIState myUiState = new UIState();
  @NonNls
  public static final String CATEGORY = "category";

  public static class UIState {
    public String lastEditedConfigurable;
  }

  private final Project myProject;

  private final ArtifactsStructureConfigurable myArtifactsStructureConfigurable;

  private History myHistory = new History(this);


  private Configurable mySelectedConfigurable;

  private final ProjectSdksModel myProjectSdksModel = new ProjectSdksModel();

  private ProjectConfigurable myProjectConfig;
  private final ProjectLibrariesConfigurable myProjectLibrariesConfig;
  private ModuleStructureConfigurable myModulesConfig;

  private final List<Configurable> myName2Config = new ArrayList<Configurable>();
  private final StructureConfigurableContext myContext;
  private final ModulesConfigurator myModuleConfigurator;
  private SdkListConfigurable mySdkListConfigurable;

  private Consumer<Configurable> myProjectStructureDialog;

  @Inject
  public ProjectStructureConfigurable(final Project project,
                                      final ProjectLibrariesConfigurable projectLibrariesConfigurable,
                                      final ModuleStructureConfigurable moduleStructureConfigurable,
                                      ArtifactsStructureConfigurable artifactsStructureConfigurable) {
    myProject = project;
    myArtifactsStructureConfigurable = artifactsStructureConfigurable;

    myModuleConfigurator = new ModulesConfigurator(myProject);
    myContext = new StructureConfigurableContext(myProject, myModuleConfigurator);
    myModuleConfigurator.setContext(myContext);

    myProjectLibrariesConfig = projectLibrariesConfigurable;
    myModulesConfig = moduleStructureConfigurable;

    myProjectLibrariesConfig.init(myContext);
    myModulesConfig.init(myContext);
    if (!project.isDefault()) {
      myArtifactsStructureConfigurable.init(myContext, myModulesConfig, myProjectLibrariesConfig);
    }

    myProjectConfig = new ProjectConfigurable(project, getContext(), getModuleConfigurator());
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    myUiState.lastEditedConfigurable = propertiesComponent.getValue("project.structure.last.edited");
  }

  public void setProjectStructureDialog(Consumer<Configurable> projectStructureDialog) {
    myProjectStructureDialog = projectStructureDialog;
  }

  public ModulesConfigurator getModuleConfigurator() {
    return myModuleConfigurator;
  }

  @Override
  @Nonnull
  @NonNls
  public String getId() {
    return "project.structure";
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return null;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ProjectBundle.message("project.settings.display.name");
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return mySelectedConfigurable != null ? mySelectedConfigurable.getHelpTopic() : "";
  }

  @Override
  public JComponent createComponent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isModified() {
    for (Configurable each : myName2Config) {
      if (each.isModified()) return true;
    }

    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    for (Configurable each : myName2Config) {
      if (each instanceof BaseStructureConfigurable && each.isModified()) {
        ((BaseStructureConfigurable)each).checkCanApply();
      }
    }

    for (Configurable each : myName2Config) {
      if (each.isModified()) {
        each.apply();
      }
    }

    myContext.getDaemonAnalyzer().clearCaches();
  }

  @Override
  public void reset() {
    // need this to ensure VFS operations will not block because of storage flushing
    // and other maintenance IO tasks run in background
    AccessToken token = HeavyProcessLatch.INSTANCE.processStarted("Resetting Project Structure");

    try {
      myContext.reset();

      myProjectSdksModel.reset();

      Configurable toSelect = null;
      for (Configurable each : myName2Config) {
        if (myUiState.lastEditedConfigurable != null && myUiState.lastEditedConfigurable.equals(each.getDisplayName())) {
          toSelect = each;
        }
        if (each instanceof MasterDetailsComponent) {
          ((MasterDetailsComponent)each).setHistory(myHistory);
        }
        each.reset();
      }

      myHistory.clear();

      if (toSelect == null && myName2Config.size() > 0) {
        toSelect = myName2Config.iterator().next();
      }

      removeSelected();

      navigateTo(toSelect != null ? createPlaceFor(toSelect) : null, false);

    }
    finally {
      token.finish();
    }
  }

  public ArtifactsStructureConfigurable getArtifactsStructureConfigurable() {
    return myArtifactsStructureConfigurable;
  }

  @Override
  public void disposeUIResources() {
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
    propertiesComponent.setValue("project.structure.last.edited", myUiState.lastEditedConfigurable);

    myContext.getDaemonAnalyzer().stop();
    for (Configurable each : myName2Config) {
      each.disposeUIResources();
    }
    myProjectStructureDialog = null;
    myContext.clear();
    myName2Config.clear();
  }

  public History getHistory() {
    return myHistory;
  }

  @Override
  public void setHistory(final History history) {
    myHistory = history;
  }

  @Override
  public void queryPlace(@Nonnull final Place place) {
    place.putPath(CATEGORY, mySelectedConfigurable);
    Place.queryFurther(mySelectedConfigurable, place);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> selectProjectGeneralSettings(final boolean requestFocus) {
    Place place = createPlaceFor(myProjectConfig);
    return navigateTo(place, requestFocus);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nullable final String moduleToSelect, @Nullable String editorNameToSelect, final boolean requestFocus) {
    Place place = createModulesPlace();
    if (moduleToSelect != null) {
      final Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleToSelect);
      assert module != null;
      place = place.putPath(ModuleStructureConfigurable.TREE_OBJECT, module).putPath(ModuleEditor.SELECTED_EDITOR_NAME, editorNameToSelect);
    }
    return navigateTo(place, requestFocus);
  }

  public Place createModulesPlace() {
    return createPlaceFor(myModulesConfig);
  }

  public Place createModulePlace(@Nonnull Module module) {
    return createModulesPlace().putPath(ModuleStructureConfigurable.TREE_OBJECT, module);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nonnull Sdk sdk, final boolean requestFocus) {
    Place place = createPlaceFor(mySdkListConfigurable);
    place.putPath(BaseStructureConfigurable.TREE_NAME, sdk.getName());
    return navigateTo(place, requestFocus);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> selectProjectOrGlobalLibrary(@Nonnull Library library, boolean requestFocus) {
    Place place = createProjectOrGlobalLibraryPlace(library);
    return navigateTo(place, requestFocus);
  }

  public Place createProjectOrGlobalLibraryPlace(Library library) {
    Place place = createPlaceFor(getConfigurableFor(library));
    place.putPath(BaseStructureConfigurable.TREE_NAME, library.getName());
    return place;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nullable Artifact artifact, boolean requestFocus) {
    Place place = createArtifactPlace(artifact);
    return navigateTo(place, requestFocus);
  }

  public Place createArtifactPlace(Artifact artifact) {
    Place place = createPlaceFor(myArtifactsStructureConfigurable);
    if (artifact != null) {
      place.putPath(BaseStructureConfigurable.TREE_NAME, artifact.getName());
    }
    return place;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> select(@Nonnull LibraryOrderEntry libraryOrderEntry, final boolean requestFocus) {
    final Library lib = libraryOrderEntry.getLibrary();
    if (lib == null || lib.getTable() == null) {
      return selectOrderEntry(libraryOrderEntry.getOwnerModule(), libraryOrderEntry);
    }
    Place place = createPlaceFor(getConfigurableFor(lib));
    place.putPath(BaseStructureConfigurable.TREE_NAME, libraryOrderEntry.getLibraryName());
    return navigateTo(place, requestFocus);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> selectOrderEntry(@Nonnull final Module module, @Nullable final OrderEntry orderEntry) {
    return ModuleStructureConfigurable.getInstance(myProject).selectOrderEntry(module, orderEntry);
  }

  @Override
  public AsyncResult<Void> navigateTo(@Nullable final Place place, final boolean requestFocus) {
    if(myProjectStructureDialog == null) {
      return AsyncResult.rejected();
    }
    final Configurable toSelect = (Configurable)place.getPath(CATEGORY);

    if (mySelectedConfigurable != toSelect) {
      if (mySelectedConfigurable instanceof BaseStructureConfigurable) {
        ((BaseStructureConfigurable)mySelectedConfigurable).onStructureUnselected();
      }
      removeSelected();

      if (toSelect != null) {
        myProjectStructureDialog.accept(toSelect);
      }

      setSelectedConfigurable(toSelect);

      if (toSelect instanceof MasterDetailsComponent) {
        final MasterDetailsComponent masterDetails = (MasterDetailsComponent)toSelect;

        masterDetails.setHistory(myHistory);
      }

      if (toSelect instanceof BaseStructureConfigurable) {
        ((BaseStructureConfigurable)toSelect).onStructureSelected();
      }
    }

    final AsyncResult<Void> result = AsyncResult.undefined();
    Place.goFurther(toSelect, place, requestFocus).notifyWhenDone(result);

    if (!myHistory.isNavigatingNow() && mySelectedConfigurable != null) {
      myHistory.pushQueryPlace();
    }

    return result;
  }

  public void setSelectedConfigurable(Configurable toSelect) {
    mySelectedConfigurable = toSelect;
    if (mySelectedConfigurable != null) {
      myUiState.lastEditedConfigurable = mySelectedConfigurable.getDisplayName();
    }
  }

  private void removeSelected() {
    mySelectedConfigurable = null;
    myUiState.lastEditedConfigurable = null;
  }

  public static ProjectStructureConfigurable getInstance(final Project project) {
    return ServiceManager.getService(project, ProjectStructureConfigurable.class);
  }

  public ProjectSdksModel getProjectSdksModel() {
    return myProjectSdksModel;
  }

  public SdkListConfigurable getSdkConfigurable() {
    if(mySdkListConfigurable == null) {
      mySdkListConfigurable = SdkListConfigurable.getInstance(myProject);
      mySdkListConfigurable.init(getContext());
    }
    return mySdkListConfigurable;
  }

  public ProjectLibrariesConfigurable getProjectLibrariesConfigurable() {
    return myProjectLibrariesConfig;
  }

  public ModuleStructureConfigurable getModulesConfigurable() {
    return myModulesConfig;
  }

  public ProjectConfigurable getProjectConfigurable() {
    return myProjectConfig;
  }

  public void setConfigurablesForDispose(List<Configurable> name2Config) {
    myName2Config.clear();
    myName2Config.addAll(name2Config);
    // we need up it
    ContainerUtil.swapElements(myName2Config, ContainerUtil.indexOf(myName2Config, mySdkListConfigurable), 0);
    ContainerUtil.swapElements(myName2Config, ContainerUtil.indexOf(myName2Config, myProjectLibrariesConfig), 1);
  }

  private static Place createPlaceFor(final Configurable configurable) {
    return new Place().putPath(CATEGORY, configurable);
  }

  public StructureConfigurableContext getContext() {
    return myContext;
  }

  public BaseLibrariesConfigurable getConfigurableFor(final Library library) {
    if (LibraryTablesRegistrar.PROJECT_LEVEL.equals(library.getTable().getTableLevel())) {
      return myProjectLibrariesConfig;
    } else {
      return null;
    }
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }
}
