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

package com.intellij.openapi.module.impl.scopes;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import consulo.roots.ContentFolderScopes;
import consulo.roots.OrderEntryWithTracking;
import consulo.roots.impl.ModuleRootsProcessor;
import consulo.roots.types.BinariesOrderRootType;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author max
 */
public class LibraryRuntimeClasspathScope extends GlobalSearchScope {
  private final ProjectFileIndex myIndex;
  private final LinkedHashSet<VirtualFile> myEntries = new LinkedHashSet<VirtualFile>();

  private int myCachedHashCode = 0;

  public LibraryRuntimeClasspathScope(final Project project, final List<Module> modules) {
    super(project);
    myIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Set<Sdk> processedSdk = new HashSet<Sdk>();
    final Set<Library> processedLibraries = new HashSet<Library>();
    final Set<Module> processedModules = new HashSet<Module>();
    final Condition<OrderEntry> condition = orderEntry -> {
      if (orderEntry instanceof ModuleOrderEntry) {
        final Module module = ((ModuleOrderEntry)orderEntry).getModule();
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

    final LibraryRuntimeClasspathScope that = (LibraryRuntimeClasspathScope)object;
    return that.myEntries.equals(myEntries);
  }

  private void buildEntries(@Nonnull final Module module,
                            @Nonnull final Set<Module> processedModules,
                            @Nonnull final Set<Library> processedLibraries,
                            @Nonnull final Set<Sdk> processedSdk,
                            Condition<OrderEntry> condition) {
    if (!processedModules.add(module)) return;

    ModuleRootManager.getInstance(module).orderEntries().recursively().satisfying(condition).process(new RootPolicy<LinkedHashSet<VirtualFile>>() {
      @Override
      public LinkedHashSet<VirtualFile> visitLibraryOrderEntry(final LibraryOrderEntry libraryOrderEntry, final LinkedHashSet<VirtualFile> value) {
        final Library library = libraryOrderEntry.getLibrary();
        if (library != null && processedLibraries.add(library)) {
          ContainerUtil.addAll(value, libraryOrderEntry.getFiles(BinariesOrderRootType.getInstance()));
        }
        return value;
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleSourceOrderEntry(final ModuleSourceOrderEntry moduleSourceOrderEntry,
                                                                    final LinkedHashSet<VirtualFile> value) {
        processedModules.add(moduleSourceOrderEntry.getOwnerModule());
        collectScopeFiles(moduleSourceOrderEntry.getOwnerModule(), value);
        return value;
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleOrderEntry(ModuleOrderEntry moduleOrderEntry, LinkedHashSet<VirtualFile> value) {
        final Module depModule = moduleOrderEntry.getModule();
        if (depModule != null) {
          collectScopeFiles(depModule, value);
        }
        return value;
      }

      private void collectScopeFiles(@Nonnull final Module module, Set<VirtualFile> set) {
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

        ModuleRootsProcessor rootsProcessor = ModuleRootsProcessor.findRootsProcessor(moduleRootManager);
        if (rootsProcessor != null) {
          rootsProcessor.processFiles(moduleRootManager, ContentFolderScopes.productionAndTest(), new CommonProcessors.CollectProcessor<VirtualFile>(set));
        }
        else {
          Collections.addAll(set, moduleRootManager.getContentFolderFiles(ContentFolderScopes.productionAndTest()));
        }
      }

      @Override
      public LinkedHashSet<VirtualFile> visitModuleExtensionSdkOrderEntry(final ModuleExtensionWithSdkOrderEntry sdkOrderEntry,
                                                                          final LinkedHashSet<VirtualFile> value) {
        final Sdk sdk = sdkOrderEntry.getSdk();
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
    final VirtualFile r1 = getFileRoot(file1);
    final VirtualFile r2 = getFileRoot(file2);
    for (VirtualFile root : myEntries) {
      if (Comparing.equal(r1, root)) return 1;
      if (Comparing.equal(r2, root)) return -1;
    }
    return 0;
  }

  @TestOnly
  public List<VirtualFile> getRoots() {
    return new ArrayList<VirtualFile>(myEntries);
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
