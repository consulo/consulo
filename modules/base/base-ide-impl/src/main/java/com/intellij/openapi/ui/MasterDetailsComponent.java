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
package com.intellij.openapi.ui;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.MasterDetails;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Function;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.options.ConfigurableUIMigrationUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.preferences.MasterDetailsConfigurable;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 29-May-2006
 */
public abstract class MasterDetailsComponent implements Configurable, MasterDetails {
  protected static final Logger LOG = Logger.getInstance(MasterDetailsComponent.class);

  protected MasterDetailsConfigurable myCurrentConfigurable;
  protected final JBSplitter mySplitter;

  private JScrollPane myMaster;

  protected final MasterDetailsState myState;

  protected Runnable TREE_UPDATER;

  {
    TREE_UPDATER = new Runnable() {
      @Override
      public void run() {
        final TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath == null) return;

        MyNode node = (MyNode)selectionPath.getLastPathComponent();
        if (node == null) return;

        myState.setLastEditedConfigurable(getNodePathString(node)); //survive after rename;
        myDetails.setText(node.getConfigurable().getBannerSlogan());
        ((DefaultTreeModel)myTree.getModel()).reload(node);
        fireItemsChangedExternally(MasterDetailsComponent.this);
      }
    };
  }

  protected MyNode myRoot = new MyRootNode();
  protected Tree myTree = new Tree();

  private final DetailsComponent myDetails = new DetailsComponent(false, false);
  protected JPanel myWholePanel;
  public JPanel myNorthPanel = new JPanel(new BorderLayout());

  private final List<ItemsChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private final Set<MasterDetailsConfigurable> myInitializedConfigurables = new HashSet<>();

  private boolean myHasDeletedItems;
  protected AutoScrollToSourceHandler myAutoScrollHandler;

  private boolean myToReInitWholePanel = true;

  private Disposable myDisposable;

  protected MasterDetailsComponent() {
    this(new MasterDetailsState());
  }

  protected MasterDetailsComponent(MasterDetailsState state) {
    myState = state;

    mySplitter = new OnePixelSplitter(false, .2f);
    mySplitter.setSplitterProportionKey("ProjectStructure.SecondLevelElements");
    mySplitter.setHonorComponentsMinimumSize(true);

    installAutoScroll();
    reInitWholePanelIfNeeded();
  }

  protected void reInitWholePanelIfNeeded() {
    if (!myToReInitWholePanel) return;

    myWholePanel = new JPanel(new BorderLayout()) {
      @Override
      public void addNotify() {
        super.addNotify();
        MasterDetailsComponent.this.addNotify();

        TreeModel m = myTree.getModel();
        if (m instanceof DefaultTreeModel) {
          DefaultTreeModel model = (DefaultTreeModel)m;
          for (int eachRow = 0; eachRow < myTree.getRowCount(); eachRow++) {
            TreePath eachPath = myTree.getPathForRow(eachRow);
            Object component = eachPath.getLastPathComponent();
            if (component instanceof TreeNode) {
              model.nodeChanged((TreeNode)component);
            }
          }
        }
      }
    };
    mySplitter.setHonorComponentsMinimumSize(true);
    myWholePanel.add(mySplitter, BorderLayout.CENTER);

    JPanel left = new JPanel(new BorderLayout()) {
      @Override
      public Dimension getMinimumSize() {
        final Dimension original = super.getMinimumSize();
        return new Dimension(Math.max(original.width, 100), original.height);
      }
    };

    left.add(myNorthPanel, BorderLayout.NORTH);
    myMaster = ScrollPaneFactory.createScrollPane(myTree, SideBorder.TOP);
    left.add(myMaster, BorderLayout.CENTER);
    mySplitter.setFirstComponent(left);

    final JPanel right = new JPanel(new BorderLayout());
    right.add(myDetails.getComponent(), BorderLayout.CENTER);

    mySplitter.setSecondComponent(right);

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myWholePanel);

    myToReInitWholePanel = false;
  }

  private void installAutoScroll() {
    myAutoScrollHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return isAutoScrollEnabled();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        //do nothing
      }

      @Override
      protected void scrollToSource(Component tree) {
        updateSelectionFromTree();
      }

      @Override
      protected boolean needToCheckFocus() {
        return false;
      }
    };
    myAutoScrollHandler.install(myTree);
  }

  protected void addNotify() {
    updateSelectionFromTree();
  }

  protected void updateSelectionFromTree() {
    TreePath[] treePaths = myTree.getSelectionPaths();
    if (treePaths != null) {
      List<MasterDetailsConfigurable> selectedConfigurables = new ArrayList<>();
      for (TreePath path : treePaths) {
        Object lastPathComponent = path.getLastPathComponent();
        if (lastPathComponent instanceof MyNode) {
          selectedConfigurables.add(((MyNode)lastPathComponent).getConfigurable());
        }
      }
      if (selectedConfigurables.size() > 1 && updateMultiSelection(selectedConfigurables)) {
        return;
      }
    }

    final TreePath path = myTree.getSelectionPath();
    if (path != null) {
      final Object lastPathComp = path.getLastPathComponent();
      if (!(lastPathComp instanceof MyNode)) return;
      final MyNode node = (MyNode)lastPathComp;
      setSelectedNode(node);
    }
    else {
      setSelectedNode(null);
    }
  }

  protected boolean updateMultiSelection(final List<MasterDetailsConfigurable> selectedConfigurables) {
    return false;
  }

  public Splitter getSplitter() {
    return mySplitter;
  }

  protected boolean isAutoScrollEnabled() {
    return true;
  }

  private void initToolbar() {
    final ArrayList<AnAction> actions = createActions(false);
    if (actions != null) {
      final DefaultActionGroup group = new DefaultActionGroup();
      for (AnAction action : actions) {
        if (action instanceof ActionGroupWithPreselection) {
          group.add(new MyActionGroupWrapper((ActionGroupWithPreselection)action));
        }
        else {
          group.add(action);
        }
      }
      ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
      toolbar.setTargetComponent(myTree);
      
      final JComponent component = toolbar.getComponent();
      myNorthPanel.add(component, BorderLayout.NORTH);
    }
  }

  public void addItemsChangeListener(ItemsChangeListener l) {
    myListeners.add(l);
  }

  protected Dimension getPanelPreferredSize() {
    return new Dimension(800, 600);
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable parentUIDisposable) {
    myDisposable = Disposable.newDisposable();

    reInitWholePanelIfNeeded();

    updateSelectionFromTree();

    final JPanel panel = new JPanel(new BorderLayout()) {
      @Override
      public Dimension getPreferredSize() {
        return getPanelPreferredSize();
      }
    };
    panel.add(myWholePanel, BorderLayout.CENTER);
    return panel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (myHasDeletedItems) return true;
    final boolean[] modified = new boolean[1];
    TreeUtil.traverseDepth(myRoot, node -> {
      if (node instanceof MyNode) {
        final MasterDetailsConfigurable configurable = ((MyNode)node).getConfigurable();
        if (isInitialized(configurable) && configurable.isModified()) {
          modified[0] = true;
          return false;
        }
      }
      return true;
    });
    return modified[0];
  }

  protected boolean isInitialized(final MasterDetailsConfigurable configurable) {
    return myInitializedConfigurables.contains(configurable);
  }

  @Override
  @RequiredUIAccess
  public void apply() throws ConfigurationException {
    processRemovedItems();
    final ConfigurationException[] ex = new ConfigurationException[1];
    TreeUtil.traverse(myRoot, node -> {
      if (node instanceof MyNode) {
        try {
          final MasterDetailsConfigurable configurable = ((MyNode)node).getConfigurable();
          if (isInitialized(configurable) && configurable.isModified()) {
            configurable.apply();
          }
        }
        catch (ConfigurationException e) {
          ex[0] = e;
          return false;
        }
      }
      return true;
    });
    if (ex[0] != null) {
      throw ex[0];
    }
    myHasDeletedItems = false;
  }

  protected abstract void processRemovedItems();

  protected abstract boolean wasObjectStored(Object editableObject);

  @RequiredUIAccess
  @Override
  public void reset() {
    loadComponentState();
    myHasDeletedItems = false;
    ((DefaultTreeModel)myTree.getModel()).reload();
    //myTree.requestFocus();
    myState.getProportions().restoreSplitterProportions(myWholePanel);

    initSelection();
  }

  protected void initSelection() {
    final Enumeration enumeration = myRoot.breadthFirstEnumeration();
    boolean selected = false;
    while (enumeration.hasMoreElements()) {
      final MyNode node = (MyNode)enumeration.nextElement();
      if (node instanceof MyRootNode) continue;
      final String path = getNodePathString(node);
      if (!selected && Comparing.strEqual(path, myState.getLastEditedConfigurable())) {
        TreeUtil.selectInTree(node, false, myTree);
        selected = true;
      }
    }
    if (!selected) {
      TreeUtil.selectFirstNode(myTree);
    }
    updateSelectionFromTree();
  }

  protected void loadComponentState() {
    final String key = getComponentStateKey();
    final MasterDetailsStateService stateService = getStateService();
    if (key != null && stateService != null) {
      final MasterDetailsState state = stateService.getComponentState(key, myState.getClass());
      if (state != null) {
        loadState(state);
      }
    }
  }

  private static String getNodePathString(final MyNode node) {
    StringBuilder path = new StringBuilder();
    MyNode current = node;
    while (current != null) {
      final Object userObject = current.getUserObject();
      if (!(userObject instanceof MasterDetailsConfigurable)) break;
      final String displayName = current.getDisplayName();
      if (StringUtil.isEmptyOrSpaces(displayName)) break;
      if (path.length() > 0) {
        path.append('|');
      }
      path.append(displayName);

      final TreeNode parent = current.getParent();
      if (!(parent instanceof MyNode)) break;
      current = (MyNode)parent;
    }
    return path.toString();
  }

  @Nullable
  @NonNls
  protected String getComponentStateKey() {
    return null;
  }

  @Nullable
  protected MasterDetailsStateService getStateService() {
    return null;
  }

  protected MasterDetailsState getState() {
    return myState;
  }

  protected void loadState(final MasterDetailsState object) {
    XmlSerializerUtil.copyBean(object, myState);
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myState.getProportions().saveSplitterProportions(myWholePanel);
    myAutoScrollHandler.cancelAllRequests();
    myDetails.disposeUIResources();
    myInitializedConfigurables.clear();
    clearChildren();
    final String key = getComponentStateKey();
    final MasterDetailsStateService stateService = getStateService();
    if (key != null && stateService != null) {
      stateService.setComponentState(key, getState());
    }
    myCurrentConfigurable = null;

    if (myDisposable != null) {
      Disposer.dispose(myDisposable);
      myDisposable = null;
    }
  }

  protected void clearChildren() {
    TreeUtil.traverseDepth(myRoot, node -> {
      if (node instanceof MyNode) {
        final MyNode treeNode = ((MyNode)node);
        treeNode.getConfigurable().disposeUIResources();
        if (!(treeNode instanceof MyRootNode)) {
          treeNode.setUserObject(null);
        }
      }
      return true;
    });
    myRoot.removeAllChildren();
  }

  @Nullable
  protected ArrayList<AnAction> createActions(final boolean fromPopup) {
    return null;
  }


  protected void initTree() {
    ((DefaultTreeModel)myTree.getModel()).setRoot(myRoot);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(myTree);
    TreeUtil.installActions(myTree);
    myTree.setCellRenderer(new ColoredTreeCellRenderer() {
      @Override
      public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof MyNode) {
          final MyNode node = ((MyNode)value);
          setIcon(node.getIcon(expanded));
          final Font font = UIUtil.getTreeFont();
          if (node.isDisplayInBold()) {
            setFont(font.deriveFont(Font.BOLD));
          }
          else {
            setFont(font.deriveFont(Font.PLAIN));
          }
          append(node.getDisplayName(), node.isDisplayInBold() ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
      }
    });
    initToolbar();
    ArrayList<AnAction> actions = createActions(true);
    if (actions != null) {
      final DefaultActionGroup group = new DefaultActionGroup();
      for (AnAction action : actions) {
        group.add(action);
      }
      actions = getAdditionalActions();
      if (actions != null) {
        group.addSeparator();
        for (AnAction action : actions) {
          group.add(action);
        }
      }
      PopupHandler.installPopupHandler(myTree, group, ActionPlaces.UNKNOWN, ActionManager.getInstance()); //popup should follow the selection
    }
  }

  @Nullable
  protected ArrayList<AnAction> getAdditionalActions() {
    return null;
  }

  public void fireItemsChangeListener(final Object editableObject) {
    for (ItemsChangeListener listener : myListeners) {
      listener.itemChanged(editableObject);
    }
  }

  private void fireItemsChangedExternally(UnnamedConfigurable configurable) {
    for (ItemsChangeListener listener : myListeners) {
      listener.itemsExternallyChanged(configurable);
    }
  }

  private void createUIComponents() {
    myTree = new Tree();
  }

  protected void addNode(MyNode nodeToAdd, MyNode parent) {
    parent.add(nodeToAdd);
    TreeUtil.sort(parent, getNodeComparator());
    ((DefaultTreeModel)myTree.getModel()).reload(parent);
  }

  protected Comparator<MyNode> getNodeComparator() {
    return (o1, o2) -> o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
  }

  public ActionCallback selectNodeInTree(final DefaultMutableTreeNode nodeToSelect) {
    return selectNodeInTree(nodeToSelect, true, false);
  }

  public ActionCallback selectNodeInTree(final DefaultMutableTreeNode nodeToSelect, boolean requestFocus) {
    return selectNodeInTree(nodeToSelect, true, requestFocus);
  }

  public ActionCallback selectNodeInTree(final DefaultMutableTreeNode nodeToSelect, boolean center, final boolean requestFocus) {
    if (requestFocus) {
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
    }
    if (nodeToSelect != null) {
      return TreeUtil.selectInTree(nodeToSelect, requestFocus, myTree, center);
    }
    else {
      return TreeUtil.selectFirstNode(myTree);
    }
  }

  @Nullable
  public Object getSelectedObject() {
    final TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath != null && selectionPath.getLastPathComponent() instanceof MyNode) {
      MyNode node = (MyNode)selectionPath.getLastPathComponent();
      final MasterDetailsConfigurable configurable = node.getConfigurable();
      LOG.assertTrue(configurable != null, "already disposed");
      return configurable.getEditableObject();
    }
    return null;
  }

  @Nullable
  public MasterDetailsConfigurable getSelectedConfigurable() {
    final TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath != null) {
      MyNode node = (MyNode)selectionPath.getLastPathComponent();
      final MasterDetailsConfigurable configurable = node.getConfigurable();
      LOG.assertTrue(configurable != null, "already disposed");
      return configurable;
    }
    return null;
  }

  public AsyncResult<TreeNode> selectNodeInTree(String displayName) {
    final MyNode nodeByName = findNodeByName(myRoot, displayName);
    if (nodeByName == null) {
      return AsyncResult.rejected();
    }
    AsyncResult<TreeNode> result = AsyncResult.undefined();
    selectNodeInTree(nodeByName, true).doWhenDone(() -> result.setDone(nodeByName)).doWhenRejected((Runnable)result::setRejected);
    return result;
  }

  public AsyncResult<TreeNode> selectNodeInTree(final Object object) {
    final MyNode nodeByName = findNodeByObject(myRoot, object);
    if (nodeByName == null) {
      return AsyncResult.rejected();
    }
    AsyncResult<TreeNode> result = AsyncResult.rejected();
    selectNodeInTree(nodeByName, true).doWhenDone(() -> {
      result.setDone(nodeByName);
    });
    return result;
  }

  @Nullable
  protected static MyNode findNodeByName(final TreeNode root, final String profileName) {
    if (profileName == null) return null; //do not suggest root node
    return findNodeByCondition(root, configurable -> Comparing.strEqual(profileName, configurable.getDisplayName()));
  }

  @Nullable
  public static MyNode findNodeByObject(final TreeNode root, final Object editableObject) {
    if (editableObject == null) return null; //do not suggest root node
    return findNodeByCondition(root, configurable -> Comparing.equal(editableObject, configurable.getEditableObject()));
  }

  protected static MyNode findNodeByCondition(final TreeNode root, final Condition<MasterDetailsConfigurable> condition) {
    final MyNode[] nodeToSelect = new MyNode[1];
    TreeUtil.traverseDepth(root, node -> {
      if (condition.value(((MyNode)node).getConfigurable())) {
        nodeToSelect[0] = (MyNode)node;
        return false;
      }
      return true;
    });
    return nodeToSelect[0];
  }

  protected void setSelectedNode(@Nullable MyNode node) {
    if (node != null) {
      myState.setLastEditedConfigurable(getNodePathString(node));
    }
    updateSelection(node != null ? node.getConfigurable() : null);
  }

  protected void updateSelection(@Nullable MasterDetailsConfigurable configurable) {
    myDetails.setText(configurable != null ? configurable.getBannerSlogan() : null);

    myCurrentConfigurable = configurable;

    if (configurable != null) {
      final JComponent comp = ConfigurableUIMigrationUtil.createComponent(configurable, myDisposable);
      if (comp == null) {
        setEmpty();
        LOG.error("createComponent() returned null. configurable=" + configurable);
      }
      else {
        myDetails.setContent(comp);
        ensureInitialized(configurable);
      }
    }
    else {
      setEmpty();
    }
  }

  public void ensureInitialized(MasterDetailsConfigurable configurable) {
    if (!isInitialized(configurable)) {
      configurable.initialize();
      configurable.reset();
      initializeConfigurable(configurable);
    }
  }

  private void setEmpty() {
    myDetails.setContent(null);
    myDetails.setEmptyContentText(getEmptySelectionString());
  }

  @Override
  public String getHelpTopic() {
    if (myCurrentConfigurable != null) {
      return myCurrentConfigurable.getHelpTopic();
    }
    return null;
  }

  @Nullable
  protected String getEmptySelectionString() {
    return null;
  }

  protected void initializeConfigurable(final MasterDetailsConfigurable configurable) {
    myInitializedConfigurables.add(configurable);
  }

  protected void checkApply(Set<MyNode> rootNodes, String prefix, String title) throws ConfigurationException {
    for (MyNode rootNode : rootNodes) {
      final Set<String> names = new HashSet<>();
      for (int i = 0; i < rootNode.getChildCount(); i++) {
        final MyNode node = (MyNode)rootNode.getChildAt(i);
        final MasterDetailsConfigurable scopeConfigurable = node.getConfigurable();
        final String name = scopeConfigurable.getDisplayName();
        if (name.trim().length() == 0) {
          selectNodeInTree(node);
          throw new ConfigurationException("Name should contain non-space characters");
        }
        if (names.contains(name)) {
          final MasterDetailsConfigurable selectedConfigurable = getSelectedConfigurable();
          if (selectedConfigurable == null || !Comparing.strEqual(selectedConfigurable.getDisplayName(), name)) {
            selectNodeInTree(node);
          }
          throw new ConfigurationException(CommonBundle.message("smth.already.exist.error.message", prefix, name), title);
        }
        names.add(name);
      }
    }
  }

  public Tree getTree() {
    return myTree;
  }

  protected void removePaths(final TreePath... paths) {
    MyNode parentNode = null;
    int idx = -1;
    for (TreePath path : paths) {
      final MyNode node = (MyNode)path.getLastPathComponent();
      final MasterDetailsConfigurable namedConfigurable = node.getConfigurable();
      final Object editableObject = namedConfigurable.getEditableObject();
      parentNode = (MyNode)node.getParent();
      idx = parentNode.getIndex(node);
      ((DefaultTreeModel)myTree.getModel()).removeNodeFromParent(node);
      myHasDeletedItems |= wasObjectStored(editableObject);
      fireItemsChangeListener(editableObject);
      onItemDeleted(editableObject);
      namedConfigurable.disposeUIResources();
    }

    if (paths.length > 0) {
      if (parentNode != null && idx != -1) {
        DefaultMutableTreeNode toSelect = null;
        if (idx < parentNode.getChildCount()) {
          toSelect = (DefaultMutableTreeNode)parentNode.getChildAt(idx);
        }
        else {
          if (idx > 0 && parentNode.getChildCount() > 0) {
            if (idx - 1 < parentNode.getChildCount()) {
              toSelect = (DefaultMutableTreeNode)parentNode.getChildAt(idx - 1);
            }
            else {
              toSelect = (DefaultMutableTreeNode)parentNode.getFirstChild();
            }
          }
          else {
            if (parentNode.isRoot() && myTree.isRootVisible()) {
              toSelect = parentNode;
            }
            else if (parentNode.getChildCount() > 0) {
              toSelect = (DefaultMutableTreeNode)parentNode.getFirstChild();
            }
          }
        }

        if (toSelect != null) {
          TreeUtil.selectInTree(toSelect, true, myTree);
        }
      }
      else {
        TreeUtil.selectFirstNode(myTree);
      }
    }
  }

  protected void onItemDeleted(Object item) {
  }

  protected class MyDeleteAction extends AnAction implements DumbAware {
    private final Condition<Object[]> myCondition;

    public MyDeleteAction() {
      this(Conditions.<Object[]>alwaysTrue());
    }

    public MyDeleteAction(Condition<Object[]> availableCondition) {
      super(CommonBundle.message("button.delete"), CommonBundle.message("button.delete"), IconUtil.getRemoveIcon());
      registerCustomShortcutSet(CommonShortcuts.DELETE, myTree);
      myCondition = availableCondition;
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      presentation.setEnabled(false);
      final TreePath[] selectionPath = myTree.getSelectionPaths();
      if (selectionPath != null) {
        Object[] nodes = ContainerUtil.map2Array(selectionPath, new Function<>() {
          @Override
          public Object fun(TreePath treePath) {
            return treePath.getLastPathComponent();
          }
        });
        if (!myCondition.value(nodes)) return;
        presentation.setEnabled(true);
      }
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      removePaths(myTree.getSelectionPaths());
    }
  }

  protected static Condition<Object[]> forAll(final Condition<Object> condition) {
    return new Condition<>() {
      @Override
      public boolean value(Object[] objects) {
        for (Object object : objects) {
          if (!condition.value(object)) return false;
        }
        return true;
      }
    };
  }

  public static class MyNode extends DefaultMutableTreeNode {
    private boolean myDisplayInBold;

    public MyNode(@Nonnull MasterDetailsConfigurable userObject) {
      super(userObject);
    }

    public MyNode(@Nonnull MasterDetailsConfigurable userObject, boolean displayInBold) {
      super(userObject);
      myDisplayInBold = displayInBold;
    }

    @Nonnull
    public String getDisplayName() {
      final Configurable configurable = ((Configurable)getUserObject());
      LOG.assertTrue(configurable != null, "Tree was already disposed");
      return configurable.getDisplayName();
    }

    public MasterDetailsConfigurable getConfigurable() {
      return (MasterDetailsConfigurable)getUserObject();
    }

    public boolean isDisplayInBold() {
      return myDisplayInBold;
    }

    public void setDisplayInBold(boolean displayInBold) {
      myDisplayInBold = displayInBold;
    }

    @Nullable
    public Image getIcon(boolean expanded) {
      // thanks to invokeLater() in TreeUtil.showAndSelect(), we can get calls to getIcon() after the tree has been disposed
      final MasterDetailsConfigurable configurable = getConfigurable();
      if (configurable != null) {
        return configurable.getIcon();
      }
      return null;
    }
  }

  @SuppressWarnings({"ConstantConditions"})
  protected static class MyRootNode extends MyNode {
    public MyRootNode() {
      super(new NamedConfigurable(false, null) {
        @Override
        public void setDisplayName(String name) {
        }

        @Override
        public Object getEditableObject() {
          return null;
        }

        @Override
        public String getBannerSlogan() {
          return null;
        }

        @Override
        public String getDisplayName() {
          return "";
        }

        @RequiredUIAccess
        @Override
        public JComponent createOptionsPanel(Disposable uiDisposable) {
          return null;
        }

        @RequiredUIAccess
        @Override
        public boolean isModified() {
          return false;
        }

        @RequiredUIAccess
        @Override
        public void apply() throws ConfigurationException {
        }

        @RequiredUIAccess
        @Override
        public void reset() {
        }

        @RequiredUIAccess
        @Override
        public void disposeUIResources() {
        }

      }, false);
    }
  }

  protected interface ItemsChangeListener {
    void itemChanged(@Nullable Object deletedItem);

    default void itemsExternallyChanged(UnnamedConfigurable configurable) {
    }
  }

  public interface ActionGroupWithPreselection {
    ActionGroup getActionGroup();

    int getDefaultIndex();
  }

  protected class MyActionGroupWrapper extends AnAction implements DumbAware {
    private ActionGroup myActionGroup;
    private ActionGroupWithPreselection myPreselection;

    public MyActionGroupWrapper(final ActionGroupWithPreselection actionGroup) {
      this(actionGroup.getActionGroup());
      myPreselection = actionGroup;
    }

    public MyActionGroupWrapper(final ActionGroup actionGroup) {
      super(actionGroup.getTemplatePresentation().getText(), actionGroup.getTemplatePresentation().getDescription(), actionGroup.getTemplatePresentation().getIcon());
      myActionGroup = actionGroup;
      registerCustomShortcutSet(actionGroup.getShortcutSet(), myTree);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      final JBPopupFactory popupFactory = JBPopupFactory.getInstance();
      final ListPopupStep step = popupFactory.createActionsStep(myActionGroup, e.getDataContext(), false, false, myActionGroup.getTemplatePresentation().getText(), myTree, true,
                                                                myPreselection != null ? myPreselection.getDefaultIndex() : 0, true);
      final ListPopup listPopup = popupFactory.createListPopup(step);
      listPopup.setHandleAutoSelectionBeforeShow(true);
      listPopup.showUnderneathOf(myNorthPanel);
    }
  }

  @Override
  public JComponent getToolbar() {
    myToReInitWholePanel = true;
    return myNorthPanel;
  }

  @Override
  public JComponent getMaster() {
    myToReInitWholePanel = true;
    return myMaster;
  }

  @Override
  public DetailsComponent getDetails() {
    myToReInitWholePanel = true;
    return myDetails;
  }

  @Override
  public void initUi() {
    createComponent();
  }
}
