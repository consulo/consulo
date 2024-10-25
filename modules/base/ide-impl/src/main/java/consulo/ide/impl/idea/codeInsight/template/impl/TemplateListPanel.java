/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.component.persist.scheme.SchemeManager;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.impl.internal.template.TemplateComparator;
import consulo.language.editor.impl.internal.template.TemplateGroup;
import consulo.language.editor.impl.internal.template.TemplateImpl;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateOptionalProcessor;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class TemplateListPanel extends JPanel implements Disposable {
    public static final String ABBREVIATION = "<abbreviation>";

    public static final Comparator<Template> TEMPLATE_COMPARATOR = TemplateComparator.INSTANCE;

    private CheckboxTree myTree;
    private final List<TemplateGroup> myTemplateGroups = new ArrayList<>();
    private JComboBox myExpandByCombo;
    private static final String SPACE = CodeInsightBundle.message("template.shortcut.space");
    private static final String TAB = CodeInsightBundle.message("template.shortcut.tab");
    private static final String ENTER = CodeInsightBundle.message("template.shortcut.enter");

    private CheckedTreeNode myTreeRoot = new CheckedTreeNode(null);

    private boolean myUpdateNeeded = false;

    private static final Logger LOG = Logger.getInstance(TemplateListPanel.class);

    private final Map<Integer, Map<TemplateOptionalProcessor, Boolean>> myTemplateOptions = new LinkedHashMap<>();
    private final Map<Integer, Map<TemplateContextType, Boolean>> myTemplateContext = new LinkedHashMap<>();
    private Wrapper myDetailsWrapper = new Wrapper();
    private JComponent myPlaceholderElement;
    private LiveTemplateSettingsEditor myCurrentTemplateEditor;

    public TemplateListPanel() {
        super(new BorderLayout());

        myDetailsWrapper.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        JLabel label = new JLabel("No live template is selected");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        myPlaceholderElement = label;

        myDetailsWrapper.setContent(myPlaceholderElement);

        add(createExpandByPanel(), BorderLayout.NORTH);

        Splitter splitter = new Splitter(true, 0.5f);
        splitter.setFirstComponent(createTable());
        splitter.setSecondComponent(myDetailsWrapper);
        add(splitter, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
        if (myCurrentTemplateEditor != null) {
            myCurrentTemplateEditor.dispose();
        }
    }

    public void reset() {
        myTemplateOptions.clear();
        myTemplateContext.clear();

        TemplateSettingsImpl templateSettings = TemplateSettingsImpl.getInstanceImpl();
        List<TemplateGroup> groups = new ArrayList<>(templateSettings.getTemplateGroups());

        Collections.sort(groups, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        initTemplates(groups, templateSettings.getLastSelectedTemplateGroup(), templateSettings.getLastSelectedTemplateKey());


        if (templateSettings.getDefaultShortcutChar() == TemplateSettingsImpl.TAB_CHAR) {
            myExpandByCombo.setSelectedItem(TAB);
        }
        else if (templateSettings.getDefaultShortcutChar() == TemplateSettingsImpl.ENTER_CHAR) {
            myExpandByCombo.setSelectedItem(ENTER);
        }
        else {
            myExpandByCombo.setSelectedItem(SPACE);
        }

        UiNotifyConnector.doWhenFirstShown(this, () -> updateTemplateDetails(false));

        myUpdateNeeded = true;
    }

    public void apply() throws ConfigurationException {
        List<TemplateGroup> templateGroups = getTemplateGroups();
        for (TemplateGroup templateGroup : templateGroups) {
            Set<String> names = new HashSet<>();

            for (TemplateImpl template : templateGroup.getElements()) {
                if (StringUtil.isEmptyOrSpaces(template.getKey())) {
                    throw new ConfigurationException(
                        "A live template with an empty key has been found in " + templateGroup.getName() + " group, " +
                            "such live templates cannot be invoked");
                }

                if (StringUtil.isEmptyOrSpaces(template.getString())) {
                    throw new ConfigurationException(
                        "A live template with an empty text has been found in " + templateGroup.getName() + " group, " +
                            "such live templates cannot be invoked"
                    );
                }

                if (!names.add(template.getKey())) {
                    throw new ConfigurationException(
                        "Duplicate " + template.getKey() + " live templates in " + templateGroup.getName() + " group"
                    );
                }
            }
        }


        for (TemplateGroup templateGroup : templateGroups) {
            for (TemplateImpl template : templateGroup.getElements()) {
                template.applyOptions(getTemplateOptions(template));
                template.applyContext(getTemplateContext(template));
            }
        }
        TemplateSettingsImpl templateSettings = TemplateSettingsImpl.getInstanceImpl();
        templateSettings.setTemplates(templateGroups);
        templateSettings.setDefaultShortcutChar(getDefaultShortcutChar());

        reset();
    }

    public boolean isModified() {
        TemplateSettingsImpl templateSettings = TemplateSettingsImpl.getInstanceImpl();
        if (templateSettings.getDefaultShortcutChar() != getDefaultShortcutChar()) {

            return true;
        }

        List<TemplateGroup> originalGroups = templateSettings.getTemplateGroups();
        List<TemplateGroup> newGroups = getTemplateGroups();

        if (checkAreEqual(collectTemplates(originalGroups), collectTemplates(newGroups))) {
            return false;
        }

        return true;
    }

    public void editTemplate(TemplateImpl template) {
        selectTemplate(template.getGroupName(), template.getKey());
        updateTemplateDetails(true);
    }

    @Nullable
    public JComponent getPreferredFocusedComponent() {
        if (getTemplate(getSingleSelectedIndex()) != null) {
            return myCurrentTemplateEditor.getKeyField();
        }
        return null;
    }

    private static List<TemplateImpl> collectTemplates(final List<TemplateGroup> groups) {
        ArrayList<TemplateImpl> result = new ArrayList<>();
        for (TemplateGroup group : groups) {
            result.addAll(group.getElements());
        }
        Collections.sort(result, (o1, o2) -> {
            final int groupsEqual = o1.getGroupName().compareToIgnoreCase(o2.getGroupName());
            if (groupsEqual != 0) {
                return groupsEqual;
            }
            return o1.getKey().compareToIgnoreCase(o2.getKey());
        });
        return result;
    }

    private boolean checkAreEqual(final List<TemplateImpl> originalGroup, final List<TemplateImpl> newGroup) {
        if (originalGroup.size() != newGroup.size()) {
            return false;
        }

        for (int i = 0; i < newGroup.size(); i++) {
            TemplateImpl newTemplate = newGroup.get(i);
            newTemplate.parseSegments();
            TemplateImpl originalTemplate = originalGroup.get(i);
            originalTemplate.parseSegments();
            if (!originalTemplate.equals(newTemplate)) {
                return false;
            }

            if (originalTemplate.isDeactivated() != newTemplate.isDeactivated()) {
                return false;
            }

            if (!newTemplate.getVariables().equals(originalTemplate.getVariables())) {
                return false;
            }

            if (!areOptionsEqual(newTemplate, originalTemplate)) {
                return false;
            }

            if (!areContextsEqual(newTemplate, originalTemplate)) {
                return false;
            }
        }

        return true;
    }

    private boolean areContextsEqual(final TemplateImpl newTemplate, final TemplateImpl originalTemplate) {
        Map<TemplateContextType, Boolean> templateContext = getTemplateContext(newTemplate);
        for (TemplateContextType processor : templateContext.keySet()) {
            if (originalTemplate.getTemplateContext().isEnabled(processor) != templateContext.get(processor)) {
                return false;
            }
        }
        return true;
    }

    private boolean areOptionsEqual(final TemplateImpl newTemplate, final TemplateImpl originalTemplate) {
        Map<TemplateOptionalProcessor, Boolean> templateOptions = getTemplateOptions(newTemplate);
        for (TemplateOptionalProcessor processor : templateOptions.keySet()) {
            if (processor.isEnabled(originalTemplate) != templateOptions.get(processor)) {
                return false;
            }
        }
        return true;
    }

    private Map<TemplateContextType, Boolean> getTemplateContext(final TemplateImpl newTemplate) {
        return myTemplateContext.get(getKey(newTemplate));
    }

    private Map<TemplateOptionalProcessor, Boolean> getTemplateOptions(final TemplateImpl newTemplate) {
        return myTemplateOptions.get(getKey(newTemplate));
    }

    private char getDefaultShortcutChar() {
        Object selectedItem = myExpandByCombo.getSelectedItem();
        if (TAB.equals(selectedItem)) {
            return TemplateSettingsImpl.TAB_CHAR;
        }
        else if (ENTER.equals(selectedItem)) {
            return TemplateSettingsImpl.ENTER_CHAR;
        }
        else {
            return TemplateSettingsImpl.SPACE_CHAR;
        }
    }

    private List<TemplateGroup> getTemplateGroups() {
        return myTemplateGroups;
    }

    private void createTemplateEditor(
        final TemplateImpl template,
        String shortcut,
        Map<TemplateOptionalProcessor, Boolean> options,
        Map<TemplateContextType, Boolean> context
    ) {
        myCurrentTemplateEditor = new LiveTemplateSettingsEditor(
            template,
            shortcut,
            options,
            context,
            new Runnable() {
                @Override
                public void run() {
                    DefaultMutableTreeNode node = getNode(getSingleSelectedIndex());
                    if (node != null) {
                        ((DefaultTreeModel)myTree.getModel()).nodeChanged(node);
                        TemplateSettingsImpl.getInstanceImpl().setLastSelectedTemplate(template.getGroupName(), template.getKey());
                    }
                }
            },
            TemplateSettingsImpl.getInstanceImpl().getTemplate(template.getKey(), template.getGroupName()) != null
        );


        myDetailsWrapper.setContent(myCurrentTemplateEditor);
    }

    private JPanel createExpandByPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.weighty = 0;
        gbConstraints.weightx = 0;
        gbConstraints.gridy = 0;
        panel.add(new JLabel(CodeInsightBundle.message("templates.dialog.shortcut.chooser.label")), gbConstraints);

        gbConstraints.gridx = 1;
        gbConstraints.insets = JBUI.insetsLeft(4);
        myExpandByCombo = new JComboBox();
        myExpandByCombo.addItem(SPACE);
        myExpandByCombo.addItem(TAB);
        myExpandByCombo.addItem(ENTER);
        panel.add(myExpandByCombo, gbConstraints);

        gbConstraints.gridx = 2;
        gbConstraints.weightx = 1;
        panel.add(new JPanel(), gbConstraints);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        return panel;
    }

    @Nullable
    private TemplateImpl getTemplate(int row) {
        JTree tree = myTree;
        TreePath path = tree.getPathForRow(row);
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            if (node.getUserObject() instanceof TemplateImpl) {
                return (TemplateImpl)node.getUserObject();
            }
        }

        return null;
    }

    @Nullable
    private TemplateGroup getGroup(int row) {
        JTree tree = myTree;
        TreePath path = tree.getPathForRow(row);
        if (path != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            if (node.getUserObject() instanceof TemplateGroup) {
                return (TemplateGroup)node.getUserObject();
            }
        }

        return null;
    }

    private void moveTemplates(Map<TemplateImpl, DefaultMutableTreeNode> map, String newGroupName) {
        List<TreePath> toSelect = new ArrayList<>();
        for (TemplateImpl template : map.keySet()) {
            DefaultMutableTreeNode oldTemplateNode = map.get(template);

            TemplateGroup oldGroup = getTemplateGroup(template.getGroupName());
            if (oldGroup != null) {
                oldGroup.removeElement(template);
            }

            template.setGroupName(newGroupName);

            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)oldTemplateNode.getParent();
            removeNodeFromParent(oldTemplateNode);
            if (parent.getChildCount() == 0) {
                removeNodeFromParent(parent);
            }

            toSelect.add(new TreePath(registerTemplate(template).getPath()));
        }

        myTree.getSelectionModel().clearSelection();
        for (TreePath path : toSelect) {
            myTree.expandPath(path.getParentPath());
            myTree.addSelectionPath(path);
            myTree.scrollRowToVisible(myTree.getRowForPath(path));
        }
    }

    @Nullable
    private DefaultMutableTreeNode getNode(final int row) {
        JTree tree = myTree;
        TreePath path = tree.getPathForRow(row);
        if (path != null) {
            return (DefaultMutableTreeNode)path.getLastPathComponent();
        }

        return null;

    }

    @Nullable
    private TemplateGroup getTemplateGroup(final String groupName) {
        for (TemplateGroup group : myTemplateGroups) {
            if (group.getName().equals(groupName)) {
                return group;
            }
        }

        return null;
    }

    private void addRow() {
        String defaultGroup = TemplateSettingsImpl.USER_GROUP_NAME;
        final DefaultMutableTreeNode node = getNode(getSingleSelectedIndex());
        if (node != null) {
            if (node.getUserObject() instanceof TemplateImpl) {
                defaultGroup = ((TemplateImpl)node.getUserObject()).getGroupName();
            }
            else if (node.getUserObject() instanceof TemplateGroup) {
                defaultGroup = ((TemplateGroup)node.getUserObject()).getName();
            }
        }

        addTemplate(new TemplateImpl(ABBREVIATION, "", defaultGroup));
    }

    public void addTemplate(TemplateImpl template) {
        myTemplateOptions.put(getKey(template), template.createOptions());
        myTemplateContext.put(getKey(template), template.createContext());

        registerTemplate(template);
        updateTemplateDetails(true);
    }

    private static int getKey(final TemplateImpl template) {
        return System.identityHashCode(template);
    }

    private void copyRow() {
        int selected = getSingleSelectedIndex();
        if (selected < 0) {
            return;
        }

        TemplateImpl orTemplate = getTemplate(selected);
        LOG.assertTrue(orTemplate != null);
        TemplateImpl template = orTemplate.copy();
        template.setKey(ABBREVIATION);
        myTemplateOptions.put(getKey(template), new HashMap<>(getTemplateOptions(orTemplate)));
        myTemplateContext.put(getKey(template), new HashMap<>(getTemplateContext(orTemplate)));
        registerTemplate(template);

        updateTemplateDetails(true);
    }

    private int getSingleSelectedIndex() {
        int[] rows = myTree.getSelectionRows();
        return rows != null && rows.length == 1 ? rows[0] : -1;
    }

    private void removeRows() {
        TreeNode toSelect = null;

        TreePath[] paths = myTree.getSelectionPaths();
        if (paths == null) {
            return;
        }

        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            Object o = node.getUserObject();
            if (o instanceof TemplateGroup) {
                //noinspection SuspiciousMethodCalls
                myTemplateGroups.remove(o);
                removeNodeFromParent(node);
            }
            else if (o instanceof TemplateImpl) {
                TemplateImpl template = (TemplateImpl)o;
                TemplateGroup templateGroup = getTemplateGroup(template.getGroupName());
                if (templateGroup != null) {
                    templateGroup.removeElement(template);
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();

                    if (templateGroup.getElements().isEmpty()) {
                        myTemplateGroups.remove(templateGroup);
                        removeNodeFromParent(parent);
                    }
                    else {
                        toSelect = parent.getChildAfter(node);
                        removeNodeFromParent(node);
                    }
                }
            }
        }

        if (toSelect instanceof DefaultMutableTreeNode) {
            setSelectedNode((DefaultMutableTreeNode)toSelect);
        }
    }

    private JPanel createTable() {
        myTreeRoot = new CheckedTreeNode(null);

        myTree = new CheckboxTree(
            new CheckboxTree.CheckboxTreeCellRenderer() {
                @Override
                public void customizeRenderer(
                    final JTree tree,
                    Object value,
                    final boolean selected,
                    final boolean expanded,
                    final boolean leaf,
                    final int row,
                    final boolean hasFocus
                ) {
                    if (!(value instanceof DefaultMutableTreeNode)) {
                        return;
                    }
                    value = ((DefaultMutableTreeNode)value).getUserObject();

                    if (value instanceof TemplateImpl) {
                        getTextRenderer().append(((TemplateImpl)value).getKey(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        String description = ((TemplateImpl)value).getDescription();
                        if (description != null && description.length() > 0) {
                            getTextRenderer().append(" (" + description + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
                        }
                    }
                    else if (value instanceof TemplateGroup) {
                        getTextRenderer().append(((TemplateGroup)value).getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                    }
                }
            },
            myTreeRoot
        ) {
            @Override
            protected void onNodeStateChanged(final CheckedTreeNode node) {
                Object obj = node.getUserObject();
                if (obj instanceof TemplateImpl) {
                    ((TemplateImpl)obj).setDeactivated(!node.isChecked());
                }
            }

            @Override
            protected void installSpeedSearch() {
                new TreeSpeedSearch(this, o -> {
                    Object object = ((DefaultMutableTreeNode)o.getLastPathComponent()).getUserObject();
                    if (object instanceof TemplateGroup) {
                        return ((TemplateGroup)object).getName();
                    }
                    if (object instanceof TemplateImpl) {
                        TemplateImpl template = (TemplateImpl)object;
                        return StringUtil.notNullize(template.getKey()) + " " + StringUtil.notNullize(template.getDescription()) + " " + template.getTemplateText();
                    }
                    return "";
                }, true);

            }
        };
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);

        myTree.getSelectionModel().addTreeSelectionListener(e -> {
            TemplateSettingsImpl templateSettings = TemplateSettingsImpl.getInstanceImpl();
            TemplateImpl template = getTemplate(getSingleSelectedIndex());
            if (template != null) {
                templateSettings.setLastSelectedTemplate(template.getGroupName(), template.getKey());
            }
            else {
                templateSettings.setLastSelectedTemplate(null, null);
                myDetailsWrapper.setContent(myPlaceholderElement);
            }

            if (myUpdateNeeded) {
                updateTemplateDetails(false);
            }
        });

        myTree.registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    myCurrentTemplateEditor.focusKey();
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        );

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                renameGroup();
                return true;
            }
        }.installOn(myTree);

        installPopup();


        DnDSupport.createBuilder(myTree)
            .setBeanProvider(dnDActionInfo -> {
                Point point = dnDActionInfo.getPoint();
                if (myTree.getPathForLocation(point.x, point.y) == null) {
                    return null;
                }

                Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();

                return !templates.isEmpty() ? new DnDDragStartBean(templates) : null;
            })
            .setDisposableParent(this).setTargetChecker(new DnDTargetChecker() {
                @Override
                public boolean update(DnDEvent event) {
                    @SuppressWarnings("unchecked") Set<String> oldGroupNames =
                        getAllGroups((Map<TemplateImpl, DefaultMutableTreeNode>)event.getAttachedObject());
                    TemplateGroup group = getDropGroup(event);
                    boolean differentGroup = group != null && !oldGroupNames.contains(group.getName());
                    event.setDropPossible(differentGroup, "");
                    return true;
                }
            })
            .setDropHandler(new DnDDropHandler() {
                @Override
                public void drop(DnDEvent event) {
                    //noinspection unchecked
                    moveTemplates(
                        (Map<TemplateImpl, DefaultMutableTreeNode>)event.getAttachedObject(),
                        ObjectUtil.assertNotNull(getDropGroup(event)).getName()
                    );
                }
            })
            .setImageProvider(dnDActionInfo -> {
                Point point = dnDActionInfo.getPoint();
                TreePath path = myTree.getPathForLocation(point.x, point.y);
                return path == null ? null : new DnDImage(DnDAwareTree.getDragImage(myTree, path, point).first);
            })
            .install();

        if (myTemplateGroups.size() > 0) {
            myTree.setSelectionInterval(0, 0);
        }

        return initToolbar().createPanel();
    }

    private ToolbarDecorator initToolbar() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree)
            .setAddAction(e -> addRow())
            .setRemoveAction(e -> removeRows())
            .disableDownAction()
            .disableUpAction()
            .addExtraAction(new AnActionButton("Copy", PlatformIconGroup.actionsCopy()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    copyRow();
                }

                @Override
                public void updateButton(AnActionEvent e) {
                    e.getPresentation().setEnabled(getTemplate(getSingleSelectedIndex()) != null);
                }
            });
        return decorator.setToolbarPosition(ActionToolbarPosition.RIGHT);
    }

    @Nullable
    private TemplateGroup getDropGroup(DnDEvent event) {
        Point point = event.getPointOn(myTree);
        return getGroup(myTree.getRowForLocation(point.x, point.y));
    }

    private void installPopup() {
        final DumbAwareAction rename = new DumbAwareAction("Rename") {
            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                final int selected = getSingleSelectedIndex();
                final TemplateGroup templateGroup = getGroup(selected);
                boolean enabled = templateGroup != null;
                e.getPresentation().setEnabled(enabled);
                e.getPresentation().setVisible(enabled);
                super.update(e);
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                renameGroup();
            }
        };
        rename.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_RENAME).getShortcutSet(), myTree);

        final DefaultActionGroup move = new DefaultActionGroup("Move", true) {
            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                final Map<TemplateImpl, DefaultMutableTreeNode> templates = getSelectedTemplates();
                boolean enabled = !templates.isEmpty();
                e.getPresentation().setEnabled(enabled);
                e.getPresentation().setVisible(enabled);

                if (enabled) {
                    Set<String> oldGroups = getAllGroups(templates);

                    removeAll();
                    SchemeManager<TemplateGroup, TemplateGroup> schemeManager = TemplateSettingsImpl.getInstanceImpl().getSchemeManager();

                    for (TemplateGroup group : getTemplateGroups()) {
                        final String newGroupName = group.getName();
                        if (!oldGroups.contains(newGroupName)) {
                            add(new DumbAwareAction(newGroupName) {
                                @RequiredUIAccess
                                @Override
                                public void actionPerformed(AnActionEvent e) {
                                    moveTemplates(templates, newGroupName);
                                }
                            });
                        }
                    }
                    addSeparator();
                    add(new DumbAwareAction("New group...") {
                        @RequiredUIAccess
                        @Override
                        public void actionPerformed(AnActionEvent e) {
                            String newName = Messages.showInputDialog(
                                myTree,
                                "Enter the new group name:",
                                "Move to a New Group",
                                null,
                                "",
                                new TemplateGroupInputValidator(null)
                            );
                            if (newName != null) {
                                moveTemplates(templates, newName);
                            }
                        }
                    });
                }
            }
        };

        myTree.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                final DefaultActionGroup group = new DefaultActionGroup();
                group.add(rename);
                group.add(move);
                ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group).getComponent().show(comp, x, y);
            }
        });
    }

    private static Set<String> getAllGroups(Map<TemplateImpl, DefaultMutableTreeNode> templates) {
        Set<String> oldGroups = new HashSet<>();
        for (TemplateImpl template : templates.keySet()) {
            oldGroups.add(template.getGroupName());
        }
        return oldGroups;
    }

    private Map<TemplateImpl, DefaultMutableTreeNode> getSelectedTemplates() {
        TreePath[] paths = myTree.getSelectionPaths();
        if (paths == null) {
            return Collections.emptyMap();
        }
        Map<TemplateImpl, DefaultMutableTreeNode> templates = new LinkedHashMap<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
            Object o = node.getUserObject();
            if (!(o instanceof TemplateImpl)) {
                return Collections.emptyMap();
            }
            templates.put((TemplateImpl)o, node);
        }
        return templates;
    }

    private void renameGroup() {
        final int selected = getSingleSelectedIndex();
        final TemplateGroup templateGroup = getGroup(selected);
        if (templateGroup == null) {
            return;
        }

        final String oldName = templateGroup.getName();
        String newName = Messages.showInputDialog(
            myTree,
            "Enter the new group name:",
            "Rename",
            null,
            oldName,
            new TemplateGroupInputValidator(oldName)
        );

        if (newName != null && !newName.equals(oldName)) {
            templateGroup.setName(newName);
            ((DefaultTreeModel)myTree.getModel()).nodeChanged(getNode(selected));
        }
    }

    private void updateTemplateDetails(boolean focusKey) {
        int selected = getSingleSelectedIndex();
        if (selected < 0 || getTemplate(selected) == null) {
            myDetailsWrapper.setContent(myPlaceholderElement);
        }
        else {
            TemplateImpl newTemplate = getTemplate(selected);
            if (myCurrentTemplateEditor == null || myCurrentTemplateEditor.getTemplate() != newTemplate) {
                if (myCurrentTemplateEditor != null) {
                    myCurrentTemplateEditor.dispose();
                }
                createTemplateEditor(
                    newTemplate,
                    (String)myExpandByCombo.getSelectedItem(),
                    getTemplateOptions(newTemplate),
                    getTemplateContext(newTemplate)
                );
                myCurrentTemplateEditor.resetUi();
                if (focusKey) {
                    myCurrentTemplateEditor.focusKey();
                }
            }

            myDetailsWrapper.setContent(myCurrentTemplateEditor);
        }
    }

    private CheckedTreeNode registerTemplate(TemplateImpl template) {
        TemplateGroup newGroup = getTemplateGroup(template.getGroupName());
        if (newGroup == null) {
            newGroup = new TemplateGroup(template.getGroupName());
            insertNewGroup(newGroup);
        }
        if (!newGroup.contains(template)) {
            newGroup.addElement(template);
        }

        CheckedTreeNode node = new CheckedTreeNode(template);
        node.setChecked(!template.isDeactivated());
        for (DefaultMutableTreeNode child = (DefaultMutableTreeNode)myTreeRoot.getFirstChild(); child != null;
             child = (DefaultMutableTreeNode)myTreeRoot.getChildAfter(child)) {
            if (((TemplateGroup)child.getUserObject()).getName().equals(template.getGroupName())) {
                int index = getIndexToInsert(child, template.getKey());
                child.insert(node, index);
                ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(child, new int[]{index});
                setSelectedNode(node);
            }
        }
        return node;
    }

    private void insertNewGroup(final TemplateGroup newGroup) {
        myTemplateGroups.add(newGroup);

        int index = getIndexToInsert(myTreeRoot, newGroup.getName());
        DefaultMutableTreeNode groupNode = new CheckedTreeNode(newGroup);
        myTreeRoot.insert(groupNode, index);
        ((DefaultTreeModel)myTree.getModel()).nodesWereInserted(myTreeRoot, new int[]{index});
    }

    private static int getIndexToInsert(DefaultMutableTreeNode parent, String key) {
        if (parent.getChildCount() == 0) {
            return 0;
        }

        int res = 0;
        for (DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getFirstChild(); child != null;
             child = (DefaultMutableTreeNode)parent.getChildAfter(child)) {
            Object o = child.getUserObject();
            String key1 = o instanceof TemplateImpl ? ((TemplateImpl)o).getKey() : ((TemplateGroup)o).getName();
            if (key1.compareToIgnoreCase(key) > 0) {
                return res;
            }
            res++;
        }
        return res;
    }

    private void setSelectedNode(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        myTree.expandPath(path.getParentPath());
        int row = myTree.getRowForPath(path);
        myTree.setSelectionRow(row);
        myTree.scrollRowToVisible(row);
    }

    private void removeNodeFromParent(DefaultMutableTreeNode node) {
        TreeNode parent = node.getParent();
        int idx = parent.getIndex(node);
        node.removeFromParent();

        ((DefaultTreeModel)myTree.getModel()).nodesWereRemoved(parent, new int[]{idx}, new TreeNode[]{node});
    }

    private void initTemplates(List<TemplateGroup> groups, String lastSelectedGroup, String lastSelectedKey) {
        myTreeRoot.removeAllChildren();
        myTemplateGroups.clear();
        for (TemplateGroup group : groups) {
            myTemplateGroups.add((TemplateGroup)group.copy());
        }

        for (TemplateGroup group : myTemplateGroups) {
            CheckedTreeNode groupNode = new CheckedTreeNode(group);
            addTemplateNodes(group, groupNode);
            myTreeRoot.add(groupNode);
        }
        fireStructureChange();

        selectTemplate(lastSelectedGroup, lastSelectedKey);
    }

    private void selectTemplate(final String lastSelectedGroup, final String lastSelectedKey) {
        TreeUtil.traverseDepth(myTreeRoot, node -> {
            Object o = ((DefaultMutableTreeNode)node).getUserObject();
            if (lastSelectedKey == null
                && o instanceof TemplateGroup
                && Comparing.equal(lastSelectedGroup, ((TemplateGroup)o).getName())
                || o instanceof TemplateImpl
                && Comparing.equal(lastSelectedKey, ((TemplateImpl)o).getKey())
                && Comparing.equal(lastSelectedGroup, ((TemplateImpl)o).getGroupName())) {
                setSelectedNode((DefaultMutableTreeNode)node);
                return false;
            }

            return true;
        });
    }

    private void fireStructureChange() {
        ((DefaultTreeModel)myTree.getModel()).nodeStructureChanged(myTreeRoot);
    }

    private void addTemplateNodes(TemplateGroup group, CheckedTreeNode groupNode) {
        List<TemplateImpl> templates = new ArrayList<>(group.getElements());
        Collections.sort(templates, TEMPLATE_COMPARATOR);
        for (final TemplateImpl template : templates) {
            myTemplateOptions.put(getKey(template), template.createOptions());
            myTemplateContext.put(getKey(template), template.createContext());
            CheckedTreeNode node = new CheckedTreeNode(template);
            node.setChecked(!template.isDeactivated());
            groupNode.add(node);
        }
    }

    private class TemplateGroupInputValidator implements InputValidator {
        private final String myOldName;

        public TemplateGroupInputValidator(String oldName) {
            myOldName = oldName;
        }

        @RequiredUIAccess
        @Override
        public boolean checkInput(String inputString) {
            return StringUtil.isNotEmpty(inputString) && (getTemplateGroup(inputString) == null || inputString.equals(myOldName));
        }

        @RequiredUIAccess
        @Override
        public boolean canClose(String inputString) {
            return checkInput(inputString);
        }
    }
}
