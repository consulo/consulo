/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.notification.impl.ui;

import consulo.application.util.SystemInfo;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.notification.impl.NotificationSettings;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.openapi.ui.ComboBoxTableRenderer;
import consulo.ide.impl.idea.openapi.ui.StripeTable;
import consulo.ide.impl.project.ui.impl.NotificationGroupRegistrator;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.ex.SystemNotifications;
import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.tree.IndexTreePathState;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 * @author spleaner
 */
public class NotificationsConfigurablePanel extends JPanel implements Disposable {
  private static final String REMOVE_KEY = "REMOVE";

  private NotificationsTreeTable myTable;
  private final JCheckBox myDisplayBalloons;
  private final JCheckBox mySystemNotifications;

  public NotificationsConfigurablePanel() {
    setLayout(new BorderLayout(5, 5));
    myTable = new NotificationsTreeTable();

    myDisplayBalloons = new JCheckBox("Display balloon notifications");
    myDisplayBalloons.setMnemonic('b');
    myDisplayBalloons.addActionListener(e -> myTable.repaint());

    mySystemNotifications = new JCheckBox("Enable system notifications");
    mySystemNotifications.setMnemonic('s');
    mySystemNotifications.setVisible(SystemNotifications.getInstance().isAvailable());

    JPanel boxes = new JPanel();
    boxes.setLayout(new BoxLayout(boxes, BoxLayout.Y_AXIS));
    boxes.add(myDisplayBalloons);
    boxes.add(mySystemNotifications);
    add(boxes, BorderLayout.NORTH);

    add(ScrollPaneFactory.createScrollPane(myTable), BorderLayout.CENTER);
    myTable.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), REMOVE_KEY);
    myTable.getActionMap().put(REMOVE_KEY, new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        removeSelected();
      }
    });
  }

  private void removeSelected() {
    myTable.removeSelected();
  }

  public void dispose() {
    myTable = null;
  }

  public boolean isModified() {
    final List<SettingsWrapper> list = myTable.getAllSettings();
    for (SettingsWrapper settingsWrapper : list) {
      if (settingsWrapper.hasChanged()) {
        return true;
      }
    }

    NotificationsConfigurationImpl configuration = NotificationsConfigurationImpl.getInstanceImpl();
    return configuration.SHOW_BALLOONS != myDisplayBalloons.isSelected() || configuration.SYSTEM_NOTIFICATIONS != mySystemNotifications.isSelected();
  }

  public void apply() {
    final List<SettingsWrapper> list = myTable.getAllSettings();
    for (SettingsWrapper settingsWrapper : list) {
      settingsWrapper.apply();
    }

    NotificationsConfigurationImpl configuration = NotificationsConfigurationImpl.getInstanceImpl();
    configuration.SHOW_BALLOONS = myDisplayBalloons.isSelected();
    configuration.SYSTEM_NOTIFICATIONS = mySystemNotifications.isSelected();
  }

  public void reset() {
    final List<SettingsWrapper> list = myTable.getAllSettings();
    for (SettingsWrapper settingsWrapper : list) {
      settingsWrapper.reset();
    }

    NotificationsConfigurationImpl configuration = NotificationsConfigurationImpl.getInstanceImpl();
    myDisplayBalloons.setSelected(configuration.SHOW_BALLOONS);
    mySystemNotifications.setSelected(configuration.SYSTEM_NOTIFICATIONS);

    myTable.invalidate();
    myTable.repaint();
  }

  private static class SettingsWrapper {
    private boolean myRemoved = false;
    private NotificationSettings mySettings;
    private final LocalizeValue myDisplayName;

    private SettingsWrapper(NotificationSettings settings, LocalizeValue displayName) {
      mySettings = settings;
      myDisplayName = displayName;
    }

    public boolean hasChanged() {
      return myRemoved || !getOriginalSettings().equals(mySettings);
    }

    public void remove() {
      myRemoved = true;
    }

    public boolean isRemoved() {
      return myRemoved;
    }

    @Nonnull
    private NotificationSettings getOriginalSettings() {
      return NotificationsConfigurationImpl.getSettings(getGroupId());
    }

    public void apply() {
      if (myRemoved) {
        NotificationsConfigurationImpl.remove(getGroupId());
      }
      else {
        NotificationsConfigurationImpl.getInstanceImpl().changeSettings(mySettings);
      }
    }

    public void reset() {
      mySettings = getOriginalSettings();
      myRemoved = false;
    }

    String getGroupId() {
      return mySettings.getGroupId();
    }

    public LocalizeValue getDisplayName() {
      return myDisplayName;
    }

    @Override
    public String toString() {
      return getGroupId() + ":" + myDisplayName;
    }
  }

  private class NotificationsTreeTable extends TreeTable {
    private static final int ID_COLUMN = 0;
    private static final int DISPLAY_TYPE_COLUMN = 1;
    private static final int LOG_COLUMN = 2;
    private static final int READ_ALOUD_COLUMN = 3;

    public NotificationsTreeTable() {
      super(new NotificationsTreeTableModel());
      StripeTable.apply(this);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      getTree().setCellRenderer(new TreeColumnCellRenderer(this));

      final TableColumn idColumn = getColumnModel().getColumn(ID_COLUMN);
      idColumn.setCellRenderer(new ColoredTableCellRenderer() {
        @Override
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
          Object userObject = node.getUserObject();
          if (userObject instanceof SettingsWrapper wrapper) {
            append(wrapper.myDisplayName.get());
          }
        }
      });
      idColumn.setPreferredWidth(200);

      final TableColumn displayTypeColumn = getColumnModel().getColumn(DISPLAY_TYPE_COLUMN);
      displayTypeColumn.setMaxWidth(300);
      displayTypeColumn.setPreferredWidth(250);
      displayTypeColumn.setCellRenderer(new ComboBoxTableRenderer<>(NotificationDisplayType.values()) {
        @Override
        protected void customizeComponent(NotificationDisplayType value, JTable table, boolean isSelected) {
          super.customizeComponent(myDisplayBalloons.isSelected() ? value : NotificationDisplayType.NONE, table, isSelected);
          if (!myDisplayBalloons.isSelected() && !isSelected) {
            setBackground(UIUtil.getComboBoxDisabledBackground());
            setForeground(UIUtil.getComboBoxDisabledForeground());
          }
        }

        @Override
        protected String getTextFor(@Nonnull NotificationDisplayType value) {
          return value.getTitle();
        }
      });

      displayTypeColumn.setCellEditor(new ComboBoxTableRenderer<>(NotificationDisplayType.values()) {
        @Override
        public boolean isCellEditable(EventObject event) {
          if (!myDisplayBalloons.isSelected()) {
            return false;
          }

          if (event instanceof MouseEvent) {
            return ((MouseEvent)event).getClickCount() >= 1;
          }

          return false;
        }

        @Override
        protected boolean isApplicable(NotificationDisplayType value, int row) {
          if (value != NotificationDisplayType.TOOL_WINDOW) return true;

          Object wrapper = ((NotificationsTreeTableModel)getTableModel()).getRowValue(row).second;
          String groupId = ((SettingsWrapper)wrapper).getGroupId();
          return NotificationsConfigurationImpl.getInstanceImpl().hasToolWindowCapability(groupId);
        }

        @Override
        protected String getTextFor(@Nonnull NotificationDisplayType value) {
          return value.getTitle();
        }
      });

      final TableColumn logColumn = getColumnModel().getColumn(LOG_COLUMN);
      logColumn.setMaxWidth(logColumn.getPreferredWidth());
      logColumn.setCellRenderer(new BooleanTableCellRenderer());

      if (SystemInfo.isMac) {
        final TableColumn readAloudColumn = getColumnModel().getColumn(READ_ALOUD_COLUMN);
        readAloudColumn.setMaxWidth(readAloudColumn.getPreferredWidth());
        readAloudColumn.setCellRenderer(new BooleanTableCellRenderer());
      }

      new TableSpeedSearch(this);
      getEmptyText().setText("No notifications configured");
      TreeUtil.expandAll(getTree());
    }

    @Override
    public Dimension getMinimumSize() {
      return calcSize(super.getMinimumSize());
    }

    @Override
    public Dimension getPreferredSize() {
      return calcSize(super.getPreferredSize());
    }

    private Dimension calcSize(@Nonnull final Dimension s) {
      final Container container = getParent();
      if (container != null) {
        final Dimension size = container.getSize();
        return new Dimension(size.width, s.height);
      }

      return s;
    }

    public List<SettingsWrapper> getAllSettings() {
      return ((NotificationsTreeTableModel)getTableModel()).getAllSettings();
    }

    public void removeSelected() {
      ListSelectionModel selectionModel = getSelectionModel();
      if (!selectionModel.isSelectionEmpty()) {
        int selection = selectionModel.getMinSelectionIndex();
        TreePath newSelection = ((NotificationsTreeTableModel)getTableModel()).removeRow(selection);
        addSelectedPath(newSelection);
      }
    }
  }

  private static class TreeColumnCellRenderer extends JLabel implements TreeCellRenderer {
    private final JTable myTable;

    public TreeColumnCellRenderer(@Nonnull JTable table) {
      myTable = table;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      setForeground(selected ? myTable.getSelectionForeground() : myTable.getForeground());
      setText(value.toString());
      return this;
    }
  }

  private static class NotificationsTreeTableModel extends DefaultTreeModel implements TreeTableModel {
    private final List<SettingsWrapper> mySettings = new ArrayList<>();
    private JTree myTree;

    public NotificationsTreeTableModel() {
      super(null);

      List<DefaultMutableTreeNode> rootChildren = new ArrayList<>();

      for (NotificationSettings setting : NotificationsConfigurationImpl.getInstanceImpl().getAllSettings()) {
        NotificationGroup group = NotificationGroupRegistrator.last().get(setting.getGroupId());
        SettingsWrapper wrapper = new SettingsWrapper(setting, group == null ? LocalizeValue.of(setting.getGroupId()) : group.getDisplayName());
        mySettings.add(wrapper);

        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(wrapper, false);
        rootChildren.add(treeNode);
      }

      Collections.sort(rootChildren, (node1, node2) -> {
        Object object1 = node1.getUserObject();
        Object object2 = node2.getUserObject();
        if (object2 instanceof SettingsWrapper) {
          return ((SettingsWrapper)object1).getDisplayName().compareTo(((SettingsWrapper)object2).getDisplayName());
        }
        return 1;
      });

      DefaultMutableTreeNode root = new DefaultMutableTreeNode();
      for (DefaultMutableTreeNode child : rootChildren) {
        root.add(child);
      }
      setRoot(root);
    }

    @Override
    public int getColumnCount() {
      return SystemInfo.isMac ? 4 : 3;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case NotificationsTreeTable.ID_COLUMN:
          return "Group";
        case NotificationsTreeTable.LOG_COLUMN:
          return "Log";
        case NotificationsTreeTable.READ_ALOUD_COLUMN:
          return "Read aloud";
        default:
          return "Popup";
      }
    }

    @Override
    public Class getColumnClass(int column) {
      if (NotificationsTreeTable.DISPLAY_TYPE_COLUMN == column) {
        return NotificationDisplayType.class;
      }
      if (NotificationsTreeTable.LOG_COLUMN == column) {
        return Boolean.class;
      }
      if (NotificationsTreeTable.READ_ALOUD_COLUMN == column) {
        return Boolean.class;
      }

      return TreeTableModel.class;
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
      return column > 0 && ((DefaultMutableTreeNode)node).getUserObject() instanceof SettingsWrapper;
    }

    @Override
    public Object getValueAt(Object node, int column) {
      if (column == 0) {
        return node;
      }

      Object object = ((DefaultMutableTreeNode)node).getUserObject();

      SettingsWrapper wrapper = (SettingsWrapper)object;
      switch (column) {
        case NotificationsTreeTable.LOG_COLUMN:
          return wrapper.mySettings.isShouldLog();
        case NotificationsTreeTable.READ_ALOUD_COLUMN:
          return wrapper.mySettings.isShouldReadAloud();
        case NotificationsTreeTable.DISPLAY_TYPE_COLUMN:
        default:
          return wrapper.mySettings.getDisplayType();
      }
    }

    @Override
    public void setValueAt(Object value, Object node, int column) {
      SettingsWrapper wrapper = (SettingsWrapper)((DefaultMutableTreeNode)node).getUserObject();

      switch (column) {
        case NotificationsTreeTable.DISPLAY_TYPE_COLUMN:
          wrapper.mySettings = wrapper.mySettings.withDisplayType((NotificationDisplayType)value);
          break;
        case NotificationsTreeTable.LOG_COLUMN:
          wrapper.mySettings = wrapper.mySettings.withShouldLog((Boolean)value);
          break;
        case NotificationsTreeTable.READ_ALOUD_COLUMN:
          wrapper.mySettings = wrapper.mySettings.withShouldReadAloud((Boolean)value);
          break;
      }
    }

    @Override
    public void setTree(JTree tree) {
      myTree = tree;
      tree.setRootVisible(false);
    }

    public TreePath removeRow(int row) {
      Pair<DefaultMutableTreeNode, Object> rowValue = getRowValue(row);
      if (rowValue.second instanceof SettingsWrapper) {
        ((SettingsWrapper)rowValue.second).remove();
      }
      else {
        removeChildSettings(rowValue.first);
      }
      return removeNode(rowValue.first);
    }

    private static void removeChildSettings(DefaultMutableTreeNode node) {
      int count = node.getChildCount();
      for (int i = 0; i < count; i++) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
        Object object = child.getUserObject();
        if (object instanceof SettingsWrapper) {
          ((SettingsWrapper)object).remove();
        }
        else {
          removeChildSettings(child);
        }
      }
    }

    private TreePath removeNode(DefaultMutableTreeNode node) {
      DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
      if (parent != null) {
        IndexTreePathState state = new IndexTreePathState(TreeUtil.getPathFromRoot(node));
        removeNodeFromParent(node);
        if (parent.isLeaf()) {
          return removeNode(parent);
        }
        return state.getRestoredPath();
      }
      return null;
    }

    public Pair<DefaultMutableTreeNode, Object> getRowValue(int row) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getPathForRow(row).getLastPathComponent();
      return Pair.create(node, node.getUserObject());
    }

    public List<SettingsWrapper> getAllSettings() {
      return mySettings;
    }
  }

  public void selectGroup(String searchQuery) {
    ObjectUtil.assertNotNull(SpeedSearchSupply.getSupply(myTable, true)).findAndSelectElement(searchQuery);
  }
}
