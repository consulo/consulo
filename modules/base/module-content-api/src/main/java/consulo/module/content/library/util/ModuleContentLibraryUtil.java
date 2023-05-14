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
package consulo.module.content.library.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 13-Feb-22
 */
public class ModuleContentLibraryUtil {
  @RequiredReadAction
  public static VirtualFile[] getLibraryRoots(final Project project) {
    return getLibraryRoots(project, true, true);
  }

  @RequiredReadAction
  public static VirtualFile[] getLibraryRoots(final Project project, final boolean includeSourceFiles, final boolean includeJdk) {
    return getLibraryRoots(ModuleManager.getInstance(project).getModules(), includeSourceFiles, includeJdk);
  }

  public static VirtualFile[] getLibraryRoots(final Module[] modules, final boolean includeSourceFiles, final boolean includeSdk) {
    Set<VirtualFile> roots = new HashSet<VirtualFile>();
    for (Module module : modules) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
      for (OrderEntry entry : orderEntries) {
        if (entry instanceof LibraryOrderEntry) {
          final Library library = ((LibraryOrderEntry)entry).getLibrary();
          if (library != null) {
            VirtualFile[] files = includeSourceFiles ? library.getFiles(SourcesOrderRootType.getInstance()) : null;
            if (files == null || files.length == 0) {
              files = library.getFiles(BinariesOrderRootType.getInstance());
            }
            ContainerUtil.addAll(roots, files);
          }
        }
        else if (includeSdk && entry instanceof ModuleExtensionWithSdkOrderEntry) {
          VirtualFile[] files = includeSourceFiles ? entry.getFiles(SourcesOrderRootType.getInstance()) : null;
          if (files == null || files.length == 0) {
            files = entry.getFiles(BinariesOrderRootType.getInstance());
          }
          ContainerUtil.addAll(roots, files);
        }
      }
    }
    return VirtualFileUtil.toVirtualFileArray(roots);
  }

  @Nullable
  public static OrderEntry findLibraryEntry(VirtualFile file, final Project project) {
    List<OrderEntry> entries = ProjectRootManager.getInstance(project).getFileIndex().getOrderEntriesForFile(file);
    for (OrderEntry entry : entries) {
      if (entry instanceof LibraryOrderEntry || entry instanceof ModuleExtensionWithSdkOrderEntry) {
        return entry;
      }
    }
    return null;
  }
}
