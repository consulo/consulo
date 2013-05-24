/*
 * Copyright 2013 Consulo.org
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
package org.consulo.psi.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
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
import org.consulo.module.extension.ModuleExtension;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    Query<VirtualFile> dirs = myDirectoryIndex.getDirectoriesByPackageName(qualifiedName, false);
    if (dirs.findFirst() == null) {
      return null;
    }

    for (VirtualFile next : dirs) {
      for (PsiPackageResolver psiPackageResolver : PsiPackageResolver.EP_NAME.getExtensions()) {
        final PsiPackage psiPackage = psiPackageResolver.resolvePackage(this, next, extensionClass, qualifiedName);
        if (psiPackage != null) {
          return psiPackage;
        }
      }
    }
    return null;
  }

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
    for (ModuleExtension moduleExtension : rootManager.getExtensions()) {
      final PsiPackage aPackage = findPackage(packageName, moduleExtension.getClass());
      if (aPackage != null) {
        return aPackage;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }
}
