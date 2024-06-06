/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.MultiMap;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.versionControlSystem.util.VcsUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class TreeModelBuilder {
  @NonNls
  private static final String ROOT_NODE_VALUE = "root";

  private static final int UNVERSIONED_MAX_SIZE = 50;

  @Nonnull
  protected final Project myProject;
  protected final boolean myShowFlatten;
  @Nonnull
  protected final DefaultTreeModel myModel;
  @Nonnull
  protected final ChangesBrowserNode myRoot;
  @Nonnull
  private final Map<ChangesBrowserNode, ChangesGroupingPolicy> myGroupingPoliciesCache;
  @Nonnull
  private final Map<ChangesBrowserNode, Map<String, ChangesBrowserNode>> myFoldersCache;

  @SuppressWarnings("unchecked")
  private static final Comparator<ChangesBrowserNode> BROWSER_NODE_COMPARATOR = (node1, node2) -> {
    int sortWeightDiff = Comparing.compare(node1.getSortWeight(), node2.getSortWeight());
    if (sortWeightDiff != 0) return sortWeightDiff;

    if (node1 instanceof Comparable && node1.getClass().equals(node2.getClass())) {
      return ((Comparable)node1).compareTo(node2);
    }
    return node1.compareUserObjects(node2.getUserObject());
  };

  protected final static Comparator<Change> PATH_LENGTH_COMPARATOR = (o1, o2) -> {
    FilePath fp1 = ChangesUtil.getFilePath(o1);
    FilePath fp2 = ChangesUtil.getFilePath(o2);

    return Comparing.compare(fp1.getPath().length(), fp2.getPath().length());
  };


  public TreeModelBuilder(@Nonnull Project project, boolean showFlatten) {
    myProject = project;
    myShowFlatten = showFlatten;
    myRoot = ChangesBrowserNode.create(myProject, ROOT_NODE_VALUE);
    myModel = new DefaultTreeModel(myRoot);
    myGroupingPoliciesCache = FactoryMap.create(changesBrowserNode -> {
      ChangesGroupingPolicyFactory factory = ChangesGroupingPolicyFactory.getInstance(myProject);
      return factory != null ? factory.createGroupingPolicy(myModel) : null;
    });
    myFoldersCache = new HashMap<>();
  }


  @Nonnull
  public static DefaultTreeModel buildEmpty(@Nonnull Project project) {
    return new DefaultTreeModel(ChangesBrowserNode.create(project, ROOT_NODE_VALUE));
  }

  @Nonnull
  public static DefaultTreeModel buildFromChanges(@Nonnull Project project,
                                                  boolean showFlatten,
                                                  @Nonnull Collection<? extends Change> changes,
                                                  @jakarta.annotation.Nullable ChangeNodeDecorator changeNodeDecorator) {
    return new TreeModelBuilder(project, showFlatten).setChanges(changes, changeNodeDecorator).build();
  }

  @Nonnull
  public static DefaultTreeModel buildFromFilePaths(@Nonnull Project project, boolean showFlatten, @Nonnull Collection<FilePath> filePaths) {
    return new TreeModelBuilder(project, showFlatten).setFilePaths(filePaths).build();
  }

  @Nonnull
  public static DefaultTreeModel buildFromChangeLists(@Nonnull Project project, boolean showFlatten, @Nonnull Collection<? extends ChangeList> changeLists) {
    return new TreeModelBuilder(project, showFlatten).setChangeLists(changeLists).build();
  }

  @Nonnull
  public static DefaultTreeModel buildFromVirtualFiles(@Nonnull Project project, boolean showFlatten, @Nonnull Collection<VirtualFile> virtualFiles) {
    return new TreeModelBuilder(project, showFlatten).setVirtualFiles(virtualFiles, null).build();
  }


  @Nonnull
  public TreeModelBuilder setChanges(@Nonnull Collection<? extends Change> changes, @jakarta.annotation.Nullable ChangeNodeDecorator changeNodeDecorator) {
    List<? extends Change> sortedChanges = ContainerUtil.sorted(changes, PATH_LENGTH_COMPARATOR);
    for (Change change : sortedChanges) {
      insertChangeNode(change, myRoot, createChangeNode(change, changeNodeDecorator));
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setUnversioned(@jakarta.annotation.Nullable List<VirtualFile> unversionedFiles) {
    if (ContainerUtil.isEmpty(unversionedFiles)) return this;
    int dirsCount = ContainerUtil.count(unversionedFiles, it -> it.isDirectory());
    int filesCount = unversionedFiles.size() - dirsCount;
    boolean manyFiles = unversionedFiles.size() > UNVERSIONED_MAX_SIZE;
    ChangesBrowserUnversionedFilesNode node = new ChangesBrowserUnversionedFilesNode(myProject, filesCount, dirsCount, manyFiles);
    return insertSpecificNodeToModel(unversionedFiles, node);
  }

  @Nonnull
  public TreeModelBuilder setIgnored(@jakarta.annotation.Nullable List<VirtualFile> ignoredFiles, boolean updatingMode) {
    if (ContainerUtil.isEmpty(ignoredFiles)) return this;
    int dirsCount = ContainerUtil.count(ignoredFiles, it -> it.isDirectory());
    int filesCount = ignoredFiles.size() - dirsCount;
    boolean manyFiles = ignoredFiles.size() > UNVERSIONED_MAX_SIZE;
    ChangesBrowserIgnoredFilesNode node = new ChangesBrowserIgnoredFilesNode(myProject, filesCount, dirsCount, manyFiles, updatingMode);
    return insertSpecificNodeToModel(ignoredFiles, node);
  }

  @Nonnull
  private TreeModelBuilder insertSpecificNodeToModel(@Nonnull List<VirtualFile> specificFiles, @Nonnull ChangesBrowserSpecificFilesNode node) {
    myModel.insertNodeInto(node, myRoot, myRoot.getChildCount());
    if (!node.isManyFiles()) {
      insertFilesIntoNode(specificFiles, node);
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setChangeLists(@Nonnull Collection<? extends ChangeList> changeLists) {
    final RemoteRevisionsCache revisionsCache = RemoteRevisionsCache.getInstance(myProject);
    for (ChangeList list : changeLists) {
      List<Change> changes = ContainerUtil.sorted(list.getChanges(), PATH_LENGTH_COMPARATOR);
      ChangeListRemoteState listRemoteState = new ChangeListRemoteState(changes.size());
      ChangesBrowserChangeListNode listNode = new ChangesBrowserChangeListNode(myProject, list, listRemoteState);
      myModel.insertNodeInto(listNode, myRoot, 0);

      for (int i = 0; i < changes.size(); i++) {
        Change change = changes.get(i);
        RemoteStatusChangeNodeDecorator decorator = new RemoteStatusChangeNodeDecorator(revisionsCache, listRemoteState, i);
        insertChangeNode(change, listNode, createChangeNode(change, decorator));
      }
    }
    return this;
  }

  protected ChangesBrowserNode createChangeNode(Change change, ChangeNodeDecorator decorator) {
    return new ChangesBrowserChangeNode(myProject, change, decorator);
  }

  @Nonnull
  public TreeModelBuilder setLockedFolders(@jakarta.annotation.Nullable List<VirtualFile> lockedFolders) {
    return setVirtualFiles(lockedFolders, ChangesBrowserNode.LOCKED_FOLDERS_TAG);
  }

  @Nonnull
  public TreeModelBuilder setModifiedWithoutEditing(@Nonnull List<VirtualFile> modifiedWithoutEditing) {
    return setVirtualFiles(modifiedWithoutEditing, ChangesBrowserNode.MODIFIED_WITHOUT_EDITING_TAG);
  }

  @Nonnull
  private TreeModelBuilder setVirtualFiles(@jakarta.annotation.Nullable Collection<VirtualFile> files, @jakarta.annotation.Nullable Object tag) {
    if (ContainerUtil.isEmpty(files)) return this;
    insertFilesIntoNode(files, createTagNode(tag));
    return this;
  }

  @Nonnull
  private ChangesBrowserNode createTagNode(@jakarta.annotation.Nullable Object tag) {
    if (tag == null) return myRoot;

    ChangesBrowserNode subtreeRoot = ChangesBrowserNode.create(myProject, tag);
    myModel.insertNodeInto(subtreeRoot, myRoot, myRoot.getChildCount());
    return subtreeRoot;
  }

  private void insertFilesIntoNode(@Nonnull Collection<VirtualFile> files, @Nonnull ChangesBrowserNode subtreeRoot) {
    List<VirtualFile> sortedFiles = ContainerUtil.sorted(files, VirtualFileHierarchicalComparator.getInstance());
    for (VirtualFile file : sortedFiles) {
      insertChangeNode(file, subtreeRoot, ChangesBrowserNode.create(myProject, file));
    }
  }

  @Nonnull
  public TreeModelBuilder setLocallyDeletedPaths(@jakarta.annotation.Nullable Collection<LocallyDeletedChange> locallyDeletedChanges) {
    if (ContainerUtil.isEmpty(locallyDeletedChanges)) return this;
    ChangesBrowserNode subtreeRoot = createTagNode(ChangesBrowserNode.LOCALLY_DELETED_NODE_TAG);

    for (LocallyDeletedChange change : locallyDeletedChanges) {
      // whether a folder does not matter
      final StaticFilePath key = new StaticFilePath(false, change.getPresentableUrl(), change.getPath().getVirtualFile());
      ChangesBrowserNode oldNode = getFolderCache(subtreeRoot).get(key.getKey());
      if (oldNode == null) {
        ChangesBrowserNode node = ChangesBrowserNode.create(change);
        ChangesBrowserNode parent = getParentNodeFor(key, subtreeRoot);
        myModel.insertNodeInto(node, parent, parent.getChildCount());
        getFolderCache(subtreeRoot).put(key.getKey(), node);
      }
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setFilePaths(@Nonnull Collection<FilePath> filePaths) {
    return setFilePaths(filePaths, myRoot);
  }

  @Nonnull
  private TreeModelBuilder setFilePaths(@Nonnull Collection<FilePath> filePaths, @Nonnull ChangesBrowserNode subtreeRoot) {
    for (FilePath file : filePaths) {
      assert file != null;
      // whether a folder does not matter
      final String path = file.getPath();
      final StaticFilePath pathKey = !FileUtil.isAbsolute(path) || VcsUtil.isPathRemote(path)
                                     ? new StaticFilePath(false, path, null)
                                     : new StaticFilePath(false, new File(file.getIOFile().getPath().replace('\\', '/')).getAbsolutePath(), file.getVirtualFile());
      ChangesBrowserNode oldNode = getFolderCache(subtreeRoot).get(pathKey.getKey());
      if (oldNode == null) {
        final ChangesBrowserNode node = ChangesBrowserNode.create(myProject, file);
        final ChangesBrowserNode parentNode = getParentNodeFor(pathKey, subtreeRoot);
        myModel.insertNodeInto(node, parentNode, 0);
        // we could also ask whether a file or directory, though for deleted files not a good idea
        getFolderCache(subtreeRoot).put(pathKey.getKey(), node);
      }
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setSwitchedRoots(@jakarta.annotation.Nullable Map<VirtualFile, String> switchedRoots) {
    if (ContainerUtil.isEmpty(switchedRoots)) return this;
    final ChangesBrowserNode rootsHeadNode = createTagNode(ChangesBrowserNode.SWITCHED_ROOTS_TAG);
    rootsHeadNode.setAttributes(SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);

    List<VirtualFile> files = ContainerUtil.sorted(switchedRoots.keySet(), VirtualFileHierarchicalComparator.getInstance());

    for (VirtualFile vf : files) {
      final ContentRevision cr = new CurrentContentRevision(VcsUtil.getFilePath(vf));
      final Change change = new Change(cr, cr, FileStatus.NOT_CHANGED);
      final String branchName = switchedRoots.get(vf);
      insertChangeNode(vf, rootsHeadNode, createChangeNode(change, new ChangeNodeDecorator() {
        @Override
        public void decorate(Change change1, SimpleColoredComponent component, boolean isShowFlatten) {
        }

        @Override
        public void preDecorate(Change change1, ChangesBrowserNodeRenderer renderer, boolean showFlatten) {
          renderer.append("[" + branchName + "] ", SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        }
      }));
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setSwitchedFiles(@Nonnull MultiMap<String, VirtualFile> switchedFiles) {
    if (switchedFiles.isEmpty()) return this;
    ChangesBrowserNode subtreeRoot = createTagNode(ChangesBrowserNode.SWITCHED_FILES_TAG);
    for (String branchName : switchedFiles.keySet()) {
      List<VirtualFile> switchedFileList = ContainerUtil.sorted(switchedFiles.get(branchName), VirtualFileHierarchicalComparator.getInstance());
      if (switchedFileList.size() > 0) {
        ChangesBrowserNode branchNode = ChangesBrowserNode.create(myProject, branchName);
        myModel.insertNodeInto(branchNode, subtreeRoot, subtreeRoot.getChildCount());

        for (VirtualFile file : switchedFileList) {
          insertChangeNode(file, branchNode, ChangesBrowserNode.create(myProject, file));
        }
      }
    }
    return this;
  }

  @Nonnull
  public TreeModelBuilder setLogicallyLockedFiles(@jakarta.annotation.Nullable Map<VirtualFile, LogicalLock> logicallyLockedFiles) {
    if (ContainerUtil.isEmpty(logicallyLockedFiles)) return this;
    final ChangesBrowserNode subtreeRoot = createTagNode(ChangesBrowserNode.LOGICALLY_LOCKED_TAG);

    List<VirtualFile> keys = ContainerUtil.sorted(logicallyLockedFiles.keySet(), VirtualFileHierarchicalComparator.getInstance());

    for (VirtualFile file : keys) {
      final LogicalLock lock = logicallyLockedFiles.get(file);
      final ChangesBrowserLogicallyLockedFile obj = new ChangesBrowserLogicallyLockedFile(myProject, file, lock);
      insertChangeNode(obj, subtreeRoot, ChangesBrowserNode.create(myProject, obj));
    }
    return this;
  }

  protected void insertChangeNode(@Nonnull Object change, @Nonnull ChangesBrowserNode subtreeRoot, @Nonnull ChangesBrowserNode node) {
    insertChangeNode(change, subtreeRoot, node, this::createPathNode);
  }

  protected void insertChangeNode(@Nonnull Object change,
                                  @Nonnull ChangesBrowserNode subtreeRoot,
                                  @Nonnull ChangesBrowserNode node,
                                  @Nonnull Convertor<StaticFilePath, ChangesBrowserNode> nodeBuilder) {
    final StaticFilePath pathKey = getKey(change);
    ChangesBrowserNode parentNode = getParentNodeFor(pathKey, subtreeRoot, nodeBuilder);
    myModel.insertNodeInto(node, parentNode, myModel.getChildCount(parentNode));

    if (pathKey.isDirectory()) {
      getFolderCache(subtreeRoot).put(pathKey.getKey(), node);
    }
  }

  @Nonnull
  public DefaultTreeModel build() {
    collapseDirectories(myModel, myRoot);
    sortNodes();
    return myModel;
  }

  private void sortNodes() {
    TreeUtil.sort(myModel, BROWSER_NODE_COMPARATOR);

    myModel.nodeStructureChanged((TreeNode)myModel.getRoot());
  }

  private static void collapseDirectories(@Nonnull DefaultTreeModel model, @Nonnull ChangesBrowserNode node) {
    if (node.getUserObject() instanceof FilePath && node.getChildCount() == 1) {
      final ChangesBrowserNode child = (ChangesBrowserNode)node.getChildAt(0);
      if (child.getUserObject() instanceof FilePath && !child.isLeaf()) {
        ChangesBrowserNode parent = (ChangesBrowserNode)node.getParent();
        final int idx = parent.getIndex(node);
        model.removeNodeFromParent(node);
        model.removeNodeFromParent(child);
        model.insertNodeInto(child, parent, idx);
        collapseDirectories(model, parent);
      }
    }
    else {
      final Enumeration children = node.children();
      while (children.hasMoreElements()) {
        ChangesBrowserNode child = (ChangesBrowserNode)children.nextElement();
        collapseDirectories(model, child);
      }
    }
  }

  @Nonnull
  private static StaticFilePath getKey(@Nonnull Object o) {
    if (o instanceof Change) {
      return staticFrom(ChangesUtil.getFilePath((Change)o));
    }
    else if (o instanceof VirtualFile) {
      return staticFrom((VirtualFile)o);
    }
    else if (o instanceof FilePath) {
      return staticFrom((FilePath)o);
    }
    else if (o instanceof ChangesBrowserLogicallyLockedFile) {
      return staticFrom(((ChangesBrowserLogicallyLockedFile)o).getUserObject());
    }
    else if (o instanceof LocallyDeletedChange) {
      return staticFrom(((LocallyDeletedChange)o).getPath());
    }

    throw new IllegalArgumentException("Unknown type - " + o.getClass());
  }

  @Nonnull
  private static StaticFilePath staticFrom(@Nonnull FilePath fp) {
    final String path = fp.getPath();
    if (fp.isNonLocal() && (!FileUtil.isAbsolute(path) || VcsUtil.isPathRemote(path))) {
      return new StaticFilePath(fp.isDirectory(), fp.getIOFile().getPath().replace('\\', '/'), fp.getVirtualFile());
    }
    return new StaticFilePath(fp.isDirectory(), new File(fp.getIOFile().getPath().replace('\\', '/')).getAbsolutePath(), fp.getVirtualFile());
  }

  @Nonnull
  private static StaticFilePath staticFrom(@Nonnull VirtualFile vf) {
    return new StaticFilePath(vf.isDirectory(), vf.getPath(), vf);
  }

  @Nonnull
  public static FilePath getPathForObject(@Nonnull Object o) {
    if (o instanceof Change) {
      return ChangesUtil.getFilePath((Change)o);
    }
    else if (o instanceof VirtualFile) {
      return VcsUtil.getFilePath((VirtualFile)o);
    }
    else if (o instanceof FilePath) {
      return (FilePath)o;
    }
    else if (o instanceof ChangesBrowserLogicallyLockedFile) {
      return VcsUtil.getFilePath(((ChangesBrowserLogicallyLockedFile)o).getUserObject());
    }
    else if (o instanceof LocallyDeletedChange) {
      return ((LocallyDeletedChange)o).getPath();
    }

    throw new IllegalArgumentException("Unknown type - " + o.getClass());
  }

  @Nonnull
  protected ChangesBrowserNode getParentNodeFor(@Nonnull StaticFilePath nodePath, @Nonnull ChangesBrowserNode subtreeRoot) {
    return getParentNodeFor(nodePath, subtreeRoot, this::createPathNode);
  }

  @Nonnull
  protected ChangesBrowserNode getParentNodeFor(@Nonnull StaticFilePath nodePath, @Nonnull ChangesBrowserNode subtreeRoot, @Nonnull Convertor<StaticFilePath, ChangesBrowserNode> nodeBuilder) {
    if (myShowFlatten) {
      return subtreeRoot;
    }

    ChangesGroupingPolicy policy = myGroupingPoliciesCache.get(subtreeRoot);
    if (policy != null) {
      ChangesBrowserNode nodeFromPolicy = policy.getParentNodeFor(nodePath, subtreeRoot);
      if (nodeFromPolicy != null) {
        return nodeFromPolicy;
      }
    }

    StaticFilePath parentPath = nodePath.getParent();
    while (parentPath != null) {
      ChangesBrowserNode oldParentNode = getFolderCache(subtreeRoot).get(parentPath.getKey());
      if (oldParentNode != null) return oldParentNode;

      ChangesBrowserNode parentNode = nodeBuilder.convert(parentPath);
      if (parentNode != null) {
        ChangesBrowserNode grandPa = getParentNodeFor(parentPath, subtreeRoot, nodeBuilder);
        myModel.insertNodeInto(parentNode, grandPa, grandPa.getChildCount());
        getFolderCache(subtreeRoot).put(parentPath.getKey(), parentNode);
        return parentNode;
      }

      parentPath = parentPath.getParent();
    }

    return subtreeRoot;
  }

  @jakarta.annotation.Nullable
  private ChangesBrowserNode createPathNode(@Nonnull StaticFilePath path) {
    FilePath filePath = path.getVf() == null ? VcsUtil.getFilePath(path.getPath(), true) : VcsUtil.getFilePath(path.getVf());
    return ChangesBrowserNode.create(myProject, filePath);
  }

  @Nonnull
  private Map<String, ChangesBrowserNode> getFolderCache(@Nonnull ChangesBrowserNode subtreeRoot) {
    return myFoldersCache.computeIfAbsent(subtreeRoot, (key) -> new HashMap<>());
  }

  public boolean isEmpty() {
    return myModel.getChildCount(myRoot) == 0;
  }

  @Nonnull
  @Deprecated
  public DefaultTreeModel buildModel(@Nonnull List<Change> changes, @jakarta.annotation.Nullable ChangeNodeDecorator changeNodeDecorator) {
    return setChanges(changes, changeNodeDecorator).build();
  }
}
