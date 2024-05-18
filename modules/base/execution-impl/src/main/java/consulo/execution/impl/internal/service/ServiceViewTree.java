// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.execution.service.ServiceViewActionUtils;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewItemState;
import consulo.execution.service.ServiceViewOptions;
import consulo.navigation.ItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.LoadingNode;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUIHelper;
import consulo.ui.ex.awt.util.ComponentUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.util.function.Function;

import static consulo.ui.ex.awt.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

final class ServiceViewTree extends Tree {
  private static final Function<TreePath, String> DISPLAY_NAME_CONVERTER = path -> {
    Object node = path.getLastPathComponent();
    if (node instanceof ServiceViewItem) {
      return ServiceViewDragHelper.getDisplayName(((ServiceViewItem)node).getViewDescriptor().getPresentation());
    }
    return node.toString();
  };

  private final TreeModel myTreeModel;

  ServiceViewTree(@Nonnull TreeModel treeModel, @Nonnull Disposable parent) {
    myTreeModel = treeModel;
    AsyncTreeModel asyncTreeModel = new AsyncTreeModel(myTreeModel, parent);
    setModel(asyncTreeModel);
    initTree();
  }

  private void initTree() {
    // look
    setRootVisible(false);
    setShowsRootHandles(true);
    setCellRenderer(new ServiceViewTreeCellRenderer());
    ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);

    // listeners
    TreeUIHelper.getInstance().installTreeSpeedSearch(this, DISPLAY_NAME_CONVERTER, true);
    ServiceViewTreeLinkMouseListener mouseListener = new ServiceViewTreeLinkMouseListener(this);
    mouseListener.installOn(this);
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@Nonnull MouseEvent e) {
        TreePath path = getClosestPathForLocation(e.getX(), e.getY());
        if (path == null) return false;

        Object lastComponent = path.getLastPathComponent();
        if (lastComponent instanceof LoadingNode) return false;

        return myTreeModel.isLeaf(lastComponent) &&
          lastComponent instanceof ServiceViewItem &&
          ((ServiceViewItem)lastComponent).getViewDescriptor().handleDoubleClick(e);
      }
    }.installOn(this);
  }

  private static final class ServiceViewTreeCellRenderer extends ServiceViewTreeCellRendererBase {
    private ServiceViewDescriptor myDescriptor;
    private ServiceViewItemState myItemState;
    private JComponent myComponent;

    @RequiredUIAccess
    @Override
    public void customizeCellRenderer(@Nonnull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      myDescriptor = value instanceof ServiceViewItem ? ((ServiceViewItem)value).getViewDescriptor() : null;
      myComponent = tree;
      myItemState = new ServiceViewItemState(selected, expanded, leaf, hasFocus);
      super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
      myDescriptor = null;
    }

    @Nullable
    @Override
    protected ItemPresentation getPresentation(Object node) {
      // Ensure that value != myTreeModel.getRoot() && !(value instanceof LoadingNode)
      if (!(node instanceof ServiceViewItem)) return null;

      ServiceViewOptions viewOptions = DataManager.getInstance().getDataContext(myComponent).getData(ServiceViewActionUtils.OPTIONS_KEY);
      assert myItemState != null;
      return ((ServiceViewItem)node).getItemPresentation(viewOptions, myItemState);
    }

    @Override
    protected Object getTag(String fragment) {
      return myDescriptor == null ? null : myDescriptor.getPresentationTag(fragment);
    }
  }
}
