package com.intellij.openapi.externalSystem.service.project;

import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2/6/12 3:28 PM
 */
public class ProjectStructureHelper {
  @Nullable
  public static Module findIdeModule(@NotNull ModuleData module, @NotNull Project ideProject) {
    return findIdeModule(module.getInternalName(), ideProject);
  }

  @Nullable
  public static Module findIdeModule(@NotNull String ideModuleName, @NotNull Project ideProject) {
    return ModuleManager.getInstance(ideProject).findModuleByName(ideModuleName);
  }

  @Nullable
  public static Library findIdeLibrary(@NotNull final LibraryData libraryData, @NotNull Project ideProject) {
    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(ideProject);
    for (Library ideLibrary : libraryTable.getLibraries()) {
      if (ExternalSystemApiUtil.isRelated(ideLibrary, libraryData)) return ideLibrary;
    }
    return null;
  }

  public static boolean isOrphanProjectLibrary(@NotNull final Library library,
                                               @NotNull final Module[] ideModules) {
    RootPolicy<Boolean> visitor = new RootPolicy<Boolean>() {
      @Override
      public Boolean visitLibraryOrderEntry(LibraryOrderEntry ideDependency, Boolean value) {
        return !ideDependency.isModuleLevel() && library == ideDependency.getLibrary();
      }
    };
    for (Module module : ideModules) {
      for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
        if (entry.accept(visitor, false)) return false;
      }
    }
    return true;
  }

  @Nullable
  public static ModuleOrderEntry findIdeModuleDependency(@NotNull ModuleDependencyData dependency, @NotNull ModifiableRootModel model) {
    for (OrderEntry entry : model.getOrderEntries()) {
      if (entry instanceof ModuleOrderEntry) {
        ModuleOrderEntry candidate = (ModuleOrderEntry)entry;
        if (dependency.getInternalName().equals(candidate.getModuleName()) &&
            dependency.getScope().equals(candidate.getScope())) {
          return candidate;
        }
      }
    }
    return null;
  }
}
