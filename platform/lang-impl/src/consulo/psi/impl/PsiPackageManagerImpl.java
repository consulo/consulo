/*
 * Copyright 2013 must-be.org
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
package consulo.psi.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Query;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.messages.MessageBus;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredReadAction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 8:05/20.05.13
 */
public class PsiPackageManagerImpl extends PsiPackageManager {
  private final Project myProject;
  private final DirectoryIndex myDirectoryIndex;

  @SuppressWarnings("UnusedDeclaration")
  private final LowMemoryWatcher myLowMemoryWatcher = LowMemoryWatcher.register(new Runnable() {
    @Override
    public void run() {
      myPackageCache.clear();
    }
  });

  private Map<Class<? extends ModuleExtension>, ConcurrentMap<String, PsiPackage>> myPackageCache =
          new ConcurrentHashMap<Class<? extends ModuleExtension>, ConcurrentMap<String, PsiPackage>>();

  public PsiPackageManagerImpl(Project project, PsiManager psiManager, DirectoryIndex directoryIndex, MessageBus bus) {
    myProject = project;
    myDirectoryIndex = directoryIndex;

    final PsiModificationTracker modificationTracker = psiManager.getModificationTracker();

    if (bus != null) {
      bus.connect().subscribe(PsiModificationTracker.TOPIC, new PsiModificationTracker.Listener() {
        private long lastTimeSeen = -1L;

        @Override
        public void modificationCountChanged() {
          final long now = modificationTracker.getJavaStructureModificationCount();
          if (lastTimeSeen != now) {
            lastTimeSeen = now;
            myPackageCache.clear();
          }
        }
      });
    }
  }

  @Override
  public void dropCache(@NotNull Class<? extends ModuleExtension> extensionClass) {
    myPackageCache.remove(extensionClass);
  }

  @RequiredReadAction
  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull String qualifiedName, @NotNull Class<? extends ModuleExtension> extensionClass) {
    ConcurrentMap<String, PsiPackage> map = myPackageCache.get(extensionClass);
    if (map != null) {
      final PsiPackage psiPackage = map.get(qualifiedName);
      if (psiPackage != null) {
        return psiPackage;
      }
    }

    final PsiPackage newPackage = createPackage(qualifiedName, extensionClass);
    if (newPackage != null) {
      if (map == null) {
        myPackageCache.put(extensionClass, map = new ConcurrentHashMap<String, PsiPackage>());
      }

      ConcurrencyUtil.cacheOrGet(map, qualifiedName, newPackage);
    }
    return newPackage;
  }

  @Nullable
  private PsiPackage createPackage(@NotNull String qualifiedName, @NotNull Class<? extends ModuleExtension> extensionClass) {
    Query<VirtualFile> dirs = myDirectoryIndex.getDirectoriesByPackageName(qualifiedName, true);
    if (dirs.findFirst() == null) {
      return null;
    }

    for (VirtualFile next : dirs) {
      PsiPackage packageFromProviders = createPackageFromProviders(next, extensionClass, qualifiedName);
      if (packageFromProviders != null) {
        return packageFromProviders;
      }

      PsiPackage packageFromLibrary = createPackageFromLibrary(next, extensionClass, qualifiedName);
      if (packageFromLibrary != null) {
        return packageFromLibrary;
      }
    }
    return null;
  }

  @Nullable
  private PsiPackage createPackageFromProviders(@NotNull VirtualFile virtualFile,
                                                @NotNull Class<? extends ModuleExtension> extensionClass,
                                                @NotNull String qualifiedName) {
    final Module moduleForFile = ModuleUtil.findModuleForFile(virtualFile, myProject);
    if (moduleForFile == null) {
      return null;
    }

    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleForFile);

    final ModuleExtension extension = moduleRootManager.getExtension(extensionClass);
    if (extension == null) {
      return null;
    }

    PsiManager psiManager = PsiManager.getInstance(myProject);
    for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
      if (p.isSupported(extension)) {
        return p.createPackage(psiManager, this, extensionClass, qualifiedName);
      }
    }
    return null;
  }

  private PsiPackage createPackageFromLibrary(@NotNull VirtualFile virtualFile,
                                              @NotNull Class<? extends ModuleExtension> extensionClass,
                                              @NotNull String qualifiedName) {
    ProjectFileIndex fileIndexFacade = ProjectFileIndex.SERVICE.getInstance(myProject);
    PsiManager psiManager = PsiManager.getInstance(myProject);
    if (fileIndexFacade.isInLibraryClasses(virtualFile)) {

      List<OrderEntry> orderEntriesForFile = fileIndexFacade.getOrderEntriesForFile(virtualFile);
      for (OrderEntry orderEntry : orderEntriesForFile) {
        Module ownerModule = orderEntry.getOwnerModule();
        ModuleExtension extension = ModuleUtilCore.getExtension(ownerModule, extensionClass);
        if (extension != null) {
          for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
            if (p.isSupported(extension)) {
              return p.createPackage(psiManager, this, extensionClass, qualifiedName);
            }
          }
        }
      }
    }
    return null;
  }

  @RequiredReadAction
  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull PsiDirectory directory, @NotNull Class<? extends ModuleExtension> extensionClass) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(directory.getProject()).getFileIndex();
    String packageName = projectFileIndex.getPackageNameByDirectory(directory.getVirtualFile());
    if (packageName == null) {
      return null;
    }
    return findPackage(packageName, extensionClass);
  }

  @RequiredReadAction
  @Override
  public PsiPackage findAnyPackage(@NotNull PsiDirectory directory) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(directory.getProject()).getFileIndex();
    String packageName = projectFileIndex.getPackageNameByDirectory(directory.getVirtualFile());
    if (packageName == null) {
      return null;
    }

    Module module = ModuleUtilCore.findModuleForPsiElement(directory);
    if (module == null) {
      return null;
    }

    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (ModuleExtension<?> moduleExtension : rootManager.getExtensions()) {
      final PsiPackage aPackage = findPackage(packageName, moduleExtension.getClass());
      if (aPackage != null) {
        return aPackage;
      }
    }
    return null;
  }

  @RequiredReadAction
  @Override
  public PsiPackage findAnyPackage(@NotNull String packageName) {
    for (ConcurrentMap<String, PsiPackage> map : myPackageCache.values()) {
      PsiPackage psiPackage = map.get(packageName);
      if (psiPackage != null) {
        return psiPackage;
      }
    }
    return null;
  }

  @RequiredReadAction
  @Override
  public boolean isValidPackageName(@NotNull Module module, @NotNull String packageName) {
    PsiPackageSupportProvider[] extensions = PsiPackageSupportProvider.EP_NAME.getExtensions();

    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (ModuleExtension<?> moduleExtension : rootManager.getExtensions()) {
      for (PsiPackageSupportProvider provider : extensions) {
        if (provider.isSupported(moduleExtension)) {
          return provider.isValidPackageName(module, packageName);
        }
      }
    }
    return true;
  }
}
