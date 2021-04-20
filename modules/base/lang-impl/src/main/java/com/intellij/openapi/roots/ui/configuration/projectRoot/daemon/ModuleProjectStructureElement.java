package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.ModuleEditor;
import com.intellij.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ModuleProjectStructureElement extends ProjectStructureElement {
  @Nonnull
  private final ModulesConfigurator myModulesConfigurator;
  @Nonnull
  private final Module myModule;

  public ModuleProjectStructureElement(@Nonnull ModulesConfigurator modulesConfigurator, @Nonnull Module module) {
    myModulesConfigurator = modulesConfigurator;
    myModule = module;
  }

  @Nonnull
  public Module getModule() {
    return myModule;
  }

  public void checkModulesNames(Project project, ProjectStructureProblemsHolder problemsHolder) {
    final ModifiableModuleModel moduleModel = myModulesConfigurator.getModuleModel();
    final Module[] all = moduleModel.getModules();
    if (!ArrayUtil.contains(myModule, all)) {
      return;//module has been deleted
    }

    for (Module each : all) {
      if (each != myModule && myModulesConfigurator.getRealName(each).equals(myModulesConfigurator.getRealName(myModule))) {
        problemsHolder.registerProblem(ProjectBundle.message("project.roots.module.duplicate.name.message"), null, ProjectStructureProblemType.error("duplicate-module-name"), createPlace(), null);
        break;
      }
    }
  }

  @Override
  public void check(Project project, ProjectStructureProblemsHolder problemsHolder) {
    checkModulesNames(project, problemsHolder);

    final ModuleRootModel rootModel = myModulesConfigurator.getRootModel(myModule);
    if (rootModel == null) return; //already disposed
    final OrderEntry[] entries = rootModel.getOrderEntries();
    for (OrderEntry entry : entries) {
      if (!entry.isValid()) {
        if (entry instanceof ModuleExtensionWithSdkOrderEntry && ((ModuleExtensionWithSdkOrderEntry)entry).getSdkName() == null) {
          problemsHolder
                  .registerProblem(ProjectBundle.message("project.roots.module.jdk.problem.message"), null, ProjectStructureProblemType.error("module-sdk-not-defined"), createPlace(entry), null);
        }
        else {
          problemsHolder.registerProblem(ProjectBundle.message("project.roots.library.problem.message", StringUtil.escapeXml(entry.getPresentableName())), null,
                                         ProjectStructureProblemType.error("invalid-module-dependency"), createPlace(entry), null);
        }
      }
      //todo[nik] highlight libraries with invalid paths in ClasspathEditor
      //else if (entry instanceof LibraryOrderEntry) {
      //  final LibraryEx library = (LibraryEx)((LibraryOrderEntry)entry).getLibrary();
      //  if (library != null) {
      //    if (!library.allPathsValid(OrderRootType.CLASSES)) {
      //      problemsHolder.registerError(ProjectBundle.message("project.roots.tooltip.library.misconfigured", entry.getName()));
      //    }
      //    else if (!library.allPathsValid(OrderRootType.SOURCES)) {
      //      problemsHolder.registerWarning(ProjectBundle.message("project.roots.tooltip.library.misconfigured", entry.getName()));
      //    }
      //  }
      //}
    }
  }

  private PlaceInProjectStructure createPlace() {
    return new PlaceInProjectStructureBase(this::modulesNavigator, this);
  }

  @RequiredUIAccess
  private  AsyncResult<Void> modulesNavigator(Project project) {
    return ShowSettingsUtil.getInstance().showProjectStructureDialog(project, projectStructureSelector -> {
      projectStructureSelector.select(myModule.getName(), null, true);
    });
  }

  private PlaceInProjectStructure createPlace(OrderEntry entry) {
    return new PlaceInModuleClasspath(myModulesConfigurator, myModule, this, entry);
  }

  @Override
  public List<ProjectStructureElementUsage> getUsagesInElement() {
    final List<ProjectStructureElementUsage> usages = new ArrayList<>();
    final ModuleEditor moduleEditor = ((ModulesConfiguratorImpl)myModulesConfigurator).getModuleEditor(myModule);
    if (moduleEditor != null) {
      for (OrderEntry entry : moduleEditor.getOrderEntries()) {
        if (entry instanceof ModuleOrderEntry) {
          ModuleOrderEntry moduleOrderEntry = (ModuleOrderEntry)entry;
          final Module module = moduleOrderEntry.getModule();
          if (module != null) {
            usages.add(new UsageInModuleClasspath(myModulesConfigurator, this, new ModuleProjectStructureElement(myModulesConfigurator, module), moduleOrderEntry.getScope()));
          }
        }
        else if (entry instanceof LibraryOrderEntry) {
          LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry)entry;
          final Library library = libraryOrderEntry.getLibrary();
          if (library != null) {
            usages.add(new UsageInModuleClasspath(myModulesConfigurator, this, new LibraryProjectStructureElement(library), libraryOrderEntry.getScope()));
          }
        }
        else if (entry instanceof ModuleExtensionWithSdkOrderEntry) {
          final Sdk jdk = ((ModuleExtensionWithSdkOrderEntry)entry).getSdk();
          if (jdk != null) {
            usages.add(new UsageInModuleClasspath(myModulesConfigurator, this, new SdkProjectStructureElement(jdk), null));
          }
        }
      }
    }
    return usages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleProjectStructureElement)) return false;

    return myModule.equals(((ModuleProjectStructureElement)o).myModule);

  }

  @Override
  public int hashCode() {
    return myModule.hashCode();
  }

  @Override
  public String getPresentableName() {
    return "Module '" + myModule.getName() + "'";
  }

  @Override
  public String getTypeName() {
    return "Module";
  }

  @Override
  public String getId() {
    return "module:" + myModule.getName();
  }
}
