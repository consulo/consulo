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
package consulo.psi.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.ObjectUtil;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.module.extension.ModuleExtension;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.psi.PsiPackageSupportProvider;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 8:05/20.05.13
 */
@Singleton
public class PsiPackageManagerImpl extends PsiPackageManager implements Disposable {
  private final Project myProject;
  private final DirectoryIndex myDirectoryIndex;

  private Map<Class<? extends ModuleExtension>, ConcurrentMap<String, Object>> myPackageCache = ContainerUtil.newConcurrentMap();

  @Inject
  public PsiPackageManagerImpl(Project project, DirectoryIndex directoryIndex) {
    myProject = project;
    myDirectoryIndex = directoryIndex;

    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileCreated(@Nonnull VirtualFileEvent event) {
        myPackageCache.clear();
      }

      @Override
      public void fileDeleted(@Nonnull VirtualFileEvent event) {
        myPackageCache.clear();
      }

      @Override
      public void fileMoved(@Nonnull VirtualFileMoveEvent event) {
        myPackageCache.clear();
      }
    }, this);

    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        myPackageCache.clear();
      }
    });

    LowMemoryWatcher.register(myPackageCache::clear, this);
  }

  @Override
  public void dropCache(@Nonnull Class<? extends ModuleExtension> extensionClass) {
    myPackageCache.remove(extensionClass);
  }

  @RequiredReadAction
  @Nullable
  @Override
  public PsiPackage findPackage(@Nonnull String qualifiedName, @Nonnull Class<? extends ModuleExtension> extensionClass) {
    ConcurrentMap<String, Object> map = myPackageCache.get(extensionClass);
    if (map != null) {
      final Object value = map.get(qualifiedName);
      // if we processed - but not found package
      if (value == ObjectUtil.NULL) {
        return null;
      }
      else if (value != null) {
        return (PsiPackage)value;
      }
    }

    PsiPackage newPackage = createPackage(qualifiedName, extensionClass);

    Object valueForInsert = ObjectUtil.notNull(newPackage, ObjectUtil.NULL);

    myPackageCache.computeIfAbsent(extensionClass, aClass -> ContainerUtil.newConcurrentMap()).putIfAbsent(qualifiedName, valueForInsert);

    return newPackage;
  }

  @Nullable
  private PsiPackage createPackage(@Nonnull String qualifiedName, @Nonnull Class<? extends ModuleExtension> extensionClass) {
    Query<VirtualFile> dirs = myDirectoryIndex.getDirectoriesByPackageName(qualifiedName, true);
    if (dirs.findFirst() == null) {
      return null;
    }

    for (VirtualFile directory : dirs) {
      PsiPackage packageFromProviders = createPackageFromProviders(directory, extensionClass, qualifiedName);
      if (packageFromProviders != null) {
        return packageFromProviders;
      }

      PsiPackage packageFromLibrary = createPackageFromLibrary(directory, extensionClass, qualifiedName);
      if (packageFromLibrary != null) {
        return packageFromLibrary;
      }
    }
    return null;
  }

  @Nullable
  private PsiPackage createPackageFromProviders(@Nonnull VirtualFile virtualFile,
                                                @Nonnull Class<? extends ModuleExtension> extensionClass,
                                                @Nonnull String qualifiedName) {
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
    for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensionList()) {
      if (p.isSupported(extension) && p.acceptVirtualFile(moduleForFile, virtualFile)) {
        return p.createPackage(psiManager, this, extensionClass, qualifiedName);
      }
    }
    return null;
  }

  private PsiPackage createPackageFromLibrary(@Nonnull VirtualFile virtualFile,
                                              @Nonnull Class<? extends ModuleExtension> extensionClass,
                                              @Nonnull String qualifiedName) {
    ProjectFileIndex fileIndexFacade = ProjectFileIndex.getInstance(myProject);
    PsiManager psiManager = PsiManager.getInstance(myProject);
    if (fileIndexFacade.isInLibraryClasses(virtualFile)) {
      List<OrderEntry> orderEntriesForFile = fileIndexFacade.getOrderEntriesForFile(virtualFile);
      for (OrderEntry orderEntry : orderEntriesForFile) {
        Module ownerModule = orderEntry.getOwnerModule();
        ModuleExtension extension = ModuleUtilCore.getExtension(ownerModule, extensionClass);
        if (extension != null) {
          for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensionList()) {
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
  public PsiPackage findPackage(@Nonnull PsiDirectory directory, @Nonnull Class<? extends ModuleExtension> extensionClass) {
    String packageName = myDirectoryIndex.getPackageName(directory.getVirtualFile());
    if (packageName == null) {
      return null;
    }
    return findPackage(packageName, extensionClass);
  }

  @RequiredReadAction
  @Override
  @Nullable
  public PsiPackage findAnyPackage(@Nonnull VirtualFile directory) {
    String packageName = myDirectoryIndex.getPackageName(directory);
    if (packageName == null) {
      return null;
    }

    PsiPackage packageFromCache = findAnyPackageFromCache(packageName);
    if (packageFromCache != null) {
      return packageFromCache;
    }

    Module module = ModuleUtilCore.findModuleForFile(directory, myProject);
    if (module != null) {
      final PsiPackage aPackage = findForModule(packageName, module);
      if (aPackage != null) return aPackage;
    }
    else {
      List<OrderEntry> orderEntriesForFile = ProjectFileIndex.getInstance(myProject).getOrderEntriesForFile(directory);
      for (OrderEntry orderEntry : orderEntriesForFile) {
        Module ownerModule = orderEntry.getOwnerModule();
        return findForModule(packageName, ownerModule);
      }
    }
    return null;
  }

  @Nullable
  @RequiredReadAction
  private PsiPackage findForModule(String packageName, Module module) {
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
  public PsiPackage findAnyPackage(@Nonnull String packageName) {
    Query<VirtualFile> dirs = myDirectoryIndex.getDirectoriesByPackageName(packageName, true);
    if (dirs.findFirst() == null) {
      return null;
    }

    PsiPackage packageFromCache = findAnyPackageFromCache(packageName);
    if (packageFromCache != null) {
      return packageFromCache;
    }

    for (VirtualFile dir : dirs) {
      PsiPackage psiPackage = findAnyPackage(dir);
      if (psiPackage != null) {
        return psiPackage;
      }
    }
    return null;
  }

  @Nullable
  private PsiPackage findAnyPackageFromCache(@Nonnull String packageName) {
    for (ConcurrentMap<String, Object> map : myPackageCache.values()) {
      Object o = map.get(packageName);
      if (o instanceof PsiPackage) {
        return (PsiPackage)o;
      }
    }
    return null;
  }

  @RequiredReadAction
  @Override
  public boolean isValidPackageName(@Nonnull Module module, @Nonnull String packageName) {
    List<PsiPackageSupportProvider> extensionList = PsiPackageSupportProvider.EP_NAME.getExtensionList();

    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
    for (ModuleExtension<?> moduleExtension : rootManager.getExtensions()) {
      for (PsiPackageSupportProvider provider : extensionList) {
        if (provider.isSupported(moduleExtension)) {
          return provider.isValidPackageName(module, packageName);
        }
      }
    }
    return true;
  }

  @Override
  public void dispose() {
    myPackageCache.clear();
  }
}
