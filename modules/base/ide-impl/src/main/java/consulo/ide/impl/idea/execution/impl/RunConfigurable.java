/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package consulo.ide.impl.idea.execution.impl;

import consulo.application.AllIcons;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configurable.BaseConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunTask;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.*;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorConfigurable;
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.RunConfigurationSelector;
import consulo.execution.impl.internal.configuration.*;
import consulo.execution.internal.RunManagerConfig;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.localize.ExecutionLocalize;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ide.impl.idea.util.config.StorageAccessors;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.RowsDnDSupport;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static consulo.ide.impl.idea.execution.impl.RunConfigurable.NodeKind.*;
import static consulo.ui.ex.awt.dnd.RowsDnDSupport.RefinedDropSupport.Position.*;

public class RunConfigurable extends BaseConfigurable {
    private static ConfigurationType HIDDEN_ITEMS_STUB = new ConfigurationType() {
        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.of();
        }

        @Override
        public Image getIcon() {
            return Image.empty(Image.DEFAULT_ICON_SIZE);
        }

        @Nonnull
        @Override
        public String getId() {
            return "";
        }

        @Override
        public ConfigurationFactory[] getConfigurationFactories() {
            return new ConfigurationFactory[0];
        }
    };

    private static final Image ADD_ICON = IconUtil.getAddIcon();
    private static final Image REMOVE_ICON = IconUtil.getRemoveIcon();

    @NonNls
    public static final String DIVIDER_PROPORTION = "dividerProportion";
    @NonNls
    private static final Object DEFAULTS = new Object() {
        @Override
        public String toString() {
            return "Defaults";
        }
    };

    private volatile boolean isDisposed = false;

    private final Project myProject;
    private RunDialogBase myRunDialog;
    private final TitlelessDecorator myTitlelessDecorator;
    final DefaultMutableTreeNode myRoot = new DefaultMutableTreeNode("Root");
    final MyTreeModel myTreeModel = new MyTreeModel(myRoot);
    final Tree myTree = new Tree(myTreeModel);
    private final JPanel myRightPanel = new JPanel(new BorderLayout());
    private final Splitter mySplitter = new OnePixelSplitter(false);
    private JPanel myWholePanel;
    private final StorageAccessors myProperties = StorageAccessors.createGlobal("RunConfigurable");
    private Configurable mySelectedConfigurable = null;
    private static final Logger LOG = Logger.getInstance(RunConfigurable.class);
    private final JTextField myRecentsLimit = new JTextField("5", 2);
    private final JCheckBox myConfirmation = new JCheckBox(ExecutionLocalize.rerunConfirmationCheckbox().get(), true);
    private final List<Pair<UnnamedConfigurable, JComponent>> myAdditionalSettings = new ArrayList<>();
    private Map<ConfigurationFactory, Configurable> myStoredComponents = new HashMap<>();
    private ToolbarDecorator myToolbarDecorator;
    private boolean isFolderCreating;
    private RunConfigurable.MyToolbarAddAction myAddAction = new MyToolbarAddAction();

    private boolean myEditorMode;

    public RunConfigurable(final Project project) {
        this(project, null, TitlelessDecorator.NOTHING);
    }

    public RunConfigurable(final Project project, @Nullable final RunDialogBase runDialog, TitlelessDecorator titlelessDecorator) {
        myProject = project;
        myRunDialog = runDialog;
        myTitlelessDecorator = titlelessDecorator;
    }

    public void setEditorMode() {
        myEditorMode = true;
    }

    @Override
    public String getDisplayName() {
        return ExecutionLocalize.runConfigurableDisplayName().get();
    }

    private void initTree() {
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        UIUtil.setLineStyleAngled(myTree);
        TreeUtil.installActions(myTree);
        new TreeSpeedSearch(myTree, o -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) o.getLastPathComponent();
            final Object userObject = node.getUserObject();
            if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                return runnerAndConfigurationSettings.getName();
            }
            else if (userObject instanceof SingleConfigurationConfigurable singleConfigurationConfigurable) {
                return singleConfigurationConfigurable.getNameText();
            }
            else if (userObject instanceof ConfigurationType configurationType) {
                return configurationType.getDisplayName().get();
            }
            else if (userObject instanceof String s) {
                return s;
            }
            return o.toString();
        });

        myTree.setCellRenderer(new ColoredTreeCellRenderer() {
            @RequiredUIAccess
            @Override
            public void customizeCellRenderer(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus
            ) {
                if (value instanceof DefaultMutableTreeNode node) {
                    final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    final Object userObject = node.getUserObject();
                    Boolean shared = null;
                    if (userObject instanceof ConfigurationType configurationType) {
                        append(
                            configurationType.getDisplayName().get(),
                            parent.isRoot() ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES
                        );
                        setIcon(configurationType.getIcon());
                    }
                    else if (userObject == DEFAULTS) {
                        append("Templates", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                        setIcon(AllIcons.General.Settings);
                    }
                    else if (userObject instanceof String s) {//Folders
                        append(s, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        setIcon(AllIcons.Nodes.Folder);
                    }
                    else if (userObject instanceof ConfigurationFactory configFactory) {
                        append(configFactory.getDisplayName().get());
                        setIcon(configFactory.getIcon());
                    }
                    else {
                        final RunManagerImpl runManager = getRunManager();
                        RunnerAndConfigurationSettings configuration = null;
                        String name = null;
                        if (userObject instanceof SingleConfigurationConfigurable) {
                            final SingleConfigurationConfigurable<?> settings = (SingleConfigurationConfigurable) userObject;
                            RunnerAndConfigurationSettings configurationSettings;
                            configurationSettings = settings.getSettings();
                            configuration = configurationSettings;
                            name = settings.getNameText();
                            shared = settings.isStoreProjectConfiguration();
                            setIcon(ProgramRunnerUtil.getConfigurationIcon(configurationSettings, !settings.isValid()));
                        }
                        else if (userObject instanceof RunnerAndConfigurationSettingsImpl settings) {
                            shared = runManager.isConfigurationShared(settings);
                            setIcon(RunManagerEx.getInstanceEx(myProject).getConfigurationIcon(settings));
                            configuration = settings;
                            name = configuration.getName();
                        }
                        if (configuration != null) {
                            append(
                                name,
                                configuration.isTemporary()
                                    ? SimpleTextAttributes.GRAY_ATTRIBUTES
                                    : SimpleTextAttributes.REGULAR_ATTRIBUTES
                            );
                        }
                    }
                    if (shared != null) {
                        consulo.ui.image.Image icon = getIcon();

                        if (shared) {
                            setIcon(ImageEffects.layered(icon, PlatformIconGroup.nodesShared()));
                        }
                        else {
                            setIcon(icon);
                        }
                    }
                    setIconTextGap(2);
                }
            }
        });
        final RunManagerEx manager = getRunManager();
        final List<ConfigurationType> factories = manager.getConfigurationFactories();
        for (ConfigurationType type : factories) {
            final List<RunnerAndConfigurationSettings> configurations = manager.getConfigurationSettingsList(type);
            if (!configurations.isEmpty()) {
                final DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(type);
                myRoot.add(typeNode);
                Map<String, DefaultMutableTreeNode> folderMapping = new HashMap<String, DefaultMutableTreeNode>();
                int folderCounter = 0;
                for (RunnerAndConfigurationSettings configuration : configurations) {
                    String folder = configuration.getFolderName();
                    if (folder != null) {
                        DefaultMutableTreeNode node = folderMapping.get(folder);
                        if (node == null) {
                            node = new DefaultMutableTreeNode(folder);
                            typeNode.insert(node, folderCounter);
                            folderCounter++;
                            folderMapping.put(folder, node);
                        }
                        node.add(new DefaultMutableTreeNode(configuration));
                    }
                    else {
                        typeNode.add(new DefaultMutableTreeNode(configuration));
                    }
                }
            }
        }

        // add defaults
        final DefaultMutableTreeNode defaults = new DefaultMutableTreeNode(DEFAULTS);
        final List<ConfigurationType> configurationTypes = RunManagerImpl.getInstanceImpl(myProject).getConfigurationFactories();
        for (final ConfigurationType type : configurationTypes) {
            if (!(type instanceof UnknownConfigurationType)) {
                ConfigurationFactory[] configurationFactories = type.getConfigurationFactories();
                DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(type);
                defaults.add(typeNode);
                if (configurationFactories.length != 1) {
                    for (ConfigurationFactory factory : configurationFactories) {
                        typeNode.add(new DefaultMutableTreeNode(factory));
                    }
                }
            }
        }
        if (defaults.getChildCount() > 0) {
            myRoot.add(defaults);
        }

        myTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final TreePath selectionPath = myTree.getSelectionPath();
                if (selectionPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                    final Object userObject = getSafeUserObject(node);
                    if (userObject instanceof SingleConfigurationConfigurable) {
                        updateRightPanel((SingleConfigurationConfigurable<RunConfiguration>) userObject);
                    }
                    else if (userObject instanceof String folderName) {
                        showFolderField(getSelectedConfigurationType(), node, folderName);
                    }
                    else {
                        if (userObject instanceof ConfigurationType configurationType || userObject == DEFAULTS) {
                            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                            if (parent.isRoot()) {
                                drawPressAddButtonMessage(userObject == DEFAULTS ? null : (ConfigurationType) userObject);
                            }
                            else {
                                final ConfigurationType type = (ConfigurationType) userObject;
                                ConfigurationFactory[] factories = type.getConfigurationFactories();
                                if (factories.length == 1) {
                                    final ConfigurationFactory factory = factories[0];
                                    showTemplateConfigurable(factory);
                                }
                                else {
                                    drawPressAddButtonMessage((ConfigurationType) userObject);
                                }
                            }
                        }
                        else if (userObject instanceof ConfigurationFactory configFactory) {
                            showTemplateConfigurable(configFactory);
                        }
                    }
                }
                updateDialog();
            }
        });
        myTree.registerKeyboardAction(
            e -> clickDefaultButton(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        );
        SwingUtilities.invokeLater(() -> {
            if (isDisposed) {
                return;
            }

            myTree.requestFocusInWindow();
            final RunnerAndConfigurationSettings settings = manager.getSelectedConfiguration();
            if (settings != null) {
                final Enumeration enumeration = myRoot.breadthFirstEnumeration();
                while (enumeration.hasMoreElements()) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
                    final Object userObject = node.getUserObject();
                    if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                        final ConfigurationType configurationType = settings.getType();
                        if (configurationType != null &&
                            Comparing.strEqual(runnerAndConfigurationSettings.getConfiguration().getType().getId(), configurationType.getId()) &&
                            Comparing.strEqual(runnerAndConfigurationSettings.getConfiguration().getName(), settings.getName())) {
                            TreeUtil.selectInTree(node, true, myTree);
                            return;
                        }
                    }
                }
            }
            else {
                mySelectedConfigurable = null;
            }
            //TreeUtil.selectInTree(defaults, true, myTree);
            drawPressAddButtonMessage(null);
        });
        sortTopLevelBranches();
        ((DefaultTreeModel) myTree.getModel()).reload();
    }

    private boolean selectConfiguration(@Nonnull RunConfiguration configuration) {
        final Enumeration enumeration = myRoot.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            Object userObject = node.getUserObject();
            if (userObject instanceof SettingsEditorConfigurable settingsEditorConfigurable) {
                userObject = settingsEditorConfigurable.getSettings();
            }
            if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                final ConfigurationType configurationType = configuration.getType();
                if (Comparing.strEqual(runnerAndConfigurationSettings.getConfiguration().getType().getId(), configurationType.getId()) &&
                    Comparing.strEqual(runnerAndConfigurationSettings.getConfiguration().getName(), configuration.getName())) {
                    TreeUtil.selectInTree(node, true, myTree);
                    return true;
                }
            }
        }
        return false;
    }

    private void showTemplateConfigurable(ConfigurationFactory factory) {
        Configurable configurable = myStoredComponents.get(factory);
        if (configurable == null) {
            configurable = new TemplateConfigurable(RunManagerImpl.getInstanceImpl(myProject).getConfigurationTemplate(factory));
            myStoredComponents.put(factory, configurable);
            configurable.reset();
        }
        updateRightPanel(configurable);
    }

    private void showFolderField(final ConfigurationType type, final DefaultMutableTreeNode node, final String folderName) {
        myRightPanel.removeAll();
        JPanel panel = new JPanel(new VerticalFlowLayout(0, 0));
        final JTextField textField = new JTextField(folderName);
        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                node.setUserObject(textField.getText());
                myTreeModel.reload(node);
            }
        });
        panel.add(LabeledComponent.left(textField, "Folder name"));
        panel.add(new JLabel(ExecutionLocalize.runConfigurationRenameFolderDisclaimer().get()));

        myRightPanel.add(panel);
        myRightPanel.revalidate();
        myRightPanel.repaint();
        if (isFolderCreating) {
            textField.selectAll();
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(textField);
        }
    }

    private Object getSafeUserObject(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
            final SingleConfigurationConfigurable<RunConfiguration> configurationConfigurable =
                SingleConfigurationConfigurable.editSettings(runnerAndConfigurationSettings, null);
            installUpdateListeners(configurationConfigurable);
            node.setUserObject(configurationConfigurable);
            return configurationConfigurable;
        }
        return userObject;
    }

    public void setRunDialog(final RunDialogBase runDialog) {
        myRunDialog = runDialog;
    }

    @RequiredUIAccess
    private void updateRightPanel(final Configurable configurable) {
        myRightPanel.removeAll();
        mySelectedConfigurable = configurable;

        myRightPanel.add(configurable.createComponent(), BorderLayout.CENTER);
        if (configurable instanceof SingleConfigurationConfigurable singleConfigurationConfigurable) {
            myRightPanel.add(singleConfigurationConfigurable.getValidationComponent(), BorderLayout.SOUTH);
        }

        setupDialogBounds();
    }

    private void sortTopLevelBranches() {
        List<TreePath> expandedPaths = TreeUtil.collectExpandedPaths(myTree);
        TreeUtil.sort(myRoot, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                final Object userObject1 = ((DefaultMutableTreeNode) o1).getUserObject();
                final Object userObject2 = ((DefaultMutableTreeNode) o2).getUserObject();
                if (userObject1 instanceof ConfigurationType configType1 && userObject2 instanceof ConfigurationType configType2) {
                    return configType1.getDisplayName().compareTo(configType2.getDisplayName());
                }
                else if (userObject1 == DEFAULTS && userObject2 instanceof ConfigurationType) {
                    return 1;
                }

                return 0;
            }
        });
        TreeUtil.restoreExpandedPaths(myTree, expandedPaths);
    }

    private void update() {
        updateDialog();
        final TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            myTreeModel.reload(node);
        }
    }

    private void installUpdateListeners(final SingleConfigurationConfigurable<RunConfiguration> info) {
        final boolean[] changed = new boolean[]{false};
        info.getEditor().addSettingsEditorListener(new SettingsEditorListener<RunnerAndConfigurationSettings>() {
            @Override
            public void stateChanged(final SettingsEditor<RunnerAndConfigurationSettings> editor) {
                update();
                final RunConfiguration configuration = info.getConfiguration();
                if (configuration instanceof LocatableConfiguration runtimeConfiguration) {
                    if (runtimeConfiguration.isGeneratedName() && !changed[0]) {
                        try {
                            final LocatableConfiguration snapshot = (LocatableConfiguration) editor.getSnapshot().getConfiguration();
                            final String generatedName = snapshot.suggestedName();
                            if (generatedName != null && generatedName.length() > 0) {
                                info.setNameText(generatedName);
                                changed[0] = false;
                            }
                        }
                        catch (ConfigurationException ignore) {
                        }
                    }
                }
                setupDialogBounds();
            }
        });

        info.addNameListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                changed[0] = true;
                update();
            }
        });

        info.addSharedListener(e -> {
            changed[0] = true;
            update();
        });
    }

    private void drawPressAddButtonMessage(final ConfigurationType configurationType) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBorder(new EmptyBorder(7, 10, 0, 0));
        panel.add(new JLabel("Press the"));

        ActionLink addIcon = new ActionLink("", IconUtil.getAddIcon(), myAddAction);
        addIcon.setBorder(new EmptyBorder(0, 5, 0, 5));
        panel.add(addIcon);

        final LocalizeValue configurationTypeDescription = configurationType != null
            ? configurationType.getConfigurationTypeDescription()
            : ExecutionLocalize.runConfigurationDefaultTypeDescription();
        panel.add(new JLabel(ExecutionLocalize.emptyRunConfigurationPanelTextLabel3(configurationTypeDescription).get()));
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(panel, true);

        myRightPanel.removeAll();
        myRightPanel.add(scrollPane, BorderLayout.CENTER);
        if (configurationType == null) {
            JPanel settingsPanel = new JPanel(new GridBagLayout());
            settingsPanel.setBorder(new EmptyBorder(7, 10, 0, 0));
            GridBag grid = new GridBag().setDefaultAnchor(GridBagConstraints.NORTHWEST);

            for (Pair<UnnamedConfigurable, JComponent> each : myAdditionalSettings) {
                settingsPanel.add(each.second, grid.nextLine().next());
            }
            settingsPanel.add(createSettingsPanel(), grid.nextLine().next());

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(settingsPanel, BorderLayout.WEST);
            wrapper.add(Box.createGlue(), BorderLayout.CENTER);

            myRightPanel.add(wrapper, BorderLayout.SOUTH);
        }
        myRightPanel.revalidate();
        myRightPanel.repaint();
    }

    private JPanel createLeftPanel() {
        initTree();
        MyRemoveAction removeAction = new MyRemoveAction();
        MyMoveAction moveUpAction = new MyMoveAction(
            ExecutionLocalize.moveUpActionName().get(),
            null,
            IconUtil.getMoveUpIcon(),
            -1
        );
        MyMoveAction moveDownAction = new MyMoveAction(
            ExecutionLocalize.moveDownActionName().get(),
            null,
            IconUtil.getMoveDownIcon(),
            1
        );
        myToolbarDecorator = ToolbarDecorator.createDecorator(myTree)
            .setAddAction(myAddAction)
            .setAddActionName(ExecutionLocalize.addNewRunConfigurationAction2Name().get())
            .setRemoveAction(removeAction)
            .setRemoveActionUpdater(removeAction)
            .setRemoveActionName(ExecutionLocalize.removeRunConfigurationActionName().get())
            .setPanelBorder(JBUI.Borders.empty())
            .setToolbarBackgroundColor((MorphColor.of(UIUtil::getPanelBackground)))
            .setMoveUpAction(moveUpAction)
            .setMoveUpActionName(ExecutionLocalize.moveUpActionName().get())
            .setMoveUpActionUpdater(moveUpAction)
            .setMoveDownAction(moveDownAction)
            .setMoveDownActionName(ExecutionLocalize.moveDownActionName().get())
            .setMoveDownActionUpdater(moveDownAction)
            .addExtraAction(new MyCopyAction())
            .addExtraAction(new MySaveAction())
            .addExtraAction(new MyEditDefaultsAction())
            .addExtraAction(new MyCreateFolderAction())
            .setButtonComparator(
                ExecutionLocalize.addNewRunConfigurationAction2Name().get(),
                ExecutionLocalize.removeRunConfigurationActionName().get(),
                ExecutionLocalize.copyConfigurationActionName().get(),
                ExecutionLocalize.actionNameSaveConfiguration().get(),
                ExecutionLocalize.runConfigurationEditDefaultConfigurationSettingsText().get(),
                ExecutionLocalize.moveUpActionName().get(),
                ExecutionLocalize.moveDownActionName().get(),
                ExecutionLocalize.runConfigurationCreateFolderText().get()
            )
            .setForcedDnD();
        return myToolbarDecorator.createPanel();
    }

    private JPanel createSettingsPanel() {
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBag g = new GridBag();

        bottomPanel.add(myConfirmation, g.nextLine().coverLine());
        bottomPanel.add(new JLabel("Temporary configurations limit:"), g.nextLine().next());
        bottomPanel.add(myRecentsLimit, g.next().anchor(GridBagConstraints.WEST));

        myRecentsLimit.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                setModified(true);
            }
        });
        myConfirmation.addChangeListener(e -> setModified(true));
        return bottomPanel;
    }

    @Nullable
    private ConfigurationType getSelectedConfigurationType() {
        final DefaultMutableTreeNode configurationTypeNode = getSelectedConfigurationTypeNode();
        return configurationTypeNode != null ? (ConfigurationType) configurationTypeNode.getUserObject() : null;
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent(Disposable uiDisposable) {
        for (RunConfigurationsSettings each : RunConfigurationsSettings.EXTENSION_POINT.getExtensionList()) {
            UnnamedConfigurable configurable = each.createConfigurable();
            myAdditionalSettings.add(Pair.create(configurable, configurable.createComponent(uiDisposable)));
        }

        myWholePanel = new JPanel(new BorderLayout());
        DataManager.registerDataProvider(myWholePanel, new DataProvider() {
            @Nullable
            @Override
            public Object getData(@Nonnull Key dataId) {
                return RunConfigurationSelector.KEY == dataId
                    ? (RunConfigurationSelector) configuration -> selectConfiguration(configuration) : null;
            }
        });

        JPanel leftPanel = createLeftPanel();
        myTitlelessDecorator.makeLeftComponentLower(leftPanel);
        mySplitter.setFirstComponent(leftPanel);
        mySplitter.setSecondComponent(myRightPanel);
        myWholePanel.add(mySplitter, BorderLayout.CENTER);

        updateDialog();

        Dimension d = myWholePanel.getPreferredSize();
        d.width = Math.max(d.width, 800);
        d.height = Math.max(d.height, 600);
        myWholePanel.setPreferredSize(d);

        mySplitter.setProportion(myProperties.getFloat(DIVIDER_PROPORTION, 0.3f));

        return myWholePanel;
    }

    public Splitter getSplitter() {
        return mySplitter;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        final RunManagerEx manager = getRunManager();
        final RunManagerConfig config = manager.getConfig();
        myRecentsLimit.setText(Integer.toString(config.getRecentsLimit()));
        myConfirmation.setSelected(config.isRestartRequiresConfirmation());

        for (Pair<UnnamedConfigurable, JComponent> each : myAdditionalSettings) {
            each.first.reset();
        }

        setModified(false);
    }

    public Configurable getSelectedConfigurable() {
        return mySelectedConfigurable;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        updateActiveConfigurationFromSelected();

        final RunManagerImpl manager = getRunManager();
        try {
            manager.fireBeginUpdate();

            final List<ConfigurationType> types = manager.getConfigurationFactories();
            List<ConfigurationType> configurationTypes = new ArrayList<>();
            for (int i = 0; i < myRoot.getChildCount(); i++) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) myRoot.getChildAt(i);
                Object userObject = node.getUserObject();
                if (userObject instanceof ConfigurationType configType) {
                    configurationTypes.add(configType);
                }
            }
            for (ConfigurationType type : types) {
                if (!configurationTypes.contains(type)) {
                    configurationTypes.add(type);
                }
            }

            for (ConfigurationType configurationType : configurationTypes) {
                applyByType(configurationType);
            }

            try {
                int i = Math.max(RunManagerConfig.MIN_RECENT_LIMIT, Integer.parseInt(myRecentsLimit.getText()));
                int oldLimit = manager.getConfig().getRecentsLimit();
                if (oldLimit != i) {
                    manager.getConfig().setRecentsLimit(i);
                    manager.checkRecentsLimit();
                }
            }
            catch (NumberFormatException e) {
                // ignore
            }
            manager.getConfig().setRestartRequiresConfirmation(myConfirmation.isSelected());

            for (Configurable configurable : myStoredComponents.values()) {
                if (configurable.isModified()) {
                    configurable.apply();
                }
            }

            for (Pair<UnnamedConfigurable, JComponent> each : myAdditionalSettings) {
                each.first.apply();
            }

            manager.saveOrder();
            setModified(false);
        }
        finally {
            manager.fireEndUpdate();
        }
        myTree.repaint();
    }

    public void updateActiveConfigurationFromSelected() {
        if (mySelectedConfigurable != null && mySelectedConfigurable instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
            RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings) singleConfigConfigurable.getSettings();

            getRunManager().setSelectedConfiguration(settings);
        }
    }

    private void applyByType(@Nonnull ConfigurationType type) throws ConfigurationException {
        RunnerAndConfigurationSettings selectedSettings = getSelectedSettings();
        int indexToMove = -1;

        DefaultMutableTreeNode typeNode = getConfigurationTypeNode(type);
        final RunManagerImpl manager = getRunManager();
        final ArrayList<RunConfigurationBean> stableConfigurations = new ArrayList<RunConfigurationBean>();
        if (typeNode != null) {
            final Set<String> names = new HashSet<String>();
            List<DefaultMutableTreeNode> configurationNodes = new ArrayList<DefaultMutableTreeNode>();
            collectNodesRecursively(typeNode, configurationNodes, CONFIGURATION, TEMPORARY_CONFIGURATION);
            for (DefaultMutableTreeNode node : configurationNodes) {
                final Object userObject = node.getUserObject();
                RunConfigurationBean configurationBean = null;
                RunnerAndConfigurationSettings settings = null;
                if (userObject instanceof SingleConfigurationConfigurable configurable) {
                    settings = (RunnerAndConfigurationSettings) configurable.getSettings();
                    if (settings.isTemporary()) {
                        applyConfiguration(typeNode, configurable);
                    }
                    configurationBean = new RunConfigurationBean(configurable);
                }
                else if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                    settings = runnerAndConfigurationSettings;
                    configurationBean = new RunConfigurationBean(
                        settings,
                        manager.isConfigurationShared(settings),
                        manager.getBeforeRunTasks(settings.getConfiguration())
                    );
                }
                if (configurationBean != null) {
                    final SingleConfigurationConfigurable configurable = configurationBean.getConfigurable();
                    final String nameText = configurable != null ? configurable.getNameText() : configurationBean.getSettings().getName();
                    if (!names.add(nameText)) {
                        TreeUtil.selectNode(myTree, node);
                        throw new ConfigurationException(type.getDisplayName() + " with name \'" + nameText + "\' already exists");
                    }
                    stableConfigurations.add(configurationBean);
                    if (settings == selectedSettings) {
                        indexToMove = stableConfigurations.size() - 1;
                    }
                }
            }
            List<DefaultMutableTreeNode> folderNodes = new ArrayList<DefaultMutableTreeNode>();
            collectNodesRecursively(typeNode, folderNodes, FOLDER);
            names.clear();
            for (DefaultMutableTreeNode node : folderNodes) {
                String folderName = (String) node.getUserObject();
                if (folderName.isEmpty()) {
                    TreeUtil.selectNode(myTree, node);
                    throw new ConfigurationException("Folder name shouldn't be empty");
                }
                if (!names.add(folderName)) {
                    TreeUtil.selectNode(myTree, node);
                    throw new ConfigurationException("Folders name \'" + folderName + "\' is duplicated");
                }
            }
        }
        // try to apply all
        for (RunConfigurationBean bean : stableConfigurations) {
            final SingleConfigurationConfigurable configurable = bean.getConfigurable();
            if (configurable != null) {
                applyConfiguration(typeNode, configurable);
            }
        }

        // if apply succeeded, update the list of configurations in RunManager
        Set<RunnerAndConfigurationSettings> toDeleteSettings = new HashSet<RunnerAndConfigurationSettings>();
        for (RunConfiguration each : manager.getConfigurationsList(type)) {
            ContainerUtil.addIfNotNull(toDeleteSettings, manager.getSettings(each));
        }

        //Just saved as 'stable' configuration shouldn't stay between temporary ones (here we order model to save)
        int shift = 0;
        if (selectedSettings != null && selectedSettings.getType() == type) {
            shift = adjustOrder();
        }
        if (shift != 0 && indexToMove != -1) {
            stableConfigurations.add(indexToMove - shift, stableConfigurations.remove(indexToMove));
        }
        for (RunConfigurationBean each : stableConfigurations) {
            toDeleteSettings.remove(each.getSettings());
            manager.addConfiguration(each.getSettings(), each.isShared(), each.getStepsBeforeLaunch(), false);
        }

        for (RunnerAndConfigurationSettings each : toDeleteSettings) {
            manager.removeConfiguration(each);
        }
    }

    static void collectNodesRecursively(DefaultMutableTreeNode parentNode, List<DefaultMutableTreeNode> nodes, NodeKind... allowed) {
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            if (ArrayUtilRt.find(allowed, getKind(child)) != -1) {
                nodes.add(child);
            }
            collectNodesRecursively(child, nodes, allowed);
        }
    }

    @Nullable
    private DefaultMutableTreeNode getConfigurationTypeNode(@Nonnull final ConfigurationType type) {
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) myRoot.getChildAt(i);
            if (node.getUserObject() == type) {
                return node;
            }
        }
        return null;
    }

    private void applyConfiguration(DefaultMutableTreeNode typeNode, SingleConfigurationConfigurable<?> configurable) throws ConfigurationException {
        try {
            if (configurable != null) {
                configurable.apply();
                RunManagerImpl.getInstanceImpl(myProject).fireRunConfigurationChanged(configurable.getSettings());
            }
        }
        catch (ConfigurationException e) {
            for (int i = 0; i < typeNode.getChildCount(); i++) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) typeNode.getChildAt(i);
                if (Comparing.equal(configurable, node.getUserObject())) {
                    TreeUtil.selectNode(myTree, node);
                    break;
                }
            }
            throw e;
        }
    }

    @Override
    public boolean isModified() {
        if (super.isModified()) {
            return true;
        }
        final RunManagerImpl runManager = getRunManager();
        final List<RunConfiguration> allConfigurations = runManager.getAllConfigurationsList();
        final List<RunConfiguration> currentConfigurations = new ArrayList<RunConfiguration>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) myRoot.getChildAt(i);
            final Object object = typeNode.getUserObject();
            if (object instanceof ConfigurationType configType) {
                final List<RunnerAndConfigurationSettings> configurationSettings = runManager.getConfigurationSettingsList(configType);
                List<DefaultMutableTreeNode> configurationNodes = new ArrayList<>();
                collectNodesRecursively(typeNode, configurationNodes, CONFIGURATION, TEMPORARY_CONFIGURATION);
                if (configurationSettings.size() != configurationNodes.size()) {
                    return true;
                }
                for (int j = 0; j < configurationNodes.size(); j++) {
                    DefaultMutableTreeNode configurationNode = configurationNodes.get(j);
                    final Object userObject = configurationNode.getUserObject();
                    if (userObject instanceof SingleConfigurationConfigurable configurable) {
                        if (!Comparing.strEqual(configurationSettings.get(j).getConfiguration().getName(), configurable.getConfiguration().getName())) {
                            return true;
                        }
                        if (configurable.isModified()) {
                            return true;
                        }
                        currentConfigurations.add(configurable.getConfiguration());
                    }
                    else if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigurationSettings) {
                        currentConfigurations.add(runnerAndConfigurationSettings.getConfiguration());
                    }
                }
            }
        }
        if (allConfigurations.size() != currentConfigurations.size() || !allConfigurations.containsAll(currentConfigurations)) {
            return true;
        }

        for (Configurable configurable : myStoredComponents.values()) {
            if (configurable.isModified()) {
                return true;
            }
        }

        for (Pair<UnnamedConfigurable, JComponent> each : myAdditionalSettings) {
            if (each.first.isModified()) {
                return true;
            }
        }

        return false;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        isDisposed = true;
        for (Configurable configurable : myStoredComponents.values()) {
            configurable.disposeUIResources();
        }
        myStoredComponents.clear();

        for (Pair<UnnamedConfigurable, JComponent> each : myAdditionalSettings) {
            each.first.disposeUIResources();
        }

        TreeUtil.traverseDepth(myRoot, node -> {
            if (node instanceof DefaultMutableTreeNode treeNode) {
                final Object userObject = treeNode.getUserObject();
                if (userObject instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                    singleConfigConfigurable.disposeUIResources();
                }
            }
            return true;
        });
        myRightPanel.removeAll();
        myProperties.setFloat(DIVIDER_PROPORTION, mySplitter.getProportion());
        mySplitter.dispose();
    }

    private void updateDialog() {
        final Executor executor = myRunDialog != null ? myRunDialog.getExecutor() : null;
        if (executor == null) {
            return;
        }
        final StringBuilder buffer = new StringBuilder();
        buffer.append(executor.getId());
        final SingleConfigurationConfigurable<RunConfiguration> configuration = getSelectedConfiguration();
        if (configuration != null) {
            buffer.append(" - ");
            buffer.append(configuration.getNameText());
        }
        myRunDialog.setOKActionEnabled(canRunConfiguration(configuration, executor));
        myRunDialog.setTitle(buffer.toString());
    }

    public void setWholePanel(JPanel wholePanel) {
        myWholePanel = wholePanel;
    }

    private void setupDialogBounds() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIUtil.setupEnclosingDialogBounds(myWholePanel);
            }
        });
    }

    @Nullable
    private SingleConfigurationConfigurable<RunConfiguration> getSelectedConfiguration() {
        final TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null) {
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            final Object userObject = treeNode.getUserObject();
            if (userObject instanceof SingleConfigurationConfigurable) {
                return (SingleConfigurationConfigurable<RunConfiguration>) userObject;
            }
        }
        return null;
    }

    private static boolean canRunConfiguration(@Nullable SingleConfigurationConfigurable<RunConfiguration> configuration, final @Nonnull Executor executor) {
        try {
            return configuration != null && RunManagerImpl.canRunConfiguration(configuration.getSnapshot(), executor);
        }
        catch (ConfigurationException e) {
            return false;
        }
    }

    RunManagerImpl getRunManager() {
        return RunManagerImpl.getInstanceImpl(myProject);
    }

    @Override
    public String getHelpTopic() {
        final ConfigurationType type = getSelectedConfigurationType();
        if (type != null) {
            return "reference.dialogs.rundebug." + type.getId();
        }
        return "reference.dialogs.rundebug";
    }

    private void clickDefaultButton() {
        if (myRunDialog != null) {
            myRunDialog.clickDefaultButton();
        }
    }

    @Nullable
    private DefaultMutableTreeNode getSelectedConfigurationTypeNode() {
        TreePath selectionPath = myTree.getSelectionPath();
        DefaultMutableTreeNode node = selectionPath != null ? (DefaultMutableTreeNode) selectionPath.getLastPathComponent() : null;
        while (node != null) {
            Object userObject = node.getUserObject();
            if (userObject instanceof ConfigurationType) {
                return node;
            }
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return null;
    }

    @Nonnull
    private DefaultMutableTreeNode getNode(int row) {
        return (DefaultMutableTreeNode) myTree.getPathForRow(row).getLastPathComponent();
    }

    @Nullable
    Trinity<Integer, Integer, RowsDnDSupport.RefinedDropSupport.Position> getAvailableDropPosition(int direction) {
        int[] rows = myTree.getSelectionRows();
        if (rows == null || rows.length != 1) {
            return null;
        }
        int oldIndex = rows[0];
        int newIndex = oldIndex + direction;

        if (!getKind((DefaultMutableTreeNode) myTree.getPathForRow(oldIndex).getLastPathComponent()).supportsDnD()) {
            return null;
        }

        while (newIndex > 0 && newIndex < myTree.getRowCount()) {
            TreePath targetPath = myTree.getPathForRow(newIndex);
            boolean allowInto = getKind((DefaultMutableTreeNode) targetPath.getLastPathComponent()) == FOLDER && !myTree.isExpanded(targetPath);
            RowsDnDSupport.RefinedDropSupport.Position position = allowInto && myTreeModel.isDropInto(myTree, oldIndex, newIndex) ?
                INTO :
                direction > 0 ? BELOW : ABOVE;
            DefaultMutableTreeNode oldNode = getNode(oldIndex);
            DefaultMutableTreeNode newNode = getNode(newIndex);
            if (oldNode.getParent() != newNode.getParent() && getKind(newNode) != FOLDER) {
                RowsDnDSupport.RefinedDropSupport.Position copy = position;
                if (position == BELOW) {
                    copy = ABOVE;
                }
                else if (position == ABOVE) {
                    copy = BELOW;
                }
                if (myTreeModel.canDrop(oldIndex, newIndex, copy)) {
                    return Trinity.create(oldIndex, newIndex, copy);
                }
            }
            if (myTreeModel.canDrop(oldIndex, newIndex, position)) {
                return Trinity.create(oldIndex, newIndex, position);
            }

            if (position == BELOW && newIndex < myTree.getRowCount() - 1 && myTreeModel.canDrop(oldIndex, newIndex + 1, ABOVE)) {
                return Trinity.create(oldIndex, newIndex + 1, ABOVE);
            }
            if (position == ABOVE && newIndex > 1 && myTreeModel.canDrop(oldIndex, newIndex - 1, BELOW)) {
                return Trinity.create(oldIndex, newIndex - 1, BELOW);
            }
            if (position == BELOW && myTreeModel.canDrop(oldIndex, newIndex, ABOVE)) {
                return Trinity.create(oldIndex, newIndex, ABOVE);
            }
            if (position == ABOVE && myTreeModel.canDrop(oldIndex, newIndex, BELOW)) {
                return Trinity.create(oldIndex, newIndex, BELOW);
            }
            newIndex += direction;
        }
        return null;
    }


    @Nonnull
    private static String createUniqueName(DefaultMutableTreeNode typeNode, @Nullable String baseName, NodeKind... kinds) {
        String str = (baseName == null) ? ExecutionLocalize.runConfigurationUnnamedNamePrefix().get() : baseName;
        List<DefaultMutableTreeNode> configurationNodes = new ArrayList<>();
        collectNodesRecursively(typeNode, configurationNodes, kinds);
        final ArrayList<String> currentNames = new ArrayList<>();
        for (DefaultMutableTreeNode node : configurationNodes) {
            final Object userObject = node.getUserObject();
            if (userObject instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                currentNames.add(singleConfigConfigurable.getNameText());
            }
            else if (userObject instanceof RunnerAndConfigurationSettingsImpl runnerAndConfigSettings) {
                currentNames.add(runnerAndConfigSettings.getName());
            }
            else if (userObject instanceof String s) {
                currentNames.add(s);
            }
        }
        return RunManager.suggestUniqueName(str, currentNames);
    }

    private SingleConfigurationConfigurable<RunConfiguration> createNewConfiguration(final RunnerAndConfigurationSettings settings, final DefaultMutableTreeNode node) {
        final SingleConfigurationConfigurable<RunConfiguration> configurationConfigurable =
            SingleConfigurationConfigurable.editSettings(settings, null);
        installUpdateListeners(configurationConfigurable);
        DefaultMutableTreeNode nodeToAdd = new DefaultMutableTreeNode(configurationConfigurable);
        myTreeModel.insertNodeInto(nodeToAdd, node, node.getChildCount());
        TreeUtil.selectNode(myTree, nodeToAdd);
        return configurationConfigurable;
    }

    private void createNewConfiguration(final ConfigurationFactory factory) {
        DefaultMutableTreeNode node = null;
        DefaultMutableTreeNode selectedNode = null;
        TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null) {
            selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        }
        DefaultMutableTreeNode typeNode = getConfigurationTypeNode(factory.getType());
        if (typeNode == null) {
            typeNode = new DefaultMutableTreeNode(factory.getType());
            myRoot.add(typeNode);
            sortTopLevelBranches();
            ((DefaultTreeModel) myTree.getModel()).reload();
        }
        node = typeNode;
        if (selectedNode != null && typeNode.isNodeDescendant(selectedNode)) {
            node = selectedNode;
            if (getKind(node).isConfiguration()) {
                node = (DefaultMutableTreeNode) node.getParent();
            }
        }
        final RunnerAndConfigurationSettings settings = getRunManager().createConfiguration(createUniqueName(typeNode, null, CONFIGURATION, TEMPORARY_CONFIGURATION), factory);
        factory.onNewConfigurationCreated(settings.getConfiguration());
        createNewConfiguration(settings, node);
    }

    private class MyToolbarAddAction extends AnAction implements AnActionButtonRunnable {
        public MyToolbarAddAction() {
            super(
                ExecutionLocalize.addNewRunConfigurationAction2Name(),
                ExecutionLocalize.addNewRunConfigurationAction2Name(),
                ADD_ICON
            );
            registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            showAddPopup(true);
        }

        @Override
        public void run(AnActionButton button) {
            showAddPopup(true);
        }

        private void showAddPopup(final boolean showApplicableTypesOnly) {
            List<ConfigurationType> allTypes = getRunManager().getConfigurationFactories(false);
            final List<ConfigurationType> configurationTypes = ConfigurationTypeSelector.getTypesToShow(myProject, showApplicableTypesOnly, allTypes);
            Collections.sort(configurationTypes, (type1, type2) -> type1.getDisplayName().compareIgnoreCase(type2.getDisplayName()));
            final int hiddenCount = allTypes.size() - configurationTypes.size();
            if (hiddenCount > 0) {
                configurationTypes.add(HIDDEN_ITEMS_STUB);
            }

            final ListPopup popup = JBPopupFactory.getInstance().createListPopup(
                new BaseListPopupStep<ConfigurationType>(ExecutionLocalize.addNewRunConfigurationAction2Name().get(), configurationTypes) {
                    @Override
                    @Nonnull
                    public String getTextFor(final ConfigurationType type) {
                        if (type == HIDDEN_ITEMS_STUB) {
                            return hiddenCount + " items more (irrelevant)...";
                        }
                        return type.getDisplayName().get();
                    }

                    @Override
                    public boolean isSpeedSearchEnabled() {
                        return true;
                    }

                    @Override
                    public boolean canBeHidden(ConfigurationType value) {
                        return true;
                    }

                    @Override
                    public Image getIconFor(final ConfigurationType type) {
                        return type.getIcon();
                    }

                    @Override
                    public PopupStep onChosen(final ConfigurationType type, final boolean finalChoice) {
                        if (hasSubstep(type)) {
                            return getSupStep(type);
                        }
                        if (type == HIDDEN_ITEMS_STUB) {
                            return doFinalStep(() -> showAddPopup(false));
                        }

                        final ConfigurationFactory[] factories = type.getConfigurationFactories();
                        if (factories.length > 0) {
                            createNewConfiguration(factories[0]);
                        }
                        return FINAL_CHOICE;
                    }

                    @Override
                    public int getDefaultOptionIndex() {
                        ConfigurationType type = getSelectedConfigurationType();
                        return type != null ? configurationTypes.indexOf(type) : super.getDefaultOptionIndex();
                    }

                    private ListPopupStep getSupStep(final ConfigurationType type) {
                        final ConfigurationFactory[] factories = type.getConfigurationFactories();
                        Arrays.sort(factories, (factory1, factory2) -> factory1.getDisplayName().compareIgnoreCase(factory2.getDisplayName()));
                        return new BaseListPopupStep<ConfigurationFactory>(
                            ExecutionLocalize.addNewRunConfigurationActionName(type.getDisplayName()).get(),
                            factories
                        ) {
                            @Override
                            @Nonnull
                            public String getTextFor(final ConfigurationFactory value) {
                                return value.getDisplayName().get();
                            }

                            @Override
                            public Image getIconFor(final ConfigurationFactory factory) {
                                return factory.getIcon();
                            }

                            @Override
                            public PopupStep onChosen(final ConfigurationFactory factory, final boolean finalChoice) {
                                createNewConfiguration(factory);
                                return FINAL_CHOICE;
                            }
                        };
                    }

                    @Override
                    public boolean hasSubstep(final ConfigurationType type) {
                        return type.getConfigurationFactories().length > 1;
                    }
                });
            //new TreeSpeedSearch(myTree);
            popup.showUnderneathOf(myToolbarDecorator.getActionsPanel());
        }
    }

    private class MyRemoveAction extends AnAction implements AnActionButtonRunnable, AnActionButtonUpdater {

        public MyRemoveAction() {
            super(
                ExecutionLocalize.removeRunConfigurationActionName(),
                ExecutionLocalize.removeRunConfigurationActionName(),
                REMOVE_ICON
            );
            registerCustomShortcutSet(CommonShortcuts.getDelete(), myTree);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            doRemove();
        }

        @Override
        public void run(AnActionButton button) {
            doRemove();
        }

        private void doRemove() {
            TreePath[] selections = myTree.getSelectionPaths();
            myTree.clearSelection();

            int nodeIndexToSelect = -1;
            DefaultMutableTreeNode parentToSelect = null;

            Set<DefaultMutableTreeNode> changedParents = new HashSet<DefaultMutableTreeNode>();
            boolean wasRootChanged = false;

            for (TreePath each : selections) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) each.getLastPathComponent();
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                NodeKind kind = getKind(node);
                if (!kind.isConfiguration() && kind != FOLDER) {
                    continue;
                }

                if (node.getUserObject() instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                    singleConfigConfigurable.disposeUIResources();
                }

                nodeIndexToSelect = parent.getIndex(node);
                parentToSelect = parent;
                myTreeModel.removeNodeFromParent(node);
                changedParents.add(parent);

                if (kind == FOLDER) {
                    List<DefaultMutableTreeNode> children = new ArrayList<DefaultMutableTreeNode>();
                    for (int i = 0; i < node.getChildCount(); i++) {
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                        Object userObject = getSafeUserObject(child);
                        if (userObject instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                            singleConfigConfigurable.setFolderName(null);
                        }
                        children.add(0, child);
                    }
                    int confIndex = 0;
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        if (getKind((DefaultMutableTreeNode) parent.getChildAt(i)).isConfiguration()) {
                            confIndex = i;
                            break;
                        }
                    }
                    for (DefaultMutableTreeNode child : children) {
                        if (getKind(child) == CONFIGURATION) {
                            myTreeModel.insertNodeInto(child, parent, confIndex);
                        }
                    }
                    confIndex = parent.getChildCount();
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        if (getKind((DefaultMutableTreeNode) parent.getChildAt(i)) == TEMPORARY_CONFIGURATION) {
                            confIndex = i;
                            break;
                        }
                    }
                    for (DefaultMutableTreeNode child : children) {
                        if (getKind(child) == TEMPORARY_CONFIGURATION) {
                            myTreeModel.insertNodeInto(child, parent, confIndex);
                        }
                    }
                }

                if (parent.getChildCount() == 0 && parent.getUserObject() instanceof ConfigurationType) {
                    changedParents.remove(parent);
                    wasRootChanged = true;

                    nodeIndexToSelect = myRoot.getIndex(parent);
                    nodeIndexToSelect = Math.max(0, nodeIndexToSelect - 1);
                    parentToSelect = myRoot;
                    parent.removeFromParent();
                }
            }

            if (wasRootChanged) {
                ((DefaultTreeModel) myTree.getModel()).reload();
            }
            else {
                for (DefaultMutableTreeNode each : changedParents) {
                    myTreeModel.reload(each);
                    myTree.expandPath(new TreePath(each));
                }
            }

            mySelectedConfigurable = null;
            if (myRoot.getChildCount() == 0) {
                drawPressAddButtonMessage(null);
            }
            else {
                if (parentToSelect.getChildCount() > 0) {
                    TreeNode nodeToSelect = nodeIndexToSelect < parentToSelect.getChildCount()
                        ? parentToSelect.getChildAt(nodeIndexToSelect)
                        : parentToSelect.getChildAt(nodeIndexToSelect - 1);
                    TreeUtil.selectInTree((DefaultMutableTreeNode) nodeToSelect, true, myTree);
                }
            }
        }


        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            boolean enabled = isEnabled(e);
            e.getPresentation().setEnabled(enabled);
        }

        @Override
        public boolean isEnabled(AnActionEvent e) {
            boolean enabled = false;
            TreePath[] selections = myTree.getSelectionPaths();
            if (selections != null) {
                for (TreePath each : selections) {
                    NodeKind kind = getKind((DefaultMutableTreeNode) each.getLastPathComponent());
                    if (kind.isConfiguration() || kind == FOLDER) {
                        enabled = true;
                        break;
                    }
                }
            }
            return enabled;
        }
    }

    private class MyCopyAction extends AnAction {
        public MyCopyAction() {
            super(
                ExecutionLocalize.copyConfigurationActionName(),
                ExecutionLocalize.copyConfigurationActionName(),
                PlatformIconGroup.actionsCopy()
            );

            final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_EDITOR_DUPLICATE);
            registerCustomShortcutSet(action.getShortcutSet(), myTree);
        }


        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            final SingleConfigurationConfigurable<RunConfiguration> configuration = getSelectedConfiguration();
            LOG.assertTrue(configuration != null);
            try {
                final DefaultMutableTreeNode typeNode = getSelectedConfigurationTypeNode();
                final RunnerAndConfigurationSettings settings = configuration.getSnapshot();
                final String copyName = createUniqueName(typeNode, configuration.getNameText(), CONFIGURATION, TEMPORARY_CONFIGURATION);
                settings.setName(copyName);
                final ConfigurationFactory factory = settings.getFactory();
                factory.onConfigurationCopied(settings.getConfiguration());
                final SingleConfigurationConfigurable<RunConfiguration> configurable = createNewConfiguration(settings, typeNode);
                ProjectIdeFocusManager.getInstance(myProject).requestFocus(configurable.getNameTextField(), true);
                configurable.getNameTextField().setSelectionStart(0);
                configurable.getNameTextField().setSelectionEnd(copyName.length());
            }
            catch (ConfigurationException e1) {
                Messages.showErrorDialog(myToolbarDecorator.getActionsPanel(), e1.getMessage(), e1.getTitle());
            }
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            final SingleConfigurationConfigurable<RunConfiguration> configuration = getSelectedConfiguration();
            e.getPresentation().setEnabled(configuration != null && !(configuration.getConfiguration() instanceof UnknownRunConfiguration));
        }
    }

    private class MySaveAction extends AnAction {

        public MySaveAction() {
            super(
                ExecutionLocalize.actionNameSaveConfiguration(),
                LocalizeValue.empty(),
                AllIcons.Actions.Menu_saveall
            );
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(final AnActionEvent e) {
            final SingleConfigurationConfigurable<RunConfiguration> configurationConfigurable = getSelectedConfiguration();
            LOG.assertTrue(configurationConfigurable != null);
            try {
                configurationConfigurable.apply();
            }
            catch (ConfigurationException e1) {
                //do nothing
            }
            final RunnerAndConfigurationSettings originalConfiguration = configurationConfigurable.getSettings();
            if (originalConfiguration.isTemporary()) {
                getRunManager().makeStable(originalConfiguration);
                adjustOrder();
            }
            myTree.repaint();
        }

        @RequiredUIAccess
        @Override
        public void update(final AnActionEvent e) {
            final SingleConfigurationConfigurable<RunConfiguration> configuration = getSelectedConfiguration();
            final Presentation presentation = e.getPresentation();
            final boolean enabled;
            if (configuration == null) {
                enabled = false;
            }
            else {
                RunnerAndConfigurationSettings settings = configuration.getSettings();
                enabled = settings != null && settings.isTemporary();
            }
            presentation.setEnabled(enabled);
            presentation.setVisible(enabled);
        }
    }

    /**
     * Just saved as 'stable' configuration shouldn't stay between temporary ones (here we order nodes in JTree only)
     *
     * @return shift (positive) for move configuration "up" to other stable configurations. Zero means "there is nothing to change"
     */


    private int adjustOrder() {
        TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath == null) {
            return 0;
        }
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        RunnerAndConfigurationSettings selectedSettings = getSettings(treeNode);
        if (selectedSettings == null || selectedSettings.isTemporary()) {
            return 0;
        }
        MutableTreeNode parent = (MutableTreeNode) treeNode.getParent();
        int initialPosition = parent.getIndex(treeNode);
        int position = initialPosition;
        DefaultMutableTreeNode node = treeNode.getPreviousSibling();
        while (node != null) {
            RunnerAndConfigurationSettings settings = getSettings(node);
            if (settings != null && settings.isTemporary()) {
                position--;
            }
            else {
                break;
            }
            node = node.getPreviousSibling();
        }
        for (int i = 0; i < initialPosition - position; i++) {
            TreeUtil.moveSelectedRow(myTree, -1);
        }
        return initialPosition - position;
    }

    private class MyMoveAction extends AnAction implements AnActionButtonRunnable, AnActionButtonUpdater {
        private final int myDirection;

        protected MyMoveAction(String text, String description, Image icon, int direction) {
            super(text, description, icon);
            myDirection = direction;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(final AnActionEvent e) {
            doMove();
        }

        private void doMove() {
            Trinity<Integer, Integer, RowsDnDSupport.RefinedDropSupport.Position> dropPosition = getAvailableDropPosition(myDirection);
            if (dropPosition != null) {
                myTreeModel.drop(dropPosition.first, dropPosition.second, dropPosition.third);
            }
        }

        @Override
        public void run(AnActionButton button) {
            doMove();
        }

        @RequiredUIAccess
        @Override
        public void update(final AnActionEvent e) {
            e.getPresentation().setEnabled(isEnabled(e));
        }

        @Override
        public boolean isEnabled(AnActionEvent e) {
            return getAvailableDropPosition(myDirection) != null;
        }
    }

    private class MyEditDefaultsAction extends AnAction {
        public MyEditDefaultsAction() {
            super(
                ExecutionLocalize.runConfigurationEditDefaultConfigurationSettingsText(),
                ExecutionLocalize.runConfigurationEditDefaultConfigurationSettingsDescription(),
                AllIcons.General.Settings
            );
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(final AnActionEvent e) {
            TreeNode defaults = TreeUtil.findNodeWithObject(DEFAULTS, myTree.getModel(), myRoot);
            if (defaults != null) {
                final ConfigurationType configurationType = getSelectedConfigurationType();
                if (configurationType != null) {
                    defaults = TreeUtil.findNodeWithObject(configurationType, myTree.getModel(), defaults);
                }
                final DefaultMutableTreeNode defaultsNode = (DefaultMutableTreeNode) defaults;
                if (defaultsNode == null) {
                    return;
                }
                final TreePath path = TreeUtil.getPath(myRoot, defaultsNode);
                myTree.expandPath(path);
                TreeUtil.selectInTree(defaultsNode, true, myTree);
                myTree.scrollPathToVisible(path);
            }
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            boolean isEnabled = TreeUtil.findNodeWithObject(DEFAULTS, myTree.getModel(), myRoot) != null;
            TreePath path = myTree.getSelectionPath();
            if (path != null) {
                Object o = path.getLastPathComponent();
                if (o instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject().equals(DEFAULTS)) {
                    isEnabled = false;
                }
                o = path.getParentPath().getLastPathComponent();
                if (o instanceof DefaultMutableTreeNode treeNode && treeNode.getUserObject().equals(DEFAULTS)) {
                    isEnabled = false;
                }
            }
            e.getPresentation().setEnabled(isEnabled);
        }
    }

    private class MyCreateFolderAction extends AnAction {
        private MyCreateFolderAction() {
            super(
                ExecutionLocalize.runConfigurationCreateFolderText(),
                ExecutionLocalize.runConfigurationCreateFolderDescription(),
                AllIcons.Nodes.Folder
            );
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            final ConfigurationType type = getSelectedConfigurationType();
            if (type == null) {
                return;
            }
            final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
            DefaultMutableTreeNode typeNode = getConfigurationTypeNode(type);
            if (typeNode == null) {
                return;
            }
            String folderName = createUniqueName(typeNode, "New Folder", FOLDER);
            List<DefaultMutableTreeNode> folders = new ArrayList<DefaultMutableTreeNode>();
            collectNodesRecursively(getConfigurationTypeNode(type), folders, FOLDER);
            final DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folderName);
            myTreeModel.insertNodeInto(folderNode, typeNode, folders.size());
            isFolderCreating = true;
            try {
                for (DefaultMutableTreeNode node : selectedNodes) {
                    int folderRow = myTree.getRowForPath(new TreePath(folderNode.getPath()));
                    int rowForPath = myTree.getRowForPath(new TreePath(node.getPath()));
                    if (getKind(node).isConfiguration() && myTreeModel.canDrop(rowForPath, folderRow, INTO)) {
                        myTreeModel.drop(rowForPath, folderRow, INTO);
                    }
                }
                myTree.setSelectionPath(new TreePath(folderNode.getPath()));
            }
            finally {
                isFolderCreating = false;
            }
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            boolean isEnabled = false;
            boolean toMove = false;
            DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
            ConfigurationType selectedType = null;
            for (DefaultMutableTreeNode node : selectedNodes) {
                ConfigurationType type = getType(node);
                if (selectedType == null) {
                    selectedType = type;
                }
                else {
                    if (!Comparing.equal(type, selectedType)) {
                        isEnabled = false;
                        break;
                    }
                }
                NodeKind kind = getKind(node);
                if (kind.isConfiguration() || (kind == CONFIGURATION_TYPE && node.getParent() == myRoot) || kind == FOLDER) {
                    isEnabled = true;
                }
                if (kind.isConfiguration()) {
                    toMove = true;
                }
            }
            e.getPresentation().setTextValue(
                toMove
                    ? ExecutionLocalize.runConfigurationCreateFolderDescriptionMove()
                    : ExecutionLocalize.runConfigurationCreateFolderDescription()
            );
            e.getPresentation().setEnabled(isEnabled);
        }
    }

    @Nullable
    private static ConfigurationType getType(DefaultMutableTreeNode node) {
        while (node != null) {
            if (node.getUserObject() instanceof ConfigurationType configType) {
                return configType;
            }
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return null;
    }

    @Nonnull
    private DefaultMutableTreeNode[] getSelectedNodes() {
        return myTree.getSelectedNodes(DefaultMutableTreeNode.class, null);
    }

    @Nullable
    private DefaultMutableTreeNode getSelectedNode() {
        DefaultMutableTreeNode[] nodes = myTree.getSelectedNodes(DefaultMutableTreeNode.class, null);
        return nodes.length > 1 ? nodes[0] : null;
    }

    @Nullable
    private RunnerAndConfigurationSettings getSelectedSettings() {
        TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        return getSettings((DefaultMutableTreeNode) selectionPath.getLastPathComponent());
    }

    @Nullable
    private static RunnerAndConfigurationSettings getSettings(DefaultMutableTreeNode treeNode) {
        if (treeNode == null) {
            return null;
        }
        RunnerAndConfigurationSettings settings = null;
        if (treeNode.getUserObject() instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
            settings = (RunnerAndConfigurationSettings) singleConfigConfigurable.getSettings();
        }
        if (treeNode.getUserObject() instanceof RunnerAndConfigurationSettings configSettings) {
            settings = configSettings;
        }
        return settings;
    }

    private static class RunConfigurationBean {
        private final RunnerAndConfigurationSettings mySettings;
        private final boolean myShared;
        private final List<BeforeRunTask> myStepsBeforeLaunch;
        private final SingleConfigurationConfigurable myConfigurable;

        public RunConfigurationBean(final RunnerAndConfigurationSettings settings,
                                    final boolean shared,
                                    final List<BeforeRunTask> stepsBeforeLaunch) {
            mySettings = settings;
            myShared = shared;
            myStepsBeforeLaunch = Collections.unmodifiableList(stepsBeforeLaunch);
            myConfigurable = null;
        }

        public RunConfigurationBean(final SingleConfigurationConfigurable configurable) {
            myConfigurable = configurable;
            mySettings = (RunnerAndConfigurationSettings) myConfigurable.getSettings();
            final ConfigurationSettingsEditorWrapper editorWrapper = (ConfigurationSettingsEditorWrapper) myConfigurable.getEditor();
            myShared = configurable.isStoreProjectConfiguration();
            myStepsBeforeLaunch = editorWrapper.getStepsBeforeLaunch();
        }

        public RunnerAndConfigurationSettings getSettings() {
            return mySettings;
        }

        public boolean isShared() {
            return myShared;
        }

        public List<BeforeRunTask> getStepsBeforeLaunch() {
            return myStepsBeforeLaunch;
        }

        public SingleConfigurationConfigurable getConfigurable() {
            return myConfigurable;
        }

        @Override
        public String toString() {
            return String.valueOf(mySettings);
        }
    }

    public interface RunDialogBase {
        void setOKActionEnabled(boolean isEnabled);

        @Nullable
        Executor getExecutor();

        void setTitle(String title);

        void clickDefaultButton();
    }

    enum NodeKind {
        CONFIGURATION_TYPE,
        FOLDER,
        CONFIGURATION,
        TEMPORARY_CONFIGURATION,
        UNKNOWN;

        boolean supportsDnD() {
            return this == FOLDER || this == CONFIGURATION || this == TEMPORARY_CONFIGURATION;
        }

        boolean isConfiguration() {
            return this == CONFIGURATION | this == TEMPORARY_CONFIGURATION;
        }
    }

    @Nonnull
    static NodeKind getKind(@Nullable DefaultMutableTreeNode node) {
        if (node == null) {
            return UNKNOWN;
        }
        Object userObject = node.getUserObject();
        if (userObject instanceof SingleConfigurationConfigurable || userObject instanceof RunnerAndConfigurationSettings) {
            RunnerAndConfigurationSettings settings = getSettings(node);
            if (settings == null) {
                return UNKNOWN;
            }
            return settings.isTemporary() ? TEMPORARY_CONFIGURATION : CONFIGURATION;
        }
        if (userObject instanceof String) {
            return FOLDER;
        }
        if (userObject instanceof ConfigurationType) {
            return CONFIGURATION_TYPE;
        }
        return UNKNOWN;
    }

    class MyTreeModel extends DefaultTreeModel implements EditableModel, RowsDnDSupport.RefinedDropSupport {
        private MyTreeModel(TreeNode root) {
            super(root);
        }

        @Override
        public void addRow() {
        }

        @Override
        public void removeRow(int index) {
        }

        @Override
        public void exchangeRows(int oldIndex, int newIndex) {
            //Do nothing, use drop() instead
        }

        @Override
        public boolean canExchangeRows(int oldIndex, int newIndex) {
            return false;//Legacy, use canDrop() instead
        }

        @Override
        public boolean canDrop(int oldIndex, int newIndex, @Nonnull Position position) {
            if (myTree.getRowCount() <= oldIndex || myTree.getRowCount() <= newIndex || oldIndex < 0 || newIndex < 0) {
                return false;
            }
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) myTree.getPathForRow(oldIndex).getLastPathComponent();
            DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) myTree.getPathForRow(newIndex).getLastPathComponent();
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) oldNode.getParent();
            DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) newNode.getParent();
            NodeKind oldKind = getKind(oldNode);
            NodeKind newKind = getKind(newNode);
            ConfigurationType oldType = getType(oldNode);
            ConfigurationType newType = getType(newNode);
            if (oldParent == newParent) {
                if (oldNode.getPreviousSibling() == newNode && position == BELOW) {
                    return false;
                }
                if (oldNode.getNextSibling() == newNode && position == ABOVE) {
                    return false;
                }
            }
            if (oldType == null) {
                return false;
            }
            if (oldType != newType) {
                DefaultMutableTreeNode typeNode = getConfigurationTypeNode(oldType);
                if (getKind(oldParent) == FOLDER && typeNode != null && typeNode.getNextSibling() == newNode && position == ABOVE) {
                    return true;
                }
                if (getKind(oldParent) == CONFIGURATION_TYPE &&
                    oldKind == FOLDER &&
                    typeNode != null &&
                    typeNode.getNextSibling() == newNode &&
                    position == ABOVE &&
                    oldParent.getLastChild() != oldNode &&
                    getKind((DefaultMutableTreeNode) oldParent.getLastChild()) == FOLDER) {
                    return true;
                }
                return false;
            }
            if (newParent == oldNode || oldParent == newNode) {
                return false;
            }
            if (oldKind == FOLDER && newKind != FOLDER) {
                if (newKind.isConfiguration() &&
                    position == ABOVE &&
                    getKind(newParent) == CONFIGURATION_TYPE &&
                    newIndex > 1 &&
                    getKind((DefaultMutableTreeNode) myTree.getPathForRow(newIndex - 1).getParentPath().getLastPathComponent()) == FOLDER) {
                    return true;
                }
                return false;
            }
            if (!oldKind.supportsDnD() || !newKind.supportsDnD()) {
                return false;
            }
            if (oldKind.isConfiguration() && newKind == FOLDER && position == ABOVE) {
                return false;
            }
            if (oldKind == TEMPORARY_CONFIGURATION && newKind == CONFIGURATION && position == ABOVE) {
                return false;
            }
            if (oldKind == CONFIGURATION && newKind == TEMPORARY_CONFIGURATION && position == BELOW) {
                return false;
            }
            if (oldKind == CONFIGURATION && newKind == TEMPORARY_CONFIGURATION && position == ABOVE) {
                return newNode.getPreviousSibling() == null ||
                    getKind(newNode.getPreviousSibling()) == CONFIGURATION ||
                    getKind(newNode.getPreviousSibling()) == FOLDER;
            }
            if (oldKind == TEMPORARY_CONFIGURATION && newKind == CONFIGURATION && position == BELOW) {
                return newNode.getNextSibling() == null || getKind(newNode.getNextSibling()) == TEMPORARY_CONFIGURATION;
            }
            if (oldParent == newParent) { //Same parent
                if (oldKind.isConfiguration() && newKind.isConfiguration()) {
                    return oldKind == newKind;//both are temporary or saved
                }
                else if (oldKind == FOLDER) {
                    return !myTree.isExpanded(newIndex) || position == ABOVE;
                }
            }
            return true;
        }

        @Override
        public boolean isDropInto(JComponent component, int oldIndex, int newIndex) {
            TreePath oldPath = myTree.getPathForRow(oldIndex);
            TreePath newPath = myTree.getPathForRow(newIndex);
            if (oldPath == null || newPath == null) {
                return false;
            }
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) oldPath.getLastPathComponent();
            DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) newPath.getLastPathComponent();
            return getKind(oldNode).isConfiguration() && getKind(newNode) == FOLDER;
        }

        @Override
        public void drop(int oldIndex, int newIndex, @Nonnull Position position) {
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) myTree.getPathForRow(oldIndex).getLastPathComponent();
            DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) myTree.getPathForRow(newIndex).getLastPathComponent();
            DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) newNode.getParent();
            NodeKind oldKind = getKind(oldNode);
            boolean wasExpanded = myTree.isExpanded(new TreePath(oldNode.getPath()));
            if (isDropInto(myTree, oldIndex, newIndex)) { //Drop in folder
                removeNodeFromParent(oldNode);
                int index = newNode.getChildCount();
                if (oldKind.isConfiguration()) {
                    int middleIndex = newNode.getChildCount();
                    for (int i = 0; i < newNode.getChildCount(); i++) {
                        if (getKind((DefaultMutableTreeNode) newNode.getChildAt(i)) == TEMPORARY_CONFIGURATION) {
                            middleIndex = i;//index of first temporary configuration in target folder
                            break;
                        }
                    }
                    if (position != INTO) {
                        if (oldIndex < newIndex) {
                            index = oldKind == CONFIGURATION ? 0 : middleIndex;
                        }
                        else {
                            index = oldKind == CONFIGURATION ? middleIndex : newNode.getChildCount();
                        }
                    }
                    else {
                        index = oldKind == TEMPORARY_CONFIGURATION ? newNode.getChildCount() : middleIndex;
                    }
                }
                insertNodeInto(oldNode, newNode, index);
                myTree.expandPath(new TreePath(newNode.getPath()));
            }
            else {
                ConfigurationType type = getType(oldNode);
                assert type != null;
                removeNodeFromParent(oldNode);
                int index;
                if (type != getType(newNode)) {
                    DefaultMutableTreeNode typeNode = getConfigurationTypeNode(type);
                    assert typeNode != null;
                    newParent = typeNode;
                    index = newParent.getChildCount();
                }
                else {
                    index = newParent.getIndex(newNode);
                    if (position == BELOW) {
                        index++;
                    }
                }
                insertNodeInto(oldNode, newParent, index);
            }
            TreePath treePath = new TreePath(oldNode.getPath());
            myTree.setSelectionPath(treePath);
            if (wasExpanded) {
                myTree.expandPath(treePath);
            }
        }

        @Override
        public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
            super.insertNodeInto(newChild, parent, index);
            if (!getKind((DefaultMutableTreeNode) newChild).isConfiguration()) {
                return;
            }
            Object userObject = getSafeUserObject((DefaultMutableTreeNode) newChild);
            String newFolderName = getKind((DefaultMutableTreeNode) parent) == FOLDER
                ? (String) ((DefaultMutableTreeNode) parent).getUserObject()
                : null;
            if (userObject instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                singleConfigConfigurable.setFolderName(newFolderName);
            }
        }

        @Override
        public void reload(TreeNode node) {
            super.reload(node);
            Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
            if (userObject instanceof String folderName) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                    Object safeUserObject = getSafeUserObject(child);
                    if (safeUserObject instanceof SingleConfigurationConfigurable singleConfigConfigurable) {
                        singleConfigConfigurable.setFolderName(folderName);
                    }
                }
            }
        }

        @Nullable
        private RunnerAndConfigurationSettings getSettings(@Nonnull DefaultMutableTreeNode treeNode) {
            Object userObject = treeNode.getUserObject();
            if (userObject instanceof SingleConfigurationConfigurable configurable) {
                return (RunnerAndConfigurationSettings) configurable.getSettings();
            }
            else if (userObject instanceof RunnerAndConfigurationSettings runnerAndConfigurationSettings) {
                return runnerAndConfigurationSettings;
            }
            return null;
        }

        @Nullable
        private ConfigurationType getType(@Nullable DefaultMutableTreeNode treeNode) {
            if (treeNode == null) {
                return null;
            }
            Object userObject = treeNode.getUserObject();
            if (userObject instanceof SingleConfigurationConfigurable configurable) {
                return configurable.getConfiguration().getType();
            }
            else if (userObject instanceof RunnerAndConfigurationSettings runnerAndConfigSettings) {
                return runnerAndConfigSettings.getType();
            }
            else if (userObject instanceof ConfigurationType configType) {
                return configType;
            }
            if (treeNode.getParent() instanceof DefaultMutableTreeNode mutableTreeNode) {
                return getType(mutableTreeNode);
            }
            return null;
        }
    }
}
