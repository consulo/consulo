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
package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocallyDeletedChange;
import consulo.versionControlSystem.impl.internal.change.ChangeListOwner;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static consulo.ui.ex.awt.FontUtil.spaceAndThinSpace;

public class ChangesBrowserNode<T> extends DefaultMutableTreeNode {
  public static final Object IGNORED_FILES_TAG = new Tag(VcsLocalize.changesNodeTitleIgnoredFiles());
  public static final Object LOCKED_FOLDERS_TAG = new Tag(VcsLocalize.changesNodeTitleLockedFolders());
  public static final Object LOGICALLY_LOCKED_TAG = new Tag(VcsLocalize.changesNodeTitleLogicallyLockedFolders());
  public static final Object UNVERSIONED_FILES_TAG = new Tag(VcsLocalize.changesNodeTitleUnversionedFiles());
  public static final Object MODIFIED_WITHOUT_EDITING_TAG = new Tag(VcsLocalize.changesNodeTitleModifiedWithoutEditing());
  public static final Object SWITCHED_FILES_TAG = new Tag(VcsLocalize.changesNodeTitleSwitchedFiles());
  public static final Object SWITCHED_ROOTS_TAG = new Tag(VcsLocalize.changesNodeTitleSwitchedRoots());
  public static final Object LOCALLY_DELETED_NODE_TAG = new Tag(VcsLocalize.changesNodeTitleLocallyDeletedFiles());

  protected static final int DEFAULT_CHANGE_LIST_SORT_WEIGHT = 1;
  protected static final int CHANGE_LIST_SORT_WEIGHT = 2;
  protected static final int MODULE_SORT_WEIGHT = 3;
  protected static final int DIRECTORY_PATH_SORT_WEIGHT = 4;
  protected static final int FILE_PATH_SORT_WEIGHT = 5;
  protected static final int CHANGE_SORT_WEIGHT = 6;
  protected static final int VIRTUAL_FILE_SORT_WEIGHT = 7;
  protected static final int UNVERSIONED_SORT_WEIGHT = 8;
  protected static final int DEFAULT_SORT_WEIGHT = 9;
  protected static final int IGNORED_SORT_WEIGHT = 10;

  public static final Function<TreePath, String> TO_TEXT_CONVERTER =
    path -> ((ChangesBrowserNode)path.getLastPathComponent()).getTextPresentation();

  private SimpleTextAttributes myAttributes;

  private int myFileCount = -1;
  private int myDirectoryCount = -1;

  protected ChangesBrowserNode(Object userObject) {
    super(userObject);
    myAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

  public static ChangesBrowserNode create(LocallyDeletedChange change) {
    return new ChangesBrowserLocallyDeletedNode(change);
  }

  public static ChangesBrowserNode create(Project project, Object userObject) {
    if (userObject instanceof Change change) {
      return new ChangesBrowserChangeNode(project, change, null);
    }
    if (userObject instanceof VirtualFile virtualFile) {
      return new ChangesBrowserFileNode(project, virtualFile);
    }
    if (userObject instanceof FilePath filePath) {
      return new ChangesBrowserFilePathNode(filePath);
    }
    if (userObject == LOCKED_FOLDERS_TAG) {
      return new ChangesBrowserLockedFoldersNode(project, userObject);
    }
    if (userObject instanceof ChangesBrowserLogicallyLockedFileImpl changesBrowserNode) {
      return changesBrowserNode;
    }
    return new ChangesBrowserNode(userObject);
  }

  @Override
  public void insert(MutableTreeNode newChild, int childIndex) {
    super.insert(newChild, childIndex);
    resetFileCounters();
  }

  @Override
  public void remove(int childIndex) {
    super.remove(childIndex);
    resetFileCounters();
  }

  protected boolean isFile() {
    return false;
  }

  protected boolean isDirectory() {
    return false;
  }

  public int getFileCount() {
    if (myFileCount == -1) {
      myFileCount = (isFile() ? 1 : 0) + toStream(children()).mapToInt(ChangesBrowserNode::getFileCount).sum();
    }
    return myFileCount;
  }

  public int getDirectoryCount() {
    if (myDirectoryCount == -1) {
      myDirectoryCount = (isDirectory() ? 1 : 0) + toStream(children()).mapToInt(ChangesBrowserNode::getDirectoryCount).sum();
    }
    return myDirectoryCount;
  }

  private void resetFileCounters() {
    myFileCount = -1;
    myDirectoryCount = -1;
  }

  public List<Change> getAllChangesUnder() {
    return getAllObjectsUnder(Change.class);
  }

  public <U> List<U> getAllObjectsUnder(Class<U> clazz) {
    return getObjectsUnderStream(clazz).collect(Collectors.toList());
  }

  public <U> Stream<U> getObjectsUnderStream(Class<U> clazz) {
    return toStream(preorderEnumeration())
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> clazz.isAssignableFrom(userObject.getClass()))
      .map(clazz::cast);
  }

  
  public List<VirtualFile> getAllFilesUnder() {
    return getFilesUnderStream().collect(Collectors.toList());
  }

  public Stream<VirtualFile> getFilesUnderStream() {
    return toStream(breadthFirstEnumeration())
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> userObject instanceof VirtualFile)
      .map(VirtualFile.class::cast)
      .filter(VirtualFile::isValid);
  }

  public List<FilePath> getAllFilePathsUnder() {
    return getFilePathsUnderStream().collect(Collectors.toList());
  }

  public Stream<FilePath> getFilePathsUnderStream() {
    return toStream(breadthFirstEnumeration())
      .filter(ChangesBrowserNode::isLeaf)
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> userObject instanceof FilePath)
      .map(FilePath.class::cast);
  }

  
  private static Stream<ChangesBrowserNode> toStream(Enumeration enumeration) {
    //noinspection unchecked
    Iterator<ChangesBrowserNode> iterator = ((Enumeration<ChangesBrowserNode>)enumeration).asIterator();
    Spliterator<ChangesBrowserNode> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL);

    return StreamSupport.stream(spliterator, false);
  }

  public void render(ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
    renderer.append(userObject.toString(), myAttributes);
    appendCount(renderer);
  }

  protected String getCountText() {
    int count = getFileCount();
    int dirCount = getDirectoryCount();
    String result = "";

    if (dirCount != 0 || count != 0) {
      result = spaceAndThinSpace() +
        (dirCount == 0
          ? VcsLocalize.changesNodeTitleChangeCount(count).get()
          : count == 0
          ? VcsLocalize.changesNodeTitleDirectoryChangeCount(dirCount).get()
          : VcsLocalize.changesNodeTitleDirectoryFileChangeCount(dirCount, count).get());
    }

    return result;
  }

  protected void appendCount(ColoredTreeCellRenderer renderer) {
    renderer.append(getCountText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Override
  public String toString() {
    return getTextPresentation();
  }

  public String getTextPresentation() {
    return userObject == null ? "" : userObject.toString();
  }

  @Override
  public T getUserObject() {
    //noinspection unchecked
    return (T)userObject;
  }

  public boolean canAcceptDrop(ChangeListDragBean dragBean) {
    return false;
  }

  public void acceptDrop(ChangeListOwner dragOwner, ChangeListDragBean dragBean) {
  }

  /**
   * Nodes with the same sort weight should share {@link #compareUserObjects} implementation
   */
  public int getSortWeight() {
    return DEFAULT_SORT_WEIGHT;
  }

  public int compareUserObjects(Object o2) {
    return 0;
  }

  public void setAttributes(SimpleTextAttributes attributes) {
    myAttributes = attributes;
  }

  protected void appendUpdatingState(ChangesBrowserNodeRenderer renderer) {
    renderer.append(
      LocalizeValue.join(
        LocalizeValue.of(getCountText().isEmpty() ? spaceAndThinSpace() : ", "),
        VcsLocalize.changesNodeTitleUpdating()
      ),
      SimpleTextAttributes.GRAYED_ATTRIBUTES
    );
  }

  @Deprecated
  public final int getCount() {
    return getFileCount();
  }

  private static class Tag {
    private final LocalizeValue myText;

    public Tag(LocalizeValue text) {
      myText = text;
    }

    @Override
    public String toString() {
      return myText.get();
    }
  }
}
