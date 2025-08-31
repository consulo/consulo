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

package consulo.language.internal;

import consulo.application.util.function.CommonProcessors;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.internal.ModuleRootsProcessor;
import consulo.module.content.layer.orderEntry.*;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author max
 */
public class LibraryRuntimeClasspathScope extends GlobalSearchScope {
  private final ProjectFileIndex myIndex;
  private final LinkedHashSet<VirtualFile> myEntries = new LinkedHashSet<>();

  private int myCachedHashCode = 0;

  public LibraryRuntimeClasspathScope(Project project, List<Module> modules) {
    super(project);
    myIndex = ProjectRootManager.getInstance(project).getFileIndex();
    Set<Sdk> processedSdk = new HashSet<>();
    Set<Library> processedLibraries = new HashSet<>();
    Set<Module> processedModules = new HashSet<>();
    Predicate<OrderEntry> condition = orderEntry -> {
      if (orderEntry instanceof ModuleOrderEntry moduleOrderEntry) {
        Module module = moduleOrderEntry.getModule();
        return module != null && !processedModules.contains(module);
      }
      return true;
    };
    for (Module module : modules) {
      buildEntries(module, processedModules, processedLibraries, processedSdk, condition);
    }
  }

  public LibraryRuntimeClasspathScope(Project project, LibraryOrderEntry entry) {
    super(project);
    myIndex = ProjectRootManager.getInstance(project).getFileIndex();
    Collections.addAll(myEntries, entry.getFiles(BinariesOrderRootType.getInstance()));
  }

  @Override
  public int hashCode() {
    if (myCachedHashCode == 0) {
      myCachedHashCode = myEntries.hashCode();
    }

    return myCachedHashCode;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (object == null || object.getClass() != LibraryRuntimeClasspathScope.class) return false;

    LibraryRuntimeClasspathScope that = (LibraryRuntimeClasspathScope)object;
    return that.myEntries.equals(myEntries);
  }

  private void buildEntries(@Nonnull Module module,
                            @Nonnull final Set<Module> processedModules,
                            @Nonnull final Set<Library> processedLibraries,
                            @Nonnull final Set<Sdk> processedSdk,
                            Predicate<OrderEntry> condition) {
    if (!processedModules.add(module)) return;

    ModuleRootManager.getInstance(module).orderEntries().recursively().satisfying(condition).process(new RootPolicy<>() {
      @Override
      public LinkedHashSet<VirtualFile> visitLibraryOrderEntry(LibraryOrderEntry libraryOrderEntry, LinkedHashSet<VirtualFile> value) {
        Library library = libraryOrderEntry.getLibrary();
        if (library != null && processedLibraries.add(library)) {
          ContainerUtil.addAll(value, libraryOrderEntry.getFiles(BinariesOrderRootType.getInstance()));
        }
        return value;
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleSourceOrderEntry(ModuleSourceOrderEntry moduleSourceOrderEntry,
                                                                    LinkedHashSet<VirtualFile> value) {
        processedModules.add(moduleSourceOrderEntry.getOwnerModule());
        collectScopeFiles(moduleSourceOrderEntry.getOwnerModule(), value);
        return value;
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, LinkedHashSet<VirtualFile> value) {
        Module depModule = moduleOrderEntry.getModule();
        if (depModule != null) {
          collectScopeFiles(depModule, value);
        }
        return value;
      }

      private void collectScopeFiles(@Nonnull Module module, Set<VirtualFile> set) {
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

        ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(moduleRootManager);
        if (rootsProcessor != null) {
          rootsProcessor.processFiles(moduleRootManager, LanguageContentFolderScopes.productionAndTest(), new CommonProcessors.CollectProcessor<>(set));
        }
        else {
          Collections.addAll(set, moduleRootManager.getContentFolderFiles(LanguageContentFolderScopes.productionAndTest()));
        }
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleExtensionSdkOrderEntry(ModuleExtensionWithSdkOrderEntry sdkOrderEntry,
                                                                          LinkedHashSet<VirtualFile> value) {
        Sdk sdk = sdkOrderEntry.getSdk();
        if (sdk != null && processedSdk.add(sdk)) {
          ContainerUtil.addAll(value, sdkOrderEntry.getFiles(BinariesOrderRootType.getInstance()));
        }
        return value;
      }

      @Override
      public LinkedHashSet<VirtualFile> visitOrderEntry(OrderEntry orderEntry, LinkedHashSet<VirtualFile> value) {
        if (orderEntry instanceof OrderEntryWithTracking) {
          ContainerUtil.addAll(value, orderEntry.getFiles(BinariesOrderRootType.getInstance()));
        }
        return value;
      }
    }, myEntries);
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return myEntries.contains(getFileRoot(file));
  }

  @Nullable
  private VirtualFile getFileRoot(VirtualFile file) {
    if (myIndex.isLibraryClassFile(file)) {
      return myIndex.getClassRootForFile(file);
    }
    if (myIndex.isInContent(file)) {
      return myIndex.getSourceRootForFile(file);
    }
    if (myIndex.isInLibraryClasses(file)) {
      return myIndex.getClassRootForFile(file);
    }
    return null;
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    VirtualFile r1 = getFileRoot(file1);
    VirtualFile r2 = getFileRoot(file2);
    for (VirtualFile root : myEntries) {
      if (Comparing.equal(r1, root)) return 1;
      if (Comparing.equal(r2, root)) return -1;
    }
    return 0;
  }

  @TestOnly
  public List<VirtualFile> getRoots() {
    return new ArrayList<>(myEntries);
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return false;
  }

  @Override
  public boolean isSearchInLibraries() {
    return true;
  }
}
