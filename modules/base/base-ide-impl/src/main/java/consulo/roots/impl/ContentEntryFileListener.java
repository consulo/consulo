/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.impl;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.containers.SmartHashSet;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ModuleRootLayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Singleton;
import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06.04.2015
 */
@Singleton
public class ContentEntryFileListener implements AsyncFileListener {
  @Nullable
  @Override
  public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
    Map<VirtualFile, Module> beforeEvents = new HashMap<>();

    for (VFileEvent event : events) {
      if (event instanceof VFileDeleteEvent) {
        VirtualFile fileToDelete = event.getFile();
        Project project = fileToDelete == null ? null : ProjectUtil.guessProjectForFile(fileToDelete);
        if (project == null) {
          continue;
        }

        Module moduleForFile = ModuleUtilCore.findModuleForFile(fileToDelete, project);

        // if module have dir url - dont need check
        if (moduleForFile == null || moduleForFile.getModuleDirUrl() != null) {
          continue;
        }

        beforeEvents.put(fileToDelete, moduleForFile);
      }
    }

    if (beforeEvents.isEmpty()) {
      return null;
    }

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        for (Map.Entry<VirtualFile, Module> fileAndModuleEntry : beforeEvents.entrySet()) {
          VirtualFile file = fileAndModuleEntry.getKey();
          Module module = fileAndModuleEntry.getValue();

          ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

          Set<String> containsLayers = new SmartHashSet<>();
          for (Map.Entry<String, ModuleRootLayer> entry : moduleRootManager.getLayers().entrySet()) {
            String key = entry.getKey();
            ModuleRootLayer value = entry.getValue();

            ContentEntry toRemove = null;
            for (ContentEntry contentEntry : value.getContentEntries()) {
              VirtualFile contentEntryFile = contentEntry.getFile();
              if (file.equals(contentEntryFile)) {
                toRemove = contentEntry;
                break;
              }
            }

            if (toRemove != null) {
              containsLayers.add(key);
            }
          }

          if (!containsLayers.isEmpty()) {
            ModifiableRootModel modifiableModel = ReadAction.compute(moduleRootManager::getModifiableModel);

            for (Map.Entry<String, ModuleRootLayer> entry : modifiableModel.getLayers().entrySet()) {
              if (containsLayers.contains(entry.getKey())) {
                ModifiableModuleRootLayer value = (ModifiableModuleRootLayer)entry.getValue();

                for (ContentEntry contentEntry : value.getContentEntries()) {
                  if (file.equals(contentEntry.getFile())) {
                    value.removeContentEntry(contentEntry);
                  }
                }
              }
            }

            SwingUtilities.invokeLater(() -> WriteAction.run(modifiableModel::commit));
          }
        }
      }
    };
  }
}
