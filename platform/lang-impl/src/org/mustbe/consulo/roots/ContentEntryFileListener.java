/*
 * Copyright 2013-2015 must-be.org
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
package org.mustbe.consulo.roots;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import consulo.lombok.annotations.Logger;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ModuleRootLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

/**
 * @author VISTALL
 * @since 06.04.2015
 */
@Logger
public class ContentEntryFileListener extends AbstractProjectComponent {
  public interface PossibleModuleForFileResolver {
    ExtensionPointName<PossibleModuleForFileResolver> EP_NAME = ExtensionPointName.create("com.intellij.possibleModuleForFileResolver");

    @Nullable
    @RequiredReadAction
    Module resolve(@NotNull Project project, @NotNull VirtualFile virtualFile);
  }

  public class Listener extends VirtualFileAdapter {
    @Override
    @RequiredReadAction
    public void fileCreated(@NotNull VirtualFileEvent event) {
      Module resolvedModule = null;
      for (PossibleModuleForFileResolver possibleModuleForFileResolver : PossibleModuleForFileResolver.EP_NAME.getExtensions()) {
        Module temp = possibleModuleForFileResolver.resolve(myProject, event.getFile());
        if (temp != null) {
          resolvedModule = temp;
          break;
        }
      }

      if (resolvedModule == null) {
        return;
      }

      LOGGER.assertTrue(resolvedModule.getModuleDirUrl() == null, "We cant add file as content entry is module created as dir based");

      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(resolvedModule);
      final ModifiableRootModel modifiableModel = moduleRootManager.getModifiableModel();

      for (ModuleRootLayer moduleRootLayer : modifiableModel.getLayers().values()) {
        ModifiableModuleRootLayer modifiableModuleRootLayer = (ModifiableModuleRootLayer)moduleRootLayer;

        modifiableModuleRootLayer.addContentEntry(event.getFile());
      }
      commitViaDumbService(modifiableModel);
    }

    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
      VirtualFile fileToDelete = event.getFile();

      Module moduleForFile = ModuleUtilCore.findModuleForFile(fileToDelete, myProject);
      // if module have dir url - dont need check
      if (moduleForFile == null || moduleForFile.getModuleDirUrl() != null) {
        return;
      }
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);
      final ModifiableRootModel modifiableModel = moduleRootManager.getModifiableModel();

      boolean processed = false;
      for (ModuleRootLayer moduleRootLayer : modifiableModel.getLayers().values()) {
        ModifiableModuleRootLayer modifiableModuleRootLayer = (ModifiableModuleRootLayer)moduleRootLayer;

        ContentEntry toRemove = null;
        for (ContentEntry contentEntry : modifiableModuleRootLayer.getContentEntries()) {
          VirtualFile contentEntryFile = contentEntry.getFile();
          if (fileToDelete.equals(contentEntryFile)) {
            toRemove = contentEntry;
            break;
          }
        }

        if(toRemove != null) {
          processed = true;
          modifiableModuleRootLayer.removeContentEntry(toRemove);
        }
      }

      if (processed) {
        commitViaDumbService(modifiableModel);
      }
      else {
        modifiableModel.dispose();
      }
    }

    private void commitViaDumbService(final ModifiableRootModel model) {
      DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, new Runnable() {
        @Override
        public void run() {
          //noinspection RequiredXAction
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              model.commit();
            }
          });
        }
      });
    }
  }

  private Listener myListener = new Listener();

  public ContentEntryFileListener(Project project) {
    super(project);
  }

  @Override
  public void initComponent() {
    VirtualFileManager.getInstance().addVirtualFileListener(myListener);
  }

  @Override
  public void disposeComponent() {
    VirtualFileManager.getInstance().removeVirtualFileListener(myListener);
  }
}
