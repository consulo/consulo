/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.externalSystem.view;

import consulo.disposer.Disposable;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.ProjectData;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.*;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.Predicate;

/**
 * Tree structure for the external projects view. Manages a {@link StructureTreeModel} backed
 * by an {@link AsyncTreeModel}, and provides node merging on refresh.
 *
 * @author Vladislav.Soroka
 */
public class ExternalProjectsStructure extends SimpleTreeStructure implements Disposable {
    private final Project myProject;
    private final SimpleTree myTree;
    private ExternalProjectsView myExternalProjectsView;
    private StructureTreeModel<ExternalProjectsStructure> myTreeModel;
    private RootNode<?> myRoot;
    private AsyncTreeModel myAsyncTreeModel;

    private final Map<String, ExternalSystemNode<?>> myNodeMapping = new HashMap<>();

    public ExternalProjectsStructure(Project project, SimpleTree tree) {
        myProject = project;
        myTree = tree;
        configureTree(tree);
    }

    public void init(ExternalProjectsView externalProjectsView) {
        myExternalProjectsView = externalProjectsView;
        myRoot = new RootNode<>();
        myTreeModel = new StructureTreeModel<>(this, this);
        myAsyncTreeModel = new AsyncTreeModel(myTreeModel, this);
        myTree.setModel(myAsyncTreeModel);
    }

   
    public Project getProject() {
        return myProject;
    }

    public void updateFrom(@Nullable SimpleNode node) {
        if (node != null) {
            myTreeModel.invalidate(node, true);
        }
    }

    public void updateUpTo(SimpleNode node) {
        SimpleNode each = node;
        while (each != null) {
            myTreeModel.invalidate(each, false);
            each = each.getParent();
        }
    }

    @Override
   
    public Object getRootElement() {
        return myRoot;
    }

    public void cleanupCache() {
        if (myRoot != null) myRoot.cleanUpCache();
        myNodeMapping.clear();
        myTreeModel.invalidate();
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    private static void configureTree(SimpleTree tree) {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
    }

    public void select(SimpleNode node) {
        myTreeModel.select(node, myTree, path -> {});
    }

    public void expand(SimpleNode node) {
        myTreeModel.expand(node, myTree, path -> {});
    }

    @Nullable
    protected Class<?>[] getVisibleNodesClasses() {
        return null;
    }

    public void updateProjects(Collection<? extends DataNode<ProjectData>> toImport) {
        List<String> toImportPaths = ContainerUtil.map(toImport, pd -> pd.getData().getLinkedExternalProjectPath());
        Collection<String> orphanProjects = ContainerUtil.subtract(
            ContainerUtil.mapNotNull(myNodeMapping.entrySet(),
                entry -> entry.getValue() instanceof ProjectNode ? entry.getKey() : null),
            toImportPaths);

        for (DataNode<ProjectData> each : toImport) {
            ProjectData projectData = each.getData();
            String projectPath = projectData.getLinkedExternalProjectPath();

            ExternalSystemNode<?> projectNode = findNodeFor(projectPath);
            if (projectNode instanceof ProjectNode) {
                doMergeChildrenChanges(projectNode, new ProjectNode(myExternalProjectsView, each));
            }
            else {
                ExternalSystemNode<?> node = myNodeMapping.remove(projectPath);
                if (node != null) {
                    SimpleNode parent = node.getParent();
                    if (parent instanceof ExternalSystemNode) {
                        ((ExternalSystemNode<?>) parent).remove(node);
                    }
                }
                projectNode = new ProjectNode(myExternalProjectsView, each);
                myNodeMapping.put(projectPath, projectNode);
            }
            doUpdateProject((ProjectNode) projectNode);
        }

        for (String orphanPath : orphanProjects) {
            ExternalSystemNode<?> projectNode = myNodeMapping.remove(orphanPath);
            if (projectNode instanceof ProjectNode) {
                SimpleNode parent = projectNode.getParent();
                if (parent instanceof ExternalSystemNode) {
                    ((ExternalSystemNode<?>) parent).remove(projectNode);
                    updateUpTo(projectNode);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void doMergeChildrenChanges(ExternalSystemNode<?> currentNode,
                                        ExternalSystemNode<?> newNode) {
        ExternalSystemNode<?>[] cached = currentNode.getCached();
        if (cached == null) {
            ((ExternalSystemNode) currentNode).mergeWith(newNode);
            return;
        }

        List<Object> duplicates = new ArrayList<>();
        Map<Object, ExternalSystemNode<?>> oldDataMap = new LinkedHashMap<>();
        for (ExternalSystemNode<?> node : cached) {
            Object key = node.getData() != null ? node.getData() : node.getName();
            if (oldDataMap.put(key, node) != null) {
                duplicates.add(key);
            }
        }

        Map<Object, ExternalSystemNode<?>> newDataMap = new LinkedHashMap<>();
        Map<Object, ExternalSystemNode<?>> unchangedNewDataMap = new LinkedHashMap<>();
        for (ExternalSystemNode<?> node : newNode.getChildren()) {
            Object key = node.getData() != null ? node.getData() : node.getName();
            if (oldDataMap.remove(key) == null) {
                newDataMap.put(key, node);
            }
            else {
                unchangedNewDataMap.put(key, node);
            }
        }

        for (Object duplicate : duplicates) {
            newDataMap.remove(duplicate);
        }

        currentNode.removeAll(oldDataMap.values());

        for (ExternalSystemNode<?> node : currentNode.getChildren()) {
            Object key = node.getData() != null ? node.getData() : node.getName();
            ExternalSystemNode<?> unchangedNewNode = unchangedNewDataMap.get(key);
            if (unchangedNewNode != null) {
                doMergeChildrenChanges(node, unchangedNewNode);
            }
        }

        updateFrom(currentNode);
        ((ExternalSystemNode) currentNode).mergeWith(newNode);
        currentNode.addAll(newDataMap.values());
    }

    private void doUpdateProject(ProjectNode node) {
        ExternalSystemNode<?> newParentNode = myRoot;
        if (!node.isVisible()) {
            newParentNode.remove(node);
        }
        else {
            node.updateProject();
            reconnectNode(node, newParentNode);
        }
    }

    private static void reconnectNode(ProjectNode node, ExternalSystemNode<?> newParentNode) {
        ExternalSystemNode<?> oldParentNode = node.getGroup();
        if (oldParentNode == null || !oldParentNode.equals(newParentNode)) {
            if (oldParentNode != null) {
                oldParentNode.remove(node);
            }
            newParentNode.add(node);
        }
    }

    @Nullable
    private ExternalSystemNode<?> findNodeFor(String projectPath) {
        return myNodeMapping.get(projectPath);
    }

    public <T extends ExternalSystemNode<?>> void updateNodesAsync(Collection<Class<? extends T>> nodeClasses) {
        myAsyncTreeModel.accept(path -> {
            Object obj = path.getLastPathComponent();
            if (obj != null && anyAssignableFrom(obj.getClass(), nodeClasses)) {
                myTreeModel.invalidate(path, false);
            }
            return TreeVisitor.Action.CONTINUE;
        }, false);
    }

    private static <T> boolean anyAssignableFrom(Class<?> clazz,
                                                  Collection<Class<? extends T>> classes) {
        for (Class<? extends T> c : classes) {
            if (c.isAssignableFrom(clazz)) return true;
        }
        return false;
    }

    public <T extends ExternalSystemNode<?>> void visitExistingNodes(Class<T> nodeClass,
                                                                      java.util.function.Consumer<T> consumer) {
        for (T node : getExistingNodes(nodeClass)) {
            consumer.accept(node);
        }
    }

    @Override
    public void dispose() {
        myExternalProjectsView = null;
        myNodeMapping.clear();
        myRoot = null;
    }

   
    public <T extends ExternalSystemNode<?>> List<T> getNodes(Class<T> nodeClass) {
        return doGetNodes(nodeClass, myRoot != null ? myRoot.getChildren() : NO_CHILDREN, new ArrayList<>(), n -> true);
    }

   
    public <T extends ExternalSystemNode<?>> List<T> getExistingNodes(Class<T> nodeClass) {
        return doGetNodes(nodeClass, myRoot != null ? myRoot.getChildren() : NO_CHILDREN, new ArrayList<>(), node -> {
            if (node instanceof ExternalSystemNode<?> esNode) {
                ExternalSystemNode<?>[] cached = esNode.getCached();
                return cached != null && cached.length > 0;
            }
            return true;
        });
    }

    private static final ExternalSystemNode<?>[] NO_CHILDREN = new ExternalSystemNode[0];

   
    private static <T extends ExternalSystemNode<?>> List<T> doGetNodes(Class<T> nodeClass,
                                                                         ExternalSystemNode<?>[] nodes,
                                                                         List<T> result,
                                                                         Predicate<SimpleNode> shouldDive) {
        for (ExternalSystemNode<?> node : nodes) {
            if (nodeClass.isInstance(node)) {
                //noinspection unchecked
                result.add((T) node);
            }
            if (shouldDive.test(node)) {
                doGetNodes(nodeClass, node.getChildren(), result, shouldDive);
            }
        }
        return result;
    }


    public <T extends ExternalSystemNode<?>> List<T> getSelectedNodes(SimpleTree tree,
                                                                       Class<T> nodeClass) {
        List<T> result = new ArrayList<>();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (selectionPaths == null) return result;
        for (TreePath path : selectionPaths) {
            // path.getLastPathComponent() returns a StructureTreeModel.Node (DefaultMutableTreeNode wrapper).
            // Use tree.getNodeFor() which extracts the SimpleNode via DefaultMutableTreeNode.getUserObject().
            SimpleNode node = tree.getNodeFor(path);
            if (nodeClass.isInstance(node)) {
                //noinspection unchecked
                result.add((T) node);
            }
        }
        return result;
    }

    /**
     * Invisible root of the project tree.
     */
    public class RootNode<T> extends ExternalSystemNode<T> {
        public RootNode() {
            super(myExternalProjectsView, null, null);
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }

    public enum ErrorLevel {
        NONE, ERROR
    }

    enum DisplayKind {
        ALWAYS, NEVER, NORMAL
    }
}
