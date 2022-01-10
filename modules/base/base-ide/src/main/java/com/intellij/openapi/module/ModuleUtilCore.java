/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.module;

import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.Processor;
import java.util.HashSet;
import com.intellij.util.graph.Graph;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.roots.ContentFolderTypeProvider;
import consulo.util.pointers.NamedPointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ModuleUtilCore {
  public static final Key<Module> KEY_MODULE = new Key<Module>("Module");

  public static boolean projectContainsFile(final Project project, VirtualFile file, boolean isLibraryElement) {
    ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
    if (isLibraryElement) {
      List<OrderEntry> orders = projectRootManager.getFileIndex().getOrderEntriesForFile(file);
      for (OrderEntry orderEntry : orders) {
        if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry || orderEntry instanceof LibraryOrderEntry) {
          return true;
        }
      }
      return false;
    }
    else {
      return projectRootManager.getFileIndex().isInContent(file);
    }
  }

  public static String getModuleNameInReadAction(@Nonnull final Module module) {
    return AccessRule.read(module::getName);
  }

  public static boolean isModuleDisposed(PsiElement element) {
    if (!element.isValid()) return true;
    final Project project = element.getProject();
    ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final PsiFile file = element.getContainingFile();
    if (file == null) return true;
    VirtualFile vFile = file.getVirtualFile();
    final Module module = vFile == null ? null : projectFileIndex.getModuleForFile(vFile);
    // element may be in library
    return module == null ? !projectFileIndex.isInLibraryClasses(vFile) : module.isDisposed();
  }

  @Nullable
  public static Module findModuleForFile(@Nonnull PsiFile file) {
    return findModuleForPsiElement(file);
  }

  @Nullable
  public static Module findModuleForFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    return fileIndex.getModuleForFile(file);
  }

  @Nullable
  @RequiredReadAction
  public static Module findModuleForPsiElement(@Nonnull PsiElement element) {
    if (!element.isValid()) return null;

    Project project = element.getProject();
    if (project.isDefault()) return null;
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

    if (element instanceof PsiFileSystemItem && (!(element instanceof PsiFile) || element.getContext() == null)) {
      VirtualFile vFile = ((PsiFileSystemItem)element).getVirtualFile();
      if (vFile == null) {
        PsiFile containingFile = element.getContainingFile();
        vFile = containingFile == null ? null : containingFile.getOriginalFile().getVirtualFile();
        if (vFile == null) {
          return element.getUserData(KEY_MODULE);
        }
      }
      if (fileIndex.isInLibrarySource(vFile) || fileIndex.isInLibraryClasses(vFile)) {
        final List<OrderEntry> orderEntries = fileIndex.getOrderEntriesForFile(vFile);
        if (orderEntries.isEmpty()) {
          return null;
        }
        if (orderEntries.size() == 1) {
          return orderEntries.get(0).getOwnerModule();
        }
        Set<Module> modules = new HashSet<Module>();
        for (OrderEntry orderEntry : orderEntries) {
          modules.add(orderEntry.getOwnerModule());
        }
        final Module[] candidates = modules.toArray(new Module[modules.size()]);
        Arrays.sort(candidates, ModuleManager.getInstance(project).moduleDependencyComparator());
        return candidates[0];
      }
      return fileIndex.getModuleForFile(vFile);
    }
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      PsiElement context;
      while ((context = containingFile.getContext()) != null) {
        final PsiFile file = context.getContainingFile();
        if (file == null) break;
        containingFile = file;
      }

      if (containingFile.getUserData(KEY_MODULE) != null) {
        return containingFile.getUserData(KEY_MODULE);
      }

      final PsiFile originalFile = containingFile.getOriginalFile();
      if (originalFile.getUserData(KEY_MODULE) != null) {
        return originalFile.getUserData(KEY_MODULE);
      }

      final VirtualFile virtualFile = originalFile.getVirtualFile();
      if (virtualFile != null) {
        return fileIndex.getModuleForFile(virtualFile);
      }
    }

    return element.getUserData(KEY_MODULE);
  }

  //ignores export flag
  public static void getDependencies(@Nonnull Module module, Set<Module> modules) {
    if (modules.contains(module)) return;
    modules.add(module);
    Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
    for (Module dependency : dependencies) {
      getDependencies(dependency, modules);
    }
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

  @Nonnull
  @RequiredReadAction
  public static List<Module> getAllDependentModules(@Nonnull Module module) {
    final ArrayList<Module> list = new ArrayList<Module>();
    final Graph<Module> graph = ModuleManager.getInstance(module.getProject()).moduleGraph();
    for (Iterator<Module> i = graph.getOut(module); i.hasNext(); ) {
      list.add(i.next());
    }
    return list;
  }

  @RequiredReadAction
  public static boolean visitMeAndDependentModules(@Nonnull final Module module, final Processor<Module> visitor) {
    if (!visitor.process(module)) {
      return false;
    }
    final List<Module> list = getAllDependentModules(module);
    for (Module dependentModule : list) {
      if (!visitor.process(dependentModule)) {
        return false;
      }
    }
    return true;
  }

  public static boolean moduleContainsFile(@Nonnull final Module module, VirtualFile file, boolean isLibraryElement) {
    ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    if (isLibraryElement) {
      OrderEntry orderEntry = moduleRootManager.getFileIndex().getOrderEntryForFile(file);
      return orderEntry instanceof ModuleExtensionWithSdkOrderEntry || orderEntry instanceof LibraryOrderEntry;
    }
    else {
      return moduleRootManager.getFileIndex().isInContent(file);
    }
  }

  @RequiredReadAction
  @Nonnull
  public static List<ContentFolder> getContentFolders(@Nonnull Project project) {
    ModuleManager moduleManager = ModuleManager.getInstance(project);
    final List<ContentFolder> contentFolders = new ArrayList<ContentFolder>();
    for (Module module : moduleManager.getModules()) {
      ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      moduleRootManager.iterateContentEntries(contentEntry -> {
        Collections.addAll(contentFolders, contentEntry.getFolders(Predicates.<ContentFolderTypeProvider>alwaysTrue()));
        return false;
      });
    }
    return contentFolders;
  }

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
  @RequiredReadAction
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull PsiElement element, @Nonnull Class<E> extensionClass) {
    Module moduleForPsiElement = findModuleForPsiElement(element);
    if (moduleForPsiElement == null) {
      return null;
    }
    return getExtension(moduleForPsiElement, extensionClass);
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

  @Nullable
  @RequiredReadAction
  public static Sdk getSdk(@Nonnull PsiElement element, @Nonnull Class<? extends ModuleExtensionWithSdk> extensionClass) {
    Module moduleForPsiElement = findModuleForPsiElement(element);
    if (moduleForPsiElement == null) {
      return null;
    }
    return getSdk(moduleForPsiElement, extensionClass);
  }

  @Nullable
  public static Sdk getSdk(@Nonnull Project project, @Nonnull VirtualFile virtualFile, @Nonnull Class<? extends ModuleExtensionWithSdk> extensionClass) {
    Module moduleForPsiElement = findModuleForFile(virtualFile, project);
    if (moduleForPsiElement == null) {
      return null;
    }
    return getSdk(moduleForPsiElement, extensionClass);
  }

  @Nonnull
  @RequiredReadAction
  public static NamedPointer<Module> createPointer(@Nonnull Module module) {
    return ModulePointerManager.getInstance(module.getProject()).create(module);
  }

  @Nonnull
  @RequiredReadAction
  public static NamedPointer<Module> createPointer(@Nonnull Project project, @Nonnull String name) {
    return ModulePointerManager.getInstance(project).create(name);
  }
}
