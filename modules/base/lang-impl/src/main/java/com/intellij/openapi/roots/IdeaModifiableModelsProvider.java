package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import consulo.ide.settings.impl.ProjectStructureSettingsUtil;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;
import java.lang.reflect.Proxy;

/**
 * @author Dennis.Ushakov
 */
@Singleton
public class IdeaModifiableModelsProvider implements ModifiableModelsProvider {
  @Override
  @Nullable
  public ModifiableRootModel getModuleModifiableModel(final Module module) {
    final Project project = module.getProject();
    final ModulesConfigurator configurator = getModulesConfigurator(project);
    if (configurator != null) {
      if (!configurator.isModuleModelCommitted()) {
        final ModifiableRootModel modelProxy = configurator.getModuleEditorModelProxy(module);
        if (modelProxy != null) {
          return modelProxy;
        }
      }
    }
    return ModuleRootManager.getInstance(module).getModifiableModel();
  }

  @Nullable
  private static ModulesConfigurator getModulesConfigurator(Project project) {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    return util.getModulesModel(project);
  }

  @Override
  public void commitModuleModifiableModel(final ModifiableRootModel model) {
    if (!(model instanceof Proxy)) {
      model.commit();
    }
    //IDEA should commit this model instead of us, because it is was given from StructureConfigurableContext
  }

  @Override
  public void disposeModuleModifiableModel(final ModifiableRootModel model) {
    if (!(model instanceof Proxy)) {
      model.dispose();
    }
    //IDEA should dispose this model instead of us, because it is was given from StructureConfigurableContext
  }

  @Override
  public LibraryTable.ModifiableModel getLibraryTableModifiableModel() {
    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      if (!project.isInitialized()) {
        continue;
      }
      LibrariesConfigurator context = getLibrariesConfigurator(project);
      LibraryTableModifiableModelProvider provider = context != null ? context.createModifiableModelProvider(LibraryTablesRegistrar.APPLICATION_LEVEL) : null;
      final LibraryTable.ModifiableModel modifiableModel = provider != null ? provider.getModifiableModel() : null;
      if (modifiableModel != null) {
        return modifiableModel;
      }
    }
    return LibraryTablesRegistrar.getInstance().getLibraryTable().getModifiableModel();
  }

  @Override
  public LibraryTable.ModifiableModel getLibraryTableModifiableModel(Project project) {
    LibrariesConfigurator context = getLibrariesConfigurator(project);
    if (context != null) {
      LibraryTableModifiableModelProvider provider = context.createModifiableModelProvider(LibraryTablesRegistrar.PROJECT_LEVEL);
      return provider.getModifiableModel();
    }
    return LibraryTablesRegistrar.getInstance().getLibraryTable(project).getModifiableModel();
  }

  @Nullable
  private static LibrariesConfigurator getLibrariesConfigurator(Project project) {
    ProjectStructureSettingsUtil util = (ProjectStructureSettingsUtil)ShowSettingsUtil.getInstance();
    return util.getLibrariesModel(project);
  }
}
