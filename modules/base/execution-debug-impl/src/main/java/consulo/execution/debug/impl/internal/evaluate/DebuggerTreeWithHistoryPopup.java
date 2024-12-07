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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeModelAdapter;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * @author nik
 */
class DebuggerTreeWithHistoryPopup<D> extends DebuggerTreeWithHistoryContainer<D> {
  @NonNls private final static String DIMENSION_SERVICE_KEY = "DebuggerActiveHint";
  private JBPopup myPopup;
  private final Editor myEditor;
  private final Point myPoint;
  @Nullable private final Runnable myHideRunnable;

  private DebuggerTreeWithHistoryPopup(
    @Nonnull D initialItem,
    @Nonnull DebuggerTreeCreator<D> creator,
    @Nonnull Editor editor,
    @Nonnull Point point,
    @Nonnull Project project,
    @Nullable Runnable hideRunnable
  ) {
    super(initialItem, creator, project);
    myEditor = editor;
    myPoint = point;
    myHideRunnable = hideRunnable;
  }

  public static <D> void showTreePopup(
    @Nonnull DebuggerTreeCreator<D> creator,
    @Nonnull D initialItem,
    @Nonnull Editor editor,
    @Nonnull Point point,
    @Nonnull Project project,
    Runnable hideRunnable
  ) {
    new DebuggerTreeWithHistoryPopup<>(initialItem, creator, editor, point, project, hideRunnable)
      .updateTree(initialItem);
  }

  private TreeModelListener createTreeListener(final Tree tree) {
    return new TreeModelAdapter() {
      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        resize(e.getTreePath(), tree);
      }
    };
  }

  @Override
  protected void updateContainer(final Tree tree, String title) {
    if (myPopup != null) {
      myPopup.cancel();
    }
    tree.getModel().addTreeModelListener(createTreeListener(tree));
    myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(createMainPanel(tree), tree)
      .setRequestFocus(true)
      .setTitle(title)
      .setResizable(true)
      .setMovable(true)
      .setDimensionServiceKey(myProject, DIMENSION_SERVICE_KEY, false)
      .setMayBeParent(true)
      .setKeyEventHandler(event -> {
        if (UIUtil.isCloseRequest(event)) {
          // Do not process a close request if the tree shows a speed search popup
          SpeedSearchSupply supply = SpeedSearchSupply.getSupply(tree);
          return supply != null && StringUtil.isEmpty(supply.getEnteredPrefix());
        }
        return false;
      })
      .addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(LightweightWindowEvent event) {
          if (myHideRunnable != null) {
            myHideRunnable.run();
          }
        }
      })
      .setCancelCallback(() -> {
        Window parent = SwingUtilities.getWindowAncestor(tree);
        if (parent != null) {
          for (Window child : parent.getOwnedWindows()) {
            if (child.isShowing()) {
              return false;
            }
          }
        }
        return true;
      })
      .createPopup();

    registerTreeDisposable(myPopup, tree);

    //Editor may be disposed before later invokator process this action
    if (myEditor.getComponent().getRootPane() == null) {
      myPopup.cancel();
      return;
    }
    myPopup.show(new RelativePoint(myEditor.getContentComponent(), myPoint));

    updateInitialBounds(tree);
  }

  private void resize(final TreePath path, JTree tree) {
    if (myPopup == null || !myPopup.isVisible()) return;
    final Window popupWindow = SwingUtilities.windowForComponent(myPopup.getContent());
    if (popupWindow == null) return;
    final Dimension size = tree.getPreferredSize();
    final Point location = popupWindow.getLocation();
    final Rectangle windowBounds = popupWindow.getBounds();
    final Rectangle bounds = tree.getPathBounds(path);
    if (bounds == null) return;

    final Rectangle targetBounds = new Rectangle(
      location.x,
      location.y,
      Math.max(Math.max(size.width, bounds.width) + 20, windowBounds.width),
      Math.max(tree.getRowCount() * bounds.height + 55, windowBounds.height)
    );
    ScreenUtil.cropRectangleToFitTheScreen(targetBounds);
    popupWindow.setBounds(targetBounds);
    popupWindow.validate();
    popupWindow.repaint();
  }

  private void updateInitialBounds(final Tree tree) {
    final Window popupWindow = SwingUtilities.windowForComponent(myPopup.getContent());
    final Dimension size = tree.getPreferredSize();
    final Point location = popupWindow.getLocation();
    final Rectangle windowBounds = popupWindow.getBounds();
    final Rectangle targetBounds = new Rectangle(
      location.x,
      location.y,
      Math.max(size.width + 250, windowBounds.width),
      Math.max(size.height, windowBounds.height)
    );
    ScreenUtil.cropRectangleToFitTheScreen(targetBounds);
    popupWindow.setBounds(targetBounds);
    popupWindow.validate();
    popupWindow.repaint();
  }
}
