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
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.ConfigurableWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.treeStructure.*;
import com.intellij.ui.treeStructure.filtered.FilteringTreeBuilder;
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeNode;
import consulo.ui.app.impl.settings.UnifiedConfigurableComparator;
import consulo.ui.color.ColorValue;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.image.ImageKey;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;

public class OptionsTree implements Disposable, OptionsEditorColleague {
  private class MyRenderer extends CellRendererPanel implements TreeCellRenderer {
    final NodeRenderer myTextLabel = new NodeRenderer();
    final JLabel myNodeIcon = new JLabel();
    final JLabel myProjectIcon = new JLabel();

    MyRenderer() {
      setLayout(new BorderLayout());
      add(BorderLayout.CENTER, myTextLabel);
      add(BorderLayout.WEST, myNodeIcon);
      add(BorderLayout.EAST, myProjectIcon);
      setBorder(JBUI.Borders.empty(1, 0, 3, 10));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      myTextLabel.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
      myTextLabel.setFont(tree.getFont());
      myProjectIcon.setIcon(null);
      
      Base baseNode = extractNode(value);
      if (baseNode instanceof ConfigurableNode) {
        Configurable configurable = baseNode.getConfigurable();

        if (OptionsEditor.isProjectConfigurable(configurable)) {
          myProjectIcon.setIcon(TargetAWT.to(PlatformIconGroup.generalProjectConfigurable()));
        }
      }
      return this;
    }
  }

  private final Configurable[] myConfigurables;
  FilteringTreeBuilder myBuilder;
  private Root myRoot;
  private final OptionsEditorContext myContext;

  private Map<Configurable, ConfigurableNode> myConfigurable2Node = new HashMap<>();

  private MergingUpdateQueue mySelection;

  private final JComponent myPanel;
  private final SimpleTree myTree;

  public OptionsTree(Configurable[] configurables, OptionsEditorContext context) {
    myConfigurables = configurables;
    myContext = context;

    myRoot = new Root();
    SimpleTreeStructure structure = new SimpleTreeStructure.Impl(myRoot);

    myTree = new SimpleTree() {
      @Override
      protected void configureUiHelper(TreeUIHelper helper) {
        // disable speed search
      }

      @Override
      public void updateUI() {
        super.updateUI();

        SwingUIDecorator.apply(SwingUIDecorator::decorateSidebarTree, this);
      }
    };

    TreeUtil.installActions(myTree);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setRootVisible(false);
    myTree.setRowHeight(JBUI.scale(24));
    myTree.setShowsRootHandles(true);
    myTree.setCellRenderer(new MyRenderer());
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

    myPanel = ScrollPaneFactory.createScrollPane(myTree, true);

    mySelection = new MergingUpdateQueue("OptionsTree", 150, false, myPanel, this, myPanel).setRestartTimerOnAdd(true);
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
  }

  @Nonnull
  public JComponent getComponent() {
    return myPanel;
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

  AsyncResult<Void> select(@Nullable Configurable configurable) {
    return queueSelection(configurable);
  }

  public void selectFirst() {
    if (myConfigurables.length > 0) {
      queueSelection(myConfigurables[0]);
    }
  }

  private Configurable myQueuedConfigurable;

  AsyncResult<Void> queueSelection(final Configurable configurable) {
    if (myBuilder.isSelectionBeingAdjusted()) {
      return AsyncResult.rejected();
    }

    final AsyncResult<Void> callback = AsyncResult.undefined();

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

            final ConfigurableNode configurableNode = myConfigurable2Node.get(configurable);
            FilteringTreeStructure.FilteringNode editorUiNode = myBuilder.getVisibleNodeFor(configurableNode);
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

  private void fireSelected(Configurable configurable, final AsyncResult<Void> callback) {
    myContext.fireSelected(configurable, this).doWhenProcessed(callback.createSetDoneRunnable());
  }

  public JTree getTree() {
    return myTree;
  }

  public List<Configurable> getPathToRoot(final Configurable configurable) {
    final ArrayList<Configurable> path = new ArrayList<>();

    ConfigurableNode eachNode = myConfigurable2Node.get(configurable);
    if (eachNode == null) return path;

    while (true) {
      path.add(eachNode.getConfigurable());
      final SimpleNode parent = eachNode.getParent();
      if (parent instanceof ConfigurableNode) {
        eachNode = (ConfigurableNode)parent;
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
  public <T extends UnnamedConfigurable> Pair<Configurable, T> findConfigurableInfo(Class<T> configurableClass) {
    for (Configurable configurable : myConfigurable2Node.keySet()) {
      T cast = ConfigurableWrapper.cast(configurable, configurableClass);
      if (cast != null) {
        return Pair.create(configurable, cast);
      }
    }
    return null;
  }

  @Nullable
  public <T extends Configurable> T findConfigurable(Class<T> configurableClass) {
    for (Configurable configurable : myConfigurable2Node.keySet()) {
      T cast = ConfigurableWrapper.cast(configurable, configurableClass);
      if (cast != null) {
        return cast;
      }
    }
    return null;
  }

  @Nullable
  public SearchableConfigurable findConfigurableById(@Nonnull String configurableId) {
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
    if (object instanceof TreePath) {
      TreePath path = (TreePath)object;
      object = path.getLastPathComponent();
    }
    if (object instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
      object = node.getUserObject();
    }
    if (object instanceof FilteringTreeStructure.FilteringNode) {
      FilteringTreeStructure.FilteringNode node = (FilteringTreeStructure.FilteringNode)object;
      object = node.getDelegate();
    }
    return object instanceof Base ? (Base)object : null;
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

    @Nonnull
    private List<ConfigurableNode> map(final Configurable[] configurables) {
      List<ConfigurableNode> result = new ArrayList<>();
      for (Configurable eachKid : configurables) {
        if (isInvisibleNode(eachKid)) {
          result.addAll(OptionsTree.this.buildChildren(eachKid, this));
        }
        else {
          result.add(new ConfigurableNode(this, eachKid));
        }
      }
      result.sort(UnifiedConfigurableComparator.INSTANCE);
      return result;
    }
  }

  private static boolean isInvisibleNode(final Configurable child) {
    return child instanceof SearchableConfigurable.Parent && !((SearchableConfigurable.Parent)child).isVisible();
  }

  private List<ConfigurableNode> buildChildren(final Configurable configurable, SimpleNode parent) {
    if (configurable instanceof Configurable.Composite) {
      final Configurable[] kids = ((Configurable.Composite)configurable).getConfigurables();
      final List<ConfigurableNode> result = new ArrayList<>(kids.length);
      for (Configurable child : kids) {
        if (isInvisibleNode(child)) {
          result.addAll(buildChildren(child, parent));
        }
        result.add(new ConfigurableNode(parent, child));
        myContext.registerKid(configurable, child);
      }

      result.sort(UnifiedConfigurableComparator.INSTANCE);
      return result;
    }
    else {
      return Collections.emptyList();
    }
  }

  private static final ConfigurableNode[] EMPTY_EN_ARRAY = new ConfigurableNode[0];

  class ConfigurableNode extends Base implements TreeNode<Configurable> {
    Configurable myConfigurable;

    ConfigurableNode(SimpleNode parent, Configurable configurable) {
      super(parent);
      myConfigurable = configurable;
      myConfigurable2Node.put(configurable, this);
    }

    @Override
    protected void update(PresentationData presentation) {
      super.update(presentation);

      String displayName = UnifiedConfigurableComparator.getConfigurableDisplayName(myConfigurable);
      if (getParent() instanceof Root) {
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
      else {
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    }

    @Override
    protected ConfigurableNode[] buildChildren() {
      List<ConfigurableNode> list = OptionsTree.this.buildChildren(myConfigurable, this);
      return list.isEmpty() ? EMPTY_EN_ARRAY : list.toArray(new ConfigurableNode[list.size()]);
    }

    @Override
    public boolean isAlwaysLeaf() {
      return !(myConfigurable instanceof Configurable.Composite) || ((Configurable.Composite)myConfigurable).getConfigurables().length == 0;
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
      return UnifiedConfigurableComparator.getConfigurableDisplayName(myConfigurable).replace("\n", " ");
    }

    @Override
    boolean isModified() {
      return myContext.getModified().contains(myConfigurable);
    }

    @Override
    boolean isError() {
      return myContext.getErrors().containsKey(myConfigurable);
    }

    @Override
    public void setRender(@Nonnull BiConsumer<Configurable, TextItemPresentation> render) {
      throw new UnsupportedOperationException("just stub, for comparator");
    }

    @Override
    public void setLeaf(boolean leaf) {
      throw new UnsupportedOperationException("just stub, for comparator");
    }

    @Override
    public boolean isLeaf() {
      throw new UnsupportedOperationException("just stub, for comparator");
    }

    @Nullable
    @Override
    public Configurable getValue() {
      return myConfigurable;
    }
  }

  @Override
  public void dispose() {
    myQueuedConfigurable = null;
  }

  @Override
  public AsyncResult<Void> onSelected(final Configurable configurable, final Configurable oldConfigurable) {
    return queueSelection(configurable);
  }

  @Override
  public AsyncResult<Void> onModifiedAdded(final Configurable colleague) {
    myTree.repaint();
    return AsyncResult.resolved();
  }

  @Override
  public AsyncResult<Void> onModifiedRemoved(final Configurable configurable) {
    myTree.repaint();
    return AsyncResult.resolved();
  }

  @Override
  public AsyncResult<Void> onErrorsChanged() {
    return AsyncResult.resolved();
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
      return nodeObject instanceof ConfigurableNode;
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
    protected Promise<?> refilterNow(Object preferredSelection, boolean adjustSelection) {
      final List<Object> toRestore = new ArrayList<>();
      if (myContext.isHoldingFilter() && !myWasHoldingFilter && myToExpandOnResetFilter == null) {
        myToExpandOnResetFilter = myBuilder.getUi().getExpandedElements();
      }
      else if (!myContext.isHoldingFilter() && myWasHoldingFilter && myToExpandOnResetFilter != null) {
        toRestore.addAll(myToExpandOnResetFilter);
        myToExpandOnResetFilter = null;
      }

      myWasHoldingFilter = myContext.isHoldingFilter();

      Promise<?> result = super.refilterNow(preferredSelection, adjustSelection);
      myRefilteringNow = true;
      return result.onSuccess((c) -> {
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
