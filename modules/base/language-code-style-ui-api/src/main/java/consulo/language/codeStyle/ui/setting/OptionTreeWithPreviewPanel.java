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

import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.CustomCodeStyleSettings;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public abstract class OptionTreeWithPreviewPanel extends CustomizableLanguageCodeStylePanel {
    private static final Logger LOG = Logger.getInstance(OptionTreeWithPreviewPanel.class);
    protected JTree myOptionsTree;
    protected final List<BooleanOptionKey> myKeys = new ArrayList<>();
    protected final JPanel myPanel = new JPanel(new GridBagLayout());

    private boolean myShowAllStandardOptions = false;
    private final Set<String> myAllowedOptions = new HashSet<>();
    protected MultiMap<LocalizeValue, CustomBooleanOptionInfo> myCustomOptions = new MultiMap<>();
    protected boolean isFirstUpdate = true;
    private final Map<String, LocalizeValue> myRenamedFields = new HashMap<>();
    private final Map<String, LocalizeValue> myRemappedGroups = new HashMap<>();

    @RequiredUIAccess
    public OptionTreeWithPreviewPanel(CodeStyleSettings settings) {
        super(settings);
    }

    @Override
    protected void init() {
        super.init();

        initTables();

        myOptionsTree = createOptionsTree();
        myOptionsTree.setCellRenderer(new MyTreeCellRenderer());
        myOptionsTree.setBackground(UIUtil.getPanelBackground());
        myOptionsTree.setBorder(JBUI.Borders.emptyRight(10));
        JScrollPane scrollPane = new JBScrollPane(myOptionsTree) {
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
    }

    @Override
    public void showAllStandardOptions() {
        myShowAllStandardOptions = true;
        updateOptions(true);
    }

    @Override
    public void showStandardOptions(String... optionNames) {
        if (isFirstUpdate) {
            Collections.addAll(myAllowedOptions, optionNames);
        }
        updateOptions(false, optionNames);
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
            myCustomOptions.putValue(
                groupName,
                new CustomBooleanOptionInfo(settingsClass, fieldName, title, groupName, anchor, anchorFieldName)
            );
        }
        enableOption(fieldName);
    }

    @Override
    public void renameStandardOption(String fieldName, LocalizeValue newTitle) {
        if (isFirstUpdate) {
            myRenamedFields.put(fieldName, newTitle);
        }
    }

    protected void updateOptions(boolean showAllStandardOptions, String... allowedOptions) {
        for (BooleanOptionKey key : myKeys) {
            String fieldName = key.myField.getName();
            if (key instanceof CustomBooleanOptionKey) {
                key.setEnabled(false);
            }
            else if (showAllStandardOptions) {
                key.setEnabled(true);
            }
            else {
                key.setEnabled(false);
                for (String optionName : allowedOptions) {
                    if (fieldName.equals(optionName)) {
                        key.setEnabled(true);
                        break;
                    }
                }
            }
        }
    }

    protected void enableOption(String optionName) {
        for (BooleanOptionKey key : myKeys) {
            if (key.myField.getName().equals(optionName)) {
                key.setEnabled(true);
            }
        }
    }

    protected JTree createOptionsTree() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        LocalizeValue groupName = LocalizeValue.empty();
        DefaultMutableTreeNode groupNode = null;

        List<BooleanOptionKey> result = sortOptions(orderByGroup(myKeys));

        for (BooleanOptionKey key : result) {
            LocalizeValue newGroupName = key.myGroupName;
            if (!newGroupName.equals(groupName) || groupNode == null) {
                groupName = newGroupName;
                groupNode = new DefaultMutableTreeNode(newGroupName);
                rootNode.add(groupNode);
            }
            if (isOptionVisible(key)) {
                groupNode.add(new MyToggleTreeNode(key, key.myTitle));
            }
        }

        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        final Tree optionsTree = new Tree(model);
        new TreeSpeedSearch(optionsTree).setComparator(new SpeedSearchComparator(false));
        TreeUtil.installActions(optionsTree);
        optionsTree.setRootVisible(false);
        UIUtil.setLineStyleAngled(optionsTree);
        optionsTree.setShowsRootHandles(true);

        optionsTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!optionsTree.isEnabled()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    TreePath treePath = optionsTree.getLeadSelectionPath();
                    selectCheckbox(treePath);
                    e.consume();
                }
            }
        });

        new ClickListener() {
            @Override
            public boolean onClick(MouseEvent e, int clickCount) {
                if (!optionsTree.isEnabled()) {
                    return false;
                }
                TreePath treePath = optionsTree.getPathForLocation(e.getX(), e.getY());
                selectCheckbox(treePath);
                return true;
            }
        }.installOn(optionsTree);

        int row = 0;
        while (row < optionsTree.getRowCount()) {
            optionsTree.expandRow(row);
            row++;
        }

        return optionsTree;
    }

    private List<BooleanOptionKey> orderByGroup(List<BooleanOptionKey> options) {
        List<LocalizeValue> groupOrder = getGroupOrder(options);
        List<BooleanOptionKey> result = new ArrayList<>(options.size());
        result.addAll(options);
        Collections.sort(
            result,
            (key1, key2) -> {
                LocalizeValue group1 = key1.myGroupName;
                LocalizeValue group2 = key2.myGroupName;
                int index1 = groupOrder.indexOf(group1);
                int index2 = groupOrder.indexOf(group2);
                if (index1 == -1 || index2 == -1) {
                    return group1.compareTo(group2);
                }
                return Integer.compare(index1, index2);
            }
        );
        return result;
    }

    protected List<LocalizeValue> getGroupOrder(List<BooleanOptionKey> options) {
        List<LocalizeValue> groupOrder = new ArrayList<>();
        for (BooleanOptionKey each : options) {
            if (each.myGroupName.isNotEmpty() && !groupOrder.contains(each.myGroupName)) {
                groupOrder.add(each.myGroupName);
            }
        }
        return groupOrder;
    }

    private void selectCheckbox(TreePath treePath) {
        if (treePath == null) {
            return;
        }
        if (treePath.getLastPathComponent() instanceof MyToggleTreeNode node) {
            if (!node.isEnabled()) {
                return;
            }
            node.setSelected(!node.isSelected());
            int row = myOptionsTree.getRowForPath(treePath);
            myOptionsTree.repaint(myOptionsTree.getRowBounds(row));
            //updatePreview();
            somethingChanged();
        }
    }

    protected abstract void initTables();

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
        TreeModel treeModel = myOptionsTree.getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        resetNode(root, settings);
        ((DefaultTreeModel) treeModel).nodeChanged(root);
    }

    private void resetNode(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyToggleTreeNode toggleTreeNode) {
            resetMyTreeNode(toggleTreeNode, settings);
            return;
        }
        for (int j = 0; j < node.getChildCount(); j++) {
            TreeNode child = node.getChildAt(j);
            resetNode(child, settings);
        }
    }

    private void resetMyTreeNode(MyToggleTreeNode childNode, CodeStyleSettings settings) {
        try {
            BooleanOptionKey key = (BooleanOptionKey) childNode.getKey();
            childNode.setSelected(key.getValue(settings));
            childNode.setEnabled(key.isEnabled());
        }
        catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.error(e);
        }
    }

    @Override
    public void apply(CodeStyleSettings settings) {
        TreeModel treeModel = myOptionsTree.getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        applyNode(root, settings);
    }

    private static void applyNode(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyToggleTreeNode toggleTreeNode) {
            applyToggleNode(toggleTreeNode, settings);
            return;
        }
        for (int j = 0; j < node.getChildCount(); j++) {
            TreeNode child = node.getChildAt(j);
            applyNode(child, settings);
        }
    }

    private static void applyToggleNode(MyToggleTreeNode childNode, CodeStyleSettings settings) {
        BooleanOptionKey key = (BooleanOptionKey) childNode.getKey();
        key.setValue(settings, childNode.isSelected());
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
        TreeModel treeModel = myOptionsTree.getModel();
        TreeNode root = (TreeNode) treeModel.getRoot();
        return isModified(root, settings);
    }

    private static boolean isModified(TreeNode node, CodeStyleSettings settings) {
        if (node instanceof MyToggleTreeNode toggleTreeNode) {
            if (isToggleNodeModified(toggleTreeNode, settings)) {
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

    private static boolean isToggleNodeModified(MyToggleTreeNode childNode, CodeStyleSettings settings) {
        try {
            BooleanOptionKey key = (BooleanOptionKey) childNode.getKey();
            return childNode.isSelected() != key.getValue(settings);
        }
        catch (IllegalArgumentException | IllegalAccessException e) {
            LOG.error(e);
        }
        return false;
    }

    protected void initBooleanField(String fieldName, LocalizeValue title, LocalizeValue groupName) {
        if (myShowAllStandardOptions || myAllowedOptions.contains(fieldName)) {
            doInitBooleanField(fieldName, title, groupName);
        }
    }

    private void doInitBooleanField(String fieldName, LocalizeValue title, LocalizeValue groupName) {
        try {
            Class styleSettingsClass = CommonCodeStyleSettings.class;
            Field field = styleSettingsClass.getField(fieldName);
            LocalizeValue actualGroupName = getRemappedGroup(fieldName, groupName);

            BooleanOptionKey key = new BooleanOptionKey(fieldName, actualGroupName, getRenamedTitle(fieldName, title), field);
            myKeys.add(key);
        }
        catch (NoSuchFieldException | SecurityException e) {
            LOG.error(e);
        }
    }

    protected void initCustomOptions(LocalizeValue groupName) {
        for (CustomBooleanOptionInfo option : myCustomOptions.get(groupName)) {
            try {
                Field field = option.mySettingsClass.getField(option.myFieldName);
                myKeys.add(new CustomBooleanOptionKey<>(
                    option.myFieldName,
                    groupName,
                    getRenamedTitle(option.myFieldName, option.myTitle),
                    option.myAnchor,
                    option.myAnchorFieldName,
                    option.mySettingsClass,
                    field
                ));
            }
            catch (NoSuchFieldException | SecurityException e) {
                LOG.error(e);
            }
        }
    }

    private LocalizeValue getRenamedTitle(String fieldName, LocalizeValue defaultTitle) {
        return myRenamedFields.getOrDefault(fieldName, defaultTitle);
    }

    protected static class MyTreeCellRenderer implements TreeCellRenderer {
        private final JLabel myLabel;
        private final JCheckBox myCheckBox;

        public MyTreeCellRenderer() {
            myLabel = new JLabel();
            myCheckBox = new JCheckBox();
            myCheckBox.setMargin(JBUI.emptyInsets());
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean isSelected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (value instanceof MyToggleTreeNode treeNode) {
                JToggleButton button = myCheckBox;
                button.setText(treeNode.getText().get());
                button.setSelected(treeNode.isSelected);
                if (isSelected) {
                    button.setForeground(UIUtil.getTreeSelectionForeground(true));
                    button.setBackground(UIUtil.getTreeSelectionBackground(true));
                }
                else {
                    button.setForeground(UIUtil.getTreeTextForeground());
                    button.setBackground(tree.getBackground());
                }

                button.setEnabled(tree.isEnabled() && treeNode.isEnabled());

                return button;
            }
            else {
                myLabel.setText(value.toString());
                myLabel.setFont(myLabel.getFont().deriveFont(Font.BOLD));
                myLabel.setOpaque(true);

                if (isSelected) {
                    myLabel.setForeground(UIUtil.getTreeSelectionForeground(true));
                    myLabel.setBackground(UIUtil.getTreeSelectionBackground(true));
                }
                else {
                    myLabel.setForeground(UIUtil.getTreeTextForeground());
                    myLabel.setBackground(tree.getBackground());
                }

                myLabel.setEnabled(tree.isEnabled());

                return myLabel;
            }
        }
    }

    private class BooleanOptionKey extends OrderedOption {
        final LocalizeValue myGroupName;
        LocalizeValue myTitle;
        final Field myField;
        private boolean myEnabled = true;

        public BooleanOptionKey(String fieldName, LocalizeValue groupName, LocalizeValue title, Field field) {
            this(fieldName, groupName, title, null, null, field);
        }

        public BooleanOptionKey(
            String fieldName,
            LocalizeValue groupName,
            LocalizeValue title,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFiledName,
            Field field
        ) {
            super(fieldName, anchor, anchorFiledName);
            myGroupName = groupName;
            myTitle = title;
            myField = field;
        }

        public void setValue(CodeStyleSettings settings, Boolean aBoolean) {
            try {
                CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
                myField.set(commonSettings, aBoolean);
            }
            catch (Throwable e) {
                LOG.error("Field: " + myField, e);
            }
        }

        public boolean getValue(CodeStyleSettings settings) throws IllegalAccessException {
            try {
                CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
                return myField.getBoolean(commonSettings);
            }
            catch (Throwable e) {
                LOG.error("Field: " + myField, e);
                return false;
            }
        }

        public void setEnabled(boolean enabled) {
            this.myEnabled = enabled;
        }

        public boolean isEnabled() {
            return this.myEnabled;
        }
    }

    private static class CustomBooleanOptionInfo {
        final Class<? extends CustomCodeStyleSettings> mySettingsClass;
        final String myFieldName;
        final LocalizeValue myTitle;
        final LocalizeValue myGroupName;
        final @Nullable OptionAnchor myAnchor;
        final @Nullable String myAnchorFieldName;

        private CustomBooleanOptionInfo(
            Class<? extends CustomCodeStyleSettings> settingsClass,
            String fieldName,
            LocalizeValue title,
            LocalizeValue groupName,
            @Nullable OptionAnchor anchor,
            @Nullable String anchorFieldName
        ) {
            mySettingsClass = settingsClass;
            myFieldName = fieldName;
            myTitle = title;
            myGroupName = groupName;
            myAnchor = anchor;
            myAnchorFieldName = anchorFieldName;
        }
    }

    private class CustomBooleanOptionKey<T extends CustomCodeStyleSettings> extends BooleanOptionKey {
        private final Class<T> mySettingsClass;

        public CustomBooleanOptionKey(
            String fieldName,
            LocalizeValue groupName,
            LocalizeValue title,
            OptionAnchor anchor,
            String anchorFieldName,
            Class<T> settingsClass,
            Field field
        ) {
            super(fieldName, groupName, title, anchor, anchorFieldName, field);
            mySettingsClass = settingsClass;
        }

        @Override
        public void setValue(CodeStyleSettings settings, Boolean aBoolean) {
            CustomCodeStyleSettings customSettings = settings.getCustomSettings(mySettingsClass);
            try {
                myField.set(customSettings, aBoolean);
            }
            catch (Throwable e) {
                LOG.error("Field: " + myField, e);
            }
        }

        @Override
        public boolean getValue(CodeStyleSettings settings) throws IllegalAccessException {
            try {
                CustomCodeStyleSettings customSettings = settings.getCustomSettings(mySettingsClass);
                return myField.getBoolean(customSettings);
            }
            catch (Throwable e) {
                LOG.error("Field: " + myField, e);
                return false;
            }
        }
    }

    private static class MyToggleTreeNode extends DefaultMutableTreeNode {
        private final Object myKey;
        private final LocalizeValue myText;
        private boolean isSelected;
        private boolean isEnabled = true;

        public MyToggleTreeNode(Object key, LocalizeValue text) {
            myKey = key;
            myText = text;
        }

        public Object getKey() {
            return myKey;
        }

        public LocalizeValue getText() {
            return myText;
        }

        public void setSelected(boolean val) {
            isSelected = val;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setEnabled(boolean val) {
            isEnabled = val;
        }

        public boolean isEnabled() {
            return isEnabled;
        }
    }

    @Override
    public JComponent getPanel() {
        return myPanel;
    }

    @Override
    public Set<LocalizeValue> processListOptions() {
        Set<LocalizeValue> result = new HashSet<>();
        for (BooleanOptionKey key : myKeys) {
            result.add(key.myTitle);
            if (key.myGroupName.isNotEmpty()) {
                result.add(key.myGroupName);
            }
        }
        result.addAll(myRenamedFields.values());
        for (LocalizeValue groupName : myCustomOptions.keySet()) {
            if (groupName.isEmpty()) {
                continue;
            }
            result.add(groupName);
            for (CustomBooleanOptionInfo trinity : myCustomOptions.get(groupName)) {
                result.add(trinity.myTitle);
            }
        }
        return result;
    }

    protected boolean shouldHideOptions() {
        return false;
    }

    private boolean isOptionVisible(BooleanOptionKey key) {
        if (!shouldHideOptions()) {
            return true;
        }
        if (myShowAllStandardOptions || myAllowedOptions.contains(key.getOptionName())) {
            return true;
        }
        for (CustomBooleanOptionInfo customOption : myCustomOptions.get(key.myGroupName)) {
            if (customOption.myFieldName.equals(key.getOptionName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void moveStandardOption(String fieldName, LocalizeValue newGroup) {
        myRemappedGroups.put(fieldName, newGroup);
    }

    private LocalizeValue getRemappedGroup(String fieldName, LocalizeValue defaultName) {
        return myRemappedGroups.getOrDefault(fieldName, defaultName);
    }
}
