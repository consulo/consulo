/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.action;

import com.google.common.base.MoreObjects;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.desktop.awt.ui.impl.window.JFrameAsUIWindow;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ide.impl.idea.util.ReflectionUtil;
import consulo.ide.impl.idea.util.ui.ColorIcon;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.Notifications;
import consulo.project.ui.notification.NotificationsManager;
import consulo.ui.AntialiasingType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.table.StripeTable;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.miginfocom.swing.MigLayout;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.*;
import java.util.function.Function;

import static java.util.Locale.ENGLISH;

public class UiInspectorAction extends ToggleAction implements DumbAware {
    private static final String CLICK_INFO = "CLICK_INFO";
    private static final String RENDERER_BOUNDS = "clicked renderer";
    private UiInspector myInspector;

    public UiInspectorAction() {
        if (Boolean.getBoolean("idea.ui.debug.mode")) {
            Application.get().invokeLater(() -> setSelected(null, true));
        }
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return myInspector != null;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        if (state) {
            if (myInspector == null) {
                myInspector = new UiInspector();
            }

            UiInspectorNotification[] existing =
                NotificationsManager.getNotificationsManager().getNotificationsOfType(UiInspectorNotification.class, null);
            if (existing.length == 0) {
                new UiInspectorNotification(
                    NotificationService.getInstance()
                        .newInfo(Notifications.SYSTEM_MESSAGES_GROUP)
                        .title(LocalizeValue.localizeTODO("UI Inspector"))
                        .content(LocalizeValue.localizeTODO("Control-Alt-Click to view component info!"))
                ).notify(null);
            }
        }
        else {
            UiInspector inspector = myInspector;
            myInspector = null;
            if (inspector != null) {
                Disposer.dispose(inspector);
            }
        }
    }

    private static class UiInspectorNotification extends Notification {
        private UiInspectorNotification(Notification.Builder notificationBuilder) {
            super(notificationBuilder);
        }
    }

    private static class InspectorWindow extends JFrameAsUIWindow {
        private InspectorTable myInspectorTable;
        private Component myComponent;
        private List<PropertyBean> myInfo;
        private Component myInitialComponent;
        private HighlightComponent myHighlightComponent;
        private HierarchyTree myHierarchyTree;
        private final JPanel myWrapperPanel;

        private InspectorWindow(@Nonnull Component component) throws HeadlessException {
            Window window = findWindow(component);
            myComponent = component;
            myInitialComponent = component;
            getRootPane().setBorder(JBUI.Borders.empty(5));

            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            setLayout(new BorderLayout());
            setTitle(component.getClass().getName());

            DefaultActionGroup actions = new DefaultActionGroup();
            actions.addAction(new AnAction("Highlight") {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    setHighlightingEnabled(myHighlightComponent == null);
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation().setEnabled(myInfo != null || (myComponent != null && myComponent.isVisible()));
                }
            });

            actions.addSeparator();

            actions.add(new DumbAwareAction("Refresh") {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    getCurrentTable().refresh();
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation().setEnabled(myComponent != null && myComponent.isVisible());
                }
            });

            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, actions, true);
            add(toolbar.getComponent(), BorderLayout.NORTH);

            myWrapperPanel = new JPanel(new BorderLayout());

            myInspectorTable = new InspectorTable(component);
            myHierarchyTree = new HierarchyTree(component) {
                @Override
                public void onComponentChanged(Component c) {
                    boolean wasHighlighted = myHighlightComponent != null;
                    setHighlightingEnabled(false);
                    switchInfo(c);
                    setHighlightingEnabled(wasHighlighted);
                }

                @Override
                public void onComponentChanged(List<PropertyBean> info) {
                    boolean wasHighlighted = myHighlightComponent != null;
                    setHighlightingEnabled(false);
                    switchInfo(info);
                    setHighlightingEnabled(wasHighlighted);
                }
            };

            myWrapperPanel.add(myInspectorTable, BorderLayout.CENTER);

            Splitter splitPane = new JBSplitter(false, "UiInspector.splitter.proportion", 0.5f);
            splitPane.setSecondComponent(myWrapperPanel);
            splitPane.setFirstComponent(new JBScrollPane(myHierarchyTree));
            add(splitPane, BorderLayout.CENTER);

            myHierarchyTree.expandPath();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    close();
                }
            });

            getRootPane().getActionMap().put("CLOSE", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    close();
                }
            });
            setHighlightingEnabled(true);
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE");
        }

        private static Window findWindow(Component component) {
            DialogWrapper dialogWrapper = DialogWrapper.findInstance(component);
            if (dialogWrapper != null) {
                return dialogWrapper.getPeer().getWindow();
            }
            return null;
        }

        private InspectorTable getCurrentTable() {
            return myInspectorTable;
        }

        private void switchInfo(@Nullable Component c) {
            if (c == null) {
                return;
            }
            myComponent = c;
            myInfo = null;
            setTitle(myComponent.getClass().getName());
            myWrapperPanel.removeAll();
            myInspectorTable = new InspectorTable(c);
            myWrapperPanel.add(myInspectorTable, BorderLayout.CENTER);
            myWrapperPanel.revalidate();
            myWrapperPanel.repaint();
        }

        private void switchInfo(@Nonnull List<PropertyBean> clickInfo) {
            myComponent = null;
            myInfo = clickInfo;
            setTitle("Click Info");
            myWrapperPanel.removeAll();
            myInspectorTable = new InspectorTable(clickInfo);
            myWrapperPanel.add(myInspectorTable, BorderLayout.CENTER);
            myWrapperPanel.revalidate();
            myWrapperPanel.repaint();
        }

        @Override
        public void dispose() {
            super.dispose();
            DialogWrapper.cleanupRootPane(rootPane);
            DialogWrapper.cleanupWindowListeners(this);
        }

        public void close() {
            if (myInitialComponent instanceof JComponent jComponent) {
                jComponent.putClientProperty(CLICK_INFO, null);
            }
            myInfo = null;
            setHighlightingEnabled(false);
            if (myComponent == null) {
                return;
            }
            myComponent = null;
            setVisible(false);
            dispose();
        }

        private void setHighlightingEnabled(boolean enable) {
            if (myHighlightComponent != null) {
                JComponent glassPane = getGlassPane(myHighlightComponent);
                if (glassPane != null) {
                    glassPane.remove(myHighlightComponent);

                    glassPane.revalidate();
                    glassPane.repaint();
                }
                myHighlightComponent = null;
            }

            if (enable && myComponent != null) {
                JComponent glassPane = getGlassPane(myComponent);
                if (glassPane != null) {
                    myHighlightComponent = new HighlightComponent(new JBColor(JBColor.GREEN, JBColor.RED));

                    Point pt = SwingUtilities.convertPoint(myComponent, new Point(0, 0), glassPane);
                    myHighlightComponent.setBounds(pt.x, pt.y, myComponent.getWidth(), myComponent.getHeight());
                    glassPane.add(myHighlightComponent);

                    glassPane.revalidate();
                    glassPane.repaint();
                }
            }
            if (enable & myInfo != null && myInitialComponent != null) {
                Rectangle bounds = null;
                for (PropertyBean bean : myInfo) {
                    if (RENDERER_BOUNDS.equals(bean.propertyName)) {
                        bounds = (Rectangle) bean.propertyValue;
                        break;
                    }
                }
                if (bounds != null) {
                    JComponent glassPane = getGlassPane(myInitialComponent);
                    if (glassPane != null) {
                        myHighlightComponent = new HighlightComponent(new JBColor(JBColor.GREEN, JBColor.RED));

                        bounds = SwingUtilities.convertRectangle(myInitialComponent, bounds, glassPane);
                        myHighlightComponent.setBounds(bounds);
                        glassPane.add(myHighlightComponent);

                        glassPane.revalidate();
                        glassPane.repaint();
                    }
                }
            }
        }

        @Nullable
        private static JComponent getGlassPane(@Nonnull Component component) {
            JRootPane rootPane = SwingUtilities.getRootPane(component);
            return rootPane == null ? null : (JComponent) rootPane.getGlassPane();
        }
    }

    private static class ComponentTreeCellRenderer extends ColoredTreeCellRenderer {
        private final Component myInitialSelection;

        ComponentTreeCellRenderer(Component initialSelection) {
            myInitialSelection = initialSelection;
            setFont(JBUI.Fonts.label(11));
            setBorder(JBUI.Borders.empty(0, 3));
        }

        @RequiredUIAccess
        @Override
        public void customizeCellRenderer(
            @Nonnull JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            Color foreground = selected ? UIUtil.getTreeSelectionForeground() : UIUtil.getTreeForeground();
            Color background = selected ? UIUtil.getTreeSelectionBackground() : null;
            if (value instanceof HierarchyTree.ComponentNode componentNode) {
                Component component = componentNode.getComponent();

                if (!selected) {
                    if (!component.isVisible()) {
                        foreground = JBColor.GRAY;
                    }
                    else if (component.getWidth() == 0 || component.getHeight() == 0) {
                        foreground = new JBColor(new Color(128, 10, 0), JBColor.BLUE);
                    }
                    else if (component.getPreferredSize() != null && (component.getSize().width < component.getPreferredSize().width || component.getSize().height < component.getPreferredSize().height)) {
                        foreground = JBColor.BLUE;
                    }

                    if (myInitialSelection == componentNode.getComponent()) {
                        background = new Color(31, 128, 8, 58);
                    }
                }
                append(getComponentName(component));
                append(": " + RectangleRenderer.toString(component.getBounds()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                if (component.isOpaque()) {
                    append(", opaque", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                if (component.isDoubleBuffered()) {
                    append(", double-buffered", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
                componentNode.setText(toString());
                setIcon(createColorIcon(component.getForeground(), component.getBackground()));
            }
            if (value instanceof HierarchyTree.ClickInfoNode) {
                append(value.toString());
                setIcon(PlatformIconGroup.nodesTarget());
            }

            setForeground(foreground);
            setBackground(background);

            SpeedSearchUtil.applySpeedSearchHighlighting(tree, this, false, selected);
        }
    }

    @Nonnull
    private static String getComponentName(Component component) {
        String name = getClassName(component);

        String componentName = component.getName();
        if (StringUtil.isNotEmpty(componentName)) {
            name += " \"" + componentName + "\"";
        }
        return name;
    }

    private static TreeModel buildModel(Component c) {
        Component parent = c.getParent();
        while (parent != null) {
            c = parent;
            parent = c.getParent();//Find root window
        }
        return new DefaultTreeModel(new UiInspectorAction.HierarchyTree.ComponentNode(c));
    }


    private abstract static class HierarchyTree extends JTree implements TreeSelectionListener {
        final Component myComponent;

        private HierarchyTree(Component c) {
            myComponent = c;
            setModel(buildModel(c));
            setCellRenderer(new ComponentTreeCellRenderer(c));
            getSelectionModel().addTreeSelectionListener(this);
            new TreeSpeedSearch(this);
            if (((JComponent) c).getClientProperty(CLICK_INFO) != null) {
                SwingUtilities.invokeLater(() -> getSelectionModel().setSelectionPath(getPathForRow(getLeadSelectionRow() + 1)));
            }
        }

        public void expandPath() {
            TreeUtil.expandAll(this);
            int count = getRowCount();
            ComponentNode node = new ComponentNode(myComponent);

            for (int i = 0; i < count; i++) {
                TreePath row = getPathForRow(i);
                if (row.getLastPathComponent().equals(node)) {
                    setSelectionPath(row);
                    scrollPathToVisible(getSelectionPath());
                    break;
                }
            }
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                onComponentChanged((Component) null);
                return;
            }
            Object component = path.getLastPathComponent();

            if (component instanceof ComponentNode componentNode) {
                Component c = componentNode.getComponent();
                onComponentChanged(c);
            }
            if (component instanceof ClickInfoNode clickInfoNode) {
                onComponentChanged(clickInfoNode.getInfo());
            }
        }

        public abstract void onComponentChanged(List<PropertyBean> info);

        public abstract void onComponentChanged(Component c);

        private static class ComponentNode extends DefaultMutableTreeNode {
            private final Component myComponent;
            String myText;

            private ComponentNode(@Nonnull Component component) {
                super(component);
                myComponent = component;
                children = prepareChildren(myComponent);
            }

            Component getComponent() {
                return myComponent;
            }

            @Override
            public String toString() {
                return myText != null ? myText : myComponent.getClass().getName();
            }

            public void setText(String value) {
                myText = value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof ComponentNode componentNode && componentNode.getComponent() == getComponent();
            }

            @SuppressWarnings({"unchecked", "UseOfObsoleteCollectionType"})
            private static Vector prepareChildren(Component parent) {
                Vector<DefaultMutableTreeNode> result = new Vector<>();
                if (parent instanceof JComponent jComponent) {
                    Object o = jComponent.getClientProperty(CLICK_INFO);
                    if (o instanceof List) {
                        //noinspection unchecked
                        result.add(new ClickInfoNode((List<PropertyBean>) o));
                    }
                }
                if (parent instanceof Container container) {
                    for (Component component : container.getComponents()) {
                        result.add(new ComponentNode(component));
                    }
                }
                if (parent instanceof Window window) {
                    Window[] children = window.getOwnedWindows();
                    for (Window child : children) {
                        if (child instanceof InspectorWindow) {
                            continue;
                        }
                        result.add(new ComponentNode(child));
                    }
                }

                return result;
            }
        }

        private static class ClickInfoNode extends DefaultMutableTreeNode {
            private final List<PropertyBean> myInfo;

            public ClickInfoNode(List<PropertyBean> info) {
                myInfo = info;
            }

            @Override
            public String toString() {
                return "Clicked Info";
            }

            public List<PropertyBean> getInfo() {
                return myInfo;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }
        }
    }

    private static class HighlightComponent extends JComponent {
        Color myColor;

        private HighlightComponent(@Nonnull final Color c) {
            myColor = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;

            Color oldColor = g2d.getColor();
            g2d.setColor(myColor);
            Composite old = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));

            Rectangle r = getBounds();

            g2d.fillRect(0, 0, r.width, r.height);

            g2d.setColor(myColor.darker());
            g2d.drawRect(0, 0, r.width - 1, r.height - 1);

            g2d.setComposite(old);
            g2d.setColor(oldColor);
        }
    }

    private static class InspectorTable extends JPanel {
        InspectorTableModel myModel;
        DimensionsComponent myDimensionComponent;

        private InspectorTable(@Nonnull final List<PropertyBean> clickInfo) {
            myModel = new InspectorTableModel(clickInfo);
            init(null);
        }

        private InspectorTable(@Nonnull final Component component) {

            myModel = new InspectorTableModel(component);
            init(component);
        }

        private void init(@Nullable Component component) {
            setLayout(new BorderLayout());
            StripeTable table = new StripeTable(myModel);
            new TableSpeedSearch(table);

            TableColumnModel columnModel = table.getColumnModel();
            TableColumn propertyColumn = columnModel.getColumn(0);
            propertyColumn.setMinWidth(JBUI.scale(200));
            propertyColumn.setMaxWidth(JBUI.scale(200));
            propertyColumn.setResizable(false);
            propertyColumn.setCellRenderer(new PropertyNameRenderer());

            TableColumn valueColumn = columnModel.getColumn(1);
            valueColumn.setMinWidth(JBUI.scale(200));
            valueColumn.setResizable(false);
            valueColumn.setCellRenderer(new ValueCellRenderer());
            valueColumn.setCellEditor(new DefaultCellEditor(new JBTextField()) {
                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    Component comp =
                        table.getCellRenderer(row, column).getTableCellRendererComponent(table, value, false, false, row, column);
                    if (comp instanceof JLabel label) {
                        value = label.getText();
                    }
                    Component result = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                    ((JComponent) result).setBorder(BorderFactory.createLineBorder(JBColor.GRAY, 1));
                    return result;
                }

                @Override
                public Object getCellEditorValue() {
                    return super.getCellEditorValue();
                }
            });

            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

            add(new JBScrollPane(table), BorderLayout.CENTER);
            if (component != null) {
                myDimensionComponent = new DimensionsComponent(component);
                add(myDimensionComponent, BorderLayout.SOUTH);
            }
        }

        public void refresh() {
            myModel.refresh();
            myDimensionComponent.update();
            myDimensionComponent.repaint();
        }

        private class PropertyNameRenderer extends DefaultTableCellRenderer {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                final TableModel model = table.getModel();
                boolean changed = false;
                if (model instanceof InspectorTableModel inspectorTableModel) {
                    changed = inspectorTableModel.myProperties.get(row).changed;
                }

                final Color fg =
                    isSelected ? table.getSelectionForeground() : changed ? JBCurrentTheme.Link.linkColor() : table.getForeground();
                final JBFont font = JBUI.Fonts.label();
                setFont(changed ? font.asBold() : font);
                setForeground(fg);
                return this;
            }
        }
    }

    private static class DimensionsComponent extends JComponent {
        Component myComponent;
        int myWidth;
        int myHeight;
        Border myBorder;
        Insets myInsets;

        private DimensionsComponent(@Nonnull final Component component) {
            myComponent = component;
            setOpaque(true);
            setBackground(JBColor.WHITE);
            setBorder(JBUI.Borders.empty(5, 0));

            setFont(JBUI.Fonts.label(9));

            update();
        }

        public void update() {
            myWidth = myComponent.getWidth();
            myHeight = myComponent.getHeight();
            if (myComponent instanceof JComponent jComponent) {
                myBorder = jComponent.getBorder();
                myInsets = jComponent.getInsets();
            }
        }

        @Override
        protected void paintComponent(final Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            GraphicsConfig config =
                new GraphicsConfig(g).setAntialiasing(UISettings.getShadowInstance().getIdeAAType() != AntialiasingType.OFF);
            Rectangle bounds = getBounds();

            g2d.setColor(getBackground());
            Insets insets = getInsets();
            g2d.fillRect(insets.left, insets.top, bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);

            final String sizeString = String.format("%d x %d", myWidth, myHeight);

            FontMetrics fm = g2d.getFontMetrics();
            int sizeWidth = fm.stringWidth(sizeString);
            int fontHeight = fm.getHeight();

            int innerBoxWidthGap = JBUI.scale(20);
            int innerBoxHeightGap = JBUI.scale(5);
            int boxSize = JBUI.scale(15);

            int centerX = bounds.width / 2;
            int centerY = bounds.height / 2;
            int innerX = centerX - sizeWidth / 2 - innerBoxWidthGap;
            int innerY = centerY - fontHeight / 2 - innerBoxHeightGap;
            int innerWidth = sizeWidth + innerBoxWidthGap * 2;
            int innerHeight = fontHeight + innerBoxHeightGap * 2;

            g2d.setColor(getForeground());
            drawCenteredString(g2d, fm, fontHeight, sizeString, centerX, centerY);

            g2d.setColor(JBColor.GRAY);
            g2d.drawRect(innerX, innerY, innerWidth, innerHeight);

            Insets borderInsets = null;
            if (myBorder != null) {
                borderInsets = myBorder.getBorderInsets(myComponent);
            }
            UIUtil.drawDottedRectangle(
                g2d,
                innerX - boxSize,
                innerY - boxSize,
                innerX + innerWidth + boxSize,
                innerY + innerHeight + boxSize
            );
            drawInsets(g2d, fm, "border", borderInsets, boxSize, fontHeight, innerX, innerY, innerWidth, innerHeight);

            g2d.drawRect(innerX - boxSize * 2, innerY - boxSize * 2, innerWidth + boxSize * 4, innerHeight + boxSize * 4);
            drawInsets(g2d, fm, "insets", myInsets, boxSize * 2, fontHeight, innerX, innerY, innerWidth, innerHeight);

            config.restore();
        }

        private static void drawInsets(
            Graphics2D g2d,
            FontMetrics fm,
            String name,
            Insets insets,
            int offset,
            int fontHeight,
            int innerX,
            int innerY,
            int innerWidth,
            int innerHeight
        ) {
            g2d.setColor(JBColor.BLACK);
            g2d.drawString(name, innerX - offset + JBUI.scale(5), innerY - offset + fontHeight);

            g2d.setColor(JBColor.GRAY);

            int outerX = innerX - offset;
            int outerWidth = innerWidth + offset * 2;
            int outerY = innerY - offset;
            int outerHeight = innerHeight + offset * 2;

            final String top = insets != null ? Integer.toString(insets.top) : "-";
            final String bottom = insets != null ? Integer.toString(insets.bottom) : "-";
            final String left = insets != null ? Integer.toString(insets.left) : "-";
            final String right = insets != null ? Integer.toString(insets.right) : "-";

            int shift = JBUI.scale(7);
            drawCenteredString(g2d, fm, fontHeight, top, outerX + outerWidth / 2, outerY + shift);
            drawCenteredString(g2d, fm, fontHeight, bottom, outerX + outerWidth / 2, outerY + outerHeight - shift);
            drawCenteredString(g2d, fm, fontHeight, left, outerX + shift, outerY + outerHeight / 2);
            drawCenteredString(g2d, fm, fontHeight, right, outerX + outerWidth - shift, outerY + outerHeight / 2);
        }

        @Override
        public Dimension getMinimumSize() {
            return JBUI.size(120);
        }

        @Override
        public Dimension getPreferredSize() {
            return JBUI.size(150);
        }
    }

    private static void drawCenteredString(Graphics2D g2d, FontMetrics fm, int fontHeight, String text, int x, int y) {
        int width = fm.stringWidth(text);
        UIUtil.drawCenteredString(g2d, new Rectangle(x - width / 2, y - fontHeight / 2, width, fontHeight), text);
    }

    private static class ValueCellRenderer implements TableCellRenderer {
        private static final Map<Class, Renderer> RENDERERS = new HashMap<>();

        static {
            RENDERERS.put(Point.class, new PointRenderer());
            RENDERERS.put(Dimension.class, new DimensionRenderer());
            RENDERERS.put(Insets.class, new InsetsRenderer());
            RENDERERS.put(Rectangle.class, new RectangleRenderer());
            RENDERERS.put(Color.class, new ColorRenderer());
            RENDERERS.put(Font.class, new FontRenderer());
            RENDERERS.put(Boolean.class, new BooleanRenderer());
            RENDERERS.put(Icon.class, new IconRenderer());
            RENDERERS.put(Border.class, new BorderRenderer());
        }

        private static final Renderer<Object> DEFAULT_RENDERER = new ObjectRenderer();

        private static final JLabel NULL_RENDERER = new JLabel("-");

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            if (value == null) {
                NULL_RENDERER.setOpaque(isSelected);
                NULL_RENDERER.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                NULL_RENDERER.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return NULL_RENDERER;
            }

            Renderer<Object> renderer = ObjectUtil.notNull(getRenderer(value.getClass()), DEFAULT_RENDERER);

            JComponent result = renderer.setValue(value);
            result.setOpaque(isSelected);
            result.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            result.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return result;
        }

        @Nullable
        private static Renderer<Object> getRenderer(Class clazz) {
            if (clazz == null) {
                return null;
            }

            Renderer<Object> renderer = (Renderer<Object>) RENDERERS.get(clazz);
            if (renderer != null) {
                return renderer;
            }

            Class[] interfaces = clazz.getInterfaces();
            for (Class aClass : interfaces) {
                renderer = getRenderer(aClass);
                if (renderer != null) {
                    return renderer;
                }
            }
            clazz = clazz.getSuperclass();
            if (clazz != null) {
                return getRenderer(clazz);
            }
            return null;
        }
    }

    private interface Renderer<T> {
        JComponent setValue(@Nonnull T value);
    }

    private static class PointRenderer extends JLabel implements Renderer<Point> {
        @Override
        public JComponent setValue(@Nonnull final Point value) {
            setText(String.valueOf(value.x) + ':' + value.y);
            return this;
        }
    }

    private static class DimensionRenderer extends JLabel implements Renderer<Dimension> {
        @Override
        public JComponent setValue(@Nonnull final Dimension value) {
            setText(String.valueOf(value.width) + "x" + value.height);
            return this;
        }
    }

    private static class InsetsRenderer extends JLabel implements Renderer<Insets> {
        @Override
        public JComponent setValue(@Nonnull final Insets value) {
            setText("top: " + value.top + " left:" + value.left + " bottom:" + value.bottom + " right:" + value.right);
            return this;
        }
    }

    private static class RectangleRenderer extends JLabel implements Renderer<Rectangle> {
        @Override
        public JComponent setValue(@Nonnull final Rectangle value) {
            setText(toString(value));
            return this;
        }

        @Nonnull
        static String toString(@Nonnull Rectangle r) {
            return r.width + "x" + r.height + " @ " + r.x + ":" + r.y;
        }
    }

    private static class ColorRenderer extends JLabel implements Renderer<Color> {
        @Override
        public JComponent setValue(@Nonnull final Color value) {
            StringBuilder sb = new StringBuilder();
            sb.append(" r:").append(value.getRed());
            sb.append(" g:").append(value.getGreen());
            sb.append(" b:").append(value.getBlue());
            sb.append(" a:").append(value.getAlpha());

            sb.append(" argb:0x");
            String hex = Integer.toHexString(value.getRGB());
            for (int i = hex.length(); i < 8; i++) sb.append('0');
            sb.append(hex.toUpperCase(ENGLISH));

            if (value instanceof UIResource) {
                sb.append(" UIResource");
            }
            setText(sb.toString());
            setIcon(createColorIcon(value));
            return this;
        }
    }

    private static class FontRenderer extends JLabel implements Renderer<Font> {
        @Override
        public JComponent setValue(@Nonnull final Font value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value.getFontName()).append(" (").append(value.getFamily()).append("), ").append(value.getSize()).append("px");
            if (Font.BOLD == (Font.BOLD & value.getStyle())) {
                sb.append(" bold");
            }
            if (Font.ITALIC == (Font.ITALIC & value.getStyle())) {
                sb.append(" italic");
            }
            if (value instanceof UIResource) {
                sb.append(" UIResource");
            }
            setText(sb.toString());
            return this;
        }
    }

    private static class BooleanRenderer extends JLabel implements Renderer<Boolean> {
        @Override
        public JComponent setValue(@Nonnull final Boolean value) {
            setText(value ? "Yes" : "No");
            return this;
        }
    }

    private static class IconRenderer extends JLabel implements Renderer<Icon> {
        @Override
        public JComponent setValue(@Nonnull final Icon value) {
            setIcon(value);
            setText(getToStringValue(value));
            return this;
        }
    }

    private static class BorderRenderer extends JLabel implements Renderer<Border> {
        @Override
        public JComponent setValue(@Nonnull final Border value) {
            setText(getTextDescription(value));

            if (value instanceof CompoundBorder compoundBorder) {
                Color insideColor = getBorderColor(compoundBorder.getInsideBorder());
                Color outsideColor = getBorderColor(compoundBorder.getOutsideBorder());
                if (insideColor != null && outsideColor != null) {
                    setIcon(TargetAWT.to(createColorIcon(outsideColor, insideColor)));
                }
                else if (insideColor != null) {
                    setIcon(createColorIcon(insideColor));
                }
                else if (outsideColor != null) {
                    setIcon(createColorIcon(outsideColor));
                }
                else {
                    setIcon(null);
                }
            }
            else {
                Color color = getBorderColor(value);
                setIcon(color != null ? createColorIcon(color) : null);
            }
            return this;
        }

        @Nullable
        private static Color getBorderColor(@Nonnull Border value) {
            if (value instanceof LineBorder lineBorder) {
                return lineBorder.getLineColor();
            }
            else if (value instanceof CustomLineBorder) {
                try {
                    return (Color) ReflectionUtil.findField(CustomLineBorder.class, Color.class, "myColor").get(value);
                }
                catch (Exception ignore) {
                }
            }

            return null;
        }

        @Nonnull
        private static String getTextDescription(@Nonnull Border value) {
            StringBuilder sb = new StringBuilder();
            sb.append(getClassName(value));

            Color color = getBorderColor(value);
            if (color != null) {
                sb.append(" color=").append(color.toString());
            }

            if (value instanceof LineBorder lineBorder) {
                if (lineBorder.getRoundedCorners()) {
                    sb.append(" roundedCorners=true");
                }
            }
            if (value instanceof TitledBorder titledBorder) {
                sb.append(" title='").append(titledBorder.getTitle()).append("'");
            }
            if (value instanceof CompoundBorder compoundBorder) {
                sb.append(" inside={").append(getTextDescription(compoundBorder.getInsideBorder())).append("}");
                sb.append(" outside={").append(getTextDescription(compoundBorder.getOutsideBorder())).append("}");
            }

            if (value instanceof UIResource) {
                sb.append(" UIResource");
            }
            sb.append(" (").append(getToStringValue(value)).append(")");

            return sb.toString();
        }
    }

    private static class ObjectRenderer extends JLabel implements Renderer<Object> {
        {
            putClientProperty("html.disable", Boolean.TRUE);
        }

        @Override
        public JComponent setValue(@Nonnull final Object value) {
            setText(getToStringValue(value));
            return this;
        }
    }

    @Nonnull
    private static String getToStringValue(@Nonnull Object value) {
        String toString = StringUtil.notNullize(String.valueOf(value), "toString()==null");
        return toString.replace('\n', ' ');
    }

    @Nonnull
    private static String getClassName(Object value) {
        Class<?> clazz0 = value.getClass();
        Class<?> clazz = clazz0.isAnonymousClass() ? clazz0.getSuperclass() : clazz0;
        return clazz.getSimpleName();
    }

    private static ColorIcon createColorIcon(Color color) {
        return JBUI.scale(new ColorIcon(13, 11, color, true));
    }

    private static Image createColorIcon(Color color1, @Nullable Color color2) {
        if (color1 == null && color2 == null) {
            return Image.empty(11);
        }
        if (color1 == null) {
            return ImageEffects.colorFilled(11, 11, TargetAWT.from(color2));
        }
        if (color2 == null) {
            return ImageEffects.colorFilled(11, 11, TargetAWT.from(color1));
        }
        return ImageEffects.twoColorFilled(11, 11, TargetAWT.from(color1), TargetAWT.from(color2));
    }


    private static class PropertyBean {
        final String propertyName;
        final Object propertyValue;
        final boolean changed;

        PropertyBean(String name, Object value) {
            this(name, value, false);
        }

        PropertyBean(String name, Object value, boolean changed) {
            propertyName = name;
            propertyValue = value;
            this.changed = changed;
        }
    }

    private static class InspectorTableModel extends AbstractTableModel {

        final List<String> PROPERTIES =
            Arrays.asList(
                "getUI",
                "getLocation",
                "getLocationOnScreen",
                "getSize",
                "isOpaque",
                "getBorder",
                "getForeground",
                "getBackground",
                "getFont",
                "getCellRenderer",
                "getCellEditor",
                "getMinimumSize",
                "getMaximumSize",
                "getPreferredSize",
                "getText",
                "isEditable",
                "getIcon",
                "getVisibleRect",
                "getLayout",
                "getAlignmentX",
                "getAlignmentY",
                "getTooltipText",
                "getToolTipText",
                "isShowing",
                "isEnabled",
                "isVisible",
                "isDoubleBuffered",
                "isFocusable",
                "isFocusCycleRoot",
                "isFocusOwner",
                "isValid",
                "isDisplayable",
                "isLightweight"
            );

        final List<String> CHECKERS =
            Arrays.asList("isForegroundSet", "isBackgroundSet", "isFontSet", "isMinimumSizeSet", "isMaximumSizeSet", "isPreferredSizeSet");

        final List<String> ACCESSIBLE_CONTEXT_PROPERTIES = Arrays.asList(
            "getAccessibleRole",
            "getAccessibleName",
            "getAccessibleDescription",
            "getAccessibleAction",
            "getAccessibleChildrenCount",
            "getAccessibleIndexInParent",
            "getAccessibleRelationSet",
            "getAccessibleStateSet",
            "getAccessibleEditableText",
            "getAccessibleTable",
            "getAccessibleText",
            "getAccessibleValue",
            "accessibleChangeSupport"
        );

        final Component myComponent;
        final List<PropertyBean> myProperties = new ArrayList<>();

        InspectorTableModel(@Nonnull List<PropertyBean> clickInfo) {
            myComponent = null;
            myProperties.addAll(clickInfo);
        }

        InspectorTableModel(@Nonnull Component c) {
            myComponent = c;

            fillTable();
        }

        void fillTable() {
            addProperties("", myComponent, PROPERTIES);
            Object addedAt = myComponent instanceof JComponent jComponent ? jComponent.getClientProperty("uiInspector.addedAt") : null;
            myProperties.add(new PropertyBean("added-at", addedAt));

            // Add properties related to Accessibility support. This is useful for manually
            // inspecting what kind (if any) of accessibility support components expose.
            boolean isAccessible = myComponent instanceof Accessible;
            myProperties.add(new PropertyBean("accessible", isAccessible));
            AccessibleContext context = myComponent.getAccessibleContext();
            myProperties.add(new PropertyBean("accessibleContext", context));
            if (isAccessible) {
                addProperties("  ", myComponent.getAccessibleContext(), ACCESSIBLE_CONTEXT_PROPERTIES);
            }

            if (myComponent instanceof Container container) {
                try {
                    addLayoutProperties(container);
                }
                catch (Exception ignored) {
                }
            }
        }

        private void addProperties(@Nonnull String prefix, @Nonnull Object component, @Nonnull List<String> methodNames) {
            Class<?> clazz0 = component.getClass();
            Class<?> clazz = clazz0.isAnonymousClass() ? clazz0.getSuperclass() : clazz0;
            myProperties.add(new PropertyBean(prefix + "class", clazz.getName()));
            StringBuilder classHierarchy = new StringBuilder();
            for (Class<?> cl = clazz.getSuperclass(); cl != null; cl = cl.getSuperclass()) {
                if (classHierarchy.length() > 0) {
                    classHierarchy.append(" -> ");
                }
                classHierarchy.append(cl.getName());
                if (JComponent.class.getName().equals(cl.getName())) {
                    break;
                }
            }
            myProperties.add(new PropertyBean(prefix + "hierarchy", classHierarchy.toString()));
            for (String name : methodNames) {
                String propertyName = ObjectUtil.notNull(StringUtil.getPropertyName(name), name);
                Object propertyValue;
                try {
                    try {
                        //noinspection ConstantConditions
                        propertyValue = ReflectionUtil.findMethod(Arrays.asList(clazz.getMethods()), name).invoke(component);
                    }
                    catch (Exception e) {
                        propertyValue = ReflectionUtil.findField(clazz, null, name).get(component);
                    }
                    boolean changed = false;
                    try {
                        final String checkerMethodName = "is" + StringUtil.capitalize(propertyName) + "Set";
                        if (CHECKERS.contains(checkerMethodName)) {
                            final Object value =
                                ReflectionUtil.findMethod(Arrays.asList(clazz.getMethods()), checkerMethodName).invoke(component);
                            if (value instanceof Boolean b) {
                                changed = b;
                            }
                        }
                    }
                    catch (Exception e) {
                        changed = false;
                    }
                    myProperties.add(new PropertyBean(prefix + propertyName, propertyValue, changed));
                }
                catch (Exception ignored) {
                }
            }
        }

        private void addLayoutProperties(@Nonnull Container component) {
            String prefix = "  ";

            LayoutManager layout = component.getLayout();
            if (layout instanceof GridBagLayout bagLayout) {
                GridBagConstraints defaultConstraints =
                    ReflectionUtil.getField(GridBagLayout.class, bagLayout, GridBagConstraints.class, "defaultConstraints");

                myProperties.add(new PropertyBean(
                    "GridBagLayout constraints",
                    String.format("defaultConstraints - %s", toString(defaultConstraints))
                ));
                if (bagLayout.columnWidths != null) {
                    myProperties.add(new PropertyBean(prefix + "columnWidths", Arrays.toString(bagLayout.columnWidths)));
                }
                if (bagLayout.rowHeights != null) {
                    myProperties.add(new PropertyBean(prefix + "rowHeights", Arrays.toString(bagLayout.rowHeights)));
                }
                if (bagLayout.columnWeights != null) {
                    myProperties.add(new PropertyBean(prefix + "columnWeights", Arrays.toString(bagLayout.columnWeights)));
                }
                if (bagLayout.rowWeights != null) {
                    myProperties.add(new PropertyBean(prefix + "rowWeights", Arrays.toString(bagLayout.rowWeights)));
                }

                for (Component child : component.getComponents()) {
                    myProperties.add(new PropertyBean(prefix + getComponentName(child), toString(bagLayout.getConstraints(child))));
                }
            }
            else if (layout instanceof BorderLayout borderLayout) {
                myProperties.add(new PropertyBean(
                    "BorderLayout constraints",
                    String.format("hgap - %s, vgap - %s", borderLayout.getHgap(), borderLayout.getVgap())
                ));

                for (Component child : component.getComponents()) {
                    myProperties.add(new PropertyBean(prefix + getComponentName(child), borderLayout.getConstraints(child)));
                }
            }
            else if (layout instanceof CardLayout cardLayout) {
                Integer currentCard = ReflectionUtil.getField(CardLayout.class, cardLayout, null, "currentCard");
                //noinspection UseOfObsoleteCollectionType
                Vector vector = ReflectionUtil.getField(CardLayout.class, cardLayout, Vector.class, "vector");
                String cardDescription = "???";
                if (vector != null && currentCard != null) {
                    Object card = vector.get(currentCard);
                    cardDescription = ReflectionUtil.getField(card.getClass(), card, String.class, "name");
                }

                myProperties.add(new PropertyBean(
                    "CardLayout constraints",
                    String.format("card - %s, hgap - %s, vgap - %s", cardDescription, cardLayout.getHgap(), cardLayout.getVgap())
                ));

                if (vector != null) {
                    for (Object card : vector) {
                        String cardName = ReflectionUtil.getField(card.getClass(), card, String.class, "name");
                        Component child = ReflectionUtil.getField(card.getClass(), card, Component.class, "comp");
                        myProperties.add(new PropertyBean(prefix + getComponentName(child), cardName));
                    }
                }
            }
            else if (layout instanceof MigLayout migLayout) {
                myProperties.add(new PropertyBean("MigLayout constraints", migLayout.getColumnConstraints()));

                for (Component child : component.getComponents()) {
                    myProperties.add(new PropertyBean(prefix + getComponentName(child), migLayout.getComponentConstraints(child)));
                }
            }
        }

        @Nonnull
        private static String toString(@Nullable GridBagConstraints constraints) {
            if (constraints == null) {
                return "null";
            }

            MoreObjects.ToStringHelper h = MoreObjects.toStringHelper("");
            appendFieldValue(h, constraints, "gridx");
            appendFieldValue(h, constraints, "gridy");
            appendFieldValue(h, constraints, "gridwidth");
            appendFieldValue(h, constraints, "gridheight");
            appendFieldValue(h, constraints, "weightx");
            appendFieldValue(h, constraints, "weighty");
            appendFieldValue(h, constraints, "anchor");
            appendFieldValue(h, constraints, "fill");
            appendFieldValue(h, constraints, "insets");
            appendFieldValue(h, constraints, "ipadx");
            appendFieldValue(h, constraints, "ipady");
            return h.toString();
        }

        private static void appendFieldValue(
            @Nonnull MoreObjects.ToStringHelper h,
            @Nonnull GridBagConstraints constraints,
            @Nonnull String field
        ) {
            Object value = ReflectionUtil.getField(GridBagConstraints.class, constraints, null, field);
            Object defaultValue = ReflectionUtil.getField(GridBagConstraints.class, new GridBagConstraints(), null, field);
            if (!Comparing.equal(value, defaultValue)) {
                h.add(field, value);
            }
        }

        @Override
        @Nullable
        public Object getValueAt(int row, int column) {
            final PropertyBean bean = myProperties.get(row);
            if (bean != null) {
                switch (column) {
                    case 0:
                        return bean.propertyName;
                    default:
                        return bean.propertyValue;
                }
            }

            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 1 && updater(myProperties.get(row)) != null;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            PropertyBean bean = myProperties.get(row);
            try {
                myProperties.set(row, new PropertyBean(bean.propertyName, ObjectUtil.notNull(updater(bean)).apply(value)));
            }
            catch (Exception ignored) {
            }
        }

        @Nullable
        public Function<Object, Object> updater(PropertyBean bean) {
            if (myComponent == null) {
                return null;
            }

            String name = bean.propertyName.trim();
            try {
                try {
                    Method getter;
                    try {
                        getter = myComponent.getClass().getMethod("get" + StringUtil.capitalize(name));
                    }
                    catch (Exception e) {
                        getter = myComponent.getClass().getMethod("is" + StringUtil.capitalize(name));
                    }
                    final Method finalGetter = getter;
                    final Method setter = myComponent.getClass().getMethod("set" + StringUtil.capitalize(name), getter.getReturnType());
                    setter.setAccessible(true);
                    return o -> {
                        try {
                            setter.invoke(myComponent, fromObject(o, finalGetter.getReturnType()));
                            return finalGetter.invoke(myComponent);
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
                catch (Exception e) {
                    final Field field = ReflectionUtil.findField(myComponent.getClass(), null, name);
                    if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                        return null;
                    }
                    return o -> {
                        try {
                            field.set(myComponent, fromObject(o, field.getType()));
                            return field.get(myComponent);
                        }
                        catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    };
                }
            }
            catch (Exception ignored) {
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return myProperties.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnIndex == 0 ? "Property" : "Value";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? String.class : Object.class;
        }

        public void refresh() {
            myProperties.clear();
            fillTable();
            fireTableDataChanged();
        }
    }

    private static class UiInspector implements AWTEventListener, Disposable {
        public UiInspector() {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.CONTAINER_EVENT_MASK);
        }

        @Override
        public void dispose() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            for (Window window : Window.getWindows()) {
                if (window instanceof InspectorWindow inspectorWindow) {
                    inspectorWindow.close();
                }
            }
        }

        public void showInspector(@Nonnull Component c) {
            Window window = new InspectorWindow(c);
            window.pack();
            window.setVisible(true);
            window.toFront();
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent mouseEvent) {
                processMouseEvent(mouseEvent);
            }
            else if (event instanceof ContainerEvent containerEvent) {
                processContainerEvent(containerEvent);
            }
        }

        private void processMouseEvent(MouseEvent me) {
            if (!me.isAltDown() || !me.isControlDown()) {
                return;
            }
            if (me.getClickCount() != 1 || me.isPopupTrigger()) {
                return;
            }
            me.consume();
            if (me.getID() != MouseEvent.MOUSE_RELEASED) {
                return;
            }
            Component component = me.getComponent();

            if (component instanceof Container container) {
                component = container.findComponentAt(me.getPoint());
            }
            else if (component == null) {
                component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            }
            if (component != null) {
                if (component instanceof JComponent jComponent) {
                    jComponent.putClientProperty(CLICK_INFO, getClickInfo(me, component));
                }
                showInspector(component);
            }
        }

        private static List<PropertyBean> getClickInfo(MouseEvent me, Component component) {
            if (me.getComponent() == null) {
                return null;
            }
            me = SwingUtilities.convertMouseEvent(me.getComponent(), me, component);
            List<PropertyBean> clickInfo = new ArrayList<>();
            //clickInfo.add(new PropertyBean("Click point", me.getPoint()));
            if (component instanceof JList list) {
                int row = list.getUI().locationToIndex(list, me.getPoint());
                if (row != -1) {
                    Component rendererComponent = list.getCellRenderer().getListCellRendererComponent(
                        list,
                        list.getModel().getElementAt(row),
                        row,
                        list.getSelectionModel().isSelectedIndex(row),
                        list.hasFocus()
                    );
                    clickInfo.add(new PropertyBean(RENDERER_BOUNDS, list.getUI().getCellBounds(list, row, row)));
                    clickInfo.addAll(new InspectorTableModel(rendererComponent).myProperties);
                    return clickInfo;
                }
            }
            if (component instanceof JTable table) {
                int row = table.rowAtPoint(me.getPoint());
                int column = table.columnAtPoint(me.getPoint());
                if (row != -1 && column != -1) {
                    Component rendererComponent = table.getCellRenderer(row, column).getTableCellRendererComponent(
                        table,
                        table.getValueAt(row, column),
                        table.getSelectionModel().isSelectedIndex(row),
                        table.hasFocus(),
                        row,
                        column
                    );
                    clickInfo.add(new PropertyBean(RENDERER_BOUNDS, table.getCellRect(row, column, true)));
                    clickInfo.addAll(new InspectorTableModel(rendererComponent).myProperties);
                    return clickInfo;
                }
            }
            if (component instanceof JTree tree) {
                TreePath path = tree.getClosestPathForLocation(me.getX(), me.getY());
                if (path != null) {
                    Object object = path.getLastPathComponent();
                    Component rendererComponent = tree.getCellRenderer().getTreeCellRendererComponent(
                        tree,
                        object,
                        tree.getSelectionModel().isPathSelected(path),
                        tree.isExpanded(path),
                        tree.getModel().isLeaf(object),
                        tree.getRowForPath(path),
                        tree.hasFocus()
                    );
                    clickInfo.add(new PropertyBean(RENDERER_BOUNDS, tree.getPathBounds(path)));
                    clickInfo.addAll(new InspectorTableModel(rendererComponent).myProperties);
                    return clickInfo;
                }
            }
            return null;
        }

        private static void processContainerEvent(ContainerEvent event) {
            Component child = event.getID() == ContainerEvent.COMPONENT_ADDED ? event.getChild() : null;
            if (child instanceof JComponent childComponent && !(event.getSource() instanceof CellRendererPane)) {
                String text = ExceptionUtil.getThrowableText(new Throwable());
                int first = text.indexOf("at com.intellij", text.indexOf("at java.awt"));
                int last = text.indexOf("at java.awt.EventQueue");
                if (last == -1) {
                    last = text.length();
                }
                String val = last > first && first > 0 ? text.substring(first, last) : null;
                childComponent.putClientProperty("uiInspector.addedAt", val);
            }
        }
    }

    @SuppressWarnings({"UseJBColor", "UseDPIAwareInsets"})
    private static Object fromObject(Object o, Class<?> type) {
        if (o == null) {
            return null;
        }
        if (type.isAssignableFrom(o.getClass())) {
            return o;
        }
        if ("null".equals(o)) {
            return null;
        }

        String value = String.valueOf(o).trim();
        if (type == int.class) {
            return Integer.parseInt(value);
        }
        if (type == boolean.class) {
            return "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
        }
        if (type == byte.class) {
            return Byte.parseByte(value);
        }
        if (type == short.class) {
            return Short.parseShort(value);
        }
        if (type == double.class) {
            return Double.parseDouble(value);
        }
        if (type == float.class) {
            return Float.parseFloat(value);
        }

        String[] s = value.split("(?i)\\s*(?:[x@:]|[a-z]+:)\\s*", 6);
        if (type == Dimension.class) {
            if (s.length == 2) {
                return new Dimension(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
            }
        }
        else if (type == Point.class) {
            if (s.length == 2) {
                return new Point(Integer.parseInt(s[0]), Integer.parseInt(s[1]));
            }
        }
        else if (type == Rectangle.class) {
            if (s.length >= 5) {
                return new Rectangle(Integer.parseInt(s[3]), Integer.parseInt(s[4]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
            }
        }
        else if (type == Insets.class) {
            if (s.length >= 5) {
                //noinspection UseDPIAwareInsets
                return new Insets(Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[4]), Integer.parseInt(s[4]));
            }
        }
        else if (type == Color.class) {
            if (s.length >= 5) {
                return new ColorUIResource(new Color(
                    Integer.parseInt(s[1]),
                    Integer.parseInt(s[2]),
                    Integer.parseInt(s[3]),
                    Integer.parseInt(s[4])
                ));
            }
        }
        throw new UnsupportedOperationException(type.toString());
    }
}
