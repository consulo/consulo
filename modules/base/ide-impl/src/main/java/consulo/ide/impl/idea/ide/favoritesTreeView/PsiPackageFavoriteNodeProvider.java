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
package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.annotation.component.ExtensionImpl;
import consulo.bookmark.ui.view.BookmarkNodeProvider;
import consulo.content.ContentIterator;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.projectView.impl.PackageViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.PackageViewModuleGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.PackageViewModuleNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewModuleGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ProjectViewModuleNode;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.tree.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author anna
 * @since 2008-01-21
 */
@ExtensionImpl
public class PsiPackageFavoriteNodeProvider implements BookmarkNodeProvider {
  @Override
  public Collection<AbstractTreeNode> getFavoriteNodes(final DataContext context, final ViewSettings viewSettings) {
    final Project project = context.getData(Project.KEY);
    if (project == null) {
      return null;
    }
    PsiElement[] elements = context.getData(PsiElement.KEY_OF_ARRAY);
    if (elements == null) {
      final PsiElement element = context.getData(PsiElement.KEY);
      if (element != null) {
        elements = new PsiElement[]{element};
      }
    }
    final Collection<AbstractTreeNode> result = new ArrayList<>();
    if (elements != null) {
      for (PsiElement element : elements) {
        if (element instanceof PsiPackage) {
          final PsiPackage psiPackage = (PsiPackage)element;
          final PsiDirectory[] directories = psiPackage.getDirectories();
          if (directories.length > 0) {
            final VirtualFile firstDir = directories[0].getVirtualFile();
            final boolean isLibraryRoot = ProjectRootsUtil.isLibraryRoot(firstDir, project);
            final PackageElement packageElement = new PackageElement(context.getData(Module.KEY), psiPackage, isLibraryRoot);
            result.add(new PackageElementNode(project, packageElement, viewSettings));
          }
        }
      }
      return result.isEmpty() ? null : result;
    }
    final String currentViewId = ProjectView.getInstance(project).getCurrentViewId();
    final Module[] modules = context.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    if (modules != null) {
      for (Module module : modules) {
        if (PackageViewPane.ID.equals(currentViewId)) {
          result.add(new PackageViewModuleNode(project, module, viewSettings));
        }
        else {
          result.add(new ProjectViewModuleNode(project, module, viewSettings));
        }
      }
    }
    else {
      final ModuleGroup[] data = context.getData(ModuleGroup.ARRAY_DATA_KEY);
      if (data != null) {
        for (ModuleGroup moduleGroup : data) {
          if (PackageViewPane.ID.equals(currentViewId)) {
            result.add(new PackageViewModuleGroupNode(project, moduleGroup, viewSettings));
          }
          else {
            result.add(new ProjectViewModuleGroupNode(project, moduleGroup, viewSettings));
          }
        }
      }
    }
    return null;
  }

  @Override
  public AbstractTreeNode createNode(final Project project, final Object element, final ViewSettings viewSettings) {
    if (element instanceof PackageElement) {
      return new PackageElementNode(project, element, viewSettings);
    }
    return null;
  }

  @Override
  public boolean elementContainsFile(final Object element, final VirtualFile vFile) {
    if (element instanceof PackageElement) {
      final Set<Boolean> find = new HashSet<>();
      final ContentIterator contentIterator = fileOrDir -> {
        if (fileOrDir != null && fileOrDir.getPath().equals(vFile.getPath())) {
          find.add(Boolean.TRUE);
        }
        return true;
      };
      final PackageElement packageElement = (PackageElement)element;
      final PsiPackage aPackage = packageElement.getPackage();
      final Project project = aPackage.getProject();
      final GlobalSearchScope scope = packageElement.getModule() != null ? GlobalSearchScope.moduleScope(packageElement.getModule()) : GlobalSearchScope.projectScope(project);
      final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      final PsiDirectory[] directories = aPackage.getDirectories(scope);
      for (PsiDirectory directory : directories) {
        projectFileIndex.iterateContentUnderDirectory(directory.getVirtualFile(), contentIterator);
      }
      return !find.isEmpty();
    }
    return false;
  }

  @Override
  public int getElementWeight(final Object element, final boolean isSortByType) {
    if (element instanceof PackageElement) {
      return 2;
    }
    return -1;
  }

  @Override
  public String getElementLocation(final Object element) {
    if (element instanceof PackageElement) {
      final PackageElement packageElement = ((PackageElement)element);
      final Module module = packageElement.getModule();
      return (module != null ? (module.getName() + ":") : "") + packageElement.getPackage().getQualifiedName();
    }
    return null;
  }

  @Override
  public boolean isInvalidElement(final Object element) {
    return element instanceof PackageElement && !((PackageElement)element).getPackage().isValid();
  }

  @Override
  @Nonnull
  public String getFavoriteTypeId() {
    return "package";
  }

  @Override
  public String getElementUrl(final Object element) {
    if (element instanceof PackageElement) {
      PackageElement packageElement = (PackageElement)element;
      PsiPackage aPackage = packageElement.getPackage();
      if (aPackage == null) {
        return null;
      }
      return aPackage.getQualifiedName();
    }
    return null;
  }

  @Override
  public String getElementModuleName(final Object element) {
    if (element instanceof PackageElement) {
      PackageElement packageElement = (PackageElement)element;
      Module module = packageElement.getModule();
      return module == null ? null : module.getName();
    }
    return null;
  }

  @Override
  public Object[] createPathFromUrl(final Project project, final String url, final String moduleName) {
    final Module module = moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null;
    // module can be null if 'show module' turned off
    final PsiPackage aPackage = PsiPackageManager.getInstance(project).findAnyPackage(url);
    if (aPackage == null) {
      return null;
    }
    PackageElement packageElement = new PackageElement(module, aPackage, false);
    return new Object[]{packageElement};
  }

  @Override
  public PsiElement getPsiElement(final Object element) {
    if (element instanceof PackageElement) {
      return ((PackageElement)element).getPackage();
    }
    return BookmarkNodeProvider.super.getPsiElement(element);
  }
}
