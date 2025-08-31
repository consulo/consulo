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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.application.ApplicationManager;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.RenameablePackagingElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.logging.Logger;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.SimpleNode;
import consulo.ui.ex.awt.tree.TreeUIHelper;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* @author nik
*/
public class LayoutTree extends SimpleDnDAwareTree implements AdvancedDnDSource {
  private static final Logger LOG = Logger.getInstance(LayoutTree.class);
  private final ArtifactEditorImpl myArtifactsEditor;

  public LayoutTree(ArtifactEditorImpl artifactsEditor) {
    myArtifactsEditor = artifactsEditor;
    setRootVisible(true);
    setShowsRootHandles(false);
    setCellEditor(new LayoutTreeCellEditor());
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      DnDManager.getInstance().registerSource(this);
    }

    //todo[nik,pegov] fix for tooltips in the tree. Otherwise tooltips will be ignored by DnDEnabled   
    setToolTipText("");
  }

  public void addSubtreeToUpdate(DefaultMutableTreeNode newNode) {
    AbstractTreeBuilder.getBuilderFor(this).addSubtreeToUpdate(newNode);
  }

  @Override
  protected void configureUiHelper(TreeUIHelper helper) {
    Convertor<TreePath, String> convertor = path -> {
      SimpleNode node = getNodeFor(path);
      if (node instanceof PackagingElementNode) {
        return ((PackagingElementNode<?>)node).getElementPresentation().getSearchName();
      }
      return "";
    };
    new TreeSpeedSearch(this, convertor, true);
  }

  private List<PackagingElementNode<?>> getNodesToDrag() {
    return getSelection().getNodes();
  }

  @Override
  public boolean canStartDragging(DnDAction action, Point dragOrigin) {
    return !getNodesToDrag().isEmpty();
  }

  @Override
  public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
    return new DnDDragStartBean(new LayoutNodesDraggingObject(myArtifactsEditor, getNodesToDrag()));
  }

  @Override
  public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, @Nonnull DnDDragStartBean bean) {
    List<PackagingElementNode<?>> nodes = getNodesToDrag();
    if (nodes.size() == 1) {
      return DnDAwareTree.getDragImage(this, getPathFor(nodes.get(0)), dragOrigin);
    }
    return DnDAwareTree.getDragImage(
      this,
      ProjectLocalize.dragNDropText0PackagingElements(nodes.size()).get(),
      dragOrigin
    );
  }

  @Override
  public void dragDropEnd() {
  }

  @Override
  public void dropActionChanged(int gestureModifiers) {
  }

  @Override
  public void dispose() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      DnDManager.getInstance().unregisterSource(this);
    }
  }

  public LayoutTreeSelection getSelection() {
    return new LayoutTreeSelection(this);
  }

  @Nullable
  public PackagingElement<?> getElementByPath(TreePath path) {
    SimpleNode node = getNodeFor(path);
    if (node instanceof PackagingElementNode) {
      List<? extends PackagingElement<?>> elements = ((PackagingElementNode<?>)node).getPackagingElements();
      if (!elements.isEmpty()) {
        return elements.get(0);
      }
    }
    return null;
  }

  public PackagingElementNode<?> getRootPackagingNode() {
    SimpleNode node = getNodeFor(new TreePath(getRootNode()));
    return node instanceof PackagingElementNode ? (PackagingElementNode<?>)node : null;
  }

  public DefaultMutableTreeNode getRootNode() {
    return (DefaultMutableTreeNode)getModel().getRoot();
  }

  public List<PackagingElementNode<?>> findNodes(Collection<? extends PackagingElement<?>> elements) {
    List<PackagingElementNode<?>> nodes = new ArrayList<>();
    TreeUtil.traverseDepth(getRootNode(), node -> {
      Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof PackagingElementNode) {
        PackagingElementNode<?> packagingNode = (PackagingElementNode<?>)userObject;
        List<? extends PackagingElement<?>> nodeElements = packagingNode.getPackagingElements();
        if (ContainerUtil.intersects(nodeElements, elements)) {
          nodes.add(packagingNode);
        }
      }
      return true;
    });
    return nodes;
  }

  public void addSubtreeToUpdate(PackagingElementNode elementNode) {
    DefaultMutableTreeNode node = TreeUtil.findNodeWithObject(getRootNode(), elementNode);
    if (node != null) {
      addSubtreeToUpdate(node);
    }
  }

  @Nullable
  public PackagingElementNode<?> findCompositeNodeByPath(String parentPath) {
    PackagingElementNode<?> node = getRootPackagingNode();
    for (String name : StringUtil.split(parentPath, "/")) {
      if (node == null) {
        return null;
      }
      node = node.findCompositeChild(name);
    }
    return node;
  }

  private class LayoutTreeCellEditor extends DefaultCellEditor {
    public LayoutTreeCellEditor() {
      super(new JTextField());
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
      JTextField field = (JTextField)super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
      Object node = ((DefaultMutableTreeNode)value).getUserObject();
      PackagingElement<?> element = ((PackagingElementNode)node).getElementIfSingle();
      LOG.assertTrue(element != null);
      String name = ((RenameablePackagingElement)element).getName();
      field.setText(name);
      int i = name.lastIndexOf('.');
      field.setSelectionStart(0);
      field.setSelectionEnd(i != -1 ? i : name.length());
      return field;
    }

    @Override
    public boolean stopCellEditing() {
      String newValue = ((JTextField)editorComponent).getText();
      TreePath path = getEditingPath();
      Object node = getNodeFor(path);
      RenameablePackagingElement currentElement = null;
      if (node instanceof PackagingElementNode) {
        PackagingElement<?> element = ((PackagingElementNode)node).getElementIfSingle();
        if (element instanceof RenameablePackagingElement) {
          currentElement = (RenameablePackagingElement)element;
        }
      }
      boolean stopped = super.stopCellEditing();
      if (stopped && currentElement != null) {
        RenameablePackagingElement finalCurrentElement = currentElement;
        myArtifactsEditor.getLayoutTreeComponent().editLayout(() -> finalCurrentElement.rename(newValue));
        myArtifactsEditor.queueValidation();
        myArtifactsEditor.getLayoutTreeComponent().updatePropertiesPanel(true);
        addSubtreeToUpdate((DefaultMutableTreeNode)path.getLastPathComponent());
        requestFocusToTree();
      }
      return stopped;
    }

    @Override
    public void cancelCellEditing() {
      super.cancelCellEditing();
      requestFocusToTree();
    }

    private void requestFocusToTree() {
      ProjectIdeFocusManager.getInstance(myArtifactsEditor.getContext().getProject()).requestFocus(LayoutTree.this, true);
    }
  }
}
