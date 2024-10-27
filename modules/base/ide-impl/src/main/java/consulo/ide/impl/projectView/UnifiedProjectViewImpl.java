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
package consulo.ide.impl.projectView;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.HelpManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.projectView.HelpID;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.LibraryGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.NamedLibraryElementNode;
import consulo.ide.impl.ui.tree.impl.TreeStructureWrappenModel;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractPsiBasedNode;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.layout.WrappedLayout;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2017-10-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedProjectViewImpl implements ProjectViewEx, Disposable {
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
            return userObject instanceof AbstractTreeNode treeNode ? treeNode.getValue()
                : userObject instanceof NodeDescriptor nodeDescriptor ? nodeDescriptor.getElement() : null;
        }

        @Override
        @RequiredReadAction
        public Object apply(@Nonnull Key<?> dataId) {
            final AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
            if (currentProjectViewPane != null) {
                final Object paneSpecificData = currentProjectViewPane.getData(dataId);
                if (paneSpecificData != null) {
                    return paneSpecificData;
                }
            }

            if (PsiElement.KEY == dataId) {
                if (currentProjectViewPane == null) {
                    return null;
                }

                TreeNode<AbstractTreeNode> selectedNode = myTree.getSelectedNode();
                if (selectedNode != null) {
                    AbstractTreeNode value = selectedNode.getValue();
                    if (value instanceof AbstractPsiBasedNode) {
                        return value.getValue();
                    }
                }
                return null;
            }
            if (PsiElement.KEY_OF_ARRAY == dataId) {
                if (currentProjectViewPane == null) {
                    return null;
                }
                PsiElement[] elements = currentProjectViewPane.getSelectedPSIElements();
                return elements.length == 0 ? null : elements;
            }
            if (Module.KEY == dataId) {
                VirtualFile[] virtualFiles = (VirtualFile[])apply(VirtualFile.KEY_OF_ARRAY);
                if (virtualFiles == null || virtualFiles.length <= 1) {
                    return null;
                }
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
            if (HelpManager.HELP_ID == dataId) {
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
                if (selected instanceof Module module) {
                    return !module.isDisposed() ? selected : null;
                }
                else if (selected instanceof PsiDirectory directory) {
                    return moduleBySingleContentRoot(directory.getVirtualFile());
                }
                else if (selected instanceof VirtualFile virtualFile) {
                    return moduleBySingleContentRoot(virtualFile);
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

        @Nullable
        private LibraryOrderEntry getSelectedLibrary() {
            final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            DefaultMutableTreeNode node = viewPane != null ? viewPane.getSelectedNode() : null;
            if (node == null) {
                return null;
            }
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            if (parent == null) {
                return null;
            }
            Object userObject = parent.getUserObject();
            if (userObject instanceof LibraryGroupNode) {
                userObject = node.getUserObject();
                if (userObject instanceof NamedLibraryElementNode namedLibraryElementNode) {
                    NamedLibraryElement element = namedLibraryElementNode.getValue();
                    OrderEntry orderEntry = element.getOrderEntry();
                    return orderEntry instanceof LibraryOrderEntry libraryOrderEntry ? libraryOrderEntry : null;
                }
                PsiDirectory directory = ((PsiDirectoryNode)userObject).getValue();
                VirtualFile virtualFile = directory.getVirtualFile();
                Module module = (Module)((AbstractTreeNode)((DefaultMutableTreeNode)parent.getParent()).getUserObject()).getValue();

                if (module == null) {
                    return null;
                }
                ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
                OrderEntry entry = index.getOrderEntryForFile(virtualFile);
                if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                    return libraryOrderEntry;
                }
            }

            return null;
        }

        @RequiredUIAccess
        private void detachLibrary(@Nonnull final LibraryOrderEntry orderEntry, @Nonnull Project project) {
            final Module module = orderEntry.getOwnerModule();
            LocalizeValue message = IdeLocalize.detachLibraryFromModule(orderEntry.getPresentableName(), module.getName());
            LocalizeValue title = IdeLocalize.detachLibrary();
            int ret = Messages.showOkCancelDialog(project, message.get(), title.get(), UIUtil.getQuestionIcon());
            if (ret != Messages.OK) {
                return;
            }
            CommandProcessor.getInstance().newCommand()
                .project(module.getProject())
                .name(title)
                .inWriteAction()
                .run(() -> {
                    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                    OrderEntry[] orderEntries = rootManager.getOrderEntries();
                    ModifiableRootModel model = rootManager.getModifiableModel();
                    OrderEntry[] modifiableEntries = model.getOrderEntries();
                    for (int i = 0; i < orderEntries.length; i++) {
                        OrderEntry entry = orderEntries[i];
                        if (entry instanceof LibraryOrderEntry libraryOrderEntry
                            && libraryOrderEntry.getLibrary() == orderEntry.getLibrary()) {
                            model.removeOrderEntry(modifiableEntries[i]);
                        }
                    }
                    model.commit();
                });
        }

        @Nullable
        @RequiredReadAction
        private Module[] getSelectedModules() {
            final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            if (viewPane == null) {
                return null;
            }
            final Object[] elements = viewPane.getSelectedElements();
            ArrayList<Module> result = new ArrayList<>();
            for (Object element : elements) {
                if (element instanceof Module module) {
                    if (!module.isDisposed()) {
                        result.add(module);
                    }
                }
                else if (element instanceof ModuleGroup moduleGroup) {
                    Collection<Module> modules = moduleGroup.modulesInGroup(myProject, true);
                    result.addAll(modules);
                }
                else if (element instanceof PsiDirectory directory) {
                    Module module = moduleBySingleContentRoot(directory.getVirtualFile());
                    if (module != null) {
                        result.add(module);
                    }
                }
                else if (element instanceof VirtualFile virtualFile) {
                    Module module = moduleBySingleContentRoot(virtualFile);
                    if (module != null) {
                        result.add(module);
                    }
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
    @Nullable
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
        ProjectViewPaneImpl projectViewPane = null;
        for (AbstractProjectViewPane pane : AbstractProjectViewPane.EP_NAME.getExtensions(myProject)) {
            if (pane instanceof ProjectViewPaneImpl projectViewPaneImpl) {
                projectViewPane = projectViewPaneImpl;
            }
        }

        assert projectViewPane != null;

        myCurrentPane = projectViewPane;

        SelectInTarget selectInTarget = projectViewPane.createSelectInTarget();
        if (selectInTarget != null) {
            mySelectInTargets.put(projectViewPane.getId(), selectInTarget);
        }

        ProjectAbstractTreeStructureBase structure = projectViewPane.createStructure();

        TreeStructureWrappenModel<AbstractTreeNode> model = new TreeStructureWrappenModel<>(structure) {
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

        myTree = Tree.create((AbstractTreeNode)structure.getRootElement(), model, this);
        WrappedLayout wrappedLayout = WrappedLayout.create(myTree);
        wrappedLayout.addUserDataProvider(new MyDataProvider());

        Content content = ContentFactory.getInstance().createUIContent(wrappedLayout, "Project", true);

        toolWindow.getContentManager().addContent(content);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <T> List<T> getSelectedElements(@Nonnull Class<T> klass) {
        List<T> result = new ArrayList<>();
        final AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        if (viewPane == null) {
            return result;
        }
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
    public void addProjectPane(ProjectViewPane pane) {
    }

    @Override
    public void removeProjectPane(ProjectViewPane instance) {
    }

    @Override
    public ProjectViewPane getProjectViewPaneById(String id) {
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

    @Override
    public void dispose() {
    }
}
