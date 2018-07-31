/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.hierarchy.HierarchyBrowserBase;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.ide.scratch.ScratchFileServiceImpl;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.util.TreeItem;
import com.intellij.util.containers.ContainerUtil;
import consulo.application.AccessRule;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.*;

@Singleton
public class PredefinedSearchScopeProviderImpl extends PredefinedSearchScopeProvider {

  @Nonnull
  @Override
  public List<SearchScope> getPredefinedScopes(@Nonnull final Project project,
                                               @Nullable final DataContext dataContext,
                                               boolean suggestSearchInLibs,
                                               boolean prevSearchFiles,
                                               boolean currentSelection,
                                               boolean usageView,
                                               boolean showEmptyScopes) {
    Collection<SearchScope> result = ContainerUtil.newLinkedHashSet();
    result.add(GlobalSearchScope.projectScope(project));
    if (suggestSearchInLibs) {
      result.add(GlobalSearchScope.allScope(project));
    }

    result.add(GlobalSearchScopesCore.projectProductionScope(project));
    result.add(GlobalSearchScopesCore.projectTestScope(project));

    result.add(ScratchFileServiceImpl.buildScratchesSearchScope());

    final GlobalSearchScope openFilesScope = GlobalSearchScopes.openFilesScope(project);
    if (openFilesScope != GlobalSearchScope.EMPTY_SCOPE) {
      result.add(openFilesScope);
    }
    else if (showEmptyScopes) {
      result.add(new LocalSearchScope(PsiElement.EMPTY_ARRAY, IdeBundle.message("scope.open.files")));
    }

    final Editor selectedTextEditor =
            ApplicationManager.getApplication().isDispatchThread() ? FileEditorManager.getInstance(project).getSelectedTextEditor() : null;
    PsiFile psiFile = selectedTextEditor == null ? null : PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument());
    PsiFile currentFile = psiFile;

    if (dataContext != null) {
      PsiElement dataContextElement = dataContext.getData(CommonDataKeys.PSI_FILE);
      if (dataContextElement == null) {
        dataContextElement = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
      }

      if (dataContextElement == null && psiFile != null) {
        dataContextElement = psiFile;
      }

      if (dataContextElement != null) {
        Module module = ModuleUtilCore.findModuleForPsiElement(dataContextElement);
        if (module == null) {
          module = dataContext.getData(LangDataKeys.MODULE);
        }
        if (module != null) {
          result.add(module.getModuleScope());
        }

        if (currentFile == null) {
          currentFile = dataContextElement.getContainingFile();
        }
      }
    }

    if (currentFile != null || showEmptyScopes) {
      PsiElement[] scope = currentFile != null ? new PsiElement[]{currentFile} : PsiElement.EMPTY_ARRAY;
      result.add(new LocalSearchScope(scope, IdeBundle.message("scope.current.file")));
    }

    if (currentSelection && selectedTextEditor != null && psiFile != null) {
      SelectionModel selectionModel = selectedTextEditor.getSelectionModel();
      if (selectionModel.hasSelection()) {
        int start = selectionModel.getSelectionStart();
        final PsiElement startElement = psiFile.findElementAt(start);
        if (startElement != null) {
          int end = selectionModel.getSelectionEnd();
          final PsiElement endElement = psiFile.findElementAt(end);
          if (endElement != null) {
            final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
            if (parent != null) {
              final List<PsiElement> elements = new ArrayList<>();
              final PsiElement[] children = parent.getChildren();
              TextRange selection = new TextRange(start, end);
              for (PsiElement child : children) {
                if (!(child instanceof PsiWhiteSpace) && child.getContainingFile() != null && selection.contains(child.getTextOffset())) {
                  elements.add(child);
                }
              }
              if (!elements.isEmpty()) {
                SearchScope local = new LocalSearchScope(PsiUtilCore.toPsiElementArray(elements), IdeBundle.message("scope.selection"));
                result.add(local);
              }
            }
          }
        }
      }
    }

    if (usageView) {
      addHierarchyScope(project, result);
      UsageView selectedUsageView = UsageViewManager.getInstance(project).getSelectedUsageView();
      if (selectedUsageView != null && !selectedUsageView.isSearchInProgress()) {
        final Set<Usage> usages = ContainerUtil.newTroveSet(selectedUsageView.getUsages());
        usages.removeAll(selectedUsageView.getExcludedUsages());

        if (prevSearchFiles) {
          final Set<VirtualFile> files = collectFiles(usages, true);
          if (!files.isEmpty()) {
            GlobalSearchScope prev = new GlobalSearchScope(project) {
              private Set<VirtualFile> myFiles;

              @Nonnull
              @Override
              public String getDisplayName() {
                return IdeBundle.message("scope.files.in.previous.search.result");
              }

              @Override
              public synchronized boolean contains(@Nonnull VirtualFile file) {
                if (myFiles == null) {
                  myFiles = collectFiles(usages, false);
                }
                return myFiles.contains(file);
              }

              @Override
              public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
                return 0;
              }

              @Override
              public boolean isSearchInModuleContent(@Nonnull Module aModule) {
                return true;
              }

              @Override
              public boolean isSearchInLibraries() {
                return true;
              }
            };
            result.add(prev);
          }
        }
        else {
          final List<PsiElement> results = new ArrayList<>(usages.size());
          for (Usage usage : usages) {
            if (usage instanceof PsiElementUsage) {
              final PsiElement element = ((PsiElementUsage)usage).getElement();
              if (element != null && element.isValid() && element.getContainingFile() != null) {
                results.add(element);
              }
            }
          }

          if (!results.isEmpty()) {
            result.add(new LocalSearchScope(PsiUtilCore.toPsiElementArray(results), IdeBundle.message("scope.previous.search.results")));
          }
        }
      }
    }

    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    if (favoritesManager != null) {
      for (final String favorite : favoritesManager.getAvailableFavoritesListNames()) {
        final Collection<TreeItem<Pair<AbstractUrl, String>>> rootUrls = favoritesManager.getFavoritesListRootUrls(favorite);
        if (rootUrls.isEmpty()) continue;  // ignore unused root
        result.add(new GlobalSearchScope(project) {
          @Nonnull
          @Override
          public String getDisplayName() {
            return "Favorite \'" + favorite + "\'";
          }

          @Override
          public boolean contains(@Nonnull final VirtualFile file) {
            ThrowableComputable<Boolean,RuntimeException> action = () -> favoritesManager.contains(favorite, file);
            return AccessRule.read(action);
          }

          @Override
          public int compare(@Nonnull final VirtualFile file1, @Nonnull final VirtualFile file2) {
            return 0;
          }

          @Override
          public boolean isSearchInModuleContent(@Nonnull final Module aModule) {
            return true;
          }

          @Override
          public boolean isSearchInLibraries() {
            return true;
          }
        });
      }
    }

    ContainerUtil.addIfNotNull(result, getSelectedFilesScope(project, dataContext));

    return ContainerUtil.newArrayList(result);
  }

  private static void addHierarchyScope(@Nonnull Project project, Collection<SearchScope> result) {
    final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.HIERARCHY);
    if (toolWindow == null) {
      return;
    }
    final ContentManager contentManager = toolWindow.getContentManager();
    final Content content = contentManager.getSelectedContent();
    if (content == null) {
      return;
    }
    final String name = content.getDisplayName();
    final JComponent component = content.getComponent();
    if (!(component instanceof HierarchyBrowserBase)) {
      return;
    }
    final HierarchyBrowserBase hierarchyBrowserBase = (HierarchyBrowserBase)component;
    final PsiElement[] elements = hierarchyBrowserBase.getAvailableElements();
    if (elements.length > 0) {
      result.add(new LocalSearchScope(elements, "Hierarchy '" + name + "' (visible nodes only)"));
    }
  }

  @Nullable
  private static SearchScope getSelectedFilesScope(final Project project, @Nullable DataContext dataContext) {
    final VirtualFile[] filesOrDirs = dataContext == null ? null : dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (filesOrDirs != null) {
      final List<VirtualFile> selectedFiles = ContainerUtil.filter(filesOrDirs, file -> !file.isDirectory());
      if (!selectedFiles.isEmpty()) {
        return GlobalSearchScope.filesScope(project, selectedFiles, "Selected Files");
      }
    }
    return null;
  }

  protected static Set<VirtualFile> collectFiles(Set<Usage> usages, boolean findFirst) {
    final Set<VirtualFile> files = new HashSet<>();
    for (Usage usage : usages) {
      if (usage instanceof PsiElementUsage) {
        PsiElement psiElement = ((PsiElementUsage)usage).getElement();
        if (psiElement != null && psiElement.isValid()) {
          PsiFile psiFile = psiElement.getContainingFile();
          if (psiFile != null) {
            VirtualFile file = psiFile.getVirtualFile();
            if (file != null) {
              files.add(file);
              if (findFirst) return files;
            }
          }
        }
      }
    }
    return files;
  }
}
