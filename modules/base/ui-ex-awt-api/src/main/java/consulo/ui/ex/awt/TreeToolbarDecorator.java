/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.awt.dnd.RowsDnDSupport;
import consulo.ui.ex.awt.tree.EditableTreeModel;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.tree.LeafState;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * @author Konstantin Bulenkov
 */
class TreeToolbarDecorator extends ToolbarDecorator {
  private final JTree myTree;
  @Nullable
  private final ElementProducer<?> myProducer;

  TreeToolbarDecorator(JTree tree, @Nullable ElementProducer<?> producer) {
    myTree = tree;
    myProducer = producer;
    myAddActionEnabled = myRemoveActionEnabled = myUpActionEnabled = myDownActionEnabled = myTree.getModel() instanceof EditableTreeModel;
    if (myTree.getModel() instanceof EditableTreeModel) {
      createDefaultTreeActions();
    }
    myTree.getSelectionModel().addTreeSelectionListener(e -> updateButtons());
    myTree.addPropertyChangeListener("enabled", evt -> updateButtons());
  }

  private void createDefaultTreeActions() {
    EditableTreeModel model = (EditableTreeModel)myTree.getModel();
    myAddAction = (button, e) -> {
      TreePath path = myTree.getSelectionPath();
      DefaultMutableTreeNode selected = path == null ? (DefaultMutableTreeNode)myTree.getModel().getRoot() : (DefaultMutableTreeNode)path.getLastPathComponent();
      Object selectedNode = selected.getUserObject();

      myTree.stopEditing();
      Object element;
      if (model instanceof DefaultTreeModel && myProducer != null) {
        element = myProducer.createElement();
        if (element == null) return;
      }
      else {
        element = null;
      }
      DefaultMutableTreeNode parent = selected;
      if ((selectedNode instanceof LeafState.Supplier && ((LeafState.Supplier)selectedNode).getLeafState() == LeafState.ALWAYS) || !selected.getAllowsChildren()) {
        parent = (DefaultMutableTreeNode)selected.getParent();
      }
      if (parent != null) {
        parent.insert(new DefaultMutableTreeNode(element), parent.getChildCount());
      }
      TreePath createdPath = model.addNode(new TreePath(parent.getPath()));
      if (path != null) {
        TreeUtil.selectPath(myTree, createdPath);
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
      }
    };

    myRemoveAction = (button, e) -> {
      myTree.stopEditing();
      TreePath path = myTree.getSelectionPath();
      model.removeNode(path);
    };
  }

  @Override
  protected JComponent getComponent() {
    return myTree;
  }

  @Override
  protected void updateButtons() {
    getActionsPanel().setEnabled(CommonActionsPanel.Buttons.REMOVE, myTree.getSelectionPath() != null);
  }

  @Override
  public ToolbarDecorator setVisibleRowCount(int rowCount) {
    myTree.setVisibleRowCount(rowCount);
    return this;
  }

  @Override
  protected boolean isModelEditable() {
    return myTree.getModel() instanceof EditableModel;
  }

  @Override
  protected void installDnDSupport() {
    RowsDnDSupport.install(myTree, (EditableModel)myTree.getModel());
  }
}
