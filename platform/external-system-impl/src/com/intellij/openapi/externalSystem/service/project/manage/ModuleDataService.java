package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectStructureHelper;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtilRt;
import org.consulo.compiler.ModuleCompilerPathsManager;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.RequiredWriteAction;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates functionality of importing gradle module to the intellij project.
 *
 * @author Denis Zhdanov
 * @since 2/7/12 2:49 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class ModuleDataService implements ProjectDataService<ModuleData, Module> {

  public static final com.intellij.openapi.util.Key<ModuleData> MODULE_DATA_KEY = com.intellij.openapi.util.Key.create("MODULE_DATA_KEY");

  private static final Logger LOG = Logger.getInstance("#" + ModuleDataService.class.getName());

  /**
   * We can't modify project modules (add/remove) until it's initialised, so, we delay that activity. Current constant
   * holds number of milliseconds to wait between 'after project initialisation' processing attempts.
   */
  private static final int PROJECT_INITIALISATION_DELAY_MS = (int)TimeUnit.SECONDS.toMillis(1);

  private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SHARED_THREAD);

  @NotNull
  @Override
  public Key<ModuleData> getTargetDataKey() {
    return ProjectKeys.MODULE;
  }

  @Override
  public void importData(@NotNull final Collection<DataNode<ModuleData>> toImport,
                         @NotNull final Project project,
                         final boolean synchronous) {
    if (toImport.isEmpty()) {
      return;
    }
    if (!project.isInitialized()) {
      myAlarm.addRequest(new ImportModulesTask(project, toImport, synchronous), PROJECT_INITIALISATION_DELAY_MS);
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @Override
      public void execute() {
        final Collection<DataNode<ModuleData>> toCreate = filterExistingModules(toImport, project);
        if (!toCreate.isEmpty()) {
          createModules(toCreate, project);
        }
        for (DataNode<ModuleData> node : toImport) {
          Module module = ProjectStructureHelper.findIdeModule(node.getData(), project);
          if (module != null) {
            syncPaths(module, node.getData());
          }
        }
      }
    });
  }

  private void createModules(@NotNull final Collection<DataNode<ModuleData>> toCreate, @NotNull final Project project) {

    final Map<DataNode<ModuleData>, Module> moduleMappings = ContainerUtilRt.newHashMap();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (DataNode<ModuleData> module : toCreate) {
          importModule(moduleManager, module);
        }
      }

      @RequiredWriteAction
      private void importModule(@NotNull ModuleManager moduleManager, @NotNull DataNode<ModuleData> module) {
        ModuleData data = module.getData();
        final Module created = moduleManager.newModule(data.getExternalName(), data.getModuleDirPath());

        // Ensure that the dependencies are clear (used to be not clear when manually removing the module and importing it via gradle)
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(created);
        final ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();

        setModuleOptions(created, module);

        RootPolicy<Object> visitor = new RootPolicy<Object>() {
          @Override
          public Object visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, Object value) {
            moduleRootModel.removeOrderEntry(libraryOrderEntry);
            return value;
          }

          @Override
          public Object visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, Object value) {
            moduleRootModel.removeOrderEntry(moduleOrderEntry);
            return value;
          }
        };
        try {
          for (OrderEntry orderEntry : moduleRootModel.getOrderEntries()) {
            orderEntry.accept(visitor, null);
          }
        }
        finally {
          moduleRootModel.commit();
        }
        moduleMappings.put(module, created);
      }
    });
  }

  @NotNull
  private Collection<DataNode<ModuleData>> filterExistingModules(@NotNull Collection<DataNode<ModuleData>> modules,
                                                                 @NotNull Project project) {
    Collection<DataNode<ModuleData>> result = ContainerUtilRt.newArrayList();
    for (DataNode<ModuleData> node : modules) {
      ModuleData moduleData = node.getData();
      Module module = ProjectStructureHelper.findIdeModule(moduleData, project);
      if (module == null) {
        result.add(node);
      }
      else {
        setModuleOptions(module, node);
      }
    }
    return result;
  }

  private static void syncPaths(@NotNull Module module, @NotNull ModuleData data) {
    ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(module);
    if (compilerPathsManager == null) {
      LOG.warn(String.format("Can't sync paths for module '%s'. Reason: no compiler extension is found for it", module.getName()));
      return;
    }
    compilerPathsManager.setInheritedCompilerOutput(data.isInheritProjectCompileOutputPath());
    if(!data.isInheritProjectCompileOutputPath()) {
      String compileOutputPath = data.getCompileOutputPath(ExternalSystemSourceType.SOURCE);
      if (compileOutputPath != null) {
        compilerPathsManager.setCompilerOutputUrl(ProductionContentFolderTypeProvider.getInstance(), VfsUtilCore.pathToUrl(compileOutputPath));
      }
      String testCompileOutputPath = data.getCompileOutputPath(ExternalSystemSourceType.TEST);
      if (testCompileOutputPath != null) {
        compilerPathsManager.setCompilerOutputUrl(TestContentFolderTypeProvider.getInstance(), VfsUtilCore.pathToUrl(testCompileOutputPath));
      }
    }
  }

  @Override
  public void removeData(@NotNull final Collection<? extends Module> modules, @NotNull final Project project, boolean synchronous) {
    if (modules.isEmpty()) {
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @Override
      @RequiredWriteAction
      public void execute() {
        ModuleManager moduleManager = ModuleManager.getInstance(project);

        for (Module module : modules) {
          if (module.isDisposed()) continue;
          moduleManager.disposeModule(module);
        }
      }
    });
  }

  public static void unlinkModuleFromExternalSystem(@NotNull Module module) {
    module.clearOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
    module.clearOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
    module.clearOption(ExternalSystemConstants.ROOT_PROJECT_PATH_KEY);
  }

  private class ImportModulesTask implements Runnable {

    private final Project myProject;
    private final Collection<DataNode<ModuleData>> myModules;
    private final boolean mySynchronous;

    ImportModulesTask(@NotNull Project project, @NotNull Collection<DataNode<ModuleData>> modules, boolean synchronous) {
      myProject = project;
      myModules = modules;
      mySynchronous = synchronous;
    }

    @Override
    public void run() {
      myAlarm.cancelAllRequests();
      if (!myProject.isInitialized()) {
        myAlarm.addRequest(new ImportModulesTask(myProject, myModules, mySynchronous), PROJECT_INITIALISATION_DELAY_MS);
        return;
      }

      importData(myModules, myProject, mySynchronous);
    }
  }

  private static void setModuleOptions(Module module, DataNode<ModuleData> moduleDataNode) {
    ModuleData moduleData = moduleDataNode.getData();
    module.putUserData(MODULE_DATA_KEY, moduleData);

    module.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, moduleData.getOwner().toString());
    module.setOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY, moduleData.getLinkedExternalProjectPath());
    module.setOption(ExternalSystemConstants.LINKED_PROJECT_ID_KEY, moduleData.getId());
    final ProjectData projectData = moduleDataNode.getData(ProjectKeys.PROJECT);
    module.setOption(ExternalSystemConstants.ROOT_PROJECT_PATH_KEY, projectData != null ? projectData.getLinkedExternalProjectPath() : "");

    if (moduleData.getGroup() != null) {
      module.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_MODULE_GROUP_KEY, moduleData.getGroup());
    }
    if (moduleData.getVersion() != null) {
      module.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_MODULE_VERSION_KEY, moduleData.getVersion());
    }

    // clear maven option
    module.clearOption("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule");
  }
}
