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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.language.editor.hierarchy.HierarchyBrowser;
import consulo.ui.ex.action.CloseTabToolbarAction;
import consulo.ui.ex.action.ContextHelpAction;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.application.progress.ProgressIndicator;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.AutoScrollToSourceHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.AbstractTreeUi;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.action.ToolWindowActions;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yole
 */
public abstract class HierarchyBrowserBase extends SimpleToolWindowPanel implements HierarchyBrowser, Disposable, DataProvider {
    private static final HierarchyNodeDescriptor[] EMPTY_DESCRIPTORS = new HierarchyNodeDescriptor[0];

    protected Content myContent;
    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
    protected final Project myProject;

    protected HierarchyBrowserBase(@Nonnull Project project) {
        super(true, true);
        myProject = project;
        myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return HierarchyBrowserManager.getInstance(myProject).getState().IS_AUTOSCROLL_TO_SOURCE;
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
                HierarchyBrowserManager.getInstance(myProject).getState().IS_AUTOSCROLL_TO_SOURCE = state;
            }
        };
    }

    @Override
    public void setContent(Content content) {
        myContent = content;
    }

    protected void buildUi(JComponent toolbar, JComponent content) {
        setToolbar(toolbar);
        setContent(content);
    }

    @Override
    public void dispose() {
    }

    protected ActionToolbar createToolbar(String place, String helpID) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        appendActions(actionGroup, helpID);
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(place, actionGroup, true);
        actionToolbar.setTargetComponent(this);
        return actionToolbar;
    }

    protected void appendActions(@Nonnull DefaultActionGroup actionGroup, @Nullable String helpID) {
        actionGroup.add(myAutoScrollToSourceHandler.createToggleAction());
        actionGroup.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EXPAND_ALL));
        actionGroup.add(ToolWindowActions.getPinAction());
        actionGroup.add(new CloseAction());
        if (helpID != null) {
            actionGroup.add(new ContextHelpAction(helpID));
        }
    }

    protected abstract JTree getCurrentTree();

    protected abstract HierarchyTreeBuilder getCurrentBuilder();

    @Nullable
    protected abstract PsiElement getElementFromDescriptor(@Nonnull HierarchyNodeDescriptor descriptor);

    @Nullable
    protected DefaultMutableTreeNode getSelectedNode() {
        JTree tree = getCurrentTree();
        if (tree == null) {
            return null;
        }
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        return path.getLastPathComponent() instanceof DefaultMutableTreeNode lastPathComponent ? lastPathComponent : null;
    }

    public PsiElement[] getAvailableElements() {
        JTree tree = getCurrentTree();
        if (tree == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        TreeModel model = tree.getModel();
        Object root = model.getRoot();
        if (!(root instanceof DefaultMutableTreeNode node)) {
            return PsiElement.EMPTY_ARRAY;
        }
        HierarchyNodeDescriptor descriptor = getDescriptor(node);
        Set<PsiElement> result = new HashSet<>();
        collectElements(descriptor, result);
        return result.toArray(PsiElement.EMPTY_ARRAY);
    }

    private void collectElements(HierarchyNodeDescriptor descriptor, Set<PsiElement> out) {
        if (descriptor == null) {
            return;
        }
        PsiElement element = getElementFromDescriptor(descriptor);
        if (element != null) {
            out.add(element.getNavigationElement());
        }
        Object[] children = descriptor.getCachedChildren();
        if (children == null) {
            return;
        }
        for (Object child : children) {
            if (child instanceof HierarchyNodeDescriptor childDescriptor) {
                collectElements(childDescriptor, out);
            }
        }
    }

    @Nullable
    protected final PsiElement getSelectedElement() {
        DefaultMutableTreeNode node = getSelectedNode();
        HierarchyNodeDescriptor descriptor = node != null ? getDescriptor(node) : null;
        return descriptor != null ? getElementFromDescriptor(descriptor) : null;
    }

    @Nullable
    protected HierarchyNodeDescriptor getDescriptor(DefaultMutableTreeNode node) {
        Object userObject = node != null ? node.getUserObject() : null;
        if (userObject instanceof HierarchyNodeDescriptor descriptor) {
            return descriptor;
        }
        return null;
    }

    public final HierarchyNodeDescriptor[] getSelectedDescriptors() {
        JTree tree = getCurrentTree();
        if (tree == null) {
            return EMPTY_DESCRIPTORS;
        }
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return EMPTY_DESCRIPTORS;
        }
        ArrayList<HierarchyNodeDescriptor> list = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
            if (path.getLastPathComponent() instanceof DefaultMutableTreeNode lastPathComponent) {
                HierarchyNodeDescriptor descriptor = getDescriptor(lastPathComponent);
                if (descriptor != null) {
                    list.add(descriptor);
                }
            }
        }
        return list.toArray(new HierarchyNodeDescriptor[list.size()]);
    }

    @Nonnull
    protected PsiElement[] getSelectedElements() {
        HierarchyNodeDescriptor[] descriptors = getSelectedDescriptors();
        ArrayList<PsiElement> elements = new ArrayList<>();
        for (HierarchyNodeDescriptor descriptor : descriptors) {
            PsiElement element = getElementFromDescriptor(descriptor);
            if (element != null) {
                elements.add(element);
            }
        }
        return PsiUtilCore.toPsiElementArray(elements);
    }


    private Navigatable[] getNavigatables() {
        HierarchyNodeDescriptor[] selectedDescriptors = getSelectedDescriptors();
        if (selectedDescriptors == null || selectedDescriptors.length == 0) {
            return null;
        }
        ArrayList<Navigatable> result = new ArrayList<>();
        for (HierarchyNodeDescriptor descriptor : selectedDescriptors) {
            Navigatable navigatable = getNavigatable(descriptor);
            if (navigatable != null) {
                result.add(navigatable);
            }
        }
        return result.toArray(new Navigatable[result.size()]);
    }

    private Navigatable getNavigatable(HierarchyNodeDescriptor descriptor) {
        if (descriptor instanceof Navigatable navigatable && descriptor.isValid()) {
            return navigatable;
        }

        PsiElement element = getElementFromDescriptor(descriptor);
        return element instanceof NavigatablePsiElement navigatablePsiElement && element.isValid() ? navigatablePsiElement : null;
    }

    @Override
    @Nullable
    public Object getData(@Nonnull Key<?> dataId) {
        if (PsiElement.KEY == dataId) {
            PsiElement anElement = getSelectedElement();
            return anElement != null && anElement.isValid() ? anElement : super.getData(dataId);
        }
        if (PsiElement.KEY_OF_ARRAY == dataId) {
            return getSelectedElements();
        }
        if (DeleteProvider.KEY == dataId) {
            return null;
        }
        if (Navigatable.KEY == dataId) {
            DefaultMutableTreeNode selectedNode = getSelectedNode();
            if (selectedNode == null) {
                return null;
            }
            HierarchyNodeDescriptor descriptor = getDescriptor(selectedNode);
            if (descriptor == null) {
                return null;
            }
            return getNavigatable(descriptor);
        }
        if (Navigatable.KEY_OF_ARRAY == dataId) {
            return getNavigatables();
        }
        if (PlatformDataKeys.TREE_EXPANDER == dataId) {
            JTree tree = getCurrentTree();
            if (tree != null) {
                return new DefaultTreeExpander(tree);
            }
        }
        return super.getData(dataId);
    }

    private final class CloseAction extends CloseTabToolbarAction {
        private CloseAction() {
        }

        @Override
        @RequiredUIAccess
        public final void actionPerformed(@Nonnull AnActionEvent e) {
            HierarchyTreeBuilder builder = getCurrentBuilder();
            AbstractTreeUi treeUi = builder != null ? builder.getUi() : null;
            ProgressIndicator progress = treeUi != null ? treeUi.getProgress() : null;
            if (progress != null) {
                progress.cancel();
            }
            myContent.getManager().removeContent(myContent, true);
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setVisible(myContent != null);
        }
    }

    protected void configureTree(@Nonnull Tree tree) {
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.setToggleClickCount(-1);
        tree.setCellRenderer(new HierarchyNodeRenderer());
        UIUtil.setLineStyleAngled(tree);
        new TreeSpeedSearch(tree);
        TreeUtil.installActions(tree);
        myAutoScrollToSourceHandler.install(tree);
    }
}
