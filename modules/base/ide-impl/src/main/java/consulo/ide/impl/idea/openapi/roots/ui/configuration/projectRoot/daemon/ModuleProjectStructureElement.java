package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModulesConfiguratorImpl;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

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
        problemsHolder.registerProblem(
          ProjectLocalize.projectRootsModuleDuplicateNameMessage().get(),
          null,
          ProjectStructureProblemType.error("duplicate-module-name"),
          createPlace(),
          null
        );
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
          problemsHolder.registerProblem(
            ProjectLocalize.projectRootsModuleJdkProblemMessage().get(),
            null,
            ProjectStructureProblemType.error("module-sdk-not-defined"),
            createPlace(entry),
            null
          );
        }
        else {
          problemsHolder.registerProblem(
            ProjectLocalize.projectRootsLibraryProblemMessage(StringUtil.escapeXml(entry.getPresentableName())).get(),
            null,
            ProjectStructureProblemType.error("invalid-module-dependency"),
            createPlace(entry),
            null
          );
        }
      }
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
