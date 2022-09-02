package consulo.ide.impl.idea.openapi.externalSystem.service.project;

import consulo.externalSystem.model.project.LibraryData;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.project.ModuleDependencyData;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.layer.orderEntry.RootPolicy;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.roots.impl.libraries.ProjectLibraryTable;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2/6/12 3:28 PM
 */
public class ProjectStructureHelper {
  @Nullable
  public static Module findIdeModule(@Nonnull ModuleData module, @Nonnull Project ideProject) {
    return findIdeModule(module.getInternalName(), ideProject);
  }

  @Nullable
  public static Module findIdeModule(@Nonnull String ideModuleName, @Nonnull Project ideProject) {
    return ModuleManager.getInstance(ideProject).findModuleByName(ideModuleName);
  }

  @Nullable
  public static Library findIdeLibrary(@Nonnull final LibraryData libraryData, @Nonnull Project ideProject) {
    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(ideProject);
    for (Library ideLibrary : libraryTable.getLibraries()) {
      if (ExternalSystemApiUtil.isRelated(ideLibrary, libraryData)) return ideLibrary;
    }
    return null;
  }

  public static boolean isOrphanProjectLibrary(@Nonnull final Library library,
                                               @Nonnull final Module[] ideModules) {
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
  public static ModuleOrderEntry findIdeModuleDependency(@Nonnull ModuleDependencyData dependency, @Nonnull ModifiableRootModel model) {
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
