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
package com.intellij.openapi.options.newEditor;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Weighted;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.*;
import com.intellij.ui.treeStructure.filtered.FilteringTreeBuilder;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import consulo.util.ui.tree.TreeDecorationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;

public class OptionsTree extends JPanel implements Disposable, OptionsEditorColleague {
  Project myProject;
  final SimpleTree myTree;
  Configurable[] myConfigurables;
  FilteringTreeBuilder myBuilder;
  Root myRoot;
  OptionsEditorContext myContext;

  private Map<Configurable, EditorNode> myConfigurable2Node = new HashMap<>();

  MergingUpdateQueue mySelection;

  public OptionsTree(Project project, Configurable[] configurables, OptionsEditorContext context) {
    super(new BorderLayout());
    myProject = project;
    myConfigurables = configurables;
    myContext = context;

    myRoot = new Root();
    final SimpleTreeStructure structure = new SimpleTreeStructure() {
      @Override
      public Object getRootElement() {
        return myRoot;
      }
    };

    myTree = new SimpleTree();
    TreeUtil.installActions(myTree);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myBuilder = new MyBuilder(structure);
    myBuilder.setFilteringMerge(300, null);
    Disposer.register(this, myBuilder);

    myTree.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        myBuilder.revalidateTree();
      }

      @Override
      public void componentMoved(final ComponentEvent e) {
        myBuilder.revalidateTree();
      }

      @Override
      public void componentShown(final ComponentEvent e) {
        myBuilder.revalidateTree();
      }
    });

    final JScrollPane scrolls = ScrollPaneFactory.createScrollPane(myTree, true);
    scrolls.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    add(scrolls, BorderLayout.CENTER);

    mySelection = new MergingUpdateQueue("OptionsTree", 150, false, this, this, this).setRestartTimerOnAdd(true);
    myTree.getSelectionModel().addTreeSelectionListener(e -> {
      final TreePath path = e.getNewLeadSelectionPath();
      if (path == null) {
        queueSelection(null);
      }
      else {
        final Base base = extractNode(path.getLastPathComponent());
        queueSelection(base != null ? base.getConfigurable() : null);
      }
    });
    myTree.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(final KeyEvent e) {
        _onTreeKeyEvent(e);
      }

      @Override
      public void keyPressed(final KeyEvent e) {
        _onTreeKeyEvent(e);
      }

      @Override
      public void keyReleased(final KeyEvent e) {
        _onTreeKeyEvent(e);
      }
    });

    updateUI();
  }

  @Override
  public void updateUI() {
    super.updateUI();

    if(myTree != null) {
      TreeDecorationUtil.decorateTree(myTree);
    }
  }

  protected void _onTreeKeyEvent(KeyEvent e) {
    final KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);

    final Object action = myTree.getInputMap().get(stroke);
    if (action == null) {
      onTreeKeyEvent(e);
    }
  }

  protected void onTreeKeyEvent(KeyEvent e) {

  }

  ActionCallback select(@Nullable Configurable configurable) {
    return queueSelection(configurable);
  }

  public void selectFirst() {
    if (myConfigurables.length > 0) {
      queueSelection(myConfigurables[0]);
    }
  }

  private Configurable myQueuedConfigurable;

  ActionCallback queueSelection(final Configurable configurable) {
    if (myBuilder.isSelectionBeingAdjusted()) {
      return new ActionCallback.Rejected();
    }

    final ActionCallback callback = new ActionCallback();

    myQueuedConfigurable = configurable;
    final Update update = new Update(this) {
      @Override
      public void run() {
        if (configurable != myQueuedConfigurable) return;

        if (configurable == null) {
          myTree.getSelectionModel().clearSelection();
          myContext.fireSelected(null, OptionsTree.this);
        }
        else {
          myBuilder.getReady(this).doWhenDone(() -> {
            if (configurable != myQueuedConfigurable) return;

            final EditorNode editorNode = myConfigurable2Node.get(configurable);
            FilteringTreeStructure.FilteringNode editorUiNode = myBuilder.getVisibleNodeFor(editorNode);
            if (editorUiNode == null) return;

            if (!myBuilder.getSelectedElements().contains(editorUiNode)) {
              myBuilder.select(editorUiNode, () -> fireSelected(configurable, callback));
            }
            else {
              myBuilder.scrollSelectionToVisible(() -> fireSelected(configurable, callback), false);
            }
          });
        }
      }

      @Override
      public void setRejected() {
        super.setRejected();
        callback.setRejected();
      }
    };
    mySelection.queue(update);
    return callback;
  }

  private void fireSelected(Configurable configurable, final ActionCallback callback) {
    myContext.fireSelected(configurable, this).doWhenProcessed(callback.createSetDoneRunnable());
  }

  public JTree getTree() {
    return myTree;
  }

  public List<Configurable> getPathToRoot(final Configurable configurable) {
    final ArrayList<Configurable> path = new ArrayList<>();

    EditorNode eachNode = myConfigurable2Node.get(configurable);
    if (eachNode == null) return path;

    while (true) {
      path.add(eachNode.getConfigurable());
      final SimpleNode parent = eachNode.getParent();
      if (parent instanceof EditorNode) {
        eachNode = (EditorNode)parent;
      }
      else {
        break;
      }
    }

    return path;
  }

  public SimpleNode findNodeFor(final Configurable toSelect) {
    return myConfigurable2Node.get(toSelect);
  }

  @Nullable
  public <T extends Configurable> T findConfigurable(Class<T> configurableClass) {
    for (Configurable configurable : myConfigurable2Node.keySet()) {
      if (configurableClass.isInstance(configurable)) {
        return configurableClass.cast(configurable);
      }
    }
    return null;
  }

  @Nullable
  public SearchableConfigurable findConfigurableById(@NotNull String configurableId) {
    for (Configurable configurable : myConfigurable2Node.keySet()) {
      if (configurable instanceof SearchableConfigurable) {
        SearchableConfigurable searchableConfigurable = (SearchableConfigurable)configurable;
        if (configurableId.equals(searchableConfigurable.getId())) {
          return searchableConfigurable;
        }
      }
    }
    return null;
  }

  @Nullable
  private Base extractNode(Object object) {
    if (object instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode uiNode = (DefaultMutableTreeNode)object;
      final Object o = uiNode.getUserObject();
      if (o instanceof FilteringTreeStructure.FilteringNode) {
        return (Base)((FilteringTreeStructure.FilteringNode)o).getDelegate();
      }
    }

    return null;
  }

  abstract static class Base extends CachingSimpleNode {

    protected Base(final SimpleNode aParent) {
      super(aParent);
    }

    String getText() {
      return null;
    }

    boolean isModified() {
      return false;
    }

    boolean isError() {
      return false;
    }

    Configurable getConfigurable() {
      return null;
    }
  }

  class Root extends Base {
    Root() {
      super(null);
    }

    @Override
    protected SimpleNode[] buildChildren() {
      return ContainerUtil.toArray(map(myConfigurables), EMPTY_EN_ARRAY);
    }

    @NotNull
    private List<EditorNode> map(final Configurable[] configurables) {
      List<EditorNode> result = new ArrayList<>();
      for (Configurable eachKid : configurables) {
        if (isInvisibleNode(eachKid)) {
          result.addAll(OptionsTree.this.buildChildren(eachKid, this));
        }
        else {
          result.add(new EditorNode(this, eachKid));
        }
      }
      return sort(result);
    }
  }

  private static boolean isInvisibleNode(final Configurable child) {
    return child instanceof SearchableConfigurable.Parent && !((SearchableConfigurable.Parent)child).isVisible();
  }

  private static List<EditorNode> sort(List<EditorNode> c) {
    List<EditorNode> cc = new ArrayList<>(c);
    Collections.sort(cc, (o1, o2) -> {
      double weight1 = getWeight(o1);
      double weight2 = getWeight(o2);
      if (weight1 != weight2) {
        return (int)(weight2 - weight1);
      }

      return getConfigurableDisplayName(o1.getConfigurable()).compareToIgnoreCase(getConfigurableDisplayName(o2.getConfigurable()));
    });
    return cc;
  }

  private static double getWeight(EditorNode node) {
    Configurable configurable = node.getConfigurable();
    if (configurable instanceof Weighted) {
      return ((Weighted)configurable).getWeight();
    }
    return 0;
  }

  private static String getConfigurableDisplayName(final Configurable c) {
    final String name = c.getDisplayName();
    return name != null ? name : "{ Unnamed Page:" + c.getClass().getSimpleName() + " }";
  }

  private List<EditorNode> buildChildren(final Configurable configurable, SimpleNode parent) {
    if (configurable instanceof Configurable.Composite) {
      final Configurable[] kids = ((Configurable.Composite)configurable).getConfigurables();
      final List<EditorNode> result = new ArrayList<>(kids.length);
      for (Configurable child : kids) {
        if (isInvisibleNode(child)) {
          result.addAll(buildChildren(child, parent));
        }
        result.add(new EditorNode(parent, child));
        myContext.registerKid(configurable, child);
      }
      return sort(result);
    }
    else {
      return Collections.emptyList();
    }
  }

  private static final EditorNode[] EMPTY_EN_ARRAY = new EditorNode[0];

  class EditorNode extends Base {
    Configurable myConfigurable;

    EditorNode(SimpleNode parent, Configurable configurable) {
      super(parent);
      myConfigurable = configurable;
      myConfigurable2Node.put(configurable, this);
    }

    @Override
    protected void update(PresentationData presentation) {
      super.update(presentation);

      String displayName = getConfigurableDisplayName(myConfigurable);
      if (getParent() instanceof Root) {
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
      else {
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    }

    @Override
    protected EditorNode[] buildChildren() {
      List<EditorNode> list = OptionsTree.this.buildChildren(myConfigurable, this);
      return list.isEmpty() ? EMPTY_EN_ARRAY : list.toArray(new EditorNode[list.size()]);
    }

    @Override
    public boolean isAlwaysLeaf() {
      return !(myConfigurable instanceof Configurable.Composite);
    }

    @Override
    Configurable getConfigurable() {
      return myConfigurable;
    }

    @Override
    public int getWeight() {
      return WeightBasedComparator.UNDEFINED_WEIGHT;
    }

    @Override
    String getText() {
      return getConfigurableDisplayName(myConfigurable).replace("\n", " ");
    }

    @Override
    boolean isModified() {
      return myContext.getModified().contains(myConfigurable);
    }

    @Override
    boolean isError() {
      return myContext.getErrors().containsKey(myConfigurable);
    }
  }

  @Override
  public void dispose() {
    myQueuedConfigurable = null;
  }

  @Override
  public ActionCallback onSelected(final Configurable configurable, final Configurable oldConfigurable) {
    return queueSelection(configurable);
  }

  @Override
  public ActionCallback onModifiedAdded(final Configurable colleague) {
    myTree.repaint();
    return new ActionCallback.Done();
  }

  @Override
  public ActionCallback onModifiedRemoved(final Configurable configurable) {
    myTree.repaint();
    return new ActionCallback.Done();
  }

  @Override
  public ActionCallback onErrorsChanged() {
    return new ActionCallback.Done();
  }

  public void processTextEvent(KeyEvent e) {
    myTree.processKeyEvent(e);
  }

  private class MyBuilder extends FilteringTreeBuilder {

    List<Object> myToExpandOnResetFilter;
    boolean myRefilteringNow;
    boolean myWasHoldingFilter;

    public MyBuilder(SimpleTreeStructure structure) {
      super(OptionsTree.this.myTree, myContext.getFilter(), structure, new WeightBasedComparator(false));
      myTree.addTreeExpansionListener(new TreeExpansionListener() {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
          invalidateExpansions();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
          invalidateExpansions();
        }
      });
    }

    private void invalidateExpansions() {
      if (!myRefilteringNow) {
        myToExpandOnResetFilter = null;
      }
    }

    @Override
    protected boolean isSelectable(final Object nodeObject) {
      return nodeObject instanceof EditorNode;
    }

    @Override
    public boolean isAutoExpandNode(final NodeDescriptor nodeDescriptor) {
      return myContext.isHoldingFilter();
    }

    @Override
    public boolean isToEnsureSelectionOnFocusGained() {
      return false;
    }

    @Override
    protected ActionCallback refilterNow(Object preferredSelection, boolean adjustSelection) {
      final List<Object> toRestore = new ArrayList<>();
      if (myContext.isHoldingFilter() && !myWasHoldingFilter && myToExpandOnResetFilter == null) {
        myToExpandOnResetFilter = myBuilder.getUi().getExpandedElements();
      }
      else if (!myContext.isHoldingFilter() && myWasHoldingFilter && myToExpandOnResetFilter != null) {
        toRestore.addAll(myToExpandOnResetFilter);
        myToExpandOnResetFilter = null;
      }

      myWasHoldingFilter = myContext.isHoldingFilter();

      ActionCallback result = super.refilterNow(preferredSelection, adjustSelection);
      myRefilteringNow = true;
      return result.doWhenDone(() -> {
        myRefilteringNow = false;
        if (!myContext.isHoldingFilter() && getSelectedElements().isEmpty()) {
          restoreExpandedState(toRestore);
        }
      });
    }

    private void restoreExpandedState(List<Object> toRestore) {
      TreePath[] selected = myTree.getSelectionPaths();
      if (selected == null) {
        selected = new TreePath[0];
      }

      List<TreePath> toCollapse = new ArrayList<>();

      for (int eachRow = 0; eachRow < myTree.getRowCount(); eachRow++) {
        if (!myTree.isExpanded(eachRow)) continue;

        TreePath eachVisiblePath = myTree.getPathForRow(eachRow);
        if (eachVisiblePath == null) continue;

        Object eachElement = myBuilder.getElementFor(eachVisiblePath.getLastPathComponent());
        if (toRestore.contains(eachElement)) continue;


        for (TreePath eachSelected : selected) {
          if (!eachVisiblePath.isDescendant(eachSelected)) {
            toCollapse.add(eachVisiblePath);
          }
        }
      }

      for (TreePath each : toCollapse) {
        myTree.collapsePath(each);
      }

    }
  }
}
