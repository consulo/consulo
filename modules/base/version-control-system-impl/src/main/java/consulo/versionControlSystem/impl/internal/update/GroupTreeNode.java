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
package consulo.versionControlSystem.impl.internal.update;

import consulo.application.AllIcons;
import consulo.project.Project;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSetBase;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.*;

/**
 * @author lesya
 */
public class GroupTreeNode extends AbstractTreeNode implements Disposable {
  private final String myName;
  private final boolean mySupportsDeletion;
  private final List<String> myFilePaths = new ArrayList<String>();
  private final Map<String, String> myErrorsMap;
  private final SimpleTextAttributes myInvalidAttributes;
  private final Project myProject;
  private final String myFileGroupId;

  public GroupTreeNode(@Nonnull String name,
                       boolean supportsDeletion,
                       @Nonnull SimpleTextAttributes invalidAttributes,
                       @Nonnull Project project,
                       @Nonnull Map<String, String> errorsMap, String id) {
    myName = name;
    mySupportsDeletion = supportsDeletion;
    myInvalidAttributes = invalidAttributes;
    myProject = project;
    myErrorsMap = errorsMap;
    myFileGroupId = id;
  }

  public String getFileGroupId() {
    return myFileGroupId;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Folder;
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getVirtualFiles() {
    ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
    for (int i = 0; i < getChildCount(); i++) {
      result.addAll(((AbstractTreeNode)getChildAt(i)).getVirtualFiles());
    }
    return result;
  }

  @Nonnull
  @Override
  public Collection<File> getFiles() {
    ArrayList<File> result = new ArrayList<File>();
    for (int i = 0; i < getChildCount(); i++) {
      result.addAll(((AbstractTreeNode)getChildAt(i)).getFiles());
    }
    return result;
  }

  @Override
  protected int getItemsCount() {
    int result = 0;
    Enumeration children = children();
    while (children.hasMoreElements()) {
      AbstractTreeNode treeNode = (AbstractTreeNode)children.nextElement();
      result += treeNode.getItemsCount();
    }
    return result;
  }

  @Override
  protected boolean showStatistics() {
    return true;
  }

  @Nonnull
  @Override
  public SimpleTextAttributes getAttributes() {
    return myFilterAttributes == null ? SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES : myFilterAttributes;
  }

  @Override
  public boolean getSupportsDeletion() {
    return mySupportsDeletion;
  }

  public void addFilePath(@Nonnull String filePath) {
    myFilePaths.add(filePath);
  }

  public void rebuild(boolean groupByPackages, @jakarta.annotation.Nullable Pair<PackageSetBase, NamedScopesHolder> filter, boolean showOnlyFilteredItems) {
    myFilterAttributes = null;
    if (containsGroups()) {
      rebuildGroups(groupByPackages, filter, showOnlyFilteredItems);
    }
    else {
      rebuildFiles(groupByPackages, filter, showOnlyFilteredItems);
    }
  }

  private void rebuildGroups(boolean groupByPackages,
                             @jakarta.annotation.Nullable Pair<PackageSetBase, NamedScopesHolder> filter,
                             boolean showOnlyFilteredItems) {
    boolean apply = false;
    for (int i = 0; i < getChildCount(); i++) {
      GroupTreeNode childGroup = (GroupTreeNode)getChildAt(i);
      childGroup.rebuild(groupByPackages, filter, showOnlyFilteredItems);
      apply |= childGroup.myFilterAttributes != null;
    }
    applyFilter(apply);
  }

  private void rebuildFiles(boolean groupByPackages,
                            @jakarta.annotation.Nullable Pair<PackageSetBase, NamedScopesHolder> filter,
                            boolean showOnlyFilteredItems) {
    for (int i = getChildCount() - 1; i >= 0; i--) {
      TreeNode node = getChildAt(i);
      if (node instanceof Disposable) {
        Disposer.dispose((Disposable)node);
      }
    }
    removeAllChildren();

    if (groupByPackages) {
      buildPackages();
      acceptFilter(filter, showOnlyFilteredItems);
    }
    else {
      buildFiles(filter, showOnlyFilteredItems);
    }

    setTreeModel(myTreeModel);

    if (myTreeModel != null) {
      myTreeModel.nodeStructureChanged(this);
    }
  }

  private void buildPackages() {
    Collection<File> files = new LinkedHashSet<File>();
    for (String myFilePath : myFilePaths) {
      files.add(new File(myFilePath));
    }
    GroupByPackages groupByPackages = new GroupByPackages(files);

    List<File> roots = groupByPackages.getRoots();
    addFiles(this, roots, files, groupByPackages, null);

  }

  private void addFiles(@Nonnull AbstractTreeNode parentNode,
                        @Nonnull List<File> roots,
                        @Nonnull final Collection<File> files,
                        @Nonnull GroupByPackages groupByPackages,
                        String parentPath) {
    Collections.sort(roots, new Comparator<File>() {
      @Override
      public int compare(File file1, File file2) {
        boolean containsFile1 = files.contains(file1);
        boolean containsFile2 = files.contains(file2);
        if (containsFile1 == containsFile2) {
          return file1.getAbsolutePath().compareToIgnoreCase(file2.getAbsolutePath());
        }
        return containsFile1 ? 1 : -1;
      }
    });

    for (File root : roots) {
      FileOrDirectoryTreeNode child = files.contains(root)
                                      ? new FileTreeNode(root.getAbsolutePath(), myInvalidAttributes, myProject, parentPath)
                                      : new DirectoryTreeNode(root.getAbsolutePath(), myProject, parentPath);
      Disposer.register((Disposable)parentNode, child);
      parentNode.add(child);
      addFiles(child, groupByPackages.getChildren(root), files, groupByPackages, child.getFilePath());
    }
  }

  private void buildFiles(@jakarta.annotation.Nullable Pair<PackageSetBase, NamedScopesHolder> filter, boolean showOnlyFilteredItems) {
    Collections.sort(myFilePaths, new Comparator<String>() {
      @Override
      public int compare(String path1, String path2) {
        return path1.compareToIgnoreCase(path2);
      }
    });

    boolean apply = false;

    for (String filePath : myFilePaths) {
      FileTreeNode child = new FileTreeNode(filePath, myInvalidAttributes, myProject, null);
      if (filter != null) {
        if (child.acceptFilter(filter, showOnlyFilteredItems)) {
          apply = true;
        }
        else if (showOnlyFilteredItems) {
          continue;
        }
      }
      String error = myErrorsMap.get(filePath);
      if (error != null) {
        child.setErrorText(error);
      }
      add(child);
      Disposer.register(this, child);
    }

    applyFilter(apply);
  }

  private boolean containsGroups() {
    return myFilePaths.isEmpty();
  }

  @Override
  public void dispose() {
  }
}
