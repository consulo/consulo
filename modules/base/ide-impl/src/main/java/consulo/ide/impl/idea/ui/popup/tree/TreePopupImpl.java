// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.tree;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ide.impl.idea.ui.popup.NextStepHandler;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.ide.impl.idea.ui.treeStructure.filtered.FilteringTreeBuilder;
import consulo.ide.impl.idea.ui.treeStructure.filtered.FilteringTreeStructure;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.SimpleTree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.TreePopup;
import consulo.ui.ex.popup.TreePopupStep;
import consulo.ui.ex.tree.AlphaComparator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TreePopupImpl extends WizardPopup implements TreePopup, NextStepHandler {
  private static final Logger LOG = Logger.getInstance(TreePopupImpl.class);
  private MyTree myWizardTree;

  private MouseMotionListener myMouseMotionListener;
  private MouseListener myMouseListener;

  private final List<TreePath> mySavedExpanded = new ArrayList<>();
  private TreePath mySavedSelected;

  private TreePath myShowingChildPath;
  private TreePath myPendingChildPath;
  private FilteringTreeBuilder myBuilder;

  public TreePopupImpl(@Nullable Project project, @Nullable JBPopup parent, @Nonnull TreePopupStep<Object> aStep, @Nullable Object parentValue) {
    super(project, parent, aStep);
    setParentValue(parentValue);
  }

  @Override
  protected JComponent createContent() {
    myWizardTree = new MyTree();
    myWizardTree.getAccessibleContext().setAccessibleName("WizardTree");
    myBuilder = new FilteringTreeBuilder(myWizardTree, this, getTreeStep().getStructure(), AlphaComparator.INSTANCE) {
      @Override
      protected boolean isSelectable(final Object nodeObject) {
        return getTreeStep().isSelectable(nodeObject, nodeObject);
      }
    };

    myBuilder.updateFromRoot();

    myWizardTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    Action action = myWizardTree.getActionMap().get("toggleSelectionPreserveAnchor");
    if (action != null) {
      action.setEnabled(false);
    }

    myWizardTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          toggleExpansion(myWizardTree.getAnchorSelectionPath());
        }
      }
    });

    myWizardTree.setRootVisible(getTreeStep().isRootVisible());
    myWizardTree.setShowsRootHandles(true);

    ToolTipManager.sharedInstance().registerComponent(myWizardTree);
    myWizardTree.setCellRenderer(new MyRenderer());

    myMouseMotionListener = new MyMouseMotionListener();
    myMouseListener = new MyMouseListener();

    registerAction("select", KeyEvent.VK_ENTER, 0, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleSelect(true, null);
      }
    });

    registerAction("toggleExpansion", KeyEvent.VK_SPACE, 0, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        toggleExpansion(myWizardTree.getSelectionPath());
      }
    });

    final Action oldExpandAction = getActionMap().get("selectChild");
    getActionMap().put("selectChild", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final TreePath path = myWizardTree.getSelectionPath();
        if (path != null && 0 == myWizardTree.getModel().getChildCount(path.getLastPathComponent())) {
          handleSelect(false, null);
          return;
        }
        oldExpandAction.actionPerformed(e);
      }
    });

    final Action oldCollapseAction = getActionMap().get("selectParent");
    getActionMap().put("selectParent", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final TreePath path = myWizardTree.getSelectionPath();
        if (shouldHidePopup(path)) {
          goBack();
          return;
        }
        oldCollapseAction.actionPerformed(e);
      }
    });

    return myWizardTree;
  }

  private boolean shouldHidePopup(TreePath path) {
    if (getParent() == null) return false;
    if (path == null) return false;
    if (!myWizardTree.isCollapsed(path)) return false;
    if (myWizardTree.isRootVisible()) {
      return path.getPathCount() == 1;
    }
    return path.getPathCount() == 2;
  }

  @Override
  protected ActionMap getActionMap() {
    return myWizardTree.getActionMap();
  }

  @Override
  protected InputMap getInputMap() {
    return myWizardTree.getInputMap();
  }

  private void addListeners() {
    myWizardTree.addMouseMotionListener(myMouseMotionListener);
    myWizardTree.addMouseListener(myMouseListener);
  }

  @Override
  public void dispose() {
    mySavedExpanded.clear();
    final Enumeration<TreePath> expanded = myWizardTree.getExpandedDescendants(new TreePath(myWizardTree.getModel().getRoot()));
    if (expanded != null) {
      while (expanded.hasMoreElements()) {
        mySavedExpanded.add(expanded.nextElement());
      }
    }
    mySavedSelected = myWizardTree.getSelectionPath();

    myWizardTree.removeMouseMotionListener(myMouseMotionListener);
    myWizardTree.removeMouseListener(myMouseListener);
    super.dispose();
  }

  @Override
  protected boolean beforeShow() {
    addListeners();

    expandAll();

    collapseAll();

    restoreExpanded();
    if (mySavedSelected != null) {
      myWizardTree.setSelectionPath(mySavedSelected);
    }

    return super.beforeShow();
  }

  @Override
  protected void afterShow() {
    selectFirstSelectableItem();
  }

  // TODO: not-tested code:
  private void selectFirstSelectableItem() {
    for (int i = 0; i < myWizardTree.getRowCount(); i++) {
      TreePath path = myWizardTree.getPathForRow(i);
      if (getTreeStep().isSelectable(path.getLastPathComponent(), extractUserObject(path.getLastPathComponent()))) {
        myWizardTree.setSelectionPath(path);
        break;
      }
    }
  }


  private void restoreExpanded() {
    if (mySavedExpanded.isEmpty()) {
      expandAll();
      return;
    }

    for (TreePath each : mySavedExpanded) {
      myWizardTree.expandPath(each);
    }
  }

  private void expandAll() {
    for (int i = 0; i < myWizardTree.getRowCount(); i++) {
      myWizardTree.expandRow(i);
    }
  }

  private void collapseAll() {
    int row = myWizardTree.getRowCount() - 1;
    while (row > 0) {
      myWizardTree.collapseRow(row);
      row--;
    }
  }

  private TreePopupStep getTreeStep() {
    return (TreePopupStep)myStep;
  }

  private class MyMouseMotionListener extends MouseMotionAdapter {
    private Point myLastMouseLocation;

    private boolean isMouseMoved(Point location) {
      if (myLastMouseLocation == null) {
        myLastMouseLocation = location;
        return false;
      }
      return !myLastMouseLocation.equals(location);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if (!isMouseMoved(e.getLocationOnScreen())) return;

      final TreePath path = getPath(e);
      if (path != null) {
        myWizardTree.setSelectionPath(path);
        notifyParentOnChildSelection();
        if (getTreeStep().isSelectable(path.getLastPathComponent(), extractUserObject(path.getLastPathComponent()))) {
          myWizardTree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          if (myPendingChildPath == null || !myPendingChildPath.equals(path)) {
            myPendingChildPath = path;
            restartTimer();
          }
          return;
        }
      }
      myWizardTree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

  }

  private TreePath getPath(MouseEvent e) {
    return myWizardTree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
  }

  private class MyMouseListener extends MouseAdapter {

    @Override
    public void mousePressed(MouseEvent e) {
      final TreePath path = getPath(e);
      if (path == null) {
        return;
      }

      if (e.getButton() != MouseEvent.BUTTON1) {
        return;
      }

      final Object selected = path.getLastPathComponent();

      if (getTreeStep().isSelectable(selected, extractUserObject(selected))) {
        handleSelect(true, e);
      }
      else {
        if (!TreeUtil.isLocationInExpandControl(myWizardTree, path, e.getX(), e.getY())) {
          toggleExpansion(path);
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

  }

  private void toggleExpansion(TreePath path) {
    if (path == null) {
      return;
    }

    if (getTreeStep().isSelectable(path.getLastPathComponent(), extractUserObject(path.getLastPathComponent()))) {
      if (myWizardTree.isExpanded(path)) {
        myWizardTree.collapsePath(path);
      }
      else {
        myWizardTree.expandPath(path);
      }
    }
  }

  private void handleSelect(boolean handleFinalChoices, MouseEvent e) {
    final boolean pathIsAlreadySelected = myShowingChildPath != null && myShowingChildPath.equals(myWizardTree.getSelectionPath());
    if (pathIsAlreadySelected) return;

    myPendingChildPath = null;

    Object selected = myWizardTree.getLastSelectedPathComponent();
    if (selected != null) {
      final Object userObject = extractUserObject(selected);
      if (getTreeStep().isSelectable(selected, userObject)) {
        disposeChildren();

        final boolean hasNextStep = myStep.hasSubstep(userObject);
        if (!hasNextStep && !handleFinalChoices) {
          myShowingChildPath = null;
          return;
        }

        AtomicBoolean insideOnChosen = new AtomicBoolean(true);
        ApplicationManager.getApplication().invokeLater(() -> {
          if (insideOnChosen.get()) {
            LOG.error("Showing dialogs from popup onChosen can result in focus issues. Please put the handler into BaseStep.doFinalStep or PopupStep.getFinalRunnable.");
          }
        }, IdeaModalityState.any());

        final PopupStep queriedStep;
        try {
          queriedStep = myStep.onChosen(userObject, handleFinalChoices);
        }
        finally {
          insideOnChosen.set(false);
        }
        if (queriedStep == PopupStep.FINAL_CHOICE || !hasNextStep) {
          setFinalRunnable(myStep.getFinalRunnable());
          setOk(true);
          disposeAllParents(e);
        }
        else {
          myShowingChildPath = myWizardTree.getSelectionPath();
          handleNextStep(queriedStep, myShowingChildPath);
          myShowingChildPath = null;
        }
      }
    }
  }

  @Override
  public void handleNextStep(PopupStep nextStep, Object parentValue) {
    final Rectangle pathBounds = myWizardTree.getPathBounds(myWizardTree.getSelectionPath());
    final Point point = new RelativePoint(myWizardTree, new Point(getContent().getWidth() + 2, (int)pathBounds.getY())).getScreenPoint();
    myChild = createPopup(this, nextStep, parentValue);
    myChild.show(getContent(), point.x - STEP_X_PADDING, point.y, true);
  }

  private class MyRenderer extends NodeRenderer {

    @RequiredUIAccess
    @Override
    public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      final boolean shouldPaintSelected = (getTreeStep().isSelectable(value, extractUserObject(value)) && selected) || (getTreeStep().isSelectable(value, extractUserObject(value)) && hasFocus);
      final boolean shouldPaintFocus = !getTreeStep().isSelectable(value, extractUserObject(value)) && selected || shouldPaintSelected || hasFocus;

      super.customizeCellRenderer(tree, value, shouldPaintSelected, expanded, leaf, row, shouldPaintFocus);
    }
  }

  @Override
  protected void process(KeyEvent aEvent) {
    myWizardTree.processKeyEvent(aEvent);
  }

  private Object extractUserObject(Object aNode) {
    Object object = ((DefaultMutableTreeNode)aNode).getUserObject();
    if (object instanceof FilteringTreeStructure.FilteringNode) {
      return ((FilteringTreeStructure.FilteringNode)object).getDelegate();
    }
    return object;
  }

  private class MyTree extends SimpleTree {
    @Override
    public void processKeyEvent(KeyEvent e) {
      e.setSource(this);
      super.processKeyEvent(e);
    }

    @Override
    public Dimension getPreferredSize() {
      final Dimension pref = super.getPreferredSize();
      return new Dimension(pref.width + 10, pref.height);
    }

    @Override
    protected void paintChildren(Graphics g) {
      super.paintChildren(g);

      Rectangle visibleRect = getVisibleRect();
      int rowForLocation = getClosestRowForLocation(0, visibleRect.y);
      int limit = rowForLocation + TreeUtil.getVisibleRowCount(this) + 1;
      for (int i = rowForLocation; i < limit; i++) {
        final TreePath eachPath = getPathForRow(i);
        if (eachPath == null) continue;

        final Object lastPathComponent = eachPath.getLastPathComponent();
        final boolean hasNextStep = getTreeStep().hasSubstep(extractUserObject(lastPathComponent));
        if (!hasNextStep) continue;

        Icon icon = UIUtil.getMenuArrowIcon(isPathSelected(eachPath));
        final Rectangle rec = getPathBounds(eachPath);
        int x = getSize().width - icon.getIconWidth() - 1;
        int y = rec.y + (rec.height - icon.getIconWidth()) / 2;
        icon.paintIcon(this, g, x, y);
      }
    }
  }

  @Override
  protected void onAutoSelectionTimer() {
    handleSelect(false, null);
  }

  @Override
  protected JComponent getPreferredFocusableComponent() {
    return myWizardTree;
  }

  @Override
  protected void onSpeedSearchPatternChanged() {
    myBuilder.refilterAsync();
  }

  @Override
  public void onChildSelectedFor(Object value) {
    TreePath path = (TreePath)value;
    if (myWizardTree.getSelectionPath() != path) {
      myWizardTree.setSelectionPath(path);
    }
  }

  @Override
  public boolean isModalContext() {
    return true;
  }

}
