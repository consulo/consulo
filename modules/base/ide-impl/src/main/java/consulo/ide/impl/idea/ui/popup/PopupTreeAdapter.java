// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.TreeUtil;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class PopupTreeAdapter<T> implements PopupChooserBuilder.PopupComponentAdapter<T> {
  private final PopupChooserBuilder myBuilder;
  private final JTree myTree;

  public PopupTreeAdapter(PopupChooserBuilder builder, JTree tree) {
    myBuilder = builder;
    myTree = tree;
  }

  @Override
  public JComponent getComponent() {
    return myTree;
  }

  @Override
  public void setItemChosenCallback(Consumer<? super T> callback) {
    myBuilder.setItemChoosenCallback(() -> {
      TreePath path = myTree.getSelectionModel().getLeadSelectionPath();
      T component = (T)path.getLastPathComponent();
      if (component != null) {
        callback.accept(component);
      }
    });
  }

  @Override
  public void setItemsChosenCallback(Consumer<? super Set<T>> callback) {
    myBuilder.setItemChoosenCallback(() -> {
      final Set<T> selection = new HashSet<>();
      for (TreePath path : myTree.getSelectionModel().getSelectionPaths()) {
        Object component = path.getLastPathComponent();
        if (component != null) {
          selection.add((T)component);
        }
      }
      if (!selection.isEmpty()) {
        callback.accept(selection);
      }
    });
  }

  @Override
  public JScrollPane createScrollPane() {
    TreeUtil.expandAll(myTree);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);

    scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

    if (myTree.getSelectionCount() == 0) {
      myTree.setSelectionRow(0);
    }

    if (myTree.getRowCount() >= 20) {
      scrollPane.getViewport().setPreferredSize(new Dimension(myTree.getPreferredScrollableViewportSize().width, 300));
    }
    else {
      scrollPane.getViewport().setPreferredSize(myTree.getPreferredSize());
    }

    if (myBuilder.isAutoselectOnMouseMove()) {
      myTree.addMouseMotionListener(new MouseMotionAdapter() {
        boolean myIsEngaged = false;

        @Override
        public void mouseMoved(MouseEvent e) {
          if (myIsEngaged) {
            final Point p = e.getPoint();
            int index = myTree.getRowForLocation(p.x, p.y);
            myTree.setSelectionRow(index);
          }
          else {
            myIsEngaged = true;
          }
        }
      });
    }

    return scrollPane;
  }
}
