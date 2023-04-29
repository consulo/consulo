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
package consulo.language.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.util.function.Processor;
import consulo.component.util.graph.Graph;
import consulo.component.util.pointer.NamedPointer;
import consulo.content.bundle.Sdk;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModulePointerManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModuleRootModel;
import consulo.module.content.layer.ModulesProvider;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.module.content.util.ModuleContentUtil;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ModuleUtilCore {
  public static final Key<Module> KEY_MODULE = Key.create("Module");

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
  @RequiredReadAction
  @Deprecated
  public static Module findModuleForFile(@Nonnull PsiFile file) {
    return file.getModule();
  }

  @Nullable
  public static Module findModuleForFile(@Nonnull VirtualFile file, @Nonnull Project project) {
    return ModuleContentUtil.findModuleForFile(file, project);
  }

  @Nullable
  @RequiredReadAction
  @Deprecated
  public static Module findModuleForPsiElement(@Nonnull PsiElement element) {
    return element.getModule();
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
  @Deprecated
  public static void collectModulesDependsOn(@Nonnull final Module module, final Set<Module> result) {
    ModuleContentUtil.collectModulesDependsOn(module, result);
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
    return ModuleContentUtil.getContentFolders(project);
  }

  @Nullable
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull Module module, @Nonnull Class<E> extensionClass) {
    return module.getExtension(extensionClass);
  }

  @Nullable
  public static ModuleExtension<?> getExtension(@Nonnull Module module, @Nonnull String key) {
    return module.getExtension(key);
  }

  @Nullable
  @RequiredReadAction
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull PsiElement element, @Nonnull Class<E> extensionClass) {
    if (!element.isValid())  {
      return null;
    }

    Module module = element.getModule();
    if (module == null) {
      return null;
    }
    return module.getExtension(extensionClass);
  }

  @Nullable
  public static <E extends ModuleExtension<E>> E getExtension(@Nonnull Project project, @Nonnull VirtualFile virtualFile, @Nonnull Class<E> extensionClass) {
    return ModuleContentUtil.getExtension(project, virtualFile, extensionClass);
  }

  @Nullable
  public static Sdk getSdk(@Nonnull Module module, @Nonnull Class<? extends ModuleExtensionWithSdk> extensionClass) {
    return ModuleContentUtil.getSdk(module, extensionClass);
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

  public static boolean hasModuleExtension(@Nonnull ModulesProvider modulesProvider, @Nonnull Class<? extends ModuleExtension> clazz) {
    for (Module module : modulesProvider.getModules()) {
      ModuleRootModel rootModel = modulesProvider.getRootModel(module);
      if (rootModel == null) {
        continue;
      }

      ModuleExtension extension = rootModel.getExtension(clazz);
      if (extension != null) {
        return true;
      }
    }
    return false;
  }
}
