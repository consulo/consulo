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

import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeTableSpeedSearch;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.table.ListTreeTableModel;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableCellRenderer;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
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
    private final Map<String, LocalizeValue> myRenamedFields = new HashMap<>();
    private boolean myShowAllStandardOptions;
    protected boolean isFirstUpdate = true;

    @RequiredUIAccess
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
        myPanel.add(
            scrollPane,
            new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0)
        );

        JPanel previewPanel = createPreviewPanel();
        myPanel.add(
            previewPanel,
            new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0)
        );

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
    public void showCustomOption(
        Class<? extends CustomCodeStyleSettings> settingsClass,
        String fieldName,
        LocalizeValue title,
        LocalizeValue groupName,
        Object... options
    ) {
        showCustomOption(settingsClass, fieldName, title, groupName, null, null, options);
    }

    @Override
    public void showCustomOption(
        Class<? extends CustomCodeStyleSettings> settingsClass,
        String fieldName,
        LocalizeValue title,
        LocalizeValue groupName,
        @Nullable OptionAnchor anchor,
        @Nullable String anchorFieldName,
        Object... options
    ) {
        if (isFirstUpdate) {
            Option option;
            if (options.length == 2) {
                option = new SelectionOption(
                    settingsClass,
                    fieldName,
                    title,
                    groupName,
                    anchor,
                    anchorFieldName,
                    (LocalizeValue[]) options[0],
                    (int[]) options[1]
                );
            }
            else {
                option = new BooleanOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName);
            }
            myCustomOptions.add(option);
            option.setEnabled(true);
        }
        else {
            for (Option each : myCustomOptions) {
                if (each instanceof FieldOption option && option.myClass == settingsClass && each.getOptionName().equals(fieldName)) {
                    each.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void renameStandardOption(String fieldName, LocalizeValue newTitle) {
        myRenamedFields.put(fieldName, newTitle);
    }

    public void showOption(String optionName) {
        myAllowedOptions.add(optionName);
    }

    protected TreeTable createOptionsTree(CodeStyleSettings settings) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        Map<LocalizeValue, DefaultMutableTreeNode> groupsMap = new HashMap<>();

        List<Option> sorted = sortOptions(ContainerUtil.concat(myOptions, myCustomOptions));
        for (Option each : sorted) {
            if (!(myCustomOptions.contains(each) || myAllowedOptions.contains(each.getOptionName()) || myShowAllStandardOptions)) {
                continue;
            }

            LocalizeValue group = each.myGroupName;
            MyTreeNode newNode = new MyTreeNode(each, each.myTitle, settings);

            DefaultMutableTreeNode groupNode = groupsMap.get(group);
            if (groupNode != null) {
                groupNode.add(newNode);
            }
            else {
                LocalizeValue groupName;

                if (group.isEmpty()) {
                    groupName = each.myTitle;
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
                if (treePath == null) {
                    return super.getCellRenderer(row, column);
                }

                Object node = treePath.getLastPathComponent();

                @SuppressWarnings("unchecked") TableCellRenderer renderer = COLUMNS[column].getRenderer(node);
                return renderer == null ? super.getCellRenderer(row, column) : renderer;
            }

            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                TreePath treePath = getTree().getPathForRow(row);
                if (treePath == null) {
                    return super.getCellEditor(row, column);
                }

                Object node = treePath.getLastPathComponent();
                @SuppressWarnings("unchecked") TableCellEditor editor = COLUMNS[column].getEditor(node);
                return editor == null ? super.getCellEditor(row, column) : editor;
            }
        };
        new TreeTableSpeedSearch(treeTable).setComparator(new SpeedSearchComparator(false));

        treeTable.setRootVisible(false);

        JTree tree = treeTable.getTree();
        tree.setCellRenderer(myTitleRenderer);
        tree.setShowsRootHandles(true);
        //myTreeTable.setRowHeight(new JComboBox(new String[]{"Sample Text"}).getPreferredSize().height);
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.setTableHeader(null);

        TreeUtil.expandAll(tree);

        treeTable.getColumnModel().getSelectionModel().setAnchorSelectionIndex(1);
        treeTable.getColumnModel().getSelectionModel().setLeadSelectionIndex(1);

        int maxWidth = tree.getPreferredScrollableViewportSize().width + 10;
        TableColumn titleColumn = treeTable.getColumnModel().getColumn(0);
        titleColumn.setPreferredWidth(maxWidth);
        titleColumn.setMinWidth(maxWidth);
        titleColumn.setMaxWidth(maxWidth);
        titleColumn.setResizable(false);

        //final TableColumn levelColumn = treeTable.getColumnModel().getColumn(1);
        //TODO[max]: better preferred size...
        //TODO[kb]: Did I fixed it by making the last column floating?
        //levelColumn.setPreferredWidth(valueSize.width);
        //levelColumn.setMaxWidth(valueSize.width);
        //levelColumn.setMinWidth(valueSize.width);
        //levelColumn.setResizable(false);

        Dimension valueSize = new JLabel(ApplicationLocalize.optionTableSizingText().get()).getPreferredSize();
        treeTable.setPreferredScrollableViewportSize(new Dimension(maxWidth + valueSize.width + 10, 20));

        return treeTable;
    }

    private LocalizeValue getRenamedTitle(String fieldOrGroupName, LocalizeValue defaultName) {
        return myRenamedFields.getOrDefault(fieldOrGroupName, defaultName);
    }

    protected abstract void initTables();

    private static void resetNode(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyTreeNode treeNode) {
            treeNode.reset(settings);
        }
        for (int j = 0; j < node.getChildCount(); j++) {
            TreeNode child = node.getChildAt(j);
            resetNode(child, settings);
        }
    }

    private static void applyNode(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyTreeNode treeNode) {
            treeNode.apply(settings);
        }
        for (int j = 0; j < node.getChildCount(); j++) {
            TreeNode child = node.getChildAt(j);
            applyNode(child, settings);
        }
    }

    private static boolean isModified(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyTreeNode treeNode) {
            if (treeNode.isModified(settings)) {
                return true;
            }
        }
        for (int j = 0; j < node.getChildCount(); j++) {
            TreeNode child = node.getChildAt(j);
            if (isModified(child, settings)) {
                return true;
            }
        }
        return false;
    }

    protected void addOption(String fieldName, LocalizeValue title) {
        addOption(fieldName, title, LocalizeValue.empty());
    }

    protected void addOption(String fieldName, LocalizeValue title, LocalizeValue[] options, int[] values) {
        addOption(fieldName, title, LocalizeValue.empty(), options, values);
    }

    protected void addOption(
        String fieldName,
        LocalizeValue title,
        LocalizeValue groupName,
        int minValue,
        int maxValue,
        int defaultValue,
        @Nullable Function<Integer, LocalizeValue> defaultValueRenderer
    ) {
        myOptions.add(new IntOption(null, fieldName, title, groupName, null, null, minValue, maxValue, defaultValue, defaultValueRenderer));
    }

    protected void addOption(String fieldName, LocalizeValue title, LocalizeValue groupName) {
        myOptions.add(new BooleanOption(null, fieldName, title, groupName, null, null));
    }

    protected void addOption(String fieldName, LocalizeValue title, LocalizeValue groupName, LocalizeValue[] options, int[] values) {
        myOptions.add(new SelectionOption(null, fieldName, title, groupName, null, null, options, values));
    }

    protected void addCustomOption(Option option) {
        myOptions.add(option);
    }

    protected abstract static class Option extends OrderedOption {
        final LocalizeValue myTitle;
        final LocalizeValue myGroupName;
        private boolean myEnabled = false;

        protected Option(
            String optionName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorOptionName
        ) {
            super(optionName, anchor, anchorOptionName);
            myTitle = title;
            myGroupName = groupName;
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
        final @Nullable Class<? extends CustomCodeStyleSettings> myClass;
        Field myField;

        public FieldOption(
            @Nullable Class<? extends CustomCodeStyleSettings> clazz,
            String fieldName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFiledName
        ) {
            super(fieldName, title, groupName, anchor, anchorFiledName);
            this.myClass = clazz;

            try {
                Class styleSettingsClass = clazz == null ? CommonCodeStyleSettings.class : clazz;
                this.myField = styleSettingsClass.getField(fieldName);
            }
            catch (NoSuchFieldException e) {
                LOG.error(e);
            }
        }

        protected Object getSettings(CodeStyleSettings settings) {
            if (myClass != null) {
                return settings.getCustomSettings(myClass);
            }
            return settings.getCommonSettings(getDefaultLanguage());
        }
    }

    private class BooleanOption extends FieldOption {
        private BooleanOption(
            Class<? extends CustomCodeStyleSettings> clazz,
            String fieldName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFiledName
        ) {
            super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
        }

        @Override
        public Object getValue(CodeStyleSettings settings) {
            try {
                return myField == null ? null : myField.getBoolean(getSettings(settings));
            }
            catch (IllegalAccessException ignore) {
                return null;
            }
        }

        @Override
        public void setValue(Object value, CodeStyleSettings settings) {
            try {
                if (myField != null) {
                    myField.setBoolean(getSettings(settings), (Boolean) value);
                }
            }
            catch (IllegalAccessException ignored) {
            }
        }
    }

    private class SelectionOption extends FieldOption {
        final LocalizeValue[] myOptions;
        final int[] myValues;

        public SelectionOption(
            Class<? extends CustomCodeStyleSettings> clazz,
            String fieldName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFiledName,
            LocalizeValue[] options,
            int[] values
        ) {
            super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
            myOptions = options;
            myValues = values;
        }

        @Override
        public Object getValue(CodeStyleSettings settings) {
            try {
                int value = myField.getInt(getSettings(settings));
                for (int i = 0; i < myValues.length; i++) {
                    if (myValues[i] == value) {
                        return myOptions[i];
                    }
                }
                LOG.error("Invalid option value " + value + " for " + myField.getName());
            }
            catch (IllegalAccessException ignore) {
            }
            return null;
        }

        @Override
        public void setValue(Object value, CodeStyleSettings settings) {
            try {
                for (int i = 0; i < myValues.length; i++) {
                    if (myOptions[i].equals(value)) {
                        myField.setInt(getSettings(settings), myValues[i]);
                        return;
                    }
                }
                LOG.error("Invalid option value " + value + " for " + myField.getName());
            }
            catch (IllegalAccessException ignore) {
            }
        }
    }

    private class IntOption extends FieldOption {
        private final int myMinValue;
        private final int myMaxValue;
        private final int myDefaultValue;
        private final @Nullable Function<Integer, LocalizeValue> myDefaultValueRenderer;

        public IntOption(
            Class<? extends CustomCodeStyleSettings> clazz,
            String fieldName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFiledName,
            int minValue,
            int maxValue,
            int defaultValue,
            @Nullable Function<Integer, LocalizeValue> defaultValueRenderer
        ) {
            super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
            myMinValue = minValue;
            myMaxValue = maxValue;
            myDefaultValue = defaultValue;
            myDefaultValueRenderer = defaultValueRenderer;
        }

        @Override
        public Object getValue(CodeStyleSettings settings) {
            try {
                return myField.getInt(getSettings(settings));
            }
            catch (IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public void setValue(Object value, CodeStyleSettings settings) {
            //noinspection EmptyCatchBlock
            try {
                if (value instanceof Integer intValue) {
                    myField.setInt(getSettings(settings), intValue);
                }
                else {
                    myField.setInt(getSettings(settings), myDefaultValue);
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
            return value instanceof Integer intValue && intValue == myDefaultValue;
        }

        public LocalizeValue getDefaultValueText() {
            return myDefaultValueRenderer != null ? myDefaultValueRenderer.apply(myDefaultValue) : LocalizeValue.empty();
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public final ColumnInfo TITLE = new ColumnInfo("TITLE") {
        @Override
        public Object valueOf(Object o) {
            if (o instanceof MyTreeNode) {
                MyTreeNode node = (MyTreeNode) o;
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
                MyTreeNode node = (MyTreeNode) o;
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
            return o instanceof MyTreeNode && ((MyTreeNode) o).isEnabled();
        }

        @Override
        public void setValue(Object o, Object o1) {
            MyTreeNode node = (MyTreeNode) o;
            node.setValue(o1);
        }
    };

    public final ColumnInfo[] COLUMNS = new ColumnInfo[]{TITLE, VALUE};

    private final TreeCellRenderer myTitleRenderer = new TreeCellRenderer() {
        private final JLabel myLabel = new JLabel();

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (value instanceof MyTreeNode node) {
                myLabel.setText(getRenamedTitle(node.getKey().getOptionName(), node.getText()).get());
                myLabel.setFont(myLabel.getFont().deriveFont(node.getKey().myGroupName.isEmpty() ? Font.BOLD : Font.PLAIN));
                myLabel.setEnabled(node.isEnabled());
            }
            else {
                myLabel.setText(String.valueOf(value));
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
        private final LocalizeValue myText;
        private Object myValue;

        public MyTreeNode(Option key, LocalizeValue text, CodeStyleSettings settings) {
            myKey = key;
            myText = text;
            myValue = key.getValue(settings);
            setUserObject(myText);
        }

        public Option getKey() {
            return myKey;
        }

        public LocalizeValue getText() {
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

        public boolean isModified(CodeStyleSettings settings) {
            return myValue != null && !myValue.equals(myKey.getValue(settings));
        }

        public void apply(CodeStyleSettings settings) {
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

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            myTable = table;
            myRow = row;
            myColumn = column;
            boolean isEnabled = true;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) ((TreeTable) table).getTree().getPathForRow(row).getLastPathComponent();
            Option key = null;
            if (node instanceof MyTreeNode treeNode) {
                isEnabled = treeNode.isEnabled();
                key = treeNode.getKey();
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
            if (value instanceof Boolean booleanValue) {
                myCheckBox.setSelected(booleanValue);
                myCheckBox.setBackground(background);
                myCheckBox.setEnabled(isEnabled);
                return myCheckBox;
            }
            else if (value instanceof LocalizeValue locValue) {
                myComboBox.setText(locValue.get());
                myComboBox.setBackground(background);
                myComboBox.setEnabled(isEnabled);
                return myComboBox;
            }
            else if (value instanceof String strValue) {
                myComboBox.setText(strValue);
                myComboBox.setBackground(background);
                myComboBox.setEnabled(isEnabled);
                return myComboBox;
            }
            else if (value instanceof Integer) {
                if (key instanceof IntOption intOption && intOption.isDefaultValue(value)) {
                    myIntLabel.setText(intOption.getDefaultValueText().get());
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

    protected @Nullable JComponent getCustomValueRenderer(String optionName, Object value) {
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
            ActionListener itemChosen = e -> {
                if (myCurrentNode != null) {
                    myCurrentNode.setValue(getCellEditorValue());
                    somethingChanged();
                }
            };
            myBooleanEditor.addActionListener(itemChosen);
            myOptionsEditor.addActionListener(itemChosen);
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
                if (value != null) {
                    return value;
                }
            }

            return null;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            DefaultMutableTreeNode defaultNode =
                (DefaultMutableTreeNode) ((TreeTable) table).getTree().getPathForRow(row).getLastPathComponent();
            myCurrentEditor = null;
            myCurrentNode = null;
            if (defaultNode instanceof MyTreeNode node) {
                myCurrentNode = node;
                if (node.getKey() instanceof BooleanOption) {
                    myCurrentEditor = myBooleanEditor;
                    myBooleanEditor.setSelected(node.getValue() == Boolean.TRUE);
                    myBooleanEditor.setEnabled(node.isEnabled());
                }
                else if (node.getKey() instanceof IntOption intOption) {
                    myCurrentEditor = myIntOptionsEditor;
                    myIntOptionsEditor.setCanBeEmpty(true);
                    myIntOptionsEditor.setMinValue(intOption.getMinValue());
                    myIntOptionsEditor.setMaxValue(intOption.getMaxValue());
                    myIntOptionsEditor.setDefaultValue(intOption.getDefaultValue());
                    myIntOptionsEditor.setValue((Integer) node.getValue());
                }
                else {
                    myCurrentEditor = getCustomNodeEditor(node);
                }
                if (myCurrentEditor == null) {
                    myCurrentEditor = myOptionsEditor;
                    myOptionsEditor.setCell(table, row, column);
                    myOptionsEditor.setText(String.valueOf(node.getValue()));
                    //noinspection ConfusingArgumentToVarargsMethod
                    myOptionsEditor.setOptions(((SelectionOption) node.getKey()).myOptions);
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

    protected @Nullable JComponent getCustomNodeEditor(MyTreeNode node) {
        return null;
    }

    protected @Nullable Object getCustomNodeEditorValue(JComponent customEditor) {
        return null;
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        TreeModel treeModel = myTreeTable.getTree().getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        applyNode(root, settings);
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        TreeModel treeModel = myTreeTable.getTree().getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        return isModified(root, settings);
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        TreeModel treeModel = myTreeTable.getTree().getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        resetNode(root, settings);
        ((DefaultTreeModel) treeModel).nodeChanged(root);
    }

    @Override
    public Set<LocalizeValue> processListOptions() {
        Set<LocalizeValue> options = new HashSet<>();
        collectOptions(options, myOptions);
        collectOptions(options, myCustomOptions);
        return options;
    }

    private static void collectOptions(Set<LocalizeValue> optionNames, List<Option> optionList) {
        for (Option option : optionList) {
            if (option.myGroupName.isNotEmpty()) {
                optionNames.add(option.myGroupName);
            }
            optionNames.add(option.myTitle);
        }
    }
}
