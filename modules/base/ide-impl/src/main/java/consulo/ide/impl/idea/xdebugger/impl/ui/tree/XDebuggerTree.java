/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.ui.tree;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.configuration.RemoteRunProfile;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XDebuggerTreeNodeHyperlink;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeLinkMouseListener;
import consulo.ide.impl.idea.util.containers.Convertor;
import consulo.ide.impl.idea.util.ui.TextTransferable;
import consulo.ide.impl.idea.xdebugger.impl.ui.DebuggerUIUtil;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.*;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.util.SingleAlarm;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.List;

/**
 * @author nik
 */
public class XDebuggerTree extends DnDAwareTree implements DataProvider, Disposable {
  private final ComponentListener myMoveListener = new ComponentAdapter() {
    @Override
    public void componentMoved(ComponentEvent e) {
      repaint(); // needed to repaint links in cell renderer on horizontal scrolling
    }
  };

  private static final Key<XDebuggerTree> XDEBUGGER_TREE_KEY = Key.create("xdebugger.tree");
  private final SingleAlarm myAlarm = new SingleAlarm(new Runnable() {
    @Override
    public void run() {
      final Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
      if (editor != null) {
        editor.getContentComponent().revalidate();
        editor.getContentComponent().repaint();
      }
    }
  }, 100, this);

  private static final Convertor<TreePath, String> SPEED_SEARCH_CONVERTER = o -> {
    String text = null;
    if (o != null) {
      final Object node = o.getLastPathComponent();
      if (node instanceof XDebuggerTreeNode) {
        text = ((XDebuggerTreeNode)node).getText().toString();
      }
    }
    return StringUtil.notNullize(text);
  };

  private static final TransferHandler DEFAULT_TRANSFER_HANDLER = new TransferHandler() {
    @Override
    protected Transferable createTransferable(JComponent c) {
      if (!(c instanceof XDebuggerTree)) {
        return null;
      }
      XDebuggerTree tree = (XDebuggerTree)c;
      //noinspection deprecation
      TreePath[] selectedPaths = tree.getSelectionPaths();
      if (selectedPaths == null || selectedPaths.length == 0) {
        return null;
      }

      StringBuilder plainBuf = new StringBuilder();
      StringBuilder htmlBuf = new StringBuilder();
      htmlBuf.append("<html>\n<body>\n<ul>\n");
      ColoredStringBuilder coloredTextContainer = new ColoredStringBuilder();
      for (TreePath path : selectedPaths) {
        htmlBuf.append("  <li>");
        Object node = path.getLastPathComponent();
        if (node != null) {
          if (node instanceof XDebuggerTreeNode) {
            ((XDebuggerTreeNode)node).appendToComponent(coloredTextContainer);
            coloredTextContainer.appendTo(plainBuf, htmlBuf);
          }
          else {
            String text = node.toString();
            plainBuf.append(text);
            htmlBuf.append(text);
          }
        }
        plainBuf.append('\n');
        htmlBuf.append("</li>\n");
      }

      // remove the last newline
      plainBuf.setLength(plainBuf.length() - 1);
      htmlBuf.append("</ul>\n</body>\n</html>");
      return new TextTransferable(htmlBuf.toString(), plainBuf.toString());
    }

    @Override
    public int getSourceActions(JComponent c) {
      return COPY;
    }
  };

  private final DefaultTreeModel myTreeModel;
  private final Project myProject;
  private final XDebuggerEditorsProvider myEditorsProvider;
  private XSourcePosition mySourcePosition;
  private final List<XDebuggerTreeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final XValueMarkers<?,?> myValueMarkers;

  public XDebuggerTree(final @Nonnull Project project,
                       final @Nonnull XDebuggerEditorsProvider editorsProvider,
                       final @Nullable XSourcePosition sourcePosition,
                       final @Nonnull String popupActionGroupId, @Nullable XValueMarkers<?, ?> valueMarkers) {
    myValueMarkers = valueMarkers;
    myProject = project;
    myEditorsProvider = editorsProvider;
    mySourcePosition = sourcePosition;
    myTreeModel = new DefaultTreeModel(null);
    setModel(myTreeModel);
    setCellRenderer(new XDebuggerTreeRenderer());
    new TreeLinkMouseListener(new XDebuggerTreeRenderer()) {
      @Override
      protected boolean doCacheLastNode() {
        return false;
      }

      @Override
      protected void handleTagClick(@Nullable Object tag, @Nonnull MouseEvent event) {
        if (tag instanceof XDebuggerTreeNodeHyperlink) {
          ((XDebuggerTreeNodeHyperlink)tag).onClick(event);
        }
      }
    }.installOn(this);
    setRootVisible(false);
    setShowsRootHandles(true);

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        return expandIfEllipsis();
      }
    }.installOn(this);

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_RIGHT) {
          expandIfEllipsis();
        }
      }
    });

    if (Boolean.valueOf(System.getProperty("xdebugger.variablesView.rss"))) {
      new XDebuggerTreeSpeedSearch(this, SPEED_SEARCH_CONVERTER);
    }
    else {
      new TreeSpeedSearch(this, SPEED_SEARCH_CONVERTER);
    }

    final ActionManager actionManager = ActionManager.getInstance();
    addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(final Component comp, final int x, final int y) {
        ActionGroup group = (ActionGroup)actionManager.getAction(popupActionGroupId);
        actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group).getComponent().show(comp, x, y);
      }
    });
    registerShortcuts();

    setTransferHandler(DEFAULT_TRANSFER_HANDLER);

    addComponentListener(myMoveListener);
  }

  public void updateEditor() {
    myAlarm.cancelAndRequest();
  }

  public boolean isUnderRemoteDebug() {
    DataContext context = DataManager.getInstance().getDataContext(this);
    ExecutionEnvironment env = context.getData(ExecutionEnvironment.KEY);
    if (env != null && env.getRunProfile() instanceof RemoteRunProfile) {
      return true;
    }
    return false;
  }

  private boolean expandIfEllipsis() {
    MessageTreeNode[] treeNodes = getSelectedNodes(MessageTreeNode.class, null);
    if (treeNodes.length == 1) {
      MessageTreeNode node = treeNodes[0];
      if (node.isEllipsis()) {
        TreeNode parent = node.getParent();
        if (parent instanceof XValueContainerNode) {
          ((XValueContainerNode)parent).startComputingChildren();
          return true;
        }
      }
    }
    return false;
  }

  public void addTreeListener(@Nonnull XDebuggerTreeListener listener) {
    myListeners.add(listener);
  }

  public void removeTreeListener(@Nonnull XDebuggerTreeListener listener) {
    myListeners.remove(listener);
  }

  public void setRoot(XDebuggerTreeNode root, final boolean rootVisible) {
    setRootVisible(rootVisible);
    myTreeModel.setRoot(root);
  }

  public XDebuggerTreeNode getRoot() {
    return (XDebuggerTreeNode)myTreeModel.getRoot();
  }

  @Nullable
  public XSourcePosition getSourcePosition() {
    return mySourcePosition;
  }

  public void setSourcePosition(final @Nullable XSourcePosition sourcePosition) {
    mySourcePosition = sourcePosition;
  }

  @Nonnull
  public XDebuggerEditorsProvider getEditorsProvider() {
    return myEditorsProvider;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nullable
  public XValueMarkers<?, ?> getValueMarkers() {
    return myValueMarkers;
  }

  public DefaultTreeModel getTreeModel() {
    return myTreeModel;
  }

  @Override
  @Nullable
  public Object getData(@Nonnull @NonNls final Key dataId) {
    if (XDEBUGGER_TREE_KEY == dataId) {
      return this;
    }
    if (PlatformDataKeys.PREDEFINED_TEXT == dataId) {
      XValueNodeImpl[] selectedNodes = getSelectedNodes(XValueNodeImpl.class, null);
      if (selectedNodes.length == 1 && selectedNodes[0].getFullValueEvaluator() == null) {
        return DebuggerUIUtil.getNodeRawValue(selectedNodes[0]);
      }
    }
    return null;
  }

  public void rebuildAndRestore(final XDebuggerTreeState treeState) {
    Object rootNode = myTreeModel.getRoot();
    if (rootNode instanceof XDebuggerTreeNode) {
      ((XDebuggerTreeNode)rootNode).clearChildren();
      if (isRootVisible() && rootNode instanceof XValueNodeImpl) {
        ((XValueNodeImpl)rootNode).getValueContainer().computePresentation((XValueNode)rootNode, XValuePlace.TREE);
      }
      treeState.restoreState(this);
      repaint();
    }
  }

  public void childrenLoaded(final @Nonnull XDebuggerTreeNode node,
                             final @Nonnull List<XValueContainerNode<?>> children,
                             final boolean last) {
    for (XDebuggerTreeListener listener : myListeners) {
      listener.childrenLoaded(node, children, last);
    }
  }

  public void nodeLoaded(final @Nonnull RestorableStateNode node, final @Nonnull String name) {
    for (XDebuggerTreeListener listener : myListeners) {
      listener.nodeLoaded(node, name);
    }
  }

  public void markNodesObsolete() {
    Object root = myTreeModel.getRoot();
    if (root instanceof XValueContainerNode<?>) {
      markNodesObsolete((XValueContainerNode<?>)root);
    }
  }

  @Override
  public void dispose() {
    // clear all possible inner fields that may still have links to debugger objects
    setModel(null);
    myTreeModel.setRoot(null);
    setCellRenderer(null);
    UIUtil.dispose(this);
    setLeadSelectionPath(null);
    setAnchorSelectionPath(null);
    removeComponentListener(myMoveListener);
    myListeners.clear();
  }

  private void registerShortcuts() {
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.SET_VALUE, this, this);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.COPY_VALUE, this, this);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.JUMP_TO_SOURCE, this, this);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.JUMP_TO_TYPE_SOURCE, this, this);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.MARK_OBJECT, this, this);
  }

  private static void markNodesObsolete(final XValueContainerNode<?> node) {
    node.setObsolete();
    List<? extends XValueContainerNode<?>> loadedChildren = node.getLoadedChildren();
    for (XValueContainerNode<?> child : loadedChildren) {
      markNodesObsolete(child);
    }
  }

  @Nullable
  public static XDebuggerTree getTree(final AnActionEvent e) {
    return e.getData(XDEBUGGER_TREE_KEY);
  }

  @Nullable
  public static XDebuggerTree getTree(DataContext context) {
    return context.getData(XDEBUGGER_TREE_KEY);
  }

  public void selectNodeOnLoad(final Condition<TreeNode> nodeFilter) {
    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(@Nonnull RestorableStateNode node, String name) {
        if (nodeFilter.value(node)) {
          setSelectionPath(node.getPath());
          removeTreeListener(this); // remove the listener on first match
        }
      }

      @Override
      public void childrenLoaded(@Nonnull XDebuggerTreeNode node, @Nonnull List<XValueContainerNode<?>> children, boolean last) {}
    });
  }

  public void expandNodesOnLoad(final Condition<TreeNode> nodeFilter) {
    addTreeListener(new XDebuggerTreeListener() {
      @Override
      public void nodeLoaded(@Nonnull RestorableStateNode node, String name) {
        if (nodeFilter.value(node) && !node.isLeaf()) {
          // cause children computing
          node.getChildCount();
        }
      }

      @Override
      public void childrenLoaded(@Nonnull XDebuggerTreeNode node, @Nonnull List<XValueContainerNode<?>> children, boolean last) {
        if (nodeFilter.value(node)) {
          expandPath(node.getPath());
        }
      }
    });
  }
}
