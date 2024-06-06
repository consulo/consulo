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
package consulo.ui.ex.awt.dnd;

import consulo.platform.Platform;
import consulo.ui.ex.awt.TransferableList;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class DnDAwareTree extends Tree implements DnDAware {
  public DnDAwareTree() {
    initDnD();
  }

  public DnDAwareTree(final TreeModel treemodel) {
    super(treemodel);
    initDnD();
  }

  public DnDAwareTree(final TreeNode root) {
    super(root);
    initDnD();
  }

  @Override
  public void processMouseEvent(final MouseEvent e) {
//todo [kirillk] to delegate this to DnDEnabler
    if (getToolTipText() == null && e.getID() == MouseEvent.MOUSE_ENTERED) return;
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (Platform.current().os().isMac() && JBSwingUtilities.isRightMouseButton(e) && e.getID() == MouseEvent.MOUSE_DRAGGED) return;
    super.processMouseMotionEvent(e);
  }

  @Override
  public final boolean isOverSelection(final Point point) {
    final TreePath path = WideSelectionTreeUI.isWideSelection(this) ? getClosestPathForLocation(point.x, point.y) : getPathForLocation(point.x, point.y);
    if (path == null) return false;
    return isPathSelected(path);
  }

  @Override
  public void dropSelectionButUnderPoint(final Point point) {
    TreeUtil.dropSelectionButUnderPoint(this, point);
  }

  @Override
  @Nonnull
  public final JComponent getComponent() {
    return this;
  }

  @Nonnull
  public static Pair<Image, Point> getDragImage(@Nonnull Tree dndAwareTree, @Nonnull TreePath path, @Nonnull Point dragOrigin) {
    int row = dndAwareTree.getRowForPath(path);
    Component comp = dndAwareTree.getCellRenderer().getTreeCellRendererComponent(dndAwareTree, path.getLastPathComponent(), false, true, true, row, false);
    return createDragImage(dndAwareTree, comp, dragOrigin, true);
  }

  @Nonnull
  public static Pair<Image, Point> getDragImage(@Nonnull Tree dndAwareTree, @Nonnull String text, @Nullable Point dragOrigin) {
    return createDragImage(dndAwareTree, new JLabel(text), dragOrigin, false);
  }

  @Nonnull
  private static Pair<Image, Point> createDragImage(@Nonnull Tree tree, @Nonnull Component c, @Nullable Point dragOrigin, boolean adjustToPathUnderDragOrigin) {
    if (c instanceof JComponent) {
      ((JComponent)c).setOpaque(true);
    }

    c.setForeground(tree.getForeground());
    c.setBackground(tree.getBackground());
    c.setFont(tree.getFont());
    c.setSize(c.getPreferredSize());
    final BufferedImage image = UIUtil.createImage(c, c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = (Graphics2D)image.getGraphics();
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
    c.paint(g2);
    g2.dispose();

    Point point = new Point(image.getWidth(null) / 2, image.getHeight(null) / 2);

    if (adjustToPathUnderDragOrigin) {
      TreePath path = tree.getPathForLocation(dragOrigin.x, dragOrigin.y);
      if (path != null) {
        Rectangle bounds = tree.getPathBounds(path);
        point = new Point(dragOrigin.x - bounds.x, dragOrigin.y - bounds.y);
      }
    }

    return new Pair<>(image, point);
  }

  private void initDnD() {
    if (!GraphicsEnvironment.isHeadless()) {
      setDragEnabled(true);
      setTransferHandler(DEFAULT_TRANSFER_HANDLER);
    }
  }

  private static final TransferHandler DEFAULT_TRANSFER_HANDLER = new TransferHandler() {
    @Override
    protected Transferable createTransferable(JComponent component) {
      if (component instanceof JTree) {
        JTree tree = (JTree)component;
        TreePath[] selection = tree.getSelectionPaths();
        if (selection != null && selection.length > 1) {
          return new TransferableList<TreePath>(selection) {
            @Override
            protected String toString(TreePath path) {
              return String.valueOf(path.getLastPathComponent());
            }
          };
        }
      }
      return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY_OR_MOVE;
    }
  };
}
