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
package consulo.ide.impl.idea.codeInspection.ui;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.ide.impl.idea.codeInspection.ex.*;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefEntity;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeComparator;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * @author max
 * @since 2001-11-04
 */
public class InspectionTree extends Tree {
    private final HashSet<Object> myExpandedUserObjects;
    @Nonnull
    private final GlobalInspectionContextImpl myContext;
    private SelectionPath mySelectionPath;

    public InspectionTree(@Nonnull Project project, @Nonnull GlobalInspectionContextImpl context) {
        super(new InspectionRootNode(project));
        myContext = context;

        setCellRenderer(new CellRenderer());
        setShowsRootHandles(true);
        UIUtil.setLineStyleAngled(this);
        addTreeWillExpandListener(new ExpandListener());

        myExpandedUserObjects = new HashSet<>();
        myExpandedUserObjects.add(project);

        TreeUtil.installActions(this);
        new TreeSpeedSearch(this, o -> InspectionsConfigTreeComparator.getDisplayTextToSort(o.getLastPathComponent().toString()));

        addTreeSelectionListener(e -> {
            TreePath newSelection = e.getNewLeadSelectionPath();
            if (newSelection != null) {
                mySelectionPath = new SelectionPath(newSelection);
            }
        });
    }

    public void removeAllNodes() {
        getRoot().removeAllChildren();
        nodeStructureChanged(getRoot());
    }

    public InspectionTreeNode getRoot() {
        return (InspectionTreeNode) getModel().getRoot();
    }

    @Nullable
    public InspectionToolWrapper getSelectedToolWrapper() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null) {
            return null;
        }
        InspectionToolWrapper toolWrapper = null;
        for (TreePath path : paths) {
            Object[] nodes = path.getPath();
            for (int j = nodes.length - 1; j >= 0; j--) {
                Object node = nodes[j];
                if (node instanceof InspectionNode) {
                    InspectionToolWrapper wrapper = ((InspectionNode) node).getToolWrapper();
                    if (toolWrapper == null) {
                        toolWrapper = wrapper;
                    }
                    else if (toolWrapper != wrapper) {
                        return null;
                    }
                    break;
                }
            }
        }

        return toolWrapper;
    }

    @Nonnull
    public RefEntity[] getSelectedElements() {
        TreePath[] selectionPaths = getSelectionPaths();
        if (selectionPaths != null) {
            InspectionToolWrapper toolWrapper = getSelectedToolWrapper();
            if (toolWrapper == null) {
                return RefEntity.EMPTY_ELEMENTS_ARRAY;
            }

            List<RefEntity> result = new ArrayList<>();
            for (TreePath selectionPath : selectionPaths) {
                InspectionTreeNode node = (InspectionTreeNode) selectionPath.getLastPathComponent();
                addElementsInNode(node, result);
            }
            return result.toArray(new RefEntity[result.size()]);
        }
        return RefEntity.EMPTY_ELEMENTS_ARRAY;
    }

    private static void addElementsInNode(InspectionTreeNode node, List<RefEntity> out) {
        if (!node.isValid()) {
            return;
        }
        if (node instanceof RefElementNode refElementNode) {
            RefEntity element = refElementNode.getElement();
            if (!out.contains(element)) {
                out.add(0, element);
            }
        }
        if (node instanceof ProblemDescriptionNode problemDescriptionNode) {
            RefEntity element = problemDescriptionNode.getElement();
            if (!out.contains(element)) {
                out.add(0, element);
            }
        }
        Enumeration children = node.children();
        while (children.hasMoreElements()) {
            InspectionTreeNode child = (InspectionTreeNode) children.nextElement();
            addElementsInNode(child, out);
        }
    }

    public CommonProblemDescriptor[] getSelectedDescriptors() {
        InspectionToolWrapper toolWrapper = getSelectedToolWrapper();
        if (getSelectionCount() == 0) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }
        TreePath[] paths = getSelectionPaths();
        Set<CommonProblemDescriptor> descriptors = new LinkedHashSet<>();
        for (TreePath path : paths) {
            Object node = path.getLastPathComponent();
            traverseDescriptors((InspectionTreeNode) node, descriptors);
        }
        return descriptors.toArray(new CommonProblemDescriptor[descriptors.size()]);
    }

    private static void traverseDescriptors(InspectionTreeNode node, Set<CommonProblemDescriptor> descriptors) {
        if (node instanceof ProblemDescriptionNode problemDescriptionNode) {
            descriptors.add(problemDescriptionNode.getDescriptor());
        }
        for (int i = node.getChildCount() - 1; i >= 0; i--) {
            traverseDescriptors((InspectionTreeNode) node.getChildAt(i), descriptors);
        }
    }

    private void nodeStructureChanged(InspectionTreeNode node) {
        ((DefaultTreeModel) getModel()).nodeStructureChanged(node);
    }

    private class ExpandListener implements TreeWillExpandListener {
        @Override
        public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
            InspectionTreeNode node = (InspectionTreeNode) event.getPath().getLastPathComponent();
            Object userObject = node.getUserObject();
            //TODO: never re-sort
            if (node.isValid() && !myExpandedUserObjects.contains(userObject)) {
                sortChildren(node);
                nodeStructureChanged(node);
            }
            myExpandedUserObjects.add(userObject);
            // Smart expand
            if (node.getChildCount() == 1) {
                Application.get().invokeLater(() -> expandPath(new TreePath(node.getPath())));
            }
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
            InspectionTreeNode node = (InspectionTreeNode) event.getPath().getLastPathComponent();
            myExpandedUserObjects.remove(node.getUserObject());
        }
    }

    @RequiredReadAction
    public void restoreExpansionAndSelection() {
        restoreExpansionStatus((InspectionTreeNode) getModel().getRoot());
        if (mySelectionPath != null) {
            mySelectionPath.restore();
        }
    }

    private void restoreExpansionStatus(InspectionTreeNode node) {
        if (myExpandedUserObjects.contains(node.getUserObject())) {
            sortChildren(node);
            TreeNode[] pathToNode = node.getPath();
            expandPath(new TreePath(pathToNode));
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                InspectionTreeNode childNode = (InspectionTreeNode) children.nextElement();
                restoreExpansionStatus(childNode);
            }
        }
    }

    private static class CellRenderer extends ColoredTreeCellRenderer {
    /*  private Project myProject;
      InspectionManagerEx myManager;
      public CellRenderer(Project project) {
        myProject = project;
        myManager = (InspectionManagerEx)InspectionManager.getInstance(myProject);
      }*/

        @Override
        public void customizeCellRenderer(
            @Nonnull JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            InspectionTreeNode node = (InspectionTreeNode) value;

            append(
                node.toString(),
                patchAttr(node, appearsBold(node) ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : getMainForegroundAttributes(node))
            );

            int problemCount = node.getProblemCount();
            if (!leaf) {
                append(
                    LocalizeValue.join(
                        LocalizeValue.space(),
                        InspectionLocalize.inspectionProblemDescriptorCount(problemCount)
                    ),
                    patchAttr(node, SimpleTextAttributes.GRAYED_ATTRIBUTES)
                );
            }

            if (!node.isValid()) {
                append(
                    LocalizeValue.join(
                        LocalizeValue.space(),
                        InspectionLocalize.inspectionInvalidNodeText()
                    ),
                    patchAttr(node, SimpleTextAttributes.ERROR_ATTRIBUTES)
                );
            }
            else {
                setIcon(node.getIcon());
            }
        }

        public static SimpleTextAttributes patchAttr(InspectionTreeNode node, SimpleTextAttributes attributes) {
            if (node.isResolved()) {
                return new SimpleTextAttributes(
                    attributes.getBgColor(),
                    attributes.getFgColor(),
                    attributes.getWaveColor(),
                    attributes.getStyle() | SimpleTextAttributes.STYLE_STRIKEOUT
                );
            }
            return attributes;
        }

        private static SimpleTextAttributes getMainForegroundAttributes(InspectionTreeNode node) {
            SimpleTextAttributes foreground = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            if (node instanceof RefElementNode refElemNode
                && refElemNode.getElement() instanceof RefElement refElem) {
                RefElement containingEntry = refElem.getContainingEntry();
                if (containingEntry.isEntry() && containingEntry.isPermanentEntry()) {
                    foreground = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.blue);
                }
            }
            FileStatus nodeStatus = node.getNodeStatus();
            if (nodeStatus != FileStatus.NOT_CHANGED) {
                foreground = new SimpleTextAttributes(
                    foreground.getBgColor(),
                    TargetAWT.to(nodeStatus.getColor()),
                    foreground.getWaveColor(),
                    foreground.getStyle()
                );
            }
            return foreground;
        }

        private static boolean appearsBold(Object node) {
            return ((InspectionTreeNode) node).appearsBold();
        }
    }

    private void sortChildren(InspectionTreeNode node) {
        List<TreeNode> children = TreeUtil.listChildren(node);
        Collections.sort(children, InspectionResultsViewComparator.getInstance());
        node.removeAllChildren();
        TreeUtil.addChildrenTo(node, children);
        ((DefaultTreeModel) getModel()).reload(node);
    }

    private class SelectionPath {
        private final Object[] myPath;
        private final int[] myIndices;

        public SelectionPath(TreePath path) {
            myPath = path.getPath();
            myIndices = new int[myPath.length];
            for (int i = 0; i < myPath.length - 1; i++) {
                InspectionTreeNode node = (InspectionTreeNode) myPath[i];
                myIndices[i + 1] = getChildIndex(node, (InspectionTreeNode) myPath[i + 1]);
            }
        }

        private int getChildIndex(InspectionTreeNode node, InspectionTreeNode child) {
            int idx = 0;
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                InspectionTreeNode ch = (InspectionTreeNode) children.nextElement();
                if (ch == child) {
                    break;
                }
                idx++;
            }
            return idx;
        }

        @RequiredReadAction
        public void restore() {
            getSelectionModel().removeSelectionPaths(getSelectionModel().getSelectionPaths());
            TreeUtil.selectPath(InspectionTree.this, restorePath());
        }

        @RequiredReadAction
        private TreePath restorePath() {
            List<Object> newPath = new ArrayList<>();

            newPath.add(getModel().getRoot());
            restorePath(newPath, 1);

            return new TreePath(newPath.toArray(new InspectionTreeNode[newPath.size()]));
        }

        @RequiredReadAction
        private void restorePath(List<Object> newPath, int idx) {
            if (idx >= myPath.length) {
                return;
            }
            InspectionTreeNode oldNode = (InspectionTreeNode) myPath[idx];

            InspectionTreeNode newRoot = (InspectionTreeNode) newPath.get(idx - 1);

            InspectionResultsViewComparator comparator = InspectionResultsViewComparator.getInstance();
            Enumeration children = newRoot.children();
            while (children.hasMoreElements()) {
                InspectionTreeNode child = (InspectionTreeNode) children.nextElement();
                if (comparator.compare(child, oldNode) == 0) {
                    newPath.add(child);
                    restorePath(newPath, idx + 1);
                    return;
                }
            }

            // Exactly same element not found. Trying to select somewhat near.
            int count = newRoot.getChildCount();
            if (count > 0) {
                if (myIndices[idx] < count) {
                    newPath.add(newRoot.getChildAt(myIndices[idx]));
                }
                else {
                    newPath.add(newRoot.getChildAt(count - 1));
                }
            }
        }
    }

    @Nonnull
    public GlobalInspectionContextImpl getContext() {
        return myContext;
    }
}
