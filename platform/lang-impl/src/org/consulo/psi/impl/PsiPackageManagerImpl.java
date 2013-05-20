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
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Query;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.messages.MessageBus;
import org.consulo.psi.PsiPackage;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageSupportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 8:05/20.05.13
 */
public class PsiPackageManagerImpl extends PsiPackageManager {
  private final ConcurrentMap<String, PsiPackage> myPackageCache = new ConcurrentHashMap<String, PsiPackage>();
  private final Module myModule;
  private final DirectoryIndex myDirectoryIndex;

  public PsiPackageManagerImpl(Module module, DirectoryIndex directoryIndex, MessageBus bus) {
    myModule = module;
    myDirectoryIndex = directoryIndex;

    PsiManager psiManager = PsiManager.getInstance(module.getProject());
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
  public void dropCache() {
    myPackageCache.clear();
  }

  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull String qualifiedName) {
    final PsiPackage psiPackage = myPackageCache.get(qualifiedName);
    if (psiPackage != null) {
      return psiPackage;
    }

    final PsiPackage newPackage = createPackage(qualifiedName);
    if (newPackage != null) {
      ConcurrencyUtil.cacheOrGet(myPackageCache, qualifiedName, newPackage);
    }
    return newPackage;
  }

  @Nullable
  private PsiPackage createPackage(@NotNull String qualifiedName) {
    Query<VirtualFile> dirs = myDirectoryIndex.getDirectoriesByPackageName(qualifiedName, false);
    if(dirs.findFirst() == null) {
      return null;
    }

    PsiManager psiManager = PsiManager.getInstance(myModule.getProject());
    final Iterator<VirtualFile> iterator = dirs.iterator();
    while (iterator.hasNext()) {
      final VirtualFile next = iterator.next();

      final Module moduleForFile = ModuleUtil.findModuleForFile(next, myModule.getProject());
      if(moduleForFile == null) {
        continue;
      }

      for (PsiPackageSupportProvider p : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
        if (p.isSupported(myModule)) {
          return p.createPackage(psiManager, this, qualifiedName);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  public PsiPackage findPackage(@NotNull PsiDirectory directory) {
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(directory.getProject()).getFileIndex();
    String packageName = projectFileIndex.getPackageNameByDirectory(directory.getVirtualFile());
    if (packageName == null) {
      return null;
    }
    return findPackage(packageName);
  }
}
