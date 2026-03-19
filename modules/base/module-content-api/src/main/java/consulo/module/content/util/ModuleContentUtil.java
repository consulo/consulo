/*
 * Copyright 2013-2022 consulo.io
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
package consulo.module.content.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.bundle.Sdk;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public class ModuleContentUtil {
  public static @Nullable <E extends ModuleExtension<E>> E getExtension(Project project, VirtualFile virtualFile, Class<E> extensionClass) {
    Module moduleForFile = findModuleForFile(virtualFile, project);
    if (moduleForFile == null) {
      return null;
    }
    return moduleForFile.getExtension(extensionClass);
  }

  public static @Nullable Sdk getSdk(Module module, Class<? extends ModuleExtensionWithSdk> extensionClass) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

    ModuleExtensionWithSdk<?> extension = moduleRootManager.getExtension(extensionClass);
    if (extension == null) {
      return null;
    }
    else {
      return extension.getSdk();
    }
  }

  @RequiredReadAction
  
  public static List<ContentFolder> getContentFolders(Project project) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    List<ContentFolder> contentFolders = new ArrayList<>();
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      moduleRootManager.iterateContentEntries(contentEntry -> {
        Collections.addAll(contentFolders, contentEntry.getFolders(ContentFolderTypeProvider.allExceptExcluded()));
        return false;
      });
    }
    return contentFolders;
  }

  public static @Nullable Module findModuleForFile(VirtualFile file, Project project) {
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return fileIndex.getModuleForFile(file);
  }

  /**
   * collect transitive module dependants
   *
   * @param module to find dependencies on
   * @param result resulted set
   */
  @RequiredReadAction
  public static void collectModulesDependsOn(Module module, Set<Module> result) {
    if (result.contains(module)) return;
    result.add(module);
    ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
    List<Module> dependentModules = moduleManager.getModuleDependentModules(module);
    for (Module dependentModule : dependentModules) {
      OrderEntry[] orderEntries = ModuleRootManager.getInstance(dependentModule).getOrderEntries();
      for (OrderEntry o : orderEntries) {
        if (o instanceof ModuleOrderEntry) {
          ModuleOrderEntry orderEntry = (ModuleOrderEntry)o;
          if (orderEntry.getModule() == module) {
            if (orderEntry.isExported()) {
              collectModulesDependsOn(dependentModule, result);
            }
            else {
              result.add(dependentModule);
            }
            break;
          }
        }
      }
    }
  }
}
