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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems;

import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.SimpleDnDAwareTree;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.SourceItemsDraggingObject;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.ExtractIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.PackAndPutIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.PutSourceItemIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.SourceItemNavigateAction;
import consulo.ui.ex.awt.tree.SimpleTreeBuilder;
import consulo.ui.ex.awt.tree.SimpleTreeStructure;
import consulo.ui.ex.awt.tree.WeightBasedComparator;
import consulo.application.ApplicationManager;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.ProjectBundle;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.dnd.AdvancedDnDSource;
import consulo.ui.ex.awt.dnd.DnDAction;
import consulo.ui.ex.awt.dnd.DnDDragStartBean;
import consulo.ui.ex.awt.dnd.DnDManager;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.lang.Pair;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class SourceItemsTree extends SimpleDnDAwareTree implements AdvancedDnDSource, Disposable {
  private final ArtifactEditorImpl myArtifactsEditor;
  private final SimpleTreeBuilder myBuilder;

  public SourceItemsTree(ArtifactEditorContext editorContext, ArtifactEditorImpl artifactsEditor) {
    myArtifactsEditor = artifactsEditor;
    myBuilder = new SimpleTreeBuilder(this, this.getBuilderModel(), new SourceItemsTreeStructure(editorContext, artifactsEditor), new WeightBasedComparator(true));
    setRootVisible(false);
    setShowsRootHandles(true);
    Disposer.register(this, myBuilder);
    PopupHandler.installPopupHandler(this, createPopupGroup(), ActionPlaces.UNKNOWN, ActionManager.getInstance());
    installDnD();
  }

  private void installDnD() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      DnDManager.getInstance().registerSource(this);
    }
  }

  private ActionGroup createPopupGroup() {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new PutSourceItemIntoDefaultLocationAction(this, myArtifactsEditor));
    group.add(new PackAndPutIntoDefaultLocationAction(this, myArtifactsEditor));
    // java specific group.add(new PutSourceItemIntoParentAndLinkViaManifestAction(this, myArtifactsEditor));
    group.add(new ExtractIntoDefaultLocationAction(this, myArtifactsEditor));

    group.add(AnSeparator.getInstance());
    group.add(new SourceItemNavigateAction(this));
    //group.add(new SourceItemFindUsagesAction(this, myArtifactsEditor.getContext().getProject(), myArtifactsEditor.getContext().getParent()));

    DefaultTreeExpander expander = new DefaultTreeExpander(this);
    final CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
    group.add(AnSeparator.getInstance());
    group.addAction(commonActionsManager.createExpandAllAction(expander, this));
    group.addAction(commonActionsManager.createCollapseAllAction(expander, this));
    return group;
  }

  public void rebuildTree() {
    myBuilder.updateFromRoot(true);
  }

  public void initTree() {
    myBuilder.initRootNode();
  }

  @Override
  public void dispose() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      DnDManager.getInstance().unregisterSource(this);
    }
  }

  private DefaultMutableTreeNode[] getSelectedTreeNodes() {
    return getSelectedNodes(DefaultMutableTreeNode.class, null);
  }

  @Override
  public boolean canStartDragging(DnDAction action, Point dragOrigin) {
    return !getSelectedItems().isEmpty();
  }

  @Override
  public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
    List<PackagingSourceItem> items = getSelectedItems();
    return new DnDDragStartBean(new SourceItemsDraggingObject(items.toArray(new PackagingSourceItem[items.size()])));
  }

  public List<SourceItemNode> getSelectedSourceItemNodes() {
    final List<SourceItemNode> nodes = new ArrayList<SourceItemNode>();
    for (DefaultMutableTreeNode treeNode : getSelectedTreeNodes()) {
      final Object userObject = treeNode.getUserObject();
      if (userObject instanceof SourceItemNode) {
        nodes.add((SourceItemNode)userObject);
      }
    }
    return nodes;
  }

  public List<PackagingSourceItem> getSelectedItems() {
    List<PackagingSourceItem> items = new ArrayList<PackagingSourceItem>();
    for (SourceItemNode node : getSelectedSourceItemNodes()) {
      final PackagingSourceItem sourceItem = node.getSourceItem();
      if (sourceItem != null && sourceItem.isProvideElements()) {
        items.add(sourceItem);
      }
    }
    return items;
  }

  @Override
  public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin) {
    final DefaultMutableTreeNode[] nodes = getSelectedTreeNodes();
    if (nodes.length == 1) {
      return DnDAwareTree.getDragImage(this, TreeUtil.getPathFromRoot(nodes[0]), dragOrigin);
    }
    return DnDAwareTree.getDragImage(this, ProjectBundle.message("drag.n.drop.text.0.packaging.elements", nodes.length), dragOrigin);
  }

  @Override
  public void dragDropEnd() {
  }

  @Override
  public void dropActionChanged(int gestureModifiers) {
  }

  private static class SourceItemsTreeStructure extends SimpleTreeStructure {
    private final ArtifactEditorContext myEditorContext;
    private final ArtifactEditorImpl myArtifactsEditor;
    private SourceItemsTreeRoot myRoot;

    public SourceItemsTreeStructure(ArtifactEditorContext editorContext, ArtifactEditorImpl artifactsEditor) {
      myEditorContext = editorContext;
      myArtifactsEditor = artifactsEditor;
    }

    @Override
    public Object getRootElement() {
      if (myRoot == null) {
        myRoot = new SourceItemsTreeRoot(myEditorContext, myArtifactsEditor);
      }
      return myRoot;
    }
  }
}
