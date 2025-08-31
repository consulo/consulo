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
package consulo.ide.impl.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.content.scope.PredefinedSearchScopeProvider;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.hierarchy.HierarchyBrowserBase;
import consulo.ide.impl.idea.ide.scratch.ScratchesSearchScope;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.usage.Usage;
import consulo.usage.UsageView;
import consulo.usage.UsageViewManager;
import consulo.usage.rule.PsiElementUsage;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.*;

@Singleton
@ServiceImpl
public class PredefinedSearchScopeProviderImpl extends PredefinedSearchScopeProvider {

  @Nonnull
  @Override
  @RequiredReadAction
  public List<SearchScope> getPredefinedScopes(@Nonnull final Project project,
                                               @Nullable DataContext dataContext,
                                               boolean suggestSearchInLibs,
                                               boolean prevSearchFiles,
                                               boolean currentSelection,
                                               boolean usageView,
                                               boolean showEmptyScopes) {
    List<SearchScope> result = new LinkedList<>();
    result.add(GlobalSearchScope.everythingScope(project));
    result.add(GlobalSearchScope.projectScope(project));
    if (suggestSearchInLibs) {
      result.add(GlobalSearchScope.allScope(project));
    }

    result.add(GlobalSearchScopesCore.projectProductionScope(project));
    result.add(GlobalSearchScopesCore.projectTestScope(project));

    result.add(ScratchesSearchScope.getScratchesScope(project));

    GlobalSearchScope openFilesScope = GlobalSearchScopes.openFilesScope(project);
    if (openFilesScope != GlobalSearchScope.EMPTY_SCOPE) {
      result.add(openFilesScope);
    }
    else if (showEmptyScopes) {
      result.add(new LocalSearchScope(PsiElement.EMPTY_ARRAY, IdeBundle.message("scope.open.files")));
    }

    Editor selectedTextEditor =
      ApplicationManager.getApplication().isDispatchThread() ? FileEditorManager.getInstance(project).getSelectedTextEditor() : null;
    PsiFile psiFile =
      selectedTextEditor == null ? null : PsiDocumentManager.getInstance(project).getPsiFile(selectedTextEditor.getDocument());
    PsiFile currentFile = psiFile;

    if (dataContext != null) {
      PsiElement dataContextElement = dataContext.getData(PsiFile.KEY);
      if (dataContextElement == null) {
        dataContextElement = dataContext.getData(PsiElement.KEY);
      }

      if (dataContextElement == null && psiFile != null) {
        dataContextElement = psiFile;
      }

      if (dataContextElement != null) {
        Module module = dataContextElement.getModule();
        if (module == null) {
          module = dataContext.getData(Module.KEY);
        }
        if (module != null) {
          result.add(GlobalSearchScope.moduleScope(module));
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
        PsiElement startElement = psiFile.findElementAt(start);
        if (startElement != null) {
          int end = selectionModel.getSelectionEnd();
          PsiElement endElement = psiFile.findElementAt(end);
          if (endElement != null) {
            PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
            if (parent != null) {
              List<PsiElement> elements = new ArrayList<>();
              PsiElement[] children = parent.getChildren();
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
        final Set<Usage> usages = new LinkedHashSet<>(selectedUsageView.getUsages());
        usages.removeAll(selectedUsageView.getExcludedUsages());

        if (prevSearchFiles) {
          Set<VirtualFile> files = collectFiles(usages, true);
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
          List<PsiElement> results = new ArrayList<>(usages.size());
          for (Usage usage : usages) {
            if (usage instanceof PsiElementUsage) {
              PsiElement element = ((PsiElementUsage)usage).getElement();
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

    ContainerUtil.addIfNotNull(result, getSelectedFilesScope(project, dataContext));

    return result;
  }

  private static void addHierarchyScope(@Nonnull Project project, Collection<SearchScope> result) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.HIERARCHY);
    if (toolWindow == null) {
      return;
    }
    ContentManager contentManager = toolWindow.getContentManager();
    Content content = contentManager.getSelectedContent();
    if (content == null) {
      return;
    }
    String name = content.getDisplayName();
    JComponent component = content.getComponent();
    if (!(component instanceof HierarchyBrowserBase)) {
      return;
    }
    HierarchyBrowserBase hierarchyBrowserBase = (HierarchyBrowserBase)component;
    PsiElement[] elements = hierarchyBrowserBase.getAvailableElements();
    if (elements.length > 0) {
      result.add(new LocalSearchScope(elements, "Hierarchy '" + name + "' (visible nodes only)"));
    }
  }

  @Nullable
  private static SearchScope getSelectedFilesScope(Project project, @Nullable DataContext dataContext) {
    VirtualFile[] filesOrDirs = dataContext == null ? null : dataContext.getData(VirtualFile.KEY_OF_ARRAY);
    if (filesOrDirs != null) {
      List<VirtualFile> selectedFiles = ContainerUtil.filter(filesOrDirs, file -> !file.isDirectory());
      if (!selectedFiles.isEmpty()) {
        return GlobalSearchScope.filesScope(project, selectedFiles, "Selected Files");
      }
    }
    return null;
  }

  protected static Set<VirtualFile> collectFiles(Set<Usage> usages, boolean findFirst) {
    Set<VirtualFile> files = new HashSet<>();
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
