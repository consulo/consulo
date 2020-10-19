package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import javax.annotation.Nullable;
import jakarta.inject.Singleton;

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
        final ModuleEditor moduleEditor = configurator.getModuleEditor(module);
        if (moduleEditor != null) {
          return moduleEditor.getModifiableRootModelProxy();
        }
      }
    }
    return ModuleRootManager.getInstance(module).getModifiableModel();
  }

  @Nullable
  private static ModulesConfigurator getModulesConfigurator(Project project) {
    StructureConfigurableContext context = getProjectStructureContext(project);
    return context != null ? context.getModulesConfigurator() : null;
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
      StructureConfigurableContext context = getProjectStructureContext(project);
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
    StructureConfigurableContext context = getProjectStructureContext(project);
    if (context != null) {
      LibraryTableModifiableModelProvider provider = context.createModifiableModelProvider(LibraryTablesRegistrar.PROJECT_LEVEL);
      return provider.getModifiableModel();
    }
    return LibraryTablesRegistrar.getInstance().getLibraryTable(project).getModifiableModel();
  }

  @Nullable
  private static StructureConfigurableContext getProjectStructureContext(Project project) {
    final ProjectStructureConfigurable structureConfigurable = ProjectStructureConfigurable.getInstance(project);
    return structureConfigurable.getContext() ;
  }
}
