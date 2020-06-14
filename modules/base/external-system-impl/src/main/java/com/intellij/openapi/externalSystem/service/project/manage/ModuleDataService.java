package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
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
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtilRt;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.annotation.access.RequiredWriteAction;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.externalSystem.module.extension.ExternalSystemMutableModuleExtension;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates functionality of importing gradle module to the intellij project.
 *
 * @author Denis Zhdanov
 * @since 2/7/12 2:49 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public class ModuleDataService implements ProjectDataService<ModuleData, Module> {
  public static ModuleDataService getInstance() {
    return EP_NAME.findExtension(ModuleDataService.class);
  }

  public static final consulo.util.dataholder.Key<ModuleData> MODULE_DATA_KEY = consulo.util.dataholder.Key.create("MODULE_DATA_KEY");

  private static final Logger LOG = Logger.getInstance(ModuleDataService.class);

  /**
   * We can't modify project modules (add/remove) until it's initialised, so, we delay that activity. Current constant
   * holds number of milliseconds to wait between 'after project initialisation' processing attempts.
   */
  private static final int PROJECT_INITIALISATION_DELAY_MS = (int)TimeUnit.SECONDS.toMillis(1);

  private Future<?> myFuture = CompletableFuture.completedFuture(null);

  @Nonnull
  @Override
  public Key<ModuleData> getTargetDataKey() {
    return ProjectKeys.MODULE;
  }

  @Override
  public void importData(@Nonnull final Collection<DataNode<ModuleData>> toImport, @Nonnull final Project project, final boolean synchronous) {
    if (toImport.isEmpty()) {
      return;
    }
    if (!project.isInitialized()) {
      myFuture = AppExecutorUtil.getAppScheduledExecutorService()
              .schedule(new ImportModulesTask(project, toImport, synchronous), PROJECT_INITIALISATION_DELAY_MS, TimeUnit.MILLISECONDS);
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
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

  @RequiredUIAccess
  private void createModules(@Nonnull final Collection<DataNode<ModuleData>> toCreate, @Nonnull final Project project) {
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
      private void importModule(@Nonnull ModuleManager moduleManager, @Nonnull DataNode<ModuleData> module) {
        ModuleData moduleData = module.getData();
        final Module created = moduleManager.newModule(moduleData.getExternalName(), moduleData.getModuleDirPath());

        // Ensure that the dependencies are clear (used to be not clear when manually removing the module and importing it via gradle)
        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(created);
        final ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();

        setModuleOptions(created, moduleRootModel, module);

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

  @Nonnull
  @RequiredUIAccess
  private Collection<DataNode<ModuleData>> filterExistingModules(@Nonnull Collection<DataNode<ModuleData>> modules, @Nonnull Project project) {
    Collection<DataNode<ModuleData>> result = ContainerUtilRt.newArrayList();
    for (DataNode<ModuleData> node : modules) {
      ModuleData moduleData = node.getData();
      Module module = ProjectStructureHelper.findIdeModule(moduleData, project);
      if (module == null) {
        result.add(node);
      }
      else {
        setModuleOptions(module, null, node);
      }
    }
    return result;
  }

  private static void syncPaths(@Nonnull Module module, @Nonnull ModuleData data) {
    ModuleCompilerPathsManager compilerPathsManager = ModuleCompilerPathsManager.getInstance(module);
    compilerPathsManager.setInheritedCompilerOutput(data.isInheritProjectCompileOutputPath());
    if (!data.isInheritProjectCompileOutputPath()) {
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
  public void removeData(@Nonnull final Collection<? extends Module> modules, @Nonnull final Project project, boolean synchronous) {
    if (modules.isEmpty()) {
      return;
    }
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(project) {
      @RequiredUIAccess
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

  @RequiredUIAccess
  public static void unlinkModuleFromExternalSystem(@Nonnull Module module) {
    ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();

    ExternalSystemMutableModuleExtension<?> extension = modifiableModel.getExtension(ExternalSystemMutableModuleExtension.class);
    if (extension != null) {
      extension.setEnabled(false);
      extension.removeAllOptions();
    }

    WriteAction.run(modifiableModel::commit);
  }

  private class ImportModulesTask implements Runnable {

    private final Project myProject;
    private final Collection<DataNode<ModuleData>> myModules;
    private final boolean mySynchronous;

    ImportModulesTask(@Nonnull Project project, @Nonnull Collection<DataNode<ModuleData>> modules, boolean synchronous) {
      myProject = project;
      myModules = modules;
      mySynchronous = synchronous;
    }

    @Override
    public void run() {
      myFuture.cancel(false);
      if (!myProject.isInitialized()) {
        myFuture = AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(new ImportModulesTask(myProject, myModules, mySynchronous), PROJECT_INITIALISATION_DELAY_MS, TimeUnit.MILLISECONDS);
        return;
      }

      importData(myModules, myProject, mySynchronous);
    }
  }

  @RequiredUIAccess
  private static void setModuleOptions(@Nonnull final Module module,
                                       @javax.annotation.Nullable final ModifiableRootModel originalModel,
                                       @Nonnull final DataNode<ModuleData> moduleDataNode) {

    ModuleData moduleData = moduleDataNode.getData();
    module.putUserData(MODULE_DATA_KEY, moduleData);

    final ProjectData projectData = moduleDataNode.getData(ProjectKeys.PROJECT);
    if (projectData == null) {
      throw new IllegalArgumentException("projectData is null");
    }

    ModifiableRootModel otherModel = originalModel == null ? ModuleRootManager.getInstance(module).getModifiableModel() : originalModel;

    // remove prev. extension
    ExternalSystemMutableModuleExtension<?> oldExtension = otherModel.getExtension(ExternalSystemMutableModuleExtension.class);
    if (oldExtension != null) {
      oldExtension.setEnabled(false);
      oldExtension.removeAllOptions();
    }

    ExternalSystemMutableModuleExtension<?> newExtension = otherModel.getExtensionWithoutCheck(projectData.getOwner().getId());
    if (newExtension == null) {
      LOG.error("ModuleExtension is not registered for externalSystem: " + projectData.getOwner().getId());
      return;
    }

    newExtension.setEnabled(true);
    newExtension.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, moduleData.getOwner().toString());
    newExtension.setOption(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY, moduleData.getLinkedExternalProjectPath());
    newExtension.setOption(ExternalSystemConstants.LINKED_PROJECT_ID_KEY, moduleData.getId());
    if (moduleData.getGroup() != null) {
      newExtension.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_MODULE_GROUP_KEY, moduleData.getGroup());
    }
    if (moduleData.getVersion() != null) {
      newExtension.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_MODULE_VERSION_KEY, moduleData.getVersion());
    }

    newExtension.setOption(ExternalSystemConstants.ROOT_PROJECT_PATH_KEY, projectData.getLinkedExternalProjectPath());

    if (originalModel == null) {
      WriteAction.run(otherModel::commit);
    }
  }
}
