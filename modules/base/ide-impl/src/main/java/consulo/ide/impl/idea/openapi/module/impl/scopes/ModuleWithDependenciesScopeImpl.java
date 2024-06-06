/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.module.impl.scopes;

import consulo.content.OrderRootType;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.psi.PsiBundle;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.OrderEnumerator;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleSourceOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.scope.ModuleWithDependenciesScope;
import consulo.module.impl.internal.layer.ModuleRootsProcessor;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class ModuleWithDependenciesScopeImpl extends GlobalSearchScope implements ModuleWithDependenciesScope {
  public static final int COMPILE = 0x01;
  public static final int LIBRARIES = 0x02;
  public static final int MODULES = 0x04;
  public static final int TESTS = 0x08;
  public static final int RUNTIME = 0x10;
  public static final int CONTENT = 0x20;

  @MagicConstant(flags = {COMPILE, LIBRARIES, MODULES, TESTS, RUNTIME, CONTENT})
  public @interface ScopeConstant {}

  private final Module myModule;
  @ScopeConstant
  private final int myOptions;

  private final ProjectFileIndex myProjectFileIndex;

  private final Set<Module> myModules;
  private final ObjectIntMap<VirtualFile> myRoots = ObjectMaps.newObjectIntHashMap();

  private ModuleRootsProcessor myRootsProcessor;
  private ModuleRootManager myModuleRootManager;

  public ModuleWithDependenciesScopeImpl(@Nonnull Module module, @ScopeConstant int options) {
    super(module.getProject());
    myModule = module;
    myOptions = options;

    myProjectFileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();

    myModuleRootManager = ModuleRootManager.getInstance(module);
    myRootsProcessor = ModuleRootsProcessor.findRootsProcessor(myModuleRootManager);
    OrderEnumerator en = myModuleRootManager.orderEntries();
    en.recursively();

    if (hasOption(COMPILE)) {
      en.exportedOnly().compileOnly();
    }
    if (hasOption(RUNTIME)) {
      en.runtimeOnly();
    }
    if (!hasOption(LIBRARIES)) en.withoutLibraries().withoutSdk();
    if (!hasOption(MODULES)) en.withoutDepModules();
    if (!hasOption(TESTS)) en.productionOnly();

    final LinkedHashSet<Module> modules = ContainerUtil.newLinkedHashSet();

    en.forEach(each -> {
      if (each instanceof ModuleOrderEntry) {
        ContainerUtil.addIfNotNull(modules, ((ModuleOrderEntry)each).getModule());
      }
      else if (each instanceof ModuleSourceOrderEntry) {
        ContainerUtil.addIfNotNull(modules, each.getOwnerModule());
      }
      return true;
    });

    myModules = new HashSet<>(modules);

    final LinkedHashSet<VirtualFile> roots = ContainerUtil.newLinkedHashSet();

    if (hasOption(CONTENT)) {
      for (Module m : modules) {
        for (ContentEntry entry : ModuleRootManager.getInstance(m).getContentEntries()) {
          ContainerUtil.addIfNotNull(entry.getFile(), roots);
        }
      }
    }
    else {
      Collections.addAll(roots, en.roots(new NotNullFunction<>() {
        @Nonnull
        @Override
        public OrderRootType apply(OrderEntry entry) {
          if (entry instanceof ModuleOrderEntry || entry instanceof ModuleSourceOrderEntry) {
            return SourcesOrderRootType.getInstance();
          }
          return BinariesOrderRootType.getInstance();
        }
      }).getRoots());
    }

    int i = 1;
    for (VirtualFile root : roots) {
      myRoots.putInt(root, i++);
    }
  }

  @Nonnull
  public Module getModule() {
    return myModule;
  }

  private boolean hasOption(@ScopeConstant int option) {
    return (myOptions & option) != 0;
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return hasOption(COMPILE) ? PsiBundle.message("search.scope.module", myModule.getName())
                              : PsiBundle.message("search.scope.module.runtime", myModule.getName());
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return myModules.contains(aModule);
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule, boolean testSources) {
    return isSearchInModuleContent(aModule) && (hasOption(TESTS) || !testSources);
  }

  @Override
  public boolean isSearchInLibraries() {
    return hasOption(LIBRARIES);
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    if (hasOption(CONTENT)) {
      return myRoots.containsKey(myProjectFileIndex.getContentRootForFile(file));
    }
    if(myProjectFileIndex.isInContent(file)) {
      if(myRootsProcessor != null) {
        if(myRootsProcessor.containsFile(myRoots, file)) {
          return true;
        }
      }
      if(myRoots.containsKey(myProjectFileIndex.getSourceRootForFile(file))) {
        return true;
      }
    }
    return myRoots.containsKey(myProjectFileIndex.getClassRootForFile(file));
  }

  @Override
  public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
    VirtualFile r1 = getFileRoot(file1);
    VirtualFile r2 = getFileRoot(file2);
    if (Comparing.equal(r1, r2)) return 0;

    if (r1 == null) return -1;
    if (r2 == null) return 1;

    int i1 = myRoots.getInt(r1);
    int i2 = myRoots.getInt(r2);
    if (i1 == 0 && i2 == 0) return 0;
    if (i1 > 0 && i2 > 0) return i2 - i1;
    return i1 > 0 ? 1 : -1;
  }

  @Nullable
  private VirtualFile getFileRoot(@Nonnull VirtualFile file) {
    if (myProjectFileIndex.isInContent(file)) {
      return myProjectFileIndex.getSourceRootForFile(file);
    }
    return myProjectFileIndex.getClassRootForFile(file);
  }

  @TestOnly
  public Collection<VirtualFile> getRoots() {
    //noinspection unchecked
    List<VirtualFile> result = new ArrayList<>(myRoots.size());
    myRoots.forEach((virtualFile, value) -> result.add(virtualFile));
    Collections.sort(result, (o1, o2) -> myRoots.getInt(o1) - myRoots.getInt(o2));
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModuleWithDependenciesScopeImpl that = (ModuleWithDependenciesScopeImpl)o;
    return myOptions == that.myOptions && myModule.equals(that.myModule);
  }

  @Override
  public int hashCode() {
    return 31 * myModule.hashCode() + myOptions;
  }

  @Override
  public String toString() {
    return "Module with dependencies:" + myModule.getName() +
           " compile:" + hasOption(COMPILE) +
           " include libraries:" + hasOption(LIBRARIES) +
           " include other modules:" + hasOption(MODULES) +
           " include tests:" + hasOption(TESTS);
  }
}
