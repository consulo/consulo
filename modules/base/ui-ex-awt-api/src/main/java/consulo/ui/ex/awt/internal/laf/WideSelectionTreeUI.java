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
package consulo.ui.ex.awt.internal.laf;

import consulo.annotation.DeprecationInfo;
import consulo.platform.Platform;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.MouseEventAdapter;
import consulo.ui.ex.awt.internal.DarkThemeCalculator;
import consulo.util.lang.SystemProperties;
import consulo.util.lang.function.Predicates;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Predicate;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo("Use DefaultTreeUI")
public class WideSelectionTreeUI extends BasicTreeUI {
    public static final String TREE_TABLE_TREE_KEY = "TreeTableTree";

    public static final String SOURCE_LIST_CLIENT_PROPERTY = "mac.ui.source.list";
    public static final String STRIPED_CLIENT_PROPERTY = "mac.ui.striped";

    private static final Border LIST_BACKGROUND_PAINTER = UIManager.getBorder("List.sourceListBackgroundPainter");
    private static final Border LIST_SELECTION_BACKGROUND_PAINTER = UIManager.getBorder("List.sourceListSelectionBackgroundPainter");
    private static final Border LIST_FOCUSED_SELECTION_BACKGROUND_PAINTER =
        UIManager.getBorder("List.sourceListFocusedSelectionBackgroundPainter");

    private static final int IDE_UI_TREE_INDENT = SystemProperties.getIntProperty("ide.ui.tree.indent", -1);

    @Nonnull
    private final Predicate<Integer> myWideSelectionCondition;
    private boolean myWideSelection;
    private boolean myOldRepaintAllRowValue;
    private boolean myForceDontPaintLines = false;

    @SuppressWarnings("unchecked")
    public WideSelectionTreeUI() {
        this(true, Predicates.<Integer>alwaysTrue());
    }

    /**
     * Creates new {@code WideSelectionTreeUI} object.
     *
     * @param wideSelection          flag that determines if wide selection should be used
     * @param wideSelectionCondition strategy that determine if wide selection should be used for a target row (it's zero-based index
     *                               is given to the condition as an argument)
     */
    public WideSelectionTreeUI(boolean wideSelection, @Nonnull Predicate<Integer> wideSelectionCondition) {
        myWideSelection = wideSelection;
        myWideSelectionCondition = wideSelectionCondition;
    }

    @Override
    public int getRightChildIndent() {
        return isCustomIndent() ? getCustomIndent() : super.getRightChildIndent();
    }

    public boolean isCustomIndent() {
        return getCustomIndent() > 0;
    }

    protected int getCustomIndent() {
        return JBUI.scale(IDE_UI_TREE_INDENT);
    }

    @Override
    protected MouseListener createMouseListener() {
        return new MouseEventAdapter<>(super.createMouseListener()) {
            @Override
            public void mouseDragged(MouseEvent event) {
                JTree tree = (JTree)event.getSource();
                Object property = tree.getClientProperty("DnD Source"); // DnDManagerImpl.SOURCE_KEY
                if (property == null) {
                    super.mouseDragged(event); // use Swing-based DnD only if custom DnD is not set
                }
            }

            @Nonnull
            @Override
            protected MouseEvent convert(@Nonnull MouseEvent event) {
                if (!event.isConsumed() && SwingUtilities.isLeftMouseButton(event)) {
                    int x = event.getX();
                    int y = event.getY();
                    JTree tree = (JTree)event.getSource();
                    if (tree.isEnabled()) {
                        TreePath path = getClosestPathForLocation(tree, x, y);
                        if (path != null && !isLocationInExpandControl(path, x, y)) {
                            Rectangle bounds = getPathBounds(tree, path);
                            if (bounds != null && bounds.y <= y && y <= (bounds.y + bounds.height)) {
                                x = Math.max(bounds.x, Math.min(x, bounds.x + bounds.width - 1));
                                if (x != event.getX()) {
                                    event = MouseEventAdapter.convert(event, tree, x, y);
                                }
                            }
                        }
                    }
                }
                return event;
            }
        };
    }

    @Override
    protected void completeUIInstall() {
        super.completeUIInstall();

        myOldRepaintAllRowValue = UIManager.getBoolean("Tree.repaintWholeRow");
        UIManager.put("Tree.repaintWholeRow", true);

        tree.setShowsRootHandles(true);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        UIManager.put("Tree.repaintWholeRow", myOldRepaintAllRowValue);
    }

    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();

        if (Boolean.TRUE.equals(tree.getClientProperty("MacTreeUi.actionsInstalled"))) {
            return;
        }

        tree.putClientProperty("MacTreeUi.actionsInstalled", Boolean.TRUE);

        InputMap inputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke("pressed LEFT"), "collapse_or_move_up");
        inputMap.put(KeyStroke.getKeyStroke("pressed RIGHT"), "expand");

        ActionMap actionMap = tree.getActionMap();

        Action expandAction = actionMap.get("expand");
        if (expandAction != null) {
            actionMap.put("expand", new TreeUIAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() instanceof JTree tree) {
                        int selectionRow = tree.getLeadSelectionRow();
                        if (selectionRow != -1) {
                            TreePath selectionPath = tree.getPathForRow(selectionRow);
                            if (selectionPath != null) {
                                boolean leaf = tree.getModel().isLeaf(selectionPath.getLastPathComponent());
                                int toSelect = -1;
                                int toScroll = -1;
                                if ((!leaf && tree.isExpanded(selectionRow)) || leaf) {
                                    if (selectionRow + 1 < tree.getRowCount()) {
                                        toSelect = selectionRow + 1;
                                        toScroll = toSelect;
                                    }
                                }
                                //todo[kb]: make cycle scrolling

                                if (toSelect != -1) {
                                    tree.setSelectionInterval(toSelect, toSelect);
                                }

                                if (toScroll != -1) {
                                    tree.scrollRowToVisible(toScroll);
                                }

                                if (toSelect != -1 || toScroll != -1) {
                                    return;
                                }
                            }
                        }
                    }

                    expandAction.actionPerformed(e);
                }
            });
        }

        actionMap.put(
            "collapse_or_move_up",
            new TreeUIAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object source = e.getSource();
                    if (source instanceof JTree tree) {
                        int selectionRow = tree.getLeadSelectionRow();
                        if (selectionRow == -1) {
                            return;
                        }

                        TreePath selectionPath = tree.getPathForRow(selectionRow);
                        if (selectionPath == null) {
                            return;
                        }

                        if (tree.getModel().isLeaf(selectionPath.getLastPathComponent()) || tree.isCollapsed(selectionRow)) {
                            TreePath parentPath = tree.getPathForRow(selectionRow).getParentPath();
                            if (parentPath != null && (parentPath.getParentPath() != null || tree.isRootVisible())) {
                                int parentRow = tree.getRowForPath(parentPath);
                                tree.scrollRowToVisible(parentRow);
                                tree.setSelectionInterval(parentRow, parentRow);
                            }
                        }
                        else {
                            tree.collapseRow(selectionRow);
                        }
                    }
                }
            }
        );
    }

    public void setForceDontPaintLines() {
        myForceDontPaintLines = true;
    }

    private abstract static class TreeUIAction extends AbstractAction implements UIResource {
    }

    @Override
    protected int getRowX(int row, int depth) {
        if (isCustomIndent()) {
            int off = tree.isRootVisible() ? 8 : 0;
            return 8 * depth + 8 + off;
        }
        else {
            return super.getRowX(row, depth);
        }
    }

    @Override
    protected void paintHorizontalPartOfLeg(
        Graphics g,
        Rectangle clipBounds,
        Insets insets,
        Rectangle bounds,
        TreePath path,
        int row,
        boolean isExpanded,
        boolean hasBeenExpanded,
        boolean isLeaf
    ) {
        if (shouldPaintLines()) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }
    }

    private boolean shouldPaintLines() {
        return !(UIUtil.isUnderAquaBasedLookAndFeel() || DarkThemeCalculator.isDark() || UIUtil.isUnderIntelliJLaF())
            && (myForceDontPaintLines || !"None".equals(tree.getClientProperty("JTree.lineStyle")));
    }

    @Override
    protected boolean isToggleSelectionEvent(MouseEvent e) {
        return SwingUtilities.isLeftMouseButton(e)
            && (Platform.current().os().isMac() ? e.isMetaDown() : e.isControlDown())
            && !e.isPopupTrigger();
    }

    @Override
    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets, TreePath path) {
        if (shouldPaintLines()) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    @Override
    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top, int bottom) {
        if (shouldPaintLines()) {
            super.paintVerticalLine(g, c, x, top, bottom);
        }
    }

    @Override
    protected Color getHashColor() {
        //if (invertLineColor && !ComparatorUtil.equalsNullable(UIUtil.getTreeSelectionForeground(), UIUtil.getTreeForeground())) {
        //    Color c = UIUtil.getTreeSelectionForeground();
        //    if (c != null) {
        //        return c.darker();
        //    }
        //}
        return super.getHashColor();
    }

    public boolean isWideSelection() {
        return myWideSelection;
    }

    @Override
    protected void paintRow(
        Graphics g,
        Rectangle clipBounds,
        Insets insets,
        Rectangle bounds,
        TreePath path,
        int row,
        boolean isExpanded,
        boolean hasBeenExpanded,
        boolean isLeaf
    ) {
        int containerWidth = tree.getParent() instanceof JViewport viewport ? viewport.getWidth() : tree.getWidth();
        int xOffset = tree.getParent() instanceof JViewport viewport ? viewport.getViewPosition().x : 0;

        if (path != null && myWideSelection) {
            boolean selected = tree.isPathSelected(path);
            Graphics2D rowGraphics = (Graphics2D)g.create();
            rowGraphics.setClip(clipBounds);

            Object sourceList = tree.getClientProperty(SOURCE_LIST_CLIENT_PROPERTY);
            Color background = tree.getBackground();

            if ((row % 2) == 0 && Boolean.TRUE.equals(tree.getClientProperty(STRIPED_CLIENT_PROPERTY))) {
                background = UIUtil.getDecoratedRowColor();
            }

            if (sourceList != null && (Boolean)sourceList) {
                if (selected) {
                    if (tree.hasFocus()) {
                        LIST_FOCUSED_SELECTION_BACKGROUND_PAINTER.paintBorder(
                            tree,
                            rowGraphics,
                            xOffset,
                            bounds.y,
                            containerWidth,
                            bounds.height
                        );
                    }
                    else {
                        LIST_SELECTION_BACKGROUND_PAINTER.paintBorder(tree, rowGraphics, xOffset, bounds.y, containerWidth, bounds.height);
                    }
                }
                else if (myWideSelectionCondition.test(row)) {
                    rowGraphics.setColor(background);
                    rowGraphics.fillRect(xOffset, bounds.y, containerWidth, bounds.height);
                }
            }
            else {
                if (selected && (UIUtil.isUnderAquaBasedLookAndFeel() || DarkThemeCalculator.isDark() || UIUtil.isUnderIntelliJLaF())) {
                    Color bg = getSelectionBackground(tree, true);

                    if (myWideSelectionCondition.test(row)) {
                        rowGraphics.setColor(bg);
                        rowGraphics.fillRect(xOffset, bounds.y, containerWidth, bounds.height);
                    }
                }
            }

            if (shouldPaintExpandControl(path, row, isExpanded, hasBeenExpanded, isLeaf)) {
                paintExpandControl(rowGraphics, bounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
            }

            super.paintRow(rowGraphics, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
            rowGraphics.dispose();
        }
        else {
            super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (myWideSelection && !UIUtil.isUnderAquaBasedLookAndFeel() && !UIUtil.isUnderDarcula() && !UIUtil.isUnderIntelliJLaF()) {
            paintSelectedRows(g, ((JTree)c));
        }
        if (myWideSelection) {
            int containerWidth = tree.getParent() instanceof JViewport viewport ? viewport.getWidth() : tree.getWidth();
            int xOffset = tree.getParent() instanceof JViewport viewport ? viewport.getViewPosition().x : 0;
            Rectangle bounds = g.getClipBounds();

            // draw background for the given clip bounds
            Object sourceList = tree.getClientProperty(SOURCE_LIST_CLIENT_PROPERTY);
            if (sourceList != null && (Boolean)sourceList) {
                Graphics2D backgroundGraphics = (Graphics2D)g.create();
                backgroundGraphics.setClip(xOffset, bounds.y, containerWidth, bounds.height);
                LIST_BACKGROUND_PAINTER.paintBorder(tree, backgroundGraphics, xOffset, bounds.y, containerWidth, bounds.height);
                backgroundGraphics.dispose();
            }
        }

        super.paint(g, c);
    }

    protected void paintSelectedRows(Graphics g, JTree tr) {
        Rectangle rect = tr.getVisibleRect();
        int firstVisibleRow = tr.getClosestRowForLocation(rect.x, rect.y);
        int lastVisibleRow = tr.getClosestRowForLocation(rect.x, rect.y + rect.height);

        for (int row = firstVisibleRow; row <= lastVisibleRow; row++) {
            if (tr.getSelectionModel().isRowSelected(row) && myWideSelectionCondition.test(row)) {
                Rectangle bounds = tr.getRowBounds(row);
                Color color = getSelectionBackground(tr, false);
                if (color != null) {
                    g.setColor(color);
                    g.fillRect(0, bounds.y, tr.getWidth(), bounds.height);
                }
            }
        }
    }

    @Override
    protected CellRendererPane createCellRendererPane() {
        return new CellRendererPane() {
            @Override
            public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean shouldValidate) {
                if (c instanceof JComponent component && myWideSelection && c.isOpaque()) {
                    component.setOpaque(false);
                }

                super.paintComponent(g, c, p, x, y, w, h, shouldValidate);
            }
        };
    }

    @Override
    protected void paintExpandControl(
        Graphics g,
        Rectangle clipBounds,
        Insets insets,
        Rectangle bounds,
        TreePath path,
        int row,
        boolean isExpanded,
        boolean hasBeenExpanded,
        boolean isLeaf
    ) {
        boolean isPathSelected = tree.getSelectionModel().isPathSelected(path);
        if (!isLeaf(row)) {
            setExpandedIcon(UIUtil.getTreeNodeIcon(true, isPathSelected, tree.hasFocus()));
            setCollapsedIcon(UIUtil.getTreeNodeIcon(false, isPathSelected, tree.hasFocus()));
        }

        super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
    }

    @Nullable
    private static Color getSelectionBackground(@Nonnull JTree tree, boolean checkProperty) {
        Object property = tree.getClientProperty(TREE_TABLE_TREE_KEY);
        if (property instanceof JTable table) {
            return table.getSelectionBackground();
        }
        boolean selection = tree.hasFocus();
        if (!selection && checkProperty) {
            selection = Boolean.TRUE.equals(property);
        }
        return UIUtil.getTreeSelectionBackground(selection);
    }
}
