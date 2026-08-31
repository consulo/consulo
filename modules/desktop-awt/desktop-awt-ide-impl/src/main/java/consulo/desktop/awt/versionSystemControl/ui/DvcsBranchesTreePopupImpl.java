// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.versionSystemControl.ui;

import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.ide.impl.idea.ui.popup.NextStepHandler;
import consulo.ide.impl.idea.ui.popup.WizardPopup;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.ProjectWindowStateService;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SeparatorWithText;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreeModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopup;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupModel;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsBranchesTreePopupStepBase;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 */
public class DvcsBranchesTreePopupImpl<T extends Repository> extends WizardPopup implements NextStepHandler, DvcsBranchesTreePopup<T> {
    private static final String DIMENSION_SERVICE_KEY = "Git.Branch.Popup";
    private static final int DRAG_AREA_HEIGHT = 8;
    private static final int DRAG_AREA_TOP_AND_BOTTOM_BORDER = 2;
    private static final int MIN_HEIGHT = 312;
    private final DvcsBranchesTreePopupModel<T> myModel;

    private MyTree myTree;

    private boolean myUserResized;

    private boolean myUserMoved;

    private MouseMotionListener myMouseMotionListener;
    private MouseListener myMouseListener;

    private TreePath myShowingChildPath;
    private TreePath myPendingChildPath;

    public DvcsBranchesTreePopupImpl(@Nullable Project project,
                                     DvcsBranchesTreePopupModel<T> model) {
        myModel = model;

        DvcsBranchesTreeModel treeModel = model.createTreeModel();

        DvcsBranchesTreePopupStepBase treePopupStep = model.createTreePopupStep(treeModel);

        super(project, null, treePopupStep);

        setParentValue(null);
        setSpeedSearchAlwaysShown();

        setDimensionServiceKey(DIMENSION_SERVICE_KEY);
        setMinimumSize(JBUI.size(375, MIN_HEIGHT));
        getContent().setPreferredSize(JBUI.size(375, MIN_HEIGHT));
        addResizeListener(() -> myUserResized = true, this);
    }


    @Override
    protected boolean isResizable() {
        return getParent() == null;
    }

    @Override
    public void storeDimensionSize() {
        if (myUserResized) {
            super.storeDimensionSize();
        }
        if (myUserMoved && getParent() == null && isVisible()) {
            Point location = getLocationOnScreen();
            ProjectWindowStateService.getInstance(getProject())
                .putLocation(DIMENSION_SERVICE_KEY, new Point2D(location.x, location.y));
        }
    }

    @Override
    public void show(RelativePoint aPoint) {
        Point stored = getStoredPopupLocation();
        if (stored != null) {
            showInScreenCoordinates(aPoint.getComponent(), stored);
        }
        else {
            super.show(aPoint);
        }
    }

    @Nullable
    private Point getStoredPopupLocation() {
        Project project = getProject();
        if (project == null) {
            return null;
        }
        Point2D location = ProjectWindowStateService.getInstance(project).getLocation(DIMENSION_SERVICE_KEY);
        return TargetAWT.to(location);
    }

    private DvcsBranchesTreePopupStepBase getTreeStep() {
        return (DvcsBranchesTreePopupStepBase) myStep;
    }

    /**
     * Separator node inserted between the actions section and the branch sections in the tree.
     */
    public static SeparatorWithText createTreeSeparator() {
        return new SeparatorWithText();
    }

    @Override
    protected JComponent createContent() {
        myTree = new MyTree(getTreeStep().getTreeModel());
        myTree.getAccessibleContext().setAccessibleName("DvcsBranchesTree");

        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(false);
        myTree.setBorder(JBUI.Borders.empty(5, 8, 0, 0));
        myTree.setCellRenderer(new DvcsBranchesTreeRenderer(getTreeStep(), false));
        ToolTipManager.sharedInstance().registerComponent(myTree);

        myMouseMotionListener = new MyMouseMotionListener();
        myMouseListener = new MyMouseListener();

        registerAction("select", KeyEvent.VK_ENTER, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSelect(true, null);
            }
        });

        final Action oldExpandAction = getActionMap().get("selectChild");
        getActionMap().put("selectChild", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = myTree.getSelectionPath();
                if (path != null && 0 == myTree.getModel().getChildCount(path.getLastPathComponent())) {
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
                TreePath path = myTree.getSelectionPath();
                if (shouldHidePopup(path)) {
                    goBack();
                    return;
                }
                oldCollapseAction.actionPerformed(e);
            }
        });

        return myTree;
    }

    private boolean shouldHidePopup(@Nullable TreePath path) {
        if (getParent() == null) {
            return false;
        }
        if (path == null) {
            return false;
        }
        if (!myTree.isCollapsed(path)) {
            return false;
        }
        return path.getPathCount() == 2;
    }

    @Override
    protected ActionMap getActionMap() {
        return myTree.getActionMap();
    }

    @Override
    protected InputMap getInputMap() {
        return myTree.getInputMap();
    }

    private void addListeners() {
        myTree.addMouseMotionListener(myMouseMotionListener);
        myTree.addMouseListener(myMouseListener);
    }

    @RequiredUIAccess
    @Override
    public void dispose() {
        myTree.removeMouseMotionListener(myMouseMotionListener);
        myTree.removeMouseListener(myMouseListener);
        super.dispose();
    }

    @Override
    protected boolean beforeShow() {
        addListeners();
        TreeUtil.expandAll(myTree);
        return super.beforeShow();
    }

    /**
     * The always-shown search field is created lazily in {@code prepareToShow()}, which runs after
     * {@link #beforeShow()} but before {@link #afterShow()}, so the placeholder and the header
     * toolbar can only be installed here.
     */
    @Override
    protected void afterShow() {
        super.afterShow();
        if (mySpeedSearchPatternField != null && getParent() == null) {
            mySpeedSearchPatternField.setPlaceholder(getTreeStep().getSearchFieldEmptyText());
            installSearchFieldToolbar();
            installDragStrip();
            installContentMargin();
        }
        selectPreferred();
    }

    private void installSearchFieldToolbar() {
        String id = myModel.getVcs().getId();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(id + "BranchesPopup", createHeaderActionGroup(), true);
        toolbar.setTargetComponent(myTree);
        JComponent toolbarComponent = toolbar.getComponent();
        toolbarComponent.setOpaque(false);
        mySpeedSearchPatternField.getTextEditor().putClientProperty("JTextField.trailingComponent", toolbarComponent);
    }

    private void installDragStrip() {
        Container headerPanel = mySpeedSearchPatternField.getParent();
        if (headerPanel == null) {
            return;
        }

        JPanel dragArea = new JPanel();
        dragArea.setPreferredSize(new Dimension(0, DRAG_AREA_HEIGHT));
        dragArea.setBorder(JBUI.Borders.empty(DRAG_AREA_TOP_AND_BOTTOM_BORDER, 0));

        WindowMoveListener moveListener = new WindowMoveListener(getContent());
        dragArea.addMouseListener(moveListener);
        dragArea.addMouseMotionListener(moveListener);
        dragArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                myUserMoved = true;
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(dragArea, BorderLayout.NORTH);
        wrapper.add(mySpeedSearchPatternField, BorderLayout.CENTER);
        headerPanel.add(wrapper, BorderLayout.CENTER);
        headerPanel.revalidate();
    }

    /**
     * Adds an inner margin around the tree content only (the scroll pane at the center of the popup
     * content), so the tree rows keep spacing from the popup edges without shifting the header
     * search field / action toolbar. The border is set on the scroll pane rather than the tree
     * because the tree lives inside the scroll pane's viewport.
     */
    private void installContentMargin() {
        JComponent content = getContent();
        if (content.getLayout() instanceof BorderLayout layout) {
            Component center = layout.getLayoutComponent(BorderLayout.CENTER);
            if (center instanceof JComponent scrollPane) {
                scrollPane.setBorder(JBUI.Borders.empty(8));
                scrollPane.revalidate();
            }
        }
    }

    private DefaultActionGroup createHeaderActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        myModel.appendHeaderActions(group::add);
        group.add(new SettingsActionGroup());
        return group;
    }

    private class SettingsActionGroup extends DefaultActionGroup {
        private SettingsActionGroup() {
            super("Settings", true);
            getTemplatePresentation().setIcon(PlatformIconGroup.generalGearplain());
            add(new DirectoryGroupingToggleAction());
        }

        @Override
        public boolean showBelowArrow() {
            return false;
        }
    }

    private class DirectoryGroupingToggleAction extends ToggleAction {
        private DirectoryGroupingToggleAction() {
            super("Group Branches by Directory");
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return getTreeStep().getTreeModel().isDirectoryGrouping();
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            getTreeStep().getTreeModel().setDirectoryGrouping(state);
            TreeUtil.expandAll(myTree);
            selectPreferred();
        }
    }

    private void selectPreferred() {
        TreePath preferred = getTreeStep().getPreferredSelection();
        if (preferred != null) {
            myTree.setSelectionPath(preferred);
            TreeUtil.scrollToVisible(myTree, preferred, true);
            return;
        }
        selectFirstSelectableItem();
    }

    private void selectFirstSelectableItem() {
        for (int i = 0; i < myTree.getRowCount(); i++) {
            TreePath path = myTree.getPathForRow(i);
            if (getTreeStep().isSelectable(TreeUtil.getUserObject(path.getLastPathComponent()))) {
                myTree.setSelectionPath(path);
                break;
            }
        }
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
            if (!isMouseMoved(e.getLocationOnScreen())) {
                return;
            }

            TreePath path = getPath(e);
            if (path != null) {
                myTree.setSelectionPath(path);
                notifyParentOnChildSelection();
                if (getTreeStep().isSelectable(TreeUtil.getUserObject(path.getLastPathComponent()))) {
                    myTree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    if (myPendingChildPath == null || !myPendingChildPath.equals(path)) {
                        myPendingChildPath = path;
                        restartTimer();
                    }
                    return;
                }
            }
            myTree.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Nullable
    private TreePath getPath(MouseEvent e) {
        return myTree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
    }

    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            TreePath path = getPath(e);
            if (path == null) {
                return;
            }
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            Object userObject = TreeUtil.getUserObject(path.getLastPathComponent());
            if (getTreeStep().isSelectable(userObject)) {
                handleSelect(true, e);
            }
            else if (!TreeUtil.isLocationInExpandControl(myTree, path, e.getX(), e.getY())) {
                toggleExpansion(path);
            }
        }
    }

    private void toggleExpansion(TreePath path) {
        if (myTree.isExpanded(path)) {
            myTree.collapsePath(path);
        }
        else {
            myTree.expandPath(path);
        }
    }

    private void handleSelect(boolean handleFinalChoices, @Nullable MouseEvent e) {
        boolean pathIsAlreadySelected = myShowingChildPath != null && myShowingChildPath.equals(myTree.getSelectionPath());
        if (pathIsAlreadySelected) {
            return;
        }

        myPendingChildPath = null;

        Object selected = myTree.getLastSelectedPathComponent();
        if (selected == null) {
            return;
        }

        Object userObject = TreeUtil.getUserObject(selected);
        if (!getTreeStep().isSelectable(userObject)) {
            return;
        }

        disposeChildren();

        boolean hasNextStep = myStep.hasSubstep(userObject);
        if (!hasNextStep && !handleFinalChoices) {
            myShowingChildPath = null;
            return;
        }

        PopupStep<?> queriedStep = myStep.onChosen(userObject, handleFinalChoices);

        if (queriedStep == PopupStep.FINAL_CHOICE || !hasNextStep) {
            setFinalRunnable(myStep.getFinalRunnable());
            setOk(true);
            disposeAllParents(e);
        }
        else {
            myShowingChildPath = myTree.getSelectionPath();
            handleNextStep(queriedStep, myShowingChildPath);
            myShowingChildPath = null;
        }
    }

    // --- ListPopup contract ---------------------------------------------------------------------
    // Implemented so the popup can be returned through MultipleTextValuesPresentation.getPopupStep()
    // and positioned by the status bar the same way as a plain list popup (anchored above the
    // widget). The tree drives selection itself, so most of the list-oriented API is a no-op.

    @Override
    @Nullable
    public ListPopupStep getListStep() {
        return null;
    }

    @Override
    public void handleSelect(boolean handleFinalChoices) {
        handleSelect(handleFinalChoices, (MouseEvent) null);
    }

    @Override
    public void handleSelect(boolean handleFinalChoices, InputEvent e) {
        handleSelect(handleFinalChoices, e instanceof MouseEvent mouseEvent ? mouseEvent : null);
    }

    @Override
    public void setHandleAutoSelectionBeforeShow(boolean autoHandle) {
    }

    @Override
    public void addListSelectionListener(ListSelectionListener listSelectionListener) {
    }

    @Override
    public void addSelectionListener(Consumer<Object> selectionListener) {
    }

    @Override
    public void handleNextStep(PopupStep nextStep, Object parentValue) {
        Rectangle pathBounds = myTree.getPathBounds(myTree.getSelectionPath());
        Point point = new RelativePoint(myTree, new Point(getContent().getWidth() + 2, (int) pathBounds.getY())).getScreenPoint();
        myChild = createPopup(this, nextStep, parentValue);
        myChild.show(getContent(), point.x - STEP_X_PADDING, point.y, true);
    }

    @Override
    protected void onAutoSelectionTimer() {
        handleSelect(false, null);
    }

    @Override
    protected void process(KeyEvent aEvent) {
        myTree.processKeyEvent(aEvent);
    }

    @Override
    protected JComponent getPreferredFocusableComponent() {
        return myTree;
    }

    @Override
    protected void onSpeedSearchPatternChanged() {
        String pattern = getSpeedSearch().getFilter();
        getTreeStep().updateTreeModelIfNeeded(myTree, pattern);
        getTreeStep().setSearchPattern(pattern);
        TreeUtil.expandAll(myTree);
        selectPreferred();
    }

    @Override
    public void onChildSelectedFor(Object value) {
        TreePath path = (TreePath) value;
        if (myTree.getSelectionPath() != path) {
            myTree.setSelectionPath(path);
        }
    }

    @Override
    public boolean isModalContext() {
        return true;
    }

    private static class MyTree extends Tree {
        MyTree(TreeModel model) {
            super(model);
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
            e.setSource(this);
            super.processKeyEvent(e);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension pref = super.getPreferredSize();
            return new Dimension(pref.width + 10, pref.height);
        }
    }
}
