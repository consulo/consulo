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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangeListDragBean;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode.IGNORED_FILES_TAG;
import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesBrowserNode.UNVERSIONED_FILES_TAG;
import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListView.getChanges;
import static consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListView.getVirtualFiles;
import static java.util.stream.Collectors.toList;

public class ChangesDnDSupport implements DnDDropHandler, DnDTargetChecker {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ChangeListManagerImpl myChangeListManager;
  @Nonnull
  private final Tree myTree;

  public static void install(@Nonnull Project project, @Nonnull Tree tree) {
    new ChangesDnDSupport(project, tree).install();
  }

  private ChangesDnDSupport(@Nonnull Project project, @Nonnull Tree tree) {
    myProject = project;
    myChangeListManager = ChangeListManagerImpl.getInstanceImpl(project);
    myTree = tree;
  }

  private void install() {
    DnDSupport.createBuilder(myTree)
              .setTargetChecker(this)
              .setDropHandler(this)
              .setImageProvider(this::createDraggedImage)
              .setBeanProvider(this::createDragStartBean)
              .setDisposableParent(myTree instanceof Disposable ? (Disposable)myTree : myProject)
              .install();
  }

  @Nonnull
  private DnDImage createDraggedImage(@Nonnull DnDActionInfo info) {
    String imageText = VcsBundle.message("changes.view.dnd.label", getSelectionCount());
    Image image = DnDAwareTree.getDragImage(myTree, imageText, null).getFirst();

    return new DnDImage(image, new Point(-image.getWidth(null), -image.getHeight(null)));
  }

  @Nullable
  private DnDDragStartBean createDragStartBean(@Nonnull DnDActionInfo info) {
    DnDDragStartBean result = null;

    if (info.isMove()) {
      Change[] changes = getChanges(myProject, myTree.getSelectionPaths()).toArray(Change[]::new);
      List<VirtualFile> unversionedFiles = getVirtualFiles(myTree.getSelectionPaths(), UNVERSIONED_FILES_TAG).collect(toList());
      List<VirtualFile> ignoredFiles = getVirtualFiles(myTree.getSelectionPaths(), IGNORED_FILES_TAG).collect(toList());

      if (changes.length > 0 || !unversionedFiles.isEmpty() || !ignoredFiles.isEmpty()) {
        result = new DnDDragStartBean(new ChangeListDragBean(myTree, changes, unversionedFiles, ignoredFiles));
      }
    }

    return result;
  }

  @Override
  public boolean update(DnDEvent aEvent) {
    aEvent.hideHighlighter();
    aEvent.setDropPossible(false, "");

    Object attached = aEvent.getAttachedObject();
    if (!(attached instanceof ChangeListDragBean)) return false;

    final ChangeListDragBean dragBean = (ChangeListDragBean)attached;
    if (dragBean.getSourceComponent() != myTree) return false;
    dragBean.setTargetNode(null);

    RelativePoint dropPoint = aEvent.getRelativePoint();
    Point onTree = dropPoint.getPoint(myTree);
    final TreePath dropPath = myTree.getPathForLocation(onTree.x, onTree.y);

    if (dropPath == null) return false;

    ChangesBrowserNode dropNode = (ChangesBrowserNode)dropPath.getLastPathComponent();
    while (!((ChangesBrowserNode)dropNode.getParent()).isRoot()) {
      dropNode = (ChangesBrowserNode)dropNode.getParent();
    }

    if (!dropNode.canAcceptDrop(dragBean)) {
      return false;
    }

    final Rectangle tableCellRect = myTree.getPathBounds(new TreePath(dropNode.getPath()));
    if (fitsInBounds(tableCellRect)) {
      aEvent.setHighlighting(new RelativeRectangle(myTree, tableCellRect), DnDEvent.DropTargetHighlightingType.RECTANGLE);
    }

    aEvent.setDropPossible(true);
    dragBean.setTargetNode(dropNode);

    return false;
  }

  @Override
  public void drop(DnDEvent aEvent) {
    Object attached = aEvent.getAttachedObject();
    if (!(attached instanceof ChangeListDragBean)) return;

    final ChangeListDragBean dragBean = (ChangeListDragBean)attached;
    final ChangesBrowserNode changesBrowserNode = dragBean.getTargetNode();
    if (changesBrowserNode != null) {
      changesBrowserNode.acceptDrop(myChangeListManager, dragBean);
    }
  }

  private boolean fitsInBounds(final Rectangle rect) {
    final Container container = myTree.getParent();
    if (container instanceof JViewport) {
      final Container scrollPane = container.getParent();
      if (scrollPane instanceof JScrollPane) {
        final Rectangle rectangle = SwingUtilities.convertRectangle(myTree, rect, scrollPane.getParent());
        return scrollPane.getBounds().contains(rectangle);
      }
    }
    return true;
  }

  private int getSelectionCount() {
    final TreePath[] paths = myTree.getSelectionModel().getSelectionPaths();
    int count = 0;
    final List<ChangesBrowserNode> nodes = new ArrayList<>();

    for (final TreePath path : paths) {
      final ChangesBrowserNode node = (ChangesBrowserNode)path.getLastPathComponent();
      if (!node.isLeaf()) {
        nodes.add(node);
        count += node.getCount();
      }
    }

    for (TreePath path : paths) {
      final ChangesBrowserNode element = (ChangesBrowserNode)path.getLastPathComponent();
      boolean child = false;
      for (final ChangesBrowserNode node : nodes) {
        if (node.isNodeChild(element)) {
          child = true;
          break;
        }
      }

      if (!child) {
        if (element.isLeaf()) count++;
      }
      else if (!element.isLeaf()) {
        count -= element.getCount();
      }
    }
    return count;
  }
}