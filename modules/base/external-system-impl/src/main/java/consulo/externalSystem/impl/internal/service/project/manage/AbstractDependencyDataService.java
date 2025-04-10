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

import consulo.externalSystem.model.project.AbstractDependencyData;
import consulo.externalSystem.service.project.manage.ProjectDataService;
import consulo.externalSystem.util.DisposeAwareProjectChange;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.externalSystem.util.Order;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.ExportableOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Denis Zhdanov
 * @since 4/14/13 11:21 PM
 */
@Order(ExternalSystemConstants.BUILTIN_SERVICE_ORDER)
public abstract class AbstractDependencyDataService<E extends AbstractDependencyData<?>, I extends ExportableOrderEntry> implements ProjectDataService<E, I> {

  public void setScope(@Nonnull final DependencyScope scope, @Nonnull final ExportableOrderEntry dependency, boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(dependency.getOwnerModule()) {
      @RequiredUIAccess
      @Override
      public void execute() {
        doForDependency(dependency, entry -> entry.setScope(scope));
      }
    });
  }

  public void setExported(final boolean exported, @Nonnull final ExportableOrderEntry dependency, boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(dependency.getOwnerModule()) {
      @RequiredUIAccess
      @Override
      public void execute() {
        doForDependency(dependency, entry -> entry.setExported(exported));
      }
    });
  }

  private static void doForDependency(@Nonnull ExportableOrderEntry entry, @Nonnull Consumer<ExportableOrderEntry> consumer) {
    // We need to get an up-to-date modifiable model to work with.
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(entry.getOwnerModule());
    final ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();
    try {
      // The thing is that intellij created order entry objects every time new modifiable model is created,
      // that's why we can't use target dependency object as is but need to get a reference to the current
      // entry object from the model instead.
      for (OrderEntry e : moduleRootModel.getOrderEntries()) {
        if (e instanceof ExportableOrderEntry exportableOrderEntry && e.getPresentableName().equals(entry.getPresentableName())) {
          consumer.accept(exportableOrderEntry);
          break;
        }
      }
    }
    finally {
      moduleRootModel.commit();
    }
  }

  @Override
  public void removeData(@Nonnull Collection<? extends I> toRemove, @Nonnull Project project, boolean synchronous) {
    if (toRemove.isEmpty()) {
      return;
    }

    Map<Module, Collection<ExportableOrderEntry>> byModule = groupByModule(toRemove);
    for (Map.Entry<Module, Collection<ExportableOrderEntry>> entry : byModule.entrySet()) {
      removeData(entry.getValue(), entry.getKey(), synchronous);
    }
  }

  @Nonnull
  private static Map<Module, Collection<ExportableOrderEntry>> groupByModule(@Nonnull Collection<? extends ExportableOrderEntry> data) {
    Map<Module, Collection<ExportableOrderEntry>> result = new HashMap<>();
    for (ExportableOrderEntry entry : data) {
      Collection<ExportableOrderEntry> entries = result.get(entry.getOwnerModule());
      if (entries == null) {
        result.put(entry.getOwnerModule(), entries = new ArrayList<>());
      }
      entries.add(entry);
    }
    return result;
  }

  public void removeData(@Nonnull Collection<? extends ExportableOrderEntry> toRemove, @Nonnull final Module module, boolean synchronous) {
    if (toRemove.isEmpty()) {
      return;
    }
    for (final ExportableOrderEntry dependency : toRemove) {
      ExternalSystemApiUtil.executeProjectChangeAction(synchronous, new DisposeAwareProjectChange(dependency.getOwnerModule()) {
        @RequiredUIAccess
        @Override
        public void execute() {
          ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
          final ModifiableRootModel moduleRootModel = moduleRootManager.getModifiableModel();
          try {
            // The thing is that intellij created order entry objects every time new modifiable model is created,
            // that's why we can't use target dependency object as is but need to get a reference to the current
            // entry object from the model instead.
            for (OrderEntry entry : moduleRootModel.getOrderEntries()) {
              if (entry instanceof ExportableOrderEntry orderEntry) {
                if (orderEntry.getPresentableName().equals(dependency.getPresentableName())
                  && orderEntry.getScope().equals(dependency.getScope())) {
                  moduleRootModel.removeOrderEntry(entry);
                  break;
                }
              }
              else if (entry.getPresentableName().equals(dependency.getPresentableName())) {
                moduleRootModel.removeOrderEntry(entry);
                break;
              }
            }
          }
          finally {
            moduleRootModel.commit();
          }
        }
      });
    }
  }
}
