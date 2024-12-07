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
package consulo.execution.debug.impl.internal.frame;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.CompositeDisposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.ui.DebuggerSessionTabBase;
import consulo.execution.debug.impl.internal.ui.DebuggerUIUtil;
import consulo.execution.debug.impl.internal.ui.XDebugSessionTab;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.action.XWatchTransferable;
import consulo.execution.debug.impl.internal.ui.tree.node.*;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.XDebugSessionData;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.awt.dnd.DnDManager;
import consulo.ui.ex.awt.dnd.DnDNativeTarget;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ListenerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class XWatchesViewImpl extends XVariablesView implements DnDNativeTarget, XWatchesView {
  private WatchesRootNode myRootNode;

  private final CompositeDisposable myDisposables = new CompositeDisposable();
  private final boolean myWatchesInVariables;

  public XWatchesViewImpl(@Nonnull XDebugSessionImpl session, boolean watchesInVariables) {
    super(session);
    myWatchesInVariables = watchesInVariables;

    XDebuggerTree tree = getTree();
    createNewRootNode(null);

    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XNEW_WATCH, tree, myDisposables);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XREMOVE_WATCH, tree, myDisposables);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XCOPY_WATCH, tree, myDisposables);
    DebuggerUIUtil.registerActionOnComponent(XDebuggerActions.XEDIT_WATCH, tree, myDisposables);

    EmptyAction.registerWithShortcutSet(
      XDebuggerActions.XNEW_WATCH,
      CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.ADD),
      tree
    );
    EmptyAction.registerWithShortcutSet(
      XDebuggerActions.XREMOVE_WATCH,
      CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.REMOVE),
      tree
    );

    DnDManager.getInstance().registerTarget(this, tree);

    new AnAction() {
      @Override
      @RequiredUIAccess
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Object contents = CopyPasteManager.getInstance().getContents(XWatchTransferable.EXPRESSIONS_FLAVOR);
        if (contents instanceof List) {
          for (Object item : ((List)contents)) {
            if (item instanceof XExpression) {
              addWatchExpression(((XExpression)item), -1, true);
            }
          }
        }
      }
    }.registerCustomShortcutSet(CommonShortcuts.getPaste(), tree, myDisposables);

    ActionToolbar toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.DEBUGGER_TOOLBAR, DebuggerSessionTabBase.getCustomizedActionGroup(XDebuggerActions.WATCHES_TREE_TOOLBAR_GROUP), !myWatchesInVariables);
    JComponent component = toolbar.getComponent();

    component.setBorder(new CustomLineBorder(UIUtil.getBorderColor(), 0, 0, myWatchesInVariables ? 0 : 1, myWatchesInVariables ? 1 : 0));
    toolbar.setTargetComponent(tree);

    if (!myWatchesInVariables) {
      getTree().getEmptyText().setText(XDebuggerLocalize.debuggerNoWatches().get());
    }
    getPanel().add(component, myWatchesInVariables ? BorderLayout.WEST : BorderLayout.NORTH);

    installEditListeners();
  }

  private void installEditListeners() {
    final XDebuggerTree watchTree = getTree();
    final Alarm quitePeriod = new Alarm();
    final Alarm editAlarm = new Alarm();
    final ClickListener mouseListener = new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
        if (!SwingUtilities.isLeftMouseButton(event)
          || ((event.getModifiers() & (InputEvent.SHIFT_MASK | InputEvent.ALT_MASK | InputEvent.CTRL_MASK | InputEvent.META_MASK)) != 0)) {
          return false;
        }
        boolean sameRow = isAboveSelectedItem(event, watchTree);
        if (!sameRow || clickCount > 1) {
          editAlarm.cancelAllRequests();
          return false;
        }
        final AnAction editWatchAction = ActionManager.getInstance().getAction(XDebuggerActions.XEDIT_WATCH);
        Presentation presentation = editWatchAction.getTemplatePresentation().clone();
        DataContext context = DataManager.getInstance().getDataContext(watchTree);
        final AnActionEvent actionEvent =
          new AnActionEvent(null, context, "WATCH_TREE", presentation, ActionManager.getInstance(), 0);
        Runnable runnable = () -> editWatchAction.actionPerformed(actionEvent);
        if (editAlarm.isEmpty() && quitePeriod.isEmpty()) {
          editAlarm.addRequest(runnable, UIUtil.getMultiClickInterval());
        }
        else {
          editAlarm.cancelAllRequests();
        }
        return false;
      }
    };
    final ClickListener mouseEmptySpaceListener = new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        if (!isAboveSelectedItem(event, watchTree)) {
          myRootNode.addNewWatch();
          return true;
        }
        return false;
      }
    };
    ListenerUtil.addClickListener(watchTree, mouseListener);
    ListenerUtil.addClickListener(watchTree, mouseEmptySpaceListener);

    final FocusListener focusListener = new FocusListener() {
      @Override
      public void focusGained(@Nonnull FocusEvent e) {
        quitePeriod.addRequest(EmptyRunnable.getInstance(), UIUtil.getMultiClickInterval());
      }

      @Override
      public void focusLost(@Nonnull FocusEvent e) {
        editAlarm.cancelAllRequests();
      }
    };
    ListenerUtil.addFocusListener(watchTree, focusListener);

    final TreeSelectionListener selectionListener =
      e -> quitePeriod.addRequest(EmptyRunnable.getInstance(), UIUtil.getMultiClickInterval());
    watchTree.addTreeSelectionListener(selectionListener);
    myDisposables.add(() -> {
      ListenerUtil.removeClickListener(watchTree, mouseListener);
      ListenerUtil.removeClickListener(watchTree, mouseEmptySpaceListener);
      ListenerUtil.removeFocusListener(watchTree, focusListener);
      watchTree.removeTreeSelectionListener(selectionListener);
    });
  }

  @Override
  public void dispose() {
    Disposer.dispose(myDisposables);
    DnDManager.getInstance().unregisterTarget(this, getTree());
    super.dispose();
  }

  private static boolean isAboveSelectedItem(MouseEvent event, XDebuggerTree watchTree) {
    Rectangle bounds = watchTree.getRowBounds(watchTree.getLeadSelectionRow());
    if (bounds != null) {
      bounds.width = watchTree.getWidth();
      if (bounds.contains(event.getPoint())) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  private void executeAction(@Nonnull String watch) {
    AnAction action = ActionManager.getInstance().getAction(watch);
    Presentation presentation = action.getTemplatePresentation().clone();
    DataContext context = DataManager.getInstance().getDataContext(getTree());

    AnActionEvent actionEvent =
      new AnActionEvent(null, context, ActionPlaces.DEBUGGER_TOOLBAR, presentation, ActionManager.getInstance(), 0);
    action.actionPerformed(actionEvent);
  }

  @Override
  public void addWatchExpression(@Nonnull XExpression expression, int index, final boolean navigateToWatchNode) {
    XDebugSession session = XDebugView.getSession(getTree());
    myRootNode.addWatchExpression(session != null ? session.getCurrentStackFrame() : null, expression, index, navigateToWatchNode);
    updateSessionData();
    if (navigateToWatchNode && session != null) {
      XDebugSessionTab.showWatchesView((XDebugSessionImpl)session);
    }
  }

  public void computeWatches() {
    myRootNode.computeWatches();
  }

  @Override
  protected XValueContainerNode createNewRootNode(@Nullable XStackFrame stackFrame) {
    WatchesRootNode node = new WatchesRootNode(getTree(), this, getExpressions(), stackFrame, myWatchesInVariables);
    myRootNode = node;
    getTree().setRoot(node, false);
    return node;
  }

  @Override
  protected void addEmptyMessage(XValueContainerNode root) {
    if (myWatchesInVariables) {
      super.addEmptyMessage(root);
    }
  }

  @Nonnull
  private XExpression[] getExpressions() {
    XDebuggerTree tree = getTree();
    XDebugSession session = XDebugView.getSession(tree);
    XExpression[] expressions;
    if (session != null) {
      expressions = ((XDebugSessionImpl)session).getSessionData().getWatchExpressions();
    }
    else {
      XDebuggerTreeNode root = tree.getRoot();
      List<? extends WatchNode> current =
        root instanceof WatchesRootNode ? ((WatchesRootNode)tree.getRoot()).getWatchChildren() : Collections.emptyList();
      List<XExpression> list = new ArrayList<>();
      for (WatchNode child : current) {
        list.add(child.getExpression());
      }
      expressions = list.toArray(new XExpression[list.size()]);
    }
    return expressions;
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (XWatchesView.DATA_KEY == dataId) {
      return this;
    }
    return super.getData(dataId);
  }

  @Override
  public void removeWatches(List<? extends XDebuggerTreeNode> nodes) {
    List<? extends WatchNode> children = myRootNode.getWatchChildren();
    int minIndex = Integer.MAX_VALUE;
    List<XDebuggerTreeNode> toRemove = new ArrayList<>();
    for (XDebuggerTreeNode node : nodes) {
      @SuppressWarnings("SuspiciousMethodCalls") int index = children.indexOf(node);
      if (index != -1) {
        toRemove.add(node);
        minIndex = Math.min(minIndex, index);
      }
    }
    myRootNode.removeChildren(toRemove);

    List<? extends WatchNode> newChildren = myRootNode.getWatchChildren();
    if (!newChildren.isEmpty()) {
      WatchNode node = newChildren.get(Math.min(minIndex, newChildren.size() - 1));
      TreeUtil.selectNode(getTree(), node);
    }
    updateSessionData();
  }

  @Override
  public void removeAllWatches() {
    myRootNode.removeAllChildren();
    updateSessionData();
  }

  public void moveWatchUp(WatchNode node) {
    myRootNode.moveUp(node);
    updateSessionData();
  }

  public void moveWatchDown(WatchNode node) {
    myRootNode.moveDown(node);
    updateSessionData();
  }

  public void updateSessionData() {
    List<XExpression> watchExpressions = new ArrayList<>();
    List<? extends WatchNode> children = myRootNode.getWatchChildren();
    for (WatchNode child : children) {
      watchExpressions.add(child.getExpression());
    }
    XDebugSession session = XDebugView.getSession(getTree());
    XExpression[] expressions = watchExpressions.toArray(new XExpression[watchExpressions.size()]);
    if (session != null) {
      ((XDebugSessionImpl)session).setWatchExpressions(expressions);
    }
    else {
      XDebugSessionData data = XDebugView.getData(XDebugSessionData.DATA_KEY, getTree());
      if (data != null) {
        data.setWatchExpressions(expressions);
      }
    }
  }

  @Override
  public boolean update(final DnDEvent aEvent) {
    Object object = aEvent.getAttachedObject();
    boolean possible = false;
    if (object instanceof XValueNodeImpl[]) {
      possible = true;
      // do not add new watch if node is dragged to itself
      if (((XValueNodeImpl[])object).length == 1) {
        Point point = aEvent.getPoint();
        XDebuggerTree tree = getTree();
        TreePath path = tree.getClosestPathForLocation(point.x, point.y);
        if (path != null && path.getLastPathComponent() == ((XValueNodeImpl[])object)[0]) {
          // the same item is under pointer, filter out place below the tree
          Rectangle pathBounds = tree.getPathBounds(path);
          possible = pathBounds != null && pathBounds.y + pathBounds.height < point.y;
        }
      }
    }
    else if (object instanceof EventInfo) {
      possible = ((EventInfo)object).getTextForFlavor(DataFlavor.stringFlavor) != null;
    }

    aEvent.setDropPossible(possible, XDebuggerLocalize.xdebuggerDropTextAddToWatches().get());

    return true;
  }

  @Override
  public void drop(DnDEvent aEvent) {
    Object object = aEvent.getAttachedObject();
    if (object instanceof XValueNodeImpl[]) {
      final XValueNodeImpl[] nodes = (XValueNodeImpl[])object;
      for (XValueNodeImpl node : nodes) {
        node.getValueContainer().calculateEvaluationExpression().doWhenDone(expression -> {
          if (expression != null) {
            //noinspection ConstantConditions
            addWatchExpression(expression, -1, false);
          }
        });
      }
    }
    else if (object instanceof EventInfo) {
      String text = ((EventInfo)object).getTextForFlavor(DataFlavor.stringFlavor);
      if (text != null) {
        //noinspection ConstantConditions
        addWatchExpression(XExpression.fromText(text), -1, false);
      }
    }
  }

  @Override
  public void cleanUpOnLeave() {
  }

  @Override
  public void updateDraggedImage(final Image image, final Point dropPoint, final Point imageOffset) {
  }
}
