/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.libraries;

import consulo.content.base.BinariesOrderRootType;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.library.util.ModuleContentLibraryUtil;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author cdr
 */
public class LibraryUtil {
  private LibraryUtil() {
  }

  public static boolean isClassAvailableInLibrary(Library library, String fqn) {
    return isClassAvailableInLibrary(library.getFiles(BinariesOrderRootType.getInstance()), fqn);
  }

  public static boolean isClassAvailableInLibrary(VirtualFile[] files, String fqn) {
    return isClassAvailableInLibrary(Arrays.asList(files), fqn);
  }

  public static boolean isClassAvailableInLibrary(List<VirtualFile> files, String fqn) {
    for (VirtualFile file : files) {
      if (findInFile(file, new StringTokenizer(fqn, "."))) return true;
    }
    return false;
  }

  @Nullable
  public static Library findLibraryByClass(String fqn, @Nullable Project project) {
    if (project != null) {
      LibraryTable projectTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
      Library library = findInTable(projectTable, fqn);
      if (library != null) {
        return library;
      }
    }
    return null;
  }


  private static boolean findInFile(VirtualFile file, StringTokenizer tokenizer) {
    if (!tokenizer.hasMoreTokens()) return true;
    StringBuilder name = new StringBuilder(tokenizer.nextToken());
    if (!tokenizer.hasMoreTokens()) {
      name.append(".class");
    }
    VirtualFile child = file.findChild(name.toString());
    return child != null && findInFile(child, tokenizer);
  }

  @Nullable
  private static Library findInTable(LibraryTable table, String fqn) {
    for (Library library : table.getLibraries()) {
      if (isClassAvailableInLibrary(library, fqn)) {
        return library;
      }
    }
    return null;
  }

  public static Library createLibrary(LibraryTable libraryTable, @NonNls String baseName) {
    String name = baseName;
    int count = 2;
    while (libraryTable.getLibraryByName(name) != null) {
      name = baseName + " (" + count++ + ")";
    }
    return libraryTable.createLibrary(name);
  }

  public static VirtualFile[] getLibraryRoots(Project project) {
    return getLibraryRoots(project, true, true);
  }

  public static VirtualFile[] getLibraryRoots(Project project, boolean includeSourceFiles, boolean includeJdk) {
    return getLibraryRoots(ModuleManager.getInstance(project).getModules(), includeSourceFiles, includeJdk);
  }

  public static VirtualFile[] getLibraryRoots(Module[] modules, boolean includeSourceFiles, boolean includeSdk) {
    return ModuleContentLibraryUtil.getLibraryRoots(modules, includeSourceFiles, includeSdk);
  }

  @Nullable
  public static Library findLibrary(@Nonnull Module module, @Nonnull String name) {
    Ref<Library> result = Ref.create(null);
    OrderEnumerator.orderEntries(module).forEachLibrary(library -> {
      if (name.equals(library.getName())) {
        result.set(library);
        return false;
      }
      return true;
    });
    return result.get();
  }

  @Nullable
  public static OrderEntry findLibraryEntry(VirtualFile file, Project project) {
    return ModuleContentLibraryUtil.findLibraryEntry(file, project);
  }

  @Nonnull
  public static String getPresentableName(@Nonnull Library library) {
    String name = library.getName();
    if (name != null) {
      return name;
    }
    if (library instanceof LibraryEx && ((LibraryEx)library).isDisposed()) {
      return "Disposed Library";
    }
    String[] urls = library.getUrls(BinariesOrderRootType.getInstance());
    if (urls.length > 0) {
      return PathUtil.getFileName(VfsUtilCore.urlToPath(urls[0]));
    }
    return "Empty Library";
  }
}
