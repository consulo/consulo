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

import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.LocallyDeletedChange;
import consulo.versionControlSystem.impl.internal.change.ChangeListOwner;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

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
  public static final Object IGNORED_FILES_TAG = new Tag("changes.nodetitle.ignored.files");
  public static final Object LOCKED_FOLDERS_TAG = new Tag("changes.nodetitle.locked.folders");
  public static final Object LOGICALLY_LOCKED_TAG = new Tag("changes.nodetitle.logicallt.locked.folders");
  public static final Object UNVERSIONED_FILES_TAG = new Tag("changes.nodetitle.unversioned.files");
  public static final Object MODIFIED_WITHOUT_EDITING_TAG = new Tag("changes.nodetitle.modified.without.editing");
  public static final Object SWITCHED_FILES_TAG = new Tag("changes.nodetitle.switched.files");
  public static final Object SWITCHED_ROOTS_TAG = new Tag("changes.nodetitle.switched.roots");
  public static final Object LOCALLY_DELETED_NODE_TAG = new Tag("changes.nodetitle.locally.deleted.files");

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

  @Nonnull
  public static ChangesBrowserNode create(@Nonnull LocallyDeletedChange change) {
    return new ChangesBrowserLocallyDeletedNode(change);
  }

  @Nonnull
  public static ChangesBrowserNode create(@Nonnull Project project, @Nonnull Object userObject) {
    if (userObject instanceof Change) {
      return new ChangesBrowserChangeNode(project, (Change)userObject, null);
    }
    if (userObject instanceof VirtualFile) {
      return new ChangesBrowserFileNode(project, (VirtualFile)userObject);
    }
    if (userObject instanceof FilePath) {
      return new ChangesBrowserFilePathNode((FilePath)userObject);
    }
    if (userObject == LOCKED_FOLDERS_TAG) {
      return new ChangesBrowserLockedFoldersNode(project, userObject);
    }
    if (userObject instanceof ChangesBrowserLogicallyLockedFile) {
      return (ChangesBrowserNode)userObject;
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

  @Nonnull
  public List<Change> getAllChangesUnder() {
    return getAllObjectsUnder(Change.class);
  }

  @Nonnull
  public <U> List<U> getAllObjectsUnder(@Nonnull Class<U> clazz) {
    return getObjectsUnderStream(clazz).collect(Collectors.toList());
  }

  @Nonnull
  public <U> Stream<U> getObjectsUnderStream(@Nonnull Class<U> clazz) {
    return toStream(preorderEnumeration())
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> clazz.isAssignableFrom(userObject.getClass()))
      .map(clazz::cast);
  }

  @Nonnull
  public List<VirtualFile> getAllFilesUnder() {
    return getFilesUnderStream().collect(Collectors.toList());
  }

  @Nonnull
  public Stream<VirtualFile> getFilesUnderStream() {
    return toStream(breadthFirstEnumeration())
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> userObject instanceof VirtualFile)
      .map(VirtualFile.class::cast)
      .filter(VirtualFile::isValid);
  }

  @Nonnull
  public List<FilePath> getAllFilePathsUnder() {
    return getFilePathsUnderStream().collect(Collectors.toList());
  }

  @Nonnull
  public Stream<FilePath> getFilePathsUnderStream() {
    return toStream(breadthFirstEnumeration())
      .filter(ChangesBrowserNode::isLeaf)
      .map(ChangesBrowserNode::getUserObject)
      .filter(userObject -> userObject instanceof FilePath)
      .map(FilePath.class::cast);
  }

  @Nonnull
  private static Stream<ChangesBrowserNode> toStream(@Nonnull Enumeration enumeration) {
    //noinspection unchecked
    Iterator<ChangesBrowserNode> iterator = ((Enumeration<ChangesBrowserNode>)enumeration).asIterator();
    Spliterator<ChangesBrowserNode> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL);

    return StreamSupport.stream(spliterator, false);
  }

  public void render(@Nonnull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
    renderer.append(userObject.toString(), myAttributes);
    appendCount(renderer);
  }

  @Nonnull
  protected String getCountText() {
    int count = getFileCount();
    int dirCount = getDirectoryCount();
    String result = "";

    if (dirCount != 0 || count != 0) {
      result = spaceAndThinSpace() +
        (dirCount == 0
          ? VcsBundle.message("changes.nodetitle.changecount", count)
          : count == 0
          ? VcsBundle.message("changes.nodetitle.directory.changecount", dirCount)
          : VcsBundle.message("changes.nodetitle.directory.file.changecount", dirCount, count));
    }

    return result;
  }

  protected void appendCount(@Nonnull ColoredTreeCellRenderer renderer) {
    renderer.append(getCountText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

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

  public boolean canAcceptDrop(final ChangeListDragBean dragBean) {
    return false;
  }

  public void acceptDrop(final ChangeListOwner dragOwner, final ChangeListDragBean dragBean) {
  }

  /**
   * Nodes with the same sort weight should share {@link #compareUserObjects} implementation
   */
  public int getSortWeight() {
    return DEFAULT_SORT_WEIGHT;
  }

  public int compareUserObjects(final Object o2) {
    return 0;
  }

  public void setAttributes(@Nonnull SimpleTextAttributes attributes) {
    myAttributes = attributes;
  }

  protected void appendUpdatingState(@Nonnull ChangesBrowserNodeRenderer renderer) {
    renderer.append((getCountText().isEmpty() ? spaceAndThinSpace() : ", ") + VcsBundle.message("changes.nodetitle.updating"),
                    SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  @Deprecated
  public final int getCount() {
    return getFileCount();
  }

  private static class Tag {
    @Nonnull
    private final String myKey;

    public Tag(@Nonnull String key) {
      myKey = key;
    }

    @Override
    public String toString() {
      return VcsBundle.message(myKey);
    }
  }
}
