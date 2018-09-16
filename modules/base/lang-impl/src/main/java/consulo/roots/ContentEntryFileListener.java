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
package consulo.roots;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.containers.SmartHashSet;
import consulo.annotations.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 06.04.2015
 */
@Singleton
public class ContentEntryFileListener implements Disposable {
  public static class Listener implements VirtualFileListener {
    private final Project myProject;

    public Listener(Project project) {
      myProject = project;
    }

    @Override
    @RequiredReadAction
    public void beforeFileDeletion(@Nonnull VirtualFileEvent event) {
      VirtualFile fileToDelete = event.getFile();

      Module moduleForFile = ModuleUtilCore.findModuleForFile(fileToDelete, myProject);

      // if module have dir url - dont need check
      if (moduleForFile == null || moduleForFile.getModuleDirUrl() != null) {
        return;
      }

      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);

      Set<String> containsLayers = new SmartHashSet<>();
      for (Map.Entry<String, ModuleRootLayer> entry : moduleRootManager.getLayers().entrySet()) {
        String key = entry.getKey();
        ModuleRootLayer value = entry.getValue();

        ContentEntry toRemove = null;
        for (ContentEntry contentEntry : value.getContentEntries()) {
          VirtualFile contentEntryFile = contentEntry.getFile();
          if (fileToDelete.equals(contentEntryFile)) {
            toRemove = contentEntry;
            break;
          }
        }

        if (toRemove != null) {
          containsLayers.add(key);
        }
      }

      if(!containsLayers.isEmpty()) {
        ModifiableRootModel modifiableModel = moduleRootManager.getModifiableModel();

        for (Map.Entry<String, ModuleRootLayer> entry : modifiableModel.getLayers().entrySet()) {
          if(containsLayers.contains(entry.getKey())) {
            ModifiableModuleRootLayer value = (ModifiableModuleRootLayer)entry.getValue();

            for (ContentEntry contentEntry : value.getContentEntries()) {
              if(fileToDelete.equals(contentEntry.getFile()))  {
                value.removeContentEntry(contentEntry);
              }
            }
          }
        }

        SwingUtilities.invokeLater(() -> WriteAction.run(modifiableModel::commit));
      }
    }
  }

  @Inject
  public ContentEntryFileListener(Application application, ProjectManager projectManager) {
    application.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void before(@Nonnull List<? extends VFileEvent> events) {
        for (Project project : projectManager.getOpenProjects()) {
          new BulkVirtualFileListenerAdapter(new Listener(project), LocalFileSystem.getInstance()).before(events);
        }
      }
    });
  }

  @Override
  public void dispose() {
  }
}
