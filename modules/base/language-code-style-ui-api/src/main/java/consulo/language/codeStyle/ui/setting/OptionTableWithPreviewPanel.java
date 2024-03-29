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
package consulo.language.codeStyle.ui.setting;

import consulo.application.ApplicationBundle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.logging.Logger;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeTableSpeedSearch;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.table.ListTreeTableModel;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableCellRenderer;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.util.collection.ContainerUtil;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author max
 */
@SuppressWarnings("Duplicates")
public abstract class OptionTableWithPreviewPanel extends CustomizableLanguageCodeStylePanel {
  private static final Logger LOG = Logger.getInstance(OptionTableWithPreviewPanel.class);

  private final static KeyStroke ENTER_KEY_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

  protected TreeTable myTreeTable;
  private final JPanel myPanel = new JPanel();

  private final List<Option> myOptions = new ArrayList<>();
  private final List<Option> myCustomOptions = new ArrayList<>();
  private final Set<String> myAllowedOptions = new HashSet<>();
  private final Map<String, String> myRenamedFields = new HashMap<>();
  private boolean myShowAllStandardOptions;
  protected boolean isFirstUpdate = true;

  public OptionTableWithPreviewPanel(CodeStyleSettings settings) {
    super(settings);
  }

  @Override
  protected void init() {
    super.init();

    myPanel.setLayout(new GridBagLayout());
    initTables();

    myTreeTable = createOptionsTree(getSettings());
    myTreeTable.setBackground(UIUtil.getPanelBackground());
    myTreeTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
    JBScrollPane scrollPane = new JBScrollPane(myTreeTable) {
      @Override
      public Dimension getMinimumSize() {
        return super.getPreferredSize();
      }
    };
    myPanel.add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));

    final JPanel previewPanel = createPreviewPanel();
    myPanel.add(previewPanel, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));

    installPreviewPanel(previewPanel);
    addPanelToWatch(myPanel);

    isFirstUpdate = false;
    customizeSettings();
  }

  @Override
  protected void resetDefaultNames() {
    myRenamedFields.clear();
  }

  @Override
  public void showAllStandardOptions() {
    myShowAllStandardOptions = true;
    for (Option each : myOptions) {
      each.setEnabled(true);
    }
    for (Option each : myCustomOptions) {
      each.setEnabled(false);
    }
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    Collections.addAll(myAllowedOptions, optionNames);
    for (Option each : myOptions) {
      each.setEnabled(false);
      for (String optionName : optionNames) {
        if (each.getOptionName().equals(optionName)) {
          each.setEnabled(true);
        }
      }
    }
    for (Option each : myCustomOptions) {
      each.setEnabled(false);
    }
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass, String fieldName, String title, String groupName, Object... options) {
    showCustomOption(settingsClass, fieldName, title, groupName, null, null, options);
  }


  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                               String fieldName,
                               String title,
                               String groupName,
                               @Nullable OptionAnchor anchor,
                               @Nullable String anchorFieldName,
                               Object... options) {
    if (isFirstUpdate) {
      Option option;
      if (options.length == 2) {
        option = new SelectionOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName, (String[])options[0], (int[])options[1]);
      }
      else {
        option = new BooleanOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName);
      }
      myCustomOptions.add(option);
      option.setEnabled(true);
    }
    else {
      for (Option each : myCustomOptions) {
        if (each instanceof FieldOption && ((FieldOption)each).clazz == settingsClass && each.getOptionName().equals(fieldName)) {
          each.setEnabled(true);
        }
      }
    }
  }

  @Override
  public void renameStandardOption(String fieldName, String newTitle) {
    myRenamedFields.put(fieldName, newTitle);
  }

  public void showOption(@Nonnull String optionName) {
    myAllowedOptions.add(optionName);
  }

  protected TreeTable createOptionsTree(CodeStyleSettings settings) {
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    Map<String, DefaultMutableTreeNode> groupsMap = new HashMap<>();

    List<Option> sorted = sortOptions(ContainerUtil.concat(myOptions, myCustomOptions));
    for (Option each : sorted) {
      if (!(myCustomOptions.contains(each) || myAllowedOptions.contains(each.getOptionName()) || myShowAllStandardOptions)) continue;

      String group = each.groupName;
      MyTreeNode newNode = new MyTreeNode(each, each.title, settings);

      DefaultMutableTreeNode groupNode = groupsMap.get(group);
      if (groupNode != null) {
        groupNode.add(newNode);
      }
      else {
        String groupName;

        if (group == null) {
          groupName = each.title;
          groupNode = newNode;
        }
        else {
          groupName = group;
          groupNode = new DefaultMutableTreeNode(groupName);
          groupNode.add(newNode);
        }
        groupsMap.put(groupName, groupNode);
        rootNode.add(groupNode);
      }
    }

    ListTreeTableModel model = new ListTreeTableModel(rootNode, COLUMNS);
    TreeTable treeTable = new TreeTable(model) {
      @Override
      public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
        TreeTableCellRenderer tableRenderer = super.createTableRenderer(treeTableModel);
        UIUtil.setLineStyleAngled(tableRenderer);
        tableRenderer.setRootVisible(false);
        tableRenderer.setShowsRootHandles(true);

        return tableRenderer;
      }

      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellRenderer(row, column);

        Object node = treePath.getLastPathComponent();

        @SuppressWarnings("unchecked") TableCellRenderer renderer = COLUMNS[column].getRenderer(node);
        return renderer == null ? super.getCellRenderer(row, column) : renderer;
      }

      @Override
      public TableCellEditor getCellEditor(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellEditor(row, column);

        Object node = treePath.getLastPathComponent();
        @SuppressWarnings("unchecked") TableCellEditor editor = COLUMNS[column].getEditor(node);
        return editor == null ? super.getCellEditor(row, column) : editor;
      }
    };
    new TreeTableSpeedSearch(treeTable).setComparator(new SpeedSearchComparator(false));

    treeTable.setRootVisible(false);

    final JTree tree = treeTable.getTree();
    tree.setCellRenderer(myTitleRenderer);
    tree.setShowsRootHandles(true);
    //myTreeTable.setRowHeight(new JComboBox(new String[]{"Sample Text"}).getPreferredSize().height);
    treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    treeTable.setTableHeader(null);

    TreeUtil.expandAll(tree);

    treeTable.getColumnModel().getSelectionModel().setAnchorSelectionIndex(1);
    treeTable.getColumnModel().getSelectionModel().setLeadSelectionIndex(1);

    int maxWidth = tree.getPreferredScrollableViewportSize().width + 10;
    final TableColumn titleColumn = treeTable.getColumnModel().getColumn(0);
    titleColumn.setPreferredWidth(maxWidth);
    titleColumn.setMinWidth(maxWidth);
    titleColumn.setMaxWidth(maxWidth);
    titleColumn.setResizable(false);

    //final TableColumn levelColumn = treeTable.getColumnModel().getColumn(1);
    //TODO[max]: better preffered size...
    //TODO[kb]: Did I fixed it by making the last column floating?
    //levelColumn.setPreferredWidth(valueSize.width);
    //levelColumn.setMaxWidth(valueSize.width);
    //levelColumn.setMinWidth(valueSize.width);
    //levelColumn.setResizable(false);

    final Dimension valueSize = new JLabel(ApplicationBundle.message("option.table.sizing.text")).getPreferredSize();
    treeTable.setPreferredScrollableViewportSize(new Dimension(maxWidth + valueSize.width + 10, 20));

    return treeTable;
  }

  private String getRenamedTitle(String fieldOrGroupName, String defaultName) {
    String result = myRenamedFields.get(fieldOrGroupName);
    return result == null ? defaultName : result;
  }

  protected abstract void initTables();

  private static void resetNode(TreeNode node, CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).reset(settings);
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      resetNode(child, settings);
    }
  }

  private static void applyNode(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).apply(settings);
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      applyNode(child, settings);
    }
  }

  private static boolean isModified(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      if (((MyTreeNode)node).isModified(settings)) return true;
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      if (isModified(child, settings)) {
        return true;
      }
    }
    return false;
  }

  protected void addOption(@Nonnull String fieldName, @Nonnull String title) {
    addOption(fieldName, title, null);
  }

  protected void addOption(@Nonnull String fieldName, @Nonnull String title, @Nonnull String[] options, @Nonnull int[] values) {
    addOption(fieldName, title, null, options, values);
  }

  protected void addOption(@Nonnull String fieldName,
                           @Nonnull String title,
                           @Nullable String groupName,
                           int minValue,
                           int maxValue,
                           int defaultValue,
                           @Nullable Function<Integer, String> defaultValueRenderer) {
    myOptions.add(new IntOption(null, fieldName, title, groupName, null, null, minValue, maxValue, defaultValue, defaultValueRenderer));
  }

  protected void addOption(@Nonnull String fieldName, @Nonnull String title, @Nullable String groupName) {
    myOptions.add(new BooleanOption(null, fieldName, title, groupName, null, null));
  }

  protected void addOption(@Nonnull String fieldName, @Nonnull String title, @Nullable String groupName, @Nonnull String[] options, @Nonnull int[] values) {
    myOptions.add(new SelectionOption(null, fieldName, title, groupName, null, null, options, values));
  }

  protected void addCustomOption(@Nonnull Option option) {
    myOptions.add(option);
  }

  protected abstract static class Option extends OrderedOption {
    @Nonnull
    final String title;
    @Nullable
    final String groupName;
    private boolean myEnabled = false;

    protected Option(@Nonnull String optionName, @Nonnull String title, @Nullable String groupName, @Nullable OptionAnchor anchor, @Nullable String anchorOptionName) {
      super(optionName, anchor, anchorOptionName);
      this.title = title;
      this.groupName = groupName;
    }

    public void setEnabled(boolean enabled) {
      myEnabled = enabled;
    }

    public boolean isEnabled() {
      return myEnabled;
    }

    public abstract Object getValue(CodeStyleSettings settings);

    public abstract void setValue(Object value, CodeStyleSettings settings);
  }

  private abstract class FieldOption extends Option {
    @Nullable
    final Class<? extends CustomCodeStyleSettings> clazz;
    Field field;

    public FieldOption(@Nullable Class<? extends CustomCodeStyleSettings> clazz,
                       @Nonnull String fieldName,
                       @Nonnull String title,
                       @Nullable String groupName,
                       @Nullable OptionAnchor anchor,
                       @Nullable String anchorFiledName) {
      super(fieldName, title, groupName, anchor, anchorFiledName);
      this.clazz = clazz;

      try {
        Class styleSettingsClass = clazz == null ? CommonCodeStyleSettings.class : clazz;
        this.field = styleSettingsClass.getField(fieldName);
      }
      catch (NoSuchFieldException e) {
        LOG.error(e);
      }
    }

    protected Object getSettings(CodeStyleSettings settings) {
      if (clazz != null) return settings.getCustomSettings(clazz);
      return settings.getCommonSettings(getDefaultLanguage());
    }

  }

  private class BooleanOption extends FieldOption {
    private BooleanOption(Class<? extends CustomCodeStyleSettings> clazz,
                          @Nonnull String fieldName,
                          @Nonnull String title,
                          @Nullable String groupName,
                          @Nullable OptionAnchor anchor,
                          @Nullable String anchorFiledName) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        return field == null ? null : field.getBoolean(getSettings(settings));
      }
      catch (IllegalAccessException ignore) {
        return null;
      }
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      try {
        if (field != null) field.setBoolean(getSettings(settings), ((Boolean)value).booleanValue());
      }
      catch (IllegalAccessException ignored) {
      }
    }
  }

  private class SelectionOption extends FieldOption {
    @Nonnull
    final String[] options;
    @Nonnull
    final int[] values;

    public SelectionOption(Class<? extends CustomCodeStyleSettings> clazz,
                           @Nonnull String fieldName,
                           @Nonnull String title,
                           @Nullable String groupName,
                           @Nullable OptionAnchor anchor,
                           @Nullable String anchorFiledName,
                           @Nonnull String[] options,
                           @Nonnull int[] values) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
      this.options = options;
      this.values = values;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        int value = field.getInt(getSettings(settings));
        for (int i = 0; i < values.length; i++) {
          if (values[i] == value) return options[i];
        }
        LOG.error("Invalid option value " + value + " for " + field.getName());
      }
      catch (IllegalAccessException ignore) {
      }
      return null;
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      try {
        for (int i = 0; i < values.length; i++) {
          if (options[i].equals(value)) {
            field.setInt(getSettings(settings), values[i]);
            return;
          }
        }
        LOG.error("Invalid option value " + value + " for " + field.getName());
      }
      catch (IllegalAccessException ignore) {
      }
    }
  }

  private class IntOption extends FieldOption {

    private final int myMinValue;
    private final int myMaxValue;
    private final int myDefaultValue;
    @Nullable
    private final Function<Integer, String> myDefaultValueRenderer;

    public IntOption(Class<? extends CustomCodeStyleSettings> clazz,
                     @Nonnull String fieldName,
                     @Nonnull String title,
                     @Nullable String groupName,
                     @Nullable OptionAnchor anchor,
                     @Nullable String anchorFiledName,
                     int minValue,
                     int maxValue,
                     int defaultValue,
                     @Nullable Function<Integer, String> defaultValueRenderer) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
      myMinValue = minValue;
      myMaxValue = maxValue;
      myDefaultValue = defaultValue;
      myDefaultValueRenderer = defaultValueRenderer;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        return field.getInt(getSettings(settings));
      }
      catch (IllegalAccessException e) {
        return null;
      }
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      //noinspection EmptyCatchBlock
      try {
        if (value instanceof Integer) {
          field.setInt(getSettings(settings), ((Integer)value).intValue());
        }
        else {
          field.setInt(getSettings(settings), myDefaultValue);
        }
      }
      catch (IllegalAccessException e) {
      }
    }

    public int getMinValue() {
      return myMinValue;
    }

    public int getMaxValue() {
      return myMaxValue;
    }

    public int getDefaultValue() {
      return myDefaultValue;
    }

    public boolean isDefaultValue(Object value) {
      return value instanceof Integer && ((Integer)value).intValue() == myDefaultValue;
    }

    @Nullable
    public String getDefaultValueText() {
      return myDefaultValueRenderer != null ? myDefaultValueRenderer.apply(myDefaultValue) : null;
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public final ColumnInfo TITLE = new ColumnInfo("TITLE") {
    @Override
    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getText();
      }
      return o.toString();
    }

    @Override
    public Class getColumnClass() {
      return TreeTableModel.class;
    }
  };

  @SuppressWarnings({"HardCodedStringLiteral"})
  public final ColumnInfo VALUE = new ColumnInfo("VALUE") {
    private final TableCellEditor myEditor = new MyValueEditor();
    private final TableCellRenderer myRenderer = new MyValueRenderer();

    @Override
    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getValue();
      }

      return null;
    }

    @Override
    public TableCellRenderer getRenderer(Object o) {
      return myRenderer;
    }

    @Override
    public TableCellEditor getEditor(Object item) {
      return myEditor;
    }

    @Override
    public boolean isCellEditable(Object o) {
      return o instanceof MyTreeNode && ((MyTreeNode)o).isEnabled();
    }

    @Override
    public void setValue(Object o, Object o1) {
      MyTreeNode node = (MyTreeNode)o;
      node.setValue(o1);
    }
  };

  public final ColumnInfo[] COLUMNS = new ColumnInfo[]{TITLE, VALUE};

  private final TreeCellRenderer myTitleRenderer = new TreeCellRenderer() {
    private final JLabel myLabel = new JLabel();

    @Nonnull
    @Override
    public Component getTreeCellRendererComponent(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      if (value instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)value;
        myLabel.setText(getRenamedTitle(node.getKey().getOptionName(), node.getText()));
        myLabel.setFont(myLabel.getFont().deriveFont(node.getKey().groupName == null ? Font.BOLD : Font.PLAIN));
        myLabel.setEnabled(node.isEnabled());
      }
      else {
        myLabel.setText(getRenamedTitle(value.toString(), value.toString()));
        myLabel.setFont(myLabel.getFont().deriveFont(Font.BOLD));
        myLabel.setEnabled(true);
      }

      Color foreground = selected ? UIUtil.getTableSelectionForeground() : UIUtil.getTableForeground();
      myLabel.setForeground(foreground);

      return myLabel;
    }
  };

  protected static class MyTreeNode extends DefaultMutableTreeNode {
    private final Option myKey;
    private final String myText;
    private Object myValue;

    public MyTreeNode(Option key, String text, CodeStyleSettings settings) {
      myKey = key;
      myText = text;
      myValue = key.getValue(settings);
      setUserObject(myText);
    }

    public Option getKey() {
      return myKey;
    }

    public String getText() {
      return myText;
    }

    public Object getValue() {
      return myValue;
    }

    public void setValue(Object value) {
      myValue = value;
    }

    public void reset(CodeStyleSettings settings) {
      setValue(myKey.getValue(settings));
    }

    public boolean isModified(final CodeStyleSettings settings) {
      return myValue != null && !myValue.equals(myKey.getValue(settings));
    }

    public void apply(final CodeStyleSettings settings) {
      myKey.setValue(myValue, settings);
    }

    public boolean isEnabled() {
      return myKey.isEnabled();
    }
  }

  private class MyValueRenderer implements TableCellRenderer {
    private JTable myTable;
    private int myRow;
    private int myColumn;
    private final OptionsLabel myComboBox = new OptionsLabel();
    private final JCheckBox myCheckBox = new JBCheckBox();
    private final JPanel myEmptyLabel = new JPanel();
    private final JLabel myIntLabel = new JLabel();

    public MyValueRenderer() {
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myComboBox);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myCheckBox);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myIntLabel);
    }

    @Nonnull
    @Override
    public Component getTableCellRendererComponent(@Nonnull JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      myTable = table;
      myRow = row;
      myColumn = column;
      boolean isEnabled = true;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)((TreeTable)table).getTree().
              getPathForRow(row).getLastPathComponent();
      Option key = null;
      if (node instanceof MyTreeNode) {
        isEnabled = ((MyTreeNode)node).isEnabled();
        key = ((MyTreeNode)node).getKey();
      }
      if (!table.isEnabled()) {
        isEnabled = false;
      }

      Color background = table.getBackground();
      if (key != null && value != null) {
        JComponent customRenderer = getCustomValueRenderer(key.getOptionName(), value);
        if (customRenderer != null) {
          return customRenderer;
        }
      }
      if (value instanceof Boolean) {
        myCheckBox.setSelected(((Boolean)value).booleanValue());
        myCheckBox.setBackground(background);
        myCheckBox.setEnabled(isEnabled);
        return myCheckBox;
      }
      else if (value instanceof String) {
        myComboBox.setText((String)value);
        myComboBox.setBackground(background);
        myComboBox.setEnabled(isEnabled);
        return myComboBox;
      }
      else if (value instanceof Integer) {
        if (key instanceof IntOption && ((IntOption)key).isDefaultValue(value)) {
          myIntLabel.setText(((IntOption)key).getDefaultValueText());
        }
        else {
          myIntLabel.setText(value.toString());
        }
        return myIntLabel;
      }

      myEmptyLabel.setBackground(background);
      return myEmptyLabel;
    }

    protected class OptionsLabel extends JLabel {
      @Override
      public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
          accessibleContext = new AccessibleOptionsLabel();
        }
        return accessibleContext;
      }

      protected class AccessibleOptionsLabel extends AccessibleJLabel implements AccessibleAction {
        @Override
        public AccessibleRole getAccessibleRole() {
          return AccessibleRole.PUSH_BUTTON;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
          return this;
        }

        @Override
        public int getAccessibleActionCount() {
          return 1;
        }

        @Override
        public String getAccessibleActionDescription(int i) {
          if (i == 0) {
            return UIManager.getString("AbstractButton.clickText");
          }
          else {
            return null;
          }
        }

        @Override
        public boolean doAccessibleAction(int i) {
          if (i == 0) {
            myTable.editCellAt(myRow, myColumn);
            return true;
          }
          else {
            return false;
          }
        }
      }
    }
  }


  @Nullable
  protected JComponent getCustomValueRenderer(@Nonnull String optionName, @Nonnull Object value) {
    return null;
  }

  /**
   * @author Konstantin Bulenkov
   */
  private class MyValueEditor extends AbstractTableCellEditor {
    public static final String STOP_CELL_EDIT_ACTION_KEY = "stopEdit";
    private final JCheckBox myBooleanEditor = new JBCheckBox();
    private final JBComboBoxTableCellEditorComponent myOptionsEditor = new JBComboBoxTableCellEditorComponent();
    private final IntegerField myIntOptionsEditor = new IntegerField();
    private JComponent myCurrentEditor = null;
    private MyTreeNode myCurrentNode = null;
    private final AbstractAction STOP_CELL_EDIT_ACTION = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopCellEditing();
      }
    };

    public MyValueEditor() {
      final ActionListener itemChoosen = new ActionListener() {
        @Override
        public void actionPerformed(@Nonnull ActionEvent e) {
          if (myCurrentNode != null) {
            myCurrentNode.setValue(getCellEditorValue());
            somethingChanged();
          }
        }
      };
      myBooleanEditor.addActionListener(itemChoosen);
      myOptionsEditor.addActionListener(itemChoosen);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myBooleanEditor);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myOptionsEditor);
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, myIntOptionsEditor);
    }

    @Override
    public Object getCellEditorValue() {
      if (myCurrentEditor == myOptionsEditor) {
        return myOptionsEditor.getEditorValue();
      }
      else if (myCurrentEditor == myBooleanEditor) {
        return myBooleanEditor.isSelected();
      }
      else if (myCurrentEditor == myIntOptionsEditor) {
        return myIntOptionsEditor.getValue();
      }
      else {
        Object value = getCustomNodeEditorValue(myCurrentEditor);
        if (value != null) return value;
      }

      return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      final DefaultMutableTreeNode defaultNode = (DefaultMutableTreeNode)((TreeTable)table).getTree().
              getPathForRow(row).getLastPathComponent();
      myCurrentEditor = null;
      myCurrentNode = null;
      if (defaultNode instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)defaultNode;
        myCurrentNode = node;
        if (node.getKey() instanceof BooleanOption) {
          myCurrentEditor = myBooleanEditor;
          myBooleanEditor.setSelected(node.getValue() == Boolean.TRUE);
          myBooleanEditor.setEnabled(node.isEnabled());
        }
        else if (node.getKey() instanceof IntOption) {
          IntOption intOption = (IntOption)node.getKey();
          myCurrentEditor = myIntOptionsEditor;
          myIntOptionsEditor.setCanBeEmpty(true);
          myIntOptionsEditor.setMinValue(intOption.getMinValue());
          myIntOptionsEditor.setMaxValue(intOption.getMaxValue());
          myIntOptionsEditor.setDefaultValue(intOption.getDefaultValue());
          myIntOptionsEditor.setValue((Integer)node.getValue());
        }
        else {
          myCurrentEditor = getCustomNodeEditor(node);
        }
        if (myCurrentEditor == null) {
          myCurrentEditor = myOptionsEditor;
          myOptionsEditor.setCell(table, row, column);
          myOptionsEditor.setText(String.valueOf(node.getValue()));
          //noinspection ConfusingArgumentToVarargsMethod
          myOptionsEditor.setOptions(((SelectionOption)node.getKey()).options);
          myOptionsEditor.setDefaultValue(node.getValue());
        }
      }

      if (myCurrentEditor != null) {
        myCurrentEditor.setBackground(table.getBackground());
        if (myCurrentEditor instanceof JTextField) {
          myCurrentEditor.getInputMap().put(ENTER_KEY_STROKE, STOP_CELL_EDIT_ACTION_KEY);
          myCurrentEditor.getActionMap().put(STOP_CELL_EDIT_ACTION_KEY, STOP_CELL_EDIT_ACTION);
        }
      }
      return myCurrentEditor;
    }
  }

  @Nullable
  protected JComponent getCustomNodeEditor(@Nonnull MyTreeNode node) {
    return null;
  }

  @Nullable
  protected Object getCustomNodeEditorValue(@Nonnull JComponent customEditor) {
    return null;
  }

  @Override
  public void apply(CodeStyleSettings settings) {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    applyNode(root, settings);
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    if (isModified(root, settings)) {
      return true;
    }
    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(final CodeStyleSettings settings) {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    resetNode(root, settings);
    ((DefaultTreeModel)treeModel).nodeChanged(root);
  }

  @Override
  public Set<String> processListOptions() {
    Set<String> options = new HashSet<>();
    collectOptions(options, myOptions);
    collectOptions(options, myCustomOptions);
    return options;
  }

  private static void collectOptions(Set<String> optionNames, final List<Option> optionList) {
    for (Option option : optionList) {
      if (option.groupName != null) {
        optionNames.add(option.groupName);
      }
      optionNames.add(option.title);
    }
  }
}
