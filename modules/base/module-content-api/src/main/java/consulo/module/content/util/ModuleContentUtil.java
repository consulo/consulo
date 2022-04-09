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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public class ModuleContentUtil {
  @Nullable
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull Module module, @Nonnull Class<E> extensionClass) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    return moduleRootManager.getExtension(extensionClass);
  }

  @Nullable
  public static ModuleExtension<?> getExtension(@Nonnull Module module, @Nonnull String key) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    return moduleRootManager.getExtension(key);
  }

  @Nullable
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull Project project, @Nonnull VirtualFile virtualFile, @Nonnull Class<E> extensionClass) {
    Module moduleForFile = findModuleForFile(virtualFile, project);
    if (moduleForFile == null) {
      return null;
    }
    return getExtension(moduleForFile, extensionClass);
  }

  @Nullable
  public static Sdk getSdk(@Nonnull Module module, @Nonnull Class<? extends ModuleExtensionWithSdk> extensionClass) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

    final ModuleExtensionWithSdk<?> extension = moduleRootManager.getExtension(extensionClass);
    if (extension == null) {
      return null;
    }
    else {
      return extension.getSdk();
    }
  }

  @RequiredReadAction
  @Nonnull
  public static List<ContentFolder> getContentFolders(@Nonnull Project project) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    final List<ContentFolder> contentFolders = new ArrayList<>();
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      moduleRootManager.iterateContentEntries(contentEntry -> {
        Collections.addAll(contentFolders, contentEntry.getFolders(ContentFolderTypeProvider.allExceptExcluded()));
        return false;
      });
    }
    return contentFolders;
  }

  @Nullable
  public static Module findModuleForFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return fileIndex.getModuleForFile(file);
  }

  /**
   * collect transitive module dependants
   *
   * @param module to find dependencies on
   * @param result resulted set
   */
  @RequiredReadAction
  public static void collectModulesDependsOn(@Nonnull final Module module, final Set<Module> result) {
    if (result.contains(module)) return;
    result.add(module);
    final ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
    final List<Module> dependentModules = moduleManager.getModuleDependentModules(module);
    for (final Module dependentModule : dependentModules) {
      final OrderEntry[] orderEntries = ModuleRootManager.getInstance(dependentModule).getOrderEntries();
      for (OrderEntry o : orderEntries) {
        if (o instanceof ModuleOrderEntry) {
          final ModuleOrderEntry orderEntry = (ModuleOrderEntry)o;
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
