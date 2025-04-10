/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.project.manage;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalSystem.impl.internal.service.project.ProjectStructureHelper;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.Key;
import consulo.externalSystem.model.ProjectKeys;
import consulo.externalSystem.model.project.ModuleData;
import consulo.externalSystem.model.project.ModuleDependencyData;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.*;

import static consulo.externalSystem.model.ProjectKeys.MODULE;

/**
 * @author Denis Zhdanov
 * @since 4/15/13 8:37 AM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
@ExtensionImpl
public class ModuleDependencyDataService extends AbstractDependencyDataService<ModuleDependencyData, ModuleOrderEntry> {

  private static final Logger LOG = Logger.getInstance(ModuleDependencyDataService.class);

  @Nonnull
  @Override
  public Key<ModuleDependencyData> getTargetDataKey() {
    return ProjectKeys.MODULE_DEPENDENCY;
  }

  @Override
  public void importData(@Nonnull Collection<DataNode<ModuleDependencyData>> toImport, @Nonnull Project project, boolean synchronous) {
    Map<DataNode<ModuleData>, List<DataNode<ModuleDependencyData>>> byModule = ExternalSystemApiUtil.groupBy(toImport, MODULE);
    for (Map.Entry<DataNode<ModuleData>, List<DataNode<ModuleDependencyData>>> entry : byModule.entrySet()) {
      Module ideModule = ProjectStructureHelper.findIdeModule(entry.getKey().getData(), project);
      if (ideModule == null) {
        ModuleDataService.getInstance().importData(Collections.singleton(entry.getKey()), project, true);
        ideModule = ProjectStructureHelper.findIdeModule(entry.getKey().getData(), project);
      }
      if (ideModule == null) {
        LOG.warn(String.format(
          "Can't import module dependencies %s. Reason: target module (%s) is not found at the ide and can't be imported",
          entry.getValue(), entry.getKey()
        ));
        continue;
      }
      importData(entry.getValue(), ideModule, synchronous);
    }
  }

  public void importData(@Nonnull final Collection<DataNode<ModuleDependencyData>> toImport,
                         @Nonnull final Module module,
                         final boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(module) {
      @RequiredUIAccess
      @Override
      public void execute() {
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        Map<Pair<String /* dependency module internal name */, /* dependency module scope */DependencyScope>, ModuleOrderEntry> toRemove =
          new HashMap<>();
        for (OrderEntry entry : moduleRootManager.getOrderEntries()) {
          if (entry instanceof ModuleOrderEntry e) {
            toRemove.put(Pair.create(e.getModuleName(), e.getScope()), e);
          }
        }

        final ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();
        try {
          for (DataNode<ModuleDependencyData> dependencyNode : toImport) {
            final ModuleDependencyData dependencyData = dependencyNode.getData();
            toRemove.remove(Pair.create(dependencyData.getInternalName(), dependencyData.getScope()));
            final String moduleName = dependencyData.getInternalName();
            Module ideDependencyModule = ProjectStructureHelper.findIdeModule(moduleName, module.getProject());
            if (ideDependencyModule == null) {
              DataNode<ProjectData> projectNode = dependencyNode.getDataNode(ProjectKeys.PROJECT);
              if (projectNode != null) {
                DataNode<ModuleData> n
                  = ExternalSystemApiUtil.find(projectNode, MODULE, node -> node.getData().equals(dependencyData.getTarget()));
                if (n != null) {
                  ModuleDataService.getInstance().importData(Collections.singleton(n), module.getProject(), true);
                  ideDependencyModule = ProjectStructureHelper.findIdeModule(moduleName, module.getProject());
                }
              }
            }

            if (ideDependencyModule == null) {
              assert false;
              return;
            }
            else if (ideDependencyModule.equals(module)) {
              // Gradle api returns recursive module dependencies (a module depends on itself) for 'gradle' project.
              continue;
            }

            ModuleOrderEntry orderEntry = ProjectStructureHelper.findIdeModuleDependency(dependencyData, moduleRootModel);
            if (orderEntry == null) {
              orderEntry = moduleRootModel.addModuleOrderEntry(ideDependencyModule);
            }
            orderEntry.setScope(dependencyData.getScope());
            orderEntry.setExported(dependencyData.isExported());
          }
        }
        finally {
          moduleRootModel.commit();
        }

        if (!toRemove.isEmpty()) {
          removeData(toRemove.values(), module, synchronous);
        }
      }
    });
  }
}
