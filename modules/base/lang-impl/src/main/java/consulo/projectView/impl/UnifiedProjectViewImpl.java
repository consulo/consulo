/*
 * Copyright 2013-2017 consulo.io
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
package consulo.projectView.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.HelpID;
import com.intellij.ide.projectView.impl.*;
import com.intellij.ide.projectView.impl.nodes.*;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import consulo.ide.projectView.ProjectViewEx;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.tree.impl.TreeStructureWrappenModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 23-Oct-17
 */
@Singleton
public class UnifiedProjectViewImpl implements ProjectViewEx {
  private final class MyDataProvider implements Function<Key<?>, Object> {
    @Nullable
    private Object getSelectedNodeElement() {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane == null) { // can happen if not initialized yet
        return null;
      }
      DefaultMutableTreeNode node = currentProjectViewPane.getSelectedNode();
      if (node == null) {
        return null;
      }
      Object userObject = node.getUserObject();
      if (userObject instanceof AbstractTreeNode) {
        return ((AbstractTreeNode)userObject).getValue();
      }
      if (!(userObject instanceof NodeDescriptor)) {
        return null;
      }
      return ((NodeDescriptor)userObject).getElement();
    }

    @Override
    public Object apply(@Nonnull Key<?> dataId) {
      final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
      if (currentProjectViewPane != null) {
        final Object paneSpecificData = currentProjectViewPane.getData(dataId);
        if (paneSpecificData != null) return paneSpecificData;
      }

      if (CommonDataKeys.PSI_ELEMENT == dataId) {
        if (currentProjectViewPane == null) return null;

        TreeNode<AbstractTreeNode> selectedNode = myTree.getSelectedNode();
        if(selectedNode != null) {
          AbstractTreeNode value = selectedNode.getValue();
          if(value instanceof AbstractPsiBasedNode) {
           return value.getValue();
          }
        }
        return null;
      }
      if (LangDataKeys.PSI_ELEMENT_ARRAY == dataId) {
        if (currentProjectViewPane == null) {
          return null;
        }
        PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
        return elements.length == 0 ? null : elements;
      }
      if (LangDataKeys.MODULE == dataId) {
        VirtualFile[] virtualFiles = (VirtualFile[])apply(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles == null || virtualFiles.length <= 1) return null;
        final Set<Module> modules = new HashSet<>();
        for (VirtualFile virtualFile : virtualFiles) {
          modules.add(ModuleUtilCore.findModuleForFile(virtualFile, myProject));
        }
        return modules.size() == 1 ? modules.iterator().next() : null;
      }
      if (LangDataKeys.TARGET_PSI_ELEMENT == dataId) {
        return null;
      }
      //if (PlatformDataKeys.CUT_PROVIDER == dataId) {
      //  return myCopyPasteDelegator.getCutProvider();
      //}
      //if (PlatformDataKeys.COPY_PROVIDER == dataId) {
      //  return myCopyPasteDelegator.getCopyProvider();
      //}
      //if (PlatformDataKeys.PASTE_PROVIDER == dataId) {
      //  return myCopyPasteDelegator.getPasteProvider();
      //}
      //if (LangDataKeys.IDE_VIEW == dataId) {
      //  return myIdeView;
      //}
      //if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER == dataId) {
      //  final Module[] modules = getSelectedModules();
      //  if (modules != null) {
      //    return myDeleteModuleProvider;
      //  }
      //  final LibraryOrderEntry orderEntry = getSelectedLibrary();
      //  if (orderEntry != null) {
      //    return new DeleteProvider() {
      //      @Override
      //      public void deleteElement(@NotNull DataContext dataContext) {
      //        detachLibrary(orderEntry, myProject);
      //      }
      //
      //      @Override
      //      public boolean canDeleteElement(@NotNull DataContext dataContext) {
      //        return true;
      //      }
      //    };
      //  }
      //  return myDeletePSIElementProvider;
      //}
      if (PlatformDataKeys.HELP_ID == dataId) {
        return HelpID.PROJECT_VIEWS;
      }
      //if (ProjectViewImpl.DATA_KEY == dataId) {
      //  return ProjectViewImpl.this;
      //}
      if (PlatformDataKeys.PROJECT_CONTEXT == dataId) {
        Object selected = getSelectedNodeElement();
        return selected instanceof Project ? selected : null;
      }
      if (LangDataKeys.MODULE_CONTEXT == dataId) {
        Object selected = getSelectedNodeElement();
        if (selected instanceof Module) {
          return !((Module)selected).isDisposed() ? selected : null;
        }
        else if (selected instanceof PsiDirectory) {
          return moduleBySingleContentRoot(((PsiDirectory)selected).getVirtualFile());
        }
        else if (selected instanceof VirtualFile) {
          return moduleBySingleContentRoot((VirtualFile)selected);
        }
        else {
          return null;
        }
      }

      if (LangDataKeys.MODULE_CONTEXT_ARRAY == dataId) {
        return getSelectedModules();
      }
      if (ModuleGroup.ARRAY_DATA_KEY == dataId) {
        final List<ModuleGroup> selectedElements = getSelectedElements(ModuleGroup.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[selectedElements.size()]);
      }
      if (LibraryGroupElement.ARRAY_DATA_KEY == dataId) {
        final List<LibraryGroupElement> selectedElements = getSelectedElements(LibraryGroupElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[selectedElements.size()]);
      }
      if (NamedLibraryElement.ARRAY_DATA_KEY == dataId) {
        final List<NamedLibraryElement> selectedElements = getSelectedElements(NamedLibraryElement.class);
        return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[selectedElements.size()]);
      }

      //if (QuickActionProvider.KEY == dataId) {
      //  return ProjectViewImpl.this;
      //}

      return null;
    }

    @javax.annotation.Nullable
    private LibraryOrderEntry getSelectedLibrary() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      DefaultMutableTreeNode node = viewPane != null ? viewPane.getSelectedNode() : null;
      if (node == null) return null;
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
      if (parent == null) return null;
      Object userObject = parent.getUserObject();
      if (userObject instanceof LibraryGroupNode) {
        userObject = node.getUserObject();
        if (userObject instanceof NamedLibraryElementNode) {
          NamedLibraryElement element = ((NamedLibraryElementNode)userObject).getValue();
          OrderEntry orderEntry = element.getOrderEntry();
          return orderEntry instanceof LibraryOrderEntry ? (LibraryOrderEntry)orderEntry : null;
        }
        PsiDirectory directory = ((PsiDirectoryNode)userObject).getValue();
        VirtualFile virtualFile = directory.getVirtualFile();
        Module module = (Module)((AbstractTreeNode)((DefaultMutableTreeNode)parent.getParent()).getUserObject()).getValue();

        if (module == null) return null;
        ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
        OrderEntry entry = index.getOrderEntryForFile(virtualFile);
        if (entry instanceof LibraryOrderEntry) {
          return (LibraryOrderEntry)entry;
        }
      }

      return null;
    }

    private void detachLibrary(@Nonnull final LibraryOrderEntry orderEntry, @Nonnull Project project) {
      final Module module = orderEntry.getOwnerModule();
      String message = IdeBundle.message("detach.library.from.module", orderEntry.getPresentableName(), module.getName());
      String title = IdeBundle.message("detach.library");
      int ret = Messages.showOkCancelDialog(project, message, title, Messages.getQuestionIcon());
      if (ret != Messages.OK) return;
      CommandProcessor.getInstance().executeCommand(module.getProject(), () -> {
        final Runnable action = () -> {
          ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
          OrderEntry[] orderEntries = rootManager.getOrderEntries();
          ModifiableRootModel model = rootManager.getModifiableModel();
          OrderEntry[] modifiableEntries = model.getOrderEntries();
          for (int i = 0; i < orderEntries.length; i++) {
            OrderEntry entry = orderEntries[i];
            if (entry instanceof LibraryOrderEntry && ((LibraryOrderEntry)entry).getLibrary() == orderEntry.getLibrary()) {
              model.removeOrderEntry(modifiableEntries[i]);
            }
          }
          model.commit();
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }, title, null);
    }

    @javax.annotation.Nullable
    private Module[] getSelectedModules() {
      final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
      if (viewPane == null) return null;
      final Object[] elements = viewPane.getSelectedElements();
      ArrayList<Module> result = new ArrayList<>();
      for (Object element : elements) {
        if (element instanceof Module) {
          final Module module = (Module)element;
          if (!module.isDisposed()) {
            result.add(module);
          }
        }
        else if (element instanceof ModuleGroup) {
          Collection<Module> modules = ((ModuleGroup)element).modulesInGroup(myProject, true);
          result.addAll(modules);
        }
        else if (element instanceof PsiDirectory) {
          Module module = moduleBySingleContentRoot(((PsiDirectory)element).getVirtualFile());
          if (module != null) result.add(module);
        }
        else if (element instanceof VirtualFile) {
          Module module = moduleBySingleContentRoot((VirtualFile)element);
          if (module != null) result.add(module);
        }
      }

      if (result.isEmpty()) {
        return null;
      }
      else {
        return result.toArray(new Module[result.size()]);
      }
    }
  }

  private final Project myProject;
  private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();

  private AbstractProjectViewPane myCurrentPane;

  private Tree<AbstractTreeNode> myTree;

  @Inject
  public UnifiedProjectViewImpl(Project project) {
    myProject = project;
  }

  /**
   * Project view has the same node for module and its single content root
   * => MODULE_CONTEXT data key should return the module when its content root is selected
   * When there are multiple content roots, they have different nodes under the module node
   * => MODULE_CONTEXT should be only available for the module node
   * otherwise VirtualFileArrayRule will return all module's content roots when just one of them is selected
   */
  @javax.annotation.Nullable
  private Module moduleBySingleContentRoot(@Nonnull VirtualFile file) {
    if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
      Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(file);
      if (module != null && !module.isDisposed() && ModuleRootManager.getInstance(module).getContentRoots().length == 1) {
        return module;
      }
    }

    return null;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus) {
    return AsyncResult.resolved(null);
  }

  @RequiredUIAccess
  @Override
  public void setupToolWindow(@Nonnull ToolWindow toolWindow, boolean loadPaneExtensions) {

    ProjectViewPane projectViewPane = null;
    for (AbstractProjectViewPane pane : AbstractProjectViewPane.EP_NAME.getExtensions(myProject)) {
      if (pane instanceof ProjectViewPane) {
        projectViewPane = (ProjectViewPane)pane;
      }
    }

    assert projectViewPane != null;

    myCurrentPane = projectViewPane;

    SelectInTarget selectInTarget = projectViewPane.createSelectInTarget();
    if (selectInTarget != null) {
      mySelectInTargets.put(projectViewPane.getId(), selectInTarget);
    }

    ProjectAbstractTreeStructureBase structure = projectViewPane.createStructure();

    TreeStructureWrappenModel<AbstractTreeNode> model = new TreeStructureWrappenModel<AbstractTreeNode>(structure) {
      @Override
      public boolean onDoubleClick(@Nonnull Tree tree, @Nonnull TreeNode node) {
        if (node.isLeaf()) {
          AbstractTreeNode value = (AbstractTreeNode)node.getValue();

          value.navigate(true);

          return false;
        }

        return true;
      }
    };

    myTree = Tree.create((AbstractTreeNode)structure.getRootElement(), model);
    WrappedLayout wrappedLayout = WrappedLayout.create(myTree);
    wrappedLayout.addUserDataProvider(new MyDataProvider());

    Content content = ContentFactory.getInstance().createUIContent(wrappedLayout, "Project", true);

    toolWindow.getContentManager().addContent(content);
  }

  @Nonnull
  private <T> List<T> getSelectedElements(@Nonnull Class<T> klass) {
    List<T> result = new ArrayList<>();
    final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
    if (viewPane == null) return result;
    final Object[] elements = viewPane.getSelectedElements();
    for (Object element : elements) {
      //element still valid
      if (element != null && klass.isAssignableFrom(element.getClass())) {
        result.add((T)element);
      }
    }
    return result;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> changeViewCB(@Nonnull String viewId, String subId) {
    return AsyncResult.done(null);
  }

  @Nullable
  @Override
  public PsiElement getParentOfCurrentSelection() {
    return null;
  }

  @Override
  public void changeView(String viewId) {

  }

  @Override
  public void changeView(String viewId, String subId) {

  }

  @Override
  public void changeView() {

  }

  @Override
  public void refresh() {

  }

  @Override
  public boolean isAutoscrollToSource(String paneId) {
    return false;
  }

  @Override
  public boolean isFlattenPackages(String paneId) {
    return false;
  }

  @Override
  public boolean isShowMembers(String paneId) {
    return false;
  }

  @Override
  public boolean isHideEmptyMiddlePackages(String paneId) {
    return false;
  }

  @Override
  public void setHideEmptyPackages(boolean hideEmptyPackages, String paneId) {

  }

  @Override
  public boolean isShowLibraryContents(String paneId) {
    return false;
  }

  @Override
  public void setShowLibraryContents(boolean showLibraryContents, String paneId) {

  }

  @Override
  public boolean isShowModules(String paneId) {
    return false;
  }

  @Override
  public void setShowModules(boolean showModules, String paneId) {

  }

  @Override
  public void addProjectPane(AbstractProjectViewPane pane) {

  }

  @Override
  public void removeProjectPane(AbstractProjectViewPane instance) {

  }

  @Override
  public AbstractProjectViewPane getProjectViewPaneById(String id) {
    return null;
  }

  @Override
  public boolean isAutoscrollFromSource(String paneId) {
    return false;
  }

  @Override
  public boolean isAbbreviatePackageNames(String paneId) {
    return false;
  }

  @Override
  public void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId) {

  }

  @Override
  public String getCurrentViewId() {
    return null;
  }

  @Override
  public boolean isManualOrder(String paneId) {
    return false;
  }

  @Override
  public void setManualOrder(@Nonnull String paneId, boolean enabled) {

  }

  @Override
  public void selectPsiElement(PsiElement element, boolean requestFocus) {

  }

  @Override
  public boolean isSortByType(String paneId) {
    return false;
  }

  @Override
  public void setSortByType(String paneId, boolean sortByType) {

  }

  @Override
  public AbstractProjectViewPane getCurrentProjectViewPane() {
    return myCurrentPane;
  }

  @Override
  public Collection<String> getPaneIds() {
    return null;
  }

  @Nonnull
  @Override
  public Collection<SelectInTarget> getSelectInTargets() {
    return mySelectInTargets.values();
  }
}
