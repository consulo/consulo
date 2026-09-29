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
package consulo.execution.impl.internal.ui;

import consulo.configurable.BaseConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationsSettings;
import consulo.execution.event.RunManagerListener;
import consulo.execution.event.RunManagerListenerEvent;
import consulo.execution.impl.internal.RunConfigurationSelector;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.configuration.UnknownConfigurationType;
import consulo.execution.impl.internal.configuration.UnknownRunConfiguration;
import consulo.execution.internal.RunManagerConfig;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.DragAndDropTransferHandler.DropPosition;
import consulo.ui.Hyperlink;
import consulo.ui.IntBox;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.execution.impl.internal.ui.RunConfigurableNodeKind.*;

public class RunConfigurable extends BaseConfigurable {
    static final Object ROOT = new Object() {
        @Override
        public String toString() {
            return "Root";
        }
    };

    static final Object DEFAULTS = new Object() {
        @Override
        public String toString() {
            return "Defaults";
        }
    };

    record TemplateType(ConfigurationType type) {
    }

    static final class Folder {
        private final ConfigurationType myType;
        private String myName;

        private Folder(ConfigurationType type, String name) {
            myType = type;
            myName = name;
        }

        ConfigurationType getType() {
            return myType;
        }

        String getName() {
            return myName;
        }

        @Override
        public String toString() {
            return myName;
        }
    }

    private record DropTarget(int oldIndex, int newIndex, DropPosition position) {
    }

    private record ActionState(
        boolean remove,
        boolean copy,
        boolean save,
        boolean editDefaults,
        boolean moveUp,
        boolean moveDown,
        boolean createFolder,
        boolean createFolderMoves
    ) {
        private static final ActionState NONE = new ActionState(false, false, false, false, false, false, false, false);
    }

    private final Project myProject;
    private final RunManagerImpl myRunManager;

    private final Map<Object, List<Object>> myChildren = new IdentityHashMap<>();
    private final Map<Object, Object> myParents = new IdentityHashMap<>();
    private final Map<RunnerAndConfigurationSettings, SingleConfigurationConfigurable<RunConfiguration>> myConfigurables = new IdentityHashMap<>();
    private final Map<ConfigurationFactory, Configurable> myTemplateConfigurables = new HashMap<>();
    private final Map<Object, TreeNode<Object>> myTreeNodes = new IdentityHashMap<>();
    private final Set<Object> myExpandedValues = ConcurrentHashMap.newKeySet();

    private final List<UnnamedConfigurable> myAdditionalSettings = new ArrayList<>();

    private @Nullable Tree<Object> myTree;
    private @Nullable ActionToolbar myToolbar;
    private @Nullable Disposable myUIDisposable;
    private @Nullable DockLayout myRightPanel;
    private @Nullable Component myDefaultsPanel;
    private @Nullable CheckBox myConfirmationBox;
    private @Nullable IntBox myRecentsLimitBox;

    private volatile @Nullable Object mySelectedValue;
    private volatile ActionState myActionState = ActionState.NONE;
    private @Nullable Configurable mySelectedConfigurable;
    private @Nullable Folder myCreatedFolder;

    private boolean myApplying;
    private boolean myDisposed;

    public RunConfigurable(Project project) {
        myProject = project;
        myRunManager = RunManagerImpl.getInstanceImpl(project);
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ExecutionLocalize.runConfigurableDisplayName();
    }

    @RequiredUIAccess
    @Override
    public Component createUIComponent(Disposable parentDisposable) {
        myUIDisposable = parentDisposable;

        Component treePanel = createTreePanel(parentDisposable);

        DockLayout rightPanel = DockLayout.create();
        rightPanel.paddingBuilder().allSet(Space.LARGE).apply();
        myRightPanel = rightPanel;

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(treePanel);
        splitLayout.setSecondComponent(rightPanel);
        splitLayout.setProportion(30);
        splitLayout.putUserData(UiDataProvider.KEY, sink -> sink.set(RunConfigurationSelector.KEY, this::selectConfiguration));

        Future<?> validationFuture = myProject.getUIAccess()
            .getScheduler()
            .scheduleWithFixedDelay(this::revalidateSelected, 500, 500, TimeUnit.MILLISECONDS);
        Disposer.register(parentDisposable, () -> validationFuture.cancel(false));

        myDefaultsPanel = createDefaultsPanel(parentDisposable);

        showPressAddMessage(null);

        selectFromManager(null);

        return splitLayout;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedUIComponent() {
        return myTree;
    }

    @RequiredUIAccess
    private Component createTreePanel(Disposable parentDisposable) {
        buildModel();

        Tree<Object> tree = Tree.create(ROOT, new ConfigurationTreeModel());
        tree.setSpeedSearchConverter(node -> getSpeedSearchText(node.getValue()));
        tree.addSelectListener(event -> onNodeSelected(event.getValue()));
        tree.addExpandListener(event -> onExpansionChanged(event.getValue().getValue(), true));
        tree.addCollapseListener(event -> onExpansionChanged(event.getValue().getValue(), false));
        tree.setTransferHandler(new ConfigurationDragAndDropHandler());
        Disposer.register(parentDisposable, tree.destroyHook());
        myTree = tree;

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        group.add(new AddAction());
        group.add(new RemoveAction());
        group.add(new CopyAction());
        group.add(new SaveAction());
        group.add(new EditDefaultsAction());
        group.add(new MoveAction(ExecutionLocalize.moveUpActionName(), PlatformIconGroup.actionsMoveup(), -1));
        group.add(new MoveAction(ExecutionLocalize.moveDownActionName(), PlatformIconGroup.actionsMovedown(), 1));
        group.add(new CreateFolderAction());

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar("RunConfigurableToolbar", group.build(), ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(tree);
        myToolbar = toolbar;

        myProject.getMessageBus().connect(parentDisposable).subscribe(RunManagerListener.class, new RunManagerListener() {
            @Override
            public void runConfigurationAdded(RunManagerListenerEvent event) {
                onExternalChange(() -> addExternalConfiguration(event.getSettings()));
            }

            @Override
            public void runConfigurationRemoved(RunManagerListenerEvent event) {
                onExternalChange(() -> removeExternalConfiguration(event.getSettings()));
            }
        });

        DockLayout panel = DockLayout.create();
        panel.top(toolbar.getUIComponent());
        panel.center(ScrollableLayout.create(tree));
        return panel;
    }

    @RequiredUIAccess
    private void showConfigurable(Configurable configurable) {
        Disposable uiDisposable = myUIDisposable;
        Component component = uiDisposable == null ? null : configurable.createUIComponent(uiDisposable);
        if (component != null) {
            setRightComponent(component);
        }
    }

    @RequiredUIAccess
    private void showFolderField(Folder folder, boolean creating) {
        TextBox textBox = TextBox.create(folder.getName());
        textBox.addValueListener(event -> renameFolder(folder, StringUtil.notNullize(event.getValue())));

        VerticalLayout panel = VerticalLayout.create();
        panel.add(LabeledBuilder.filled(ExecutionLocalize.runConfigurationFolderNameLabel(), textBox));
        panel.add(Label.create(ExecutionLocalize.runConfigurationRenameFolderDisclaimer()));
        setRightComponent(panel);

        if (creating) {
            textBox.selectAll();
            textBox.focus();
        }
    }

    @RequiredUIAccess
    private void showPressAddMessage(@Nullable ConfigurationType configurationType) {
        Component defaultsPanel = myDefaultsPanel;
        if (configurationType == null && defaultsPanel != null) {
            setRightComponent(defaultsPanel);
        }
        else {
            setRightComponent(createPressAddMessage(configurationType));
        }
    }

    @RequiredUIAccess
    private void setRightComponent(Component component) {
        DockLayout rightPanel = myRightPanel;
        if (rightPanel != null) {
            rightPanel.center(component);
        }
    }

    @RequiredUIAccess
    private DockLayout createPressAddMessage(@Nullable ConfigurationType configurationType) {
        Hyperlink addLink = Hyperlink.create(LocalizeValue.empty(), event -> showAddPopup(true, popup -> popup.showBy(event)));
        addLink.setIcon(PlatformIconGroup.generalAdd());

        LocalizeValue typeDescription = configurationType != null
            ? configurationType.getConfigurationTypeDescription()
            : ExecutionLocalize.runConfigurationDefaultTypeDescription();

        HorizontalLayout message = HorizontalLayout.create(Space.SMALL);
        message.add(Label.create(ExecutionLocalize.emptyRunConfigurationPanelTextLabel1()));
        message.add(addLink);
        message.add(Label.create(ExecutionLocalize.emptyRunConfigurationPanelTextLabel3(typeDescription)));

        DockLayout panel = DockLayout.create();
        panel.top(message);
        return panel;
    }

    @RequiredUIAccess
    private Component createDefaultsPanel(Disposable parentDisposable) {
        VerticalLayout settings = VerticalLayout.create();

        myProject.getApplication().getExtensionPoint(RunConfigurationsSettings.class).forEach(each -> {
            UnnamedConfigurable configurable = each.createConfigurable();
            Component component = configurable.createUIComponent(parentDisposable);
            if (component != null) {
                myAdditionalSettings.add(configurable);
                settings.add(component);
            }
        });

        RunManagerConfig config = myRunManager.getConfig();

        CheckBox confirmationBox = CheckBox.create(ExecutionLocalize.rerunConfirmationCheckbox());
        confirmationBox.setValue(config.isRestartRequiresConfirmation(), false);
        settings.add(confirmationBox);
        myConfirmationBox = confirmationBox;

        IntBox recentsLimitBox = IntBox.create(config.getRecentsLimit());
        recentsLimitBox.setRange(RunManagerConfig.MIN_RECENT_LIMIT, Integer.MAX_VALUE);
        settings.add(LabeledBuilder.sided(ExecutionLocalize.runConfigurationTemporaryLimitLabel(), recentsLimitBox));
        myRecentsLimitBox = recentsLimitBox;

        DockLayout panel = createPressAddMessage(null);
        panel.bottom(settings);
        return panel;
    }

    @RequiredUIAccess
    private void revalidateSelected() {
        SingleConfigurationConfigurable<RunConfiguration> configurable = getSelectedConfiguration();
        if (configurable != null && !myDisposed) {
            configurable.revalidateIfEdited();
        }
    }

    @RequiredUIAccess
    private void resetGeneralSettings() {
        RunManagerConfig config = myRunManager.getConfig();

        CheckBox confirmationBox = myConfirmationBox;
        if (confirmationBox != null) {
            confirmationBox.setValue(config.isRestartRequiresConfirmation(), false);
        }

        IntBox recentsLimitBox = myRecentsLimitBox;
        if (recentsLimitBox != null) {
            recentsLimitBox.setValue(config.getRecentsLimit(), false);
        }

        for (UnnamedConfigurable each : myAdditionalSettings) {
            each.reset();
        }
    }

    @RequiredUIAccess
    private boolean isGeneralSettingsModified() {
        RunManagerConfig config = myRunManager.getConfig();

        CheckBox confirmationBox = myConfirmationBox;
        if (confirmationBox != null && Boolean.TRUE.equals(confirmationBox.getValue()) != config.isRestartRequiresConfirmation()) {
            return true;
        }

        IntBox recentsLimitBox = myRecentsLimitBox;
        Integer recentsLimit = recentsLimitBox == null ? null : recentsLimitBox.getValue();
        if (recentsLimit != null && recentsLimit != config.getRecentsLimit()) {
            return true;
        }

        for (UnnamedConfigurable each : myAdditionalSettings) {
            if (each.isModified()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void applyGeneralSettings(RunManagerImpl manager) throws ConfigurationException {
        IntBox recentsLimitBox = myRecentsLimitBox;
        Integer recentsLimit = recentsLimitBox == null ? null : recentsLimitBox.getValue();
        if (recentsLimit != null) {
            applyRecentsLimit(manager, recentsLimit);
        }

        CheckBox confirmationBox = myConfirmationBox;
        if (confirmationBox != null) {
            manager.getConfig().setRestartRequiresConfirmation(Boolean.TRUE.equals(confirmationBox.getValue()));
        }

        for (UnnamedConfigurable each : myAdditionalSettings) {
            each.apply();
        }
    }

    private class ConfigurationTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            for (Object child : new ArrayList<>(getChildren(parentValue == null ? ROOT : parentValue))) {
                TreeNode<Object> node = nodeFactory.apply(child);
                node.setLeaf(getChildren(child).isEmpty());
                node.setRenderer(RunConfigurable.this::renderValue);
                myTreeNodes.put(child, node);
            }
        }
    }

    private void buildModel() {
        myChildren.clear();
        myParents.clear();

        List<ConfigurationType> types = myRunManager.getConfigurationFactories();
        for (ConfigurationType type : types) {
            List<RunnerAndConfigurationSettings> configurations = myRunManager.getConfigurationSettingsList(type);
            if (configurations.isEmpty()) {
                continue;
            }

            add(ROOT, type);
            Map<String, Folder> folders = new HashMap<>();
            int folderCounter = 0;
            for (RunnerAndConfigurationSettings configuration : configurations) {
                String folderName = configuration.getFolderName();
                if (folderName != null) {
                    Folder folder = folders.get(folderName);
                    if (folder == null) {
                        folder = new Folder(type, folderName);
                        insert(type, folder, folderCounter++);
                        folders.put(folderName, folder);
                    }
                    add(folder, configuration);
                }
                else {
                    add(type, configuration);
                }
            }
        }

        for (ConfigurationType type : types) {
            if (!(type instanceof UnknownConfigurationType)) {
                TemplateType templateType = new TemplateType(type);
                add(DEFAULTS, templateType);
                ConfigurationFactory[] factories = type.getConfigurationFactories();
                if (factories.length != 1) {
                    for (ConfigurationFactory factory : factories) {
                        add(templateType, factory);
                    }
                }
            }
        }
        if (!getChildren(DEFAULTS).isEmpty()) {
            add(ROOT, DEFAULTS);
        }

        sortTypeNodes();
    }

    private void sortTypeNodes() {
        for (Object parent : new Object[]{ROOT, DEFAULTS}) {
            List<Object> children = myChildren.get(parent);
            if (children != null) {
                children.sort(RunConfigurable::compareTypeValues);
            }
        }
    }

    private static int compareTypeValues(Object o1, Object o2) {
        ConfigurationType type1 = asType(o1);
        ConfigurationType type2 = asType(o2);
        if (type1 != null && type2 != null) {
            return type1.getDisplayName().compareTo(type2.getDisplayName());
        }
        else if (o1 == DEFAULTS && type2 != null) {
            return 1;
        }
        else if (o2 == DEFAULTS && type1 != null) {
            return -1;
        }
        return 0;
    }

    private static @Nullable ConfigurationType asType(@Nullable Object value) {
        if (value instanceof ConfigurationType type) {
            return type;
        }
        if (value instanceof TemplateType templateType) {
            return templateType.type();
        }
        return null;
    }

    private static void applyRecentsLimit(RunManagerImpl manager, int recentsLimit) {
        int limit = Math.max(RunManagerConfig.MIN_RECENT_LIMIT, recentsLimit);
        if (manager.getConfig().getRecentsLimit() != limit) {
            manager.getConfig().setRecentsLimit(limit);
            manager.checkRecentsLimit();
        }
    }

    private @Nullable Object getParent(Object value) {
        return myParents.get(value);
    }

    private List<Object> getChildren(Object parent) {
        List<Object> children = myChildren.get(parent);
        return children == null ? List.of() : children;
    }

    private int getIndex(Object parent, Object child) {
        return indexOf(getChildren(parent), child);
    }

    private static int indexOf(List<Object> values, Object value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == value) {
                return i;
            }
        }
        return -1;
    }

    private void insert(Object parent, Object child, int index) {
        detach(child);
        myChildren.computeIfAbsent(parent, it -> new ArrayList<>()).add(index, child);
        myParents.put(child, parent);
    }

    private void add(Object parent, Object child) {
        insert(parent, child, getParent(child) == parent ? getChildren(parent).size() - 1 : getChildren(parent).size());
    }

    private void detach(Object child) {
        Object parent = myParents.remove(child);
        List<Object> siblings = parent == null ? null : myChildren.get(parent);
        if (siblings != null) {
            int index = indexOf(siblings, child);
            if (index >= 0) {
                siblings.remove(index);
            }
        }
    }

    private void removeValue(Object value) {
        detach(value);
        myChildren.remove(value);
        myExpandedValues.remove(value);
    }

    private @Nullable Object getSibling(Object value, int offset) {
        Object parent = getParent(value);
        if (parent == null) {
            return null;
        }
        List<Object> siblings = getChildren(parent);
        int index = indexOf(siblings, value) + offset;
        return index >= 0 && index < siblings.size() ? siblings.get(index) : null;
    }

    private boolean isAncestor(Object ancestor, @Nullable Object value) {
        for (Object current = value; current != null; current = getParent(current)) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    private boolean isAttached(Object value) {
        return isAncestor(ROOT, value);
    }

    private boolean isRootType(ConfigurationType type) {
        return getParent(type) == ROOT;
    }

    private List<Object> collect(Object parent, RunConfigurableNodeKind... allowed) {
        List<Object> result = new ArrayList<>();
        collect(parent, result, allowed);
        return result;
    }

    private void collect(Object parent, List<Object> result, RunConfigurableNodeKind... allowed) {
        for (Object child : getChildren(parent)) {
            if (ArrayUtil.find(allowed, getKind(child)) != -1) {
                result.add(child);
            }
            collect(child, result, allowed);
        }
    }

    private RunConfigurableNodeKind getKind(@Nullable Object value) {
        if (value instanceof RunnerAndConfigurationSettings settings) {
            return settings.isTemporary() ? TEMPORARY_CONFIGURATION : CONFIGURATION;
        }
        if (value instanceof Folder) {
            return FOLDER;
        }
        if (value instanceof ConfigurationType || value instanceof TemplateType) {
            return CONFIGURATION_TYPE;
        }
        return UNKNOWN;
    }

    private @Nullable ConfigurationType getType(@Nullable Object value) {
        for (Object current = value; current != null; current = getParent(current)) {
            ConfigurationType type = asType(current);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    private String createUniqueName(Object parent, @Nullable String baseName, RunConfigurableNodeKind... kinds) {
        String name = baseName == null ? ExecutionLocalize.runConfigurationUnnamedNamePrefix().get() : baseName;
        List<String> currentNames = new ArrayList<>();
        for (Object value : collect(parent, kinds)) {
            if (value instanceof RunnerAndConfigurationSettings settings) {
                SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
                currentNames.add(configurable != null ? configurable.getNameText() : settings.getName());
            }
            else if (value instanceof Folder folder) {
                currentNames.add(folder.getName());
            }
        }
        return RunManager.suggestUniqueName(name, currentNames);
    }

    @RequiredUIAccess
    private SingleConfigurationConfigurable<RunConfiguration> getConfigurable(RunnerAndConfigurationSettings settings) {
        SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
        if (configurable == null) {
            configurable = SingleConfigurationConfigurable.editSettings(settings, null);
            myConfigurables.put(settings, configurable);
            configurable.addPresentationListener(() -> refreshPresentation(settings));
        }
        return configurable;
    }

    @RequiredUIAccess
    private void insertInto(Object child, Object parent, int index) {
        insert(parent, child, index);
        if (child instanceof RunnerAndConfigurationSettings settings) {
            getConfigurable(settings).setFolderName(parent instanceof Folder folder ? folder.getName() : null);
        }
    }

    @RequiredUIAccess
    private void renameFolder(Folder folder, String name) {
        folder.myName = name;
        for (Object child : new ArrayList<>(getChildren(folder))) {
            if (child instanceof RunnerAndConfigurationSettings settings) {
                getConfigurable(settings).setFolderName(name);
            }
        }
        refreshPresentation(folder);
    }

    private void renderValue(Object value, TextItemPresentation presentation) {
        if (value == DEFAULTS) {
            presentation.withIcon(PlatformIconGroup.generalSettings());
            presentation.append(ExecutionLocalize.runConfigurationTemplatesNodeName(), TextAttribute.REGULAR_BOLD);
            return;
        }

        if (value instanceof ConfigurationType type) {
            presentation.withIcon(type.getIcon());
            presentation.append(type.getDisplayName(), TextAttribute.REGULAR_BOLD);
            return;
        }

        if (value instanceof TemplateType templateType) {
            presentation.withIcon(templateType.type().getIcon());
            presentation.append(templateType.type().getDisplayName());
            return;
        }

        if (value instanceof ConfigurationFactory factory) {
            presentation.withIcon(factory.getIcon());
            presentation.append(factory.getDisplayName());
            return;
        }

        if (value instanceof Folder folder) {
            presentation.withIcon(PlatformIconGroup.nodesFolder());
            presentation.append(LocalizeValue.of(folder.getName()));
            return;
        }

        if (value instanceof RunnerAndConfigurationSettings settings) {
            SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
            String name;
            boolean shared;
            Image icon;
            if (configurable != null) {
                name = configurable.getNameText();
                shared = configurable.isStoreProjectConfiguration();
                icon = ProgramRunnerUtil.getConfigurationIcon(settings, configurable.hasValidationError());
            }
            else {
                name = settings.getName();
                shared = myRunManager.isConfigurationShared(settings);
                icon = myRunManager.getConfigurationIcon(settings);
            }

            presentation.withIcon(shared ? ImageEffects.layered(icon, PlatformIconGroup.nodesShared()) : icon);
            presentation.append(LocalizeValue.of(name), settings.isTemporary() ? TextAttribute.GRAYED : TextAttribute.REGULAR);
        }
    }

    private String getSpeedSearchText(@Nullable Object value) {
        if (value instanceof RunnerAndConfigurationSettings settings) {
            SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
            return configurable != null ? configurable.getNameText() : settings.getName();
        }
        if (value == DEFAULTS) {
            return ExecutionLocalize.runConfigurationTemplatesNodeName().get();
        }
        ConfigurationType type = asType(value);
        if (type != null) {
            return type.getDisplayName().get();
        }
        if (value instanceof ConfigurationFactory factory) {
            return factory.getDisplayName().get();
        }
        return String.valueOf(value);
    }

    @RequiredUIAccess
    private void onNodeSelected(@Nullable TreeNode<Object> treeNode) {
        Object value = treeNode == null ? null : treeNode.getValue();
        mySelectedValue = value;
        mySelectedConfigurable = null;

        if (value instanceof RunnerAndConfigurationSettings settings) {
            SingleConfigurationConfigurable<RunConfiguration> configurable = getConfigurable(settings);
            mySelectedConfigurable = configurable;
            showConfigurable(configurable);
        }
        else if (value instanceof Folder folder) {
            boolean creating = myCreatedFolder == folder;
            myCreatedFolder = null;
            showFolderField(folder, creating);
        }
        else if (value == DEFAULTS) {
            showPressAddMessage(null);
        }
        else if (value instanceof ConfigurationType type) {
            showPressAddMessage(type);
        }
        else if (value instanceof TemplateType templateType) {
            ConfigurationFactory[] factories = templateType.type().getConfigurationFactories();
            if (factories.length == 1) {
                showTemplateConfigurable(factories[0]);
            }
            else {
                showPressAddMessage(templateType.type());
            }
        }
        else if (value instanceof ConfigurationFactory factory) {
            showTemplateConfigurable(factory);
        }

        updateActions();
    }

    @RequiredUIAccess
    private void showTemplateConfigurable(ConfigurationFactory factory) {
        Configurable configurable = myTemplateConfigurables.get(factory);
        if (configurable == null) {
            configurable = new TemplateConfigurable(myRunManager.getConfigurationTemplate(factory));
            myTemplateConfigurables.put(factory, configurable);
            configurable.reset();
        }
        mySelectedConfigurable = configurable;
        showConfigurable(configurable);
    }

    @RequiredUIAccess
    private void onExpansionChanged(@Nullable Object value, boolean expanded) {
        if (value == null) {
            return;
        }

        if (expanded) {
            myExpandedValues.add(value);
        }
        else {
            myExpandedValues.remove(value);
        }
        updateActions();
    }

    @RequiredUIAccess
    private void updateActions() {
        if (myDisposed) {
            return;
        }

        myActionState = computeActionState();

        ActionToolbar toolbar = myToolbar;
        if (toolbar != null) {
            toolbar.updateActionsAsync();
        }
    }

    @RequiredUIAccess
    private ActionState computeActionState() {
        Object selected = mySelectedValue;
        RunConfigurableNodeKind kind = getKind(selected);

        Set<Object> expanded = new HashSet<>(myExpandedValues);
        List<Object> rows = collectVisibleRows(expanded);

        return new ActionState(
            kind.isConfiguration() || kind == FOLDER,
            selected instanceof RunnerAndConfigurationSettings settings && !(settings.getConfiguration() instanceof UnknownRunConfiguration),
            selected instanceof RunnerAndConfigurationSettings settings && settings.isTemporary(),
            canEditTemplate(selected),
            getAvailableDropPosition(rows, expanded, selected, -1) != null,
            getAvailableDropPosition(rows, expanded, selected, 1) != null,
            kind.isConfiguration() || kind == FOLDER || kind == CONFIGURATION_TYPE && selected != null && getParent(selected) == ROOT,
            kind.isConfiguration()
        );
    }

    @RequiredUIAccess
    private void refreshPresentation(Object value) {
        Tree<Object> tree = myTree;
        TreeNode<Object> treeNode = myTreeNodes.get(value);
        if (tree != null && treeNode != null && !myDisposed) {
            tree.refreshItem(treeNode);
        }
    }

    @RequiredUIAccess
    private CompletableFuture<?> refreshTree(@Nullable Object valueToSelect) {
        Tree<Object> tree = myTree;
        if (tree == null || myDisposed) {
            return CompletableFuture.completedFuture(null);
        }

        List<Object> expanded = new ArrayList<>();
        for (Object value : myExpandedValues) {
            if (isAttached(value)) {
                expanded.add(value);
            }
            else {
                myExpandedValues.remove(value);
            }
        }

        myTreeNodes.clear();
        updateActions();

        UIAccess uiAccess = myProject.getUIAccess();
        CompletableFuture<Object> result = new CompletableFuture<>();
        tree.refreshAll().whenComplete((ignored, error) -> uiAccess.give(() -> {
            List<CompletableFuture<?>> reveals = new ArrayList<>();
            for (Object value : expanded) {
                reveals.add(revealValue(value, false));
            }
            if (valueToSelect != null && isAttached(valueToSelect)) {
                reveals.add(revealValue(valueToSelect, true));
            }
            CompletableFuture.allOf(reveals.toArray(new CompletableFuture<?>[0])).whenComplete((done, revealError) -> result.complete(null));
        }));
        return result;
    }

    @RequiredUIAccess
    private CompletableFuture<?> revealValue(Object value, boolean select) {
        Tree<Object> tree = myTree;
        TreeNode<Object> rootNode = tree == null ? null : tree.getRootNode();
        if (rootNode == null || myDisposed) {
            return CompletableFuture.completedFuture(null);
        }

        List<Object> path = new ArrayList<>();
        for (Object current = value; current != null && current != ROOT; current = getParent(current)) {
            path.add(0, current);
        }

        CompletableFuture<Object> result = new CompletableFuture<>();
        if (path.isEmpty()) {
            result.complete(null);
        }
        else {
            revealPath(rootNode, path, 0, select, result);
        }
        return result;
    }

    private void revealPath(TreeNode<Object> parent, List<Object> path, int index, boolean select, CompletableFuture<Object> result) {
        Object expected = path.get(index);
        UIAccess uiAccess = myProject.getUIAccess();

        parent.findChild(value -> value == expected).whenComplete((child, error) -> uiAccess.give(() -> {
            Tree<Object> tree = myTree;
            if (child == null || tree == null || myDisposed) {
                result.complete(null);
                return;
            }

            if (index == path.size() - 1) {
                if (select) {
                    tree.select(child);
                    result.complete(null);
                }
                else {
                    tree.expand(child).whenComplete((ignored, expandError) -> result.complete(null));
                }
                return;
            }

            tree.expand(child).whenComplete((ignored, expandError) -> uiAccess.give(() -> revealPath(child, path, index + 1, select, result)));
        }));
    }

    @RequiredUIAccess
    public void selectFromManager(@Nullable RunConfiguration selected) {
        RunConfiguration configuration = selected;
        if (configuration == null) {
            RunnerAndConfigurationSettings settings = myRunManager.getSelectedConfiguration();
            configuration = settings == null ? null : settings.getConfiguration();
        }

        if (configuration == null) {
            mySelectedConfigurable = null;
            return;
        }

        selectConfiguration(configuration);
    }

    @RequiredUIAccess
    private void selectConfiguration(RunConfiguration configuration) {
        for (Object value : collect(ROOT, CONFIGURATION, TEMPORARY_CONFIGURATION)) {
            RunConfiguration nodeConfiguration = ((RunnerAndConfigurationSettings) value).getConfiguration();
            if (nodeConfiguration == configuration
                || Comparing.strEqual(nodeConfiguration.getType().getId(), configuration.getType().getId())
                && Comparing.strEqual(nodeConfiguration.getName(), configuration.getName())) {
                revealValue(value, true);
                return;
            }
        }
    }

    @RequiredUIAccess
    private void showAddPopup(boolean showApplicableTypesOnly, Consumer<ListPopup> show) {
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(RunConfigurationTypePopupStep.create(
            myProject,
            myRunManager,
            showApplicableTypesOnly,
            getSelectedConfigurationType(),
            this::createNewConfiguration,
            () -> showAddPopup(false, show)
        ));
        show.accept(popup);
    }

    private @Nullable ConfigurationType getSelectedConfigurationType() {
        return getType(mySelectedValue);
    }

    private @Nullable SingleConfigurationConfigurable<RunConfiguration> getSelectedConfiguration() {
        return mySelectedValue instanceof RunnerAndConfigurationSettings settings ? myConfigurables.get(settings) : null;
    }

    private @Nullable RunnerAndConfigurationSettings getSelectedSettings() {
        return mySelectedValue instanceof RunnerAndConfigurationSettings settings ? settings : null;
    }

    public @Nullable Configurable getSelectedConfigurable() {
        return mySelectedConfigurable;
    }

    public void updateActiveConfigurationFromSelected() {
        if (mySelectedConfigurable instanceof SingleConfigurationConfigurable<?> configurable) {
            myRunManager.setSelectedConfiguration(configurable.getSettings());
        }
    }

    @RequiredUIAccess
    private void createNewConfiguration(ConfigurationFactory factory) {
        Object selected = mySelectedValue;
        ConfigurationType type = factory.getType();
        if (!isRootType(type)) {
            add(ROOT, type);
            sortTypeNodes();
        }

        Object parent = type;
        if (selected != null && isAncestor(type, selected)) {
            parent = selected;
            if (getKind(parent).isConfiguration()) {
                Object selectedParent = getParent(parent);
                parent = selectedParent == null ? type : selectedParent;
            }
        }

        RunnerAndConfigurationSettings settings =
            myRunManager.createConfiguration(createUniqueName(type, null, CONFIGURATION, TEMPORARY_CONFIGURATION), factory);
        factory.onNewConfigurationCreated(settings.getConfiguration());

        addConfiguration(settings, parent);
        refreshTree(settings);
    }

    @RequiredUIAccess
    private SingleConfigurationConfigurable<RunConfiguration> addConfiguration(RunnerAndConfigurationSettings settings, Object parent) {
        SingleConfigurationConfigurable<RunConfiguration> configurable = getConfigurable(settings);
        insertInto(settings, parent, getChildren(parent).size());
        return configurable;
    }

    @RequiredUIAccess
    private void removeSelected() {
        Object value = mySelectedValue;
        Object parent = value == null ? null : getParent(value);
        RunConfigurableNodeKind kind = getKind(value);
        if (value == null || parent == null || !kind.isConfiguration() && kind != FOLDER) {
            return;
        }

        if (value instanceof RunnerAndConfigurationSettings settings) {
            SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.remove(settings);
            if (configurable != null) {
                configurable.disposeUIResources();
            }
        }

        int indexToSelect = getIndex(parent, value);
        Object parentToSelect = parent;
        List<Object> children = new ArrayList<>(getChildren(value));
        removeValue(value);

        if (kind == FOLDER) {
            List<Object> reversed = new ArrayList<>();
            for (Object child : children) {
                if (child instanceof RunnerAndConfigurationSettings settings) {
                    getConfigurable(settings).setFolderName(null);
                }
                reversed.add(0, child);
            }

            int configurationIndex = 0;
            for (int i = 0; i < getChildren(parent).size(); i++) {
                if (getKind(getChildren(parent).get(i)).isConfiguration()) {
                    configurationIndex = i;
                    break;
                }
            }
            for (Object child : reversed) {
                if (getKind(child) == CONFIGURATION) {
                    insertInto(child, parent, configurationIndex);
                }
            }

            configurationIndex = getChildren(parent).size();
            for (int i = 0; i < getChildren(parent).size(); i++) {
                if (getKind(getChildren(parent).get(i)) == TEMPORARY_CONFIGURATION) {
                    configurationIndex = i;
                    break;
                }
            }
            for (Object child : reversed) {
                if (getKind(child) == TEMPORARY_CONFIGURATION) {
                    insertInto(child, parent, configurationIndex);
                }
            }
        }

        if (getChildren(parent).isEmpty() && parent instanceof ConfigurationType) {
            indexToSelect = Math.max(0, getIndex(ROOT, parent) - 1);
            parentToSelect = ROOT;
            removeValue(parent);
        }

        mySelectedValue = null;
        mySelectedConfigurable = null;

        List<Object> candidates = getChildren(parentToSelect);
        Object valueToSelect = null;
        if (!candidates.isEmpty()) {
            valueToSelect = indexToSelect < candidates.size() ? candidates.get(indexToSelect) : candidates.get(indexToSelect - 1);
        }

        if (valueToSelect == null) {
            showPressAddMessage(null);
        }

        refreshTree(valueToSelect);
    }

    @RequiredUIAccess
    private void copySelected() {
        RunnerAndConfigurationSettings selectedSettings = getSelectedSettings();
        ConfigurationType type = getType(selectedSettings);
        if (selectedSettings == null || type == null) {
            return;
        }

        SingleConfigurationConfigurable<RunConfiguration> configurable = getConfigurable(selectedSettings);
        try {
            RunnerAndConfigurationSettings settings = configurable.getSnapshot();
            if (settings == null) {
                return;
            }

            String copyName = createUniqueName(type, configurable.getNameText(), CONFIGURATION, TEMPORARY_CONFIGURATION);
            settings.setName(copyName);
            ConfigurationFactory factory = settings.getFactory();
            factory.onConfigurationCopied(settings.getConfiguration());

            SingleConfigurationConfigurable<RunConfiguration> copy = addConfiguration(settings, type);

            UIAccess uiAccess = myProject.getUIAccess();
            refreshTree(settings).whenComplete((ignored, error) -> uiAccess.give(() -> {
                if (!myDisposed) {
                    copy.selectNameText();
                }
            }));
        }
        catch (ConfigurationException e) {
            MessageBoxes.okError(LocalizeValue.of(StringUtil.notNullize(e.getMessage())))
                .title(e.getTitle())
                .showAsync(myTree);
        }
    }

    @RequiredUIAccess
    private void saveSelected() {
        RunnerAndConfigurationSettings settings = getSelectedSettings();
        if (settings == null) {
            return;
        }

        SingleConfigurationConfigurable<RunConfiguration> configurable = getConfigurable(settings);
        try {
            configurable.apply();
        }
        catch (ConfigurationException ignored) {
        }

        if (settings.isTemporary()) {
            myApplying = true;
            try {
                myRunManager.makeStable(settings);
            }
            finally {
                myApplying = false;
            }
            adjustOrder();
        }

        refreshTree(settings);
    }

    private int adjustOrder() {
        RunnerAndConfigurationSettings settings = getSelectedSettings();
        Object parent = settings == null ? null : getParent(settings);
        if (settings == null || parent == null || settings.isTemporary()) {
            return 0;
        }

        int initialPosition = getIndex(parent, settings);
        int position = initialPosition;
        Object previous = getSibling(settings, -1);
        while (previous instanceof RunnerAndConfigurationSettings previousSettings && previousSettings.isTemporary()) {
            position--;
            previous = getSibling(previous, -1);
        }

        if (position != initialPosition) {
            insert(parent, settings, position);
        }
        return initialPosition - position;
    }

    private @Nullable Object findTemplateNode(@Nullable ConfigurationType type) {
        if (getParent(DEFAULTS) != ROOT) {
            return null;
        }
        if (type == null) {
            return DEFAULTS;
        }
        for (Object child : getChildren(DEFAULTS)) {
            if (child instanceof TemplateType templateType && templateType.type() == type) {
                return child;
            }
        }
        return null;
    }

    private boolean canEditTemplate(@Nullable Object selected) {
        if (getParent(DEFAULTS) != ROOT) {
            return false;
        }
        if (selected == null) {
            return true;
        }
        return selected != DEFAULTS && getParent(selected) != DEFAULTS;
    }

    private List<Object> collectVisibleRows(Set<Object> expanded) {
        List<Object> rows = new ArrayList<>();
        collectVisibleRows(ROOT, expanded, rows);
        return rows;
    }

    private void collectVisibleRows(Object parent, Set<Object> expanded, List<Object> rows) {
        for (Object child : getChildren(parent)) {
            rows.add(child);
            if (expanded.contains(child)) {
                collectVisibleRows(child, expanded, rows);
            }
        }
    }

    private boolean isDropInto(Object oldValue, Object newValue) {
        return getKind(oldValue).isConfiguration() && getKind(newValue) == FOLDER;
    }

    private boolean canDrop(List<Object> rows, Set<Object> expanded, int oldIndex, int newIndex, DropPosition position) {
        if (rows.size() <= oldIndex || rows.size() <= newIndex || oldIndex < 0 || newIndex < 0) {
            return false;
        }
        Object oldValue = rows.get(oldIndex);
        Object newValue = rows.get(newIndex);
        Object oldParent = getParent(oldValue);
        Object newParent = getParent(newValue);
        RunConfigurableNodeKind oldKind = getKind(oldValue);
        RunConfigurableNodeKind newKind = getKind(newValue);
        ConfigurationType oldType = getType(oldValue);
        ConfigurationType newType = getType(newValue);
        if (oldParent == newParent) {
            if (getSibling(oldValue, -1) == newValue && position == DropPosition.BELOW) {
                return false;
            }
            if (getSibling(oldValue, 1) == newValue && position == DropPosition.ABOVE) {
                return false;
            }
        }
        if (oldType == null || oldParent == null) {
            return false;
        }
        if (oldType != newType) {
            Object typeNode = isRootType(oldType) ? oldType : null;
            if (getKind(oldParent) == FOLDER && typeNode != null && getSibling(typeNode, 1) == newValue && position == DropPosition.ABOVE) {
                return true;
            }
            List<Object> oldSiblings = getChildren(oldParent);
            Object oldLast = oldSiblings.isEmpty() ? null : oldSiblings.get(oldSiblings.size() - 1);
            return getKind(oldParent) == CONFIGURATION_TYPE
                && oldKind == FOLDER
                && typeNode != null
                && getSibling(typeNode, 1) == newValue
                && position == DropPosition.ABOVE
                && oldLast != oldValue
                && getKind(oldLast) == FOLDER;
        }
        if (newParent == oldValue || oldParent == newValue) {
            return false;
        }
        if (oldKind == FOLDER && newKind != FOLDER) {
            return newKind.isConfiguration()
                && position == DropPosition.ABOVE
                && getKind(newParent) == CONFIGURATION_TYPE
                && newIndex > 1
                && getKind(getParent(rows.get(newIndex - 1))) == FOLDER;
        }
        if (!oldKind.supportsDnD() || !newKind.supportsDnD()) {
            return false;
        }
        if (oldKind.isConfiguration() && newKind == FOLDER && position == DropPosition.ABOVE) {
            return false;
        }
        if (oldKind == TEMPORARY_CONFIGURATION && newKind == CONFIGURATION && position == DropPosition.ABOVE) {
            return false;
        }
        if (oldKind == CONFIGURATION && newKind == TEMPORARY_CONFIGURATION && position == DropPosition.BELOW) {
            return false;
        }
        if (oldKind == CONFIGURATION && newKind == TEMPORARY_CONFIGURATION && position == DropPosition.ABOVE) {
            Object previous = getSibling(newValue, -1);
            return previous == null || getKind(previous) == CONFIGURATION || getKind(previous) == FOLDER;
        }
        if (oldKind == TEMPORARY_CONFIGURATION && newKind == CONFIGURATION && position == DropPosition.BELOW) {
            Object next = getSibling(newValue, 1);
            return next == null || getKind(next) == TEMPORARY_CONFIGURATION;
        }
        if (oldParent == newParent) {
            if (oldKind.isConfiguration() && newKind.isConfiguration()) {
                return oldKind == newKind;
            }
            else if (oldKind == FOLDER) {
                return !expanded.contains(newValue) || position == DropPosition.ABOVE;
            }
        }
        return true;
    }

    private @Nullable DropTarget getAvailableDropPosition(List<Object> rows, Set<Object> expanded, @Nullable Object selected, int direction) {
        int oldIndex = selected == null ? -1 : indexOf(rows, selected);
        if (oldIndex < 0 || !getKind(selected).supportsDnD()) {
            return null;
        }

        int newIndex = oldIndex + direction;
        while (newIndex > 0 && newIndex < rows.size()) {
            Object oldValue = rows.get(oldIndex);
            Object newValue = rows.get(newIndex);
            boolean allowInto = getKind(newValue) == FOLDER && !expanded.contains(newValue);
            DropPosition position = allowInto && isDropInto(oldValue, newValue)
                ? DropPosition.INTO
                : direction > 0 ? DropPosition.BELOW : DropPosition.ABOVE;
            if (getParent(oldValue) != getParent(newValue) && getKind(newValue) != FOLDER) {
                DropPosition copy = position;
                if (position == DropPosition.BELOW) {
                    copy = DropPosition.ABOVE;
                }
                else if (position == DropPosition.ABOVE) {
                    copy = DropPosition.BELOW;
                }
                if (canDrop(rows, expanded, oldIndex, newIndex, copy)) {
                    return new DropTarget(oldIndex, newIndex, copy);
                }
            }
            if (canDrop(rows, expanded, oldIndex, newIndex, position)) {
                return new DropTarget(oldIndex, newIndex, position);
            }

            if (position == DropPosition.BELOW && newIndex < rows.size() - 1 && canDrop(rows, expanded, oldIndex, newIndex + 1, DropPosition.ABOVE)) {
                return new DropTarget(oldIndex, newIndex + 1, DropPosition.ABOVE);
            }
            if (position == DropPosition.ABOVE && newIndex > 1 && canDrop(rows, expanded, oldIndex, newIndex - 1, DropPosition.BELOW)) {
                return new DropTarget(oldIndex, newIndex - 1, DropPosition.BELOW);
            }
            if (position == DropPosition.BELOW && canDrop(rows, expanded, oldIndex, newIndex, DropPosition.ABOVE)) {
                return new DropTarget(oldIndex, newIndex, DropPosition.ABOVE);
            }
            if (position == DropPosition.ABOVE && canDrop(rows, expanded, oldIndex, newIndex, DropPosition.BELOW)) {
                return new DropTarget(oldIndex, newIndex, DropPosition.BELOW);
            }
            newIndex += direction;
        }
        return null;
    }

    @RequiredUIAccess
    private void moveSelected(int direction) {
        Set<Object> expanded = new HashSet<>(myExpandedValues);
        List<Object> rows = collectVisibleRows(expanded);
        DropTarget target = getAvailableDropPosition(rows, expanded, mySelectedValue, direction);
        if (target != null) {
            refreshTree(move(rows, expanded, target));
        }
    }

    @RequiredUIAccess
    private Object move(List<Object> rows, Set<Object> expanded, DropTarget target) {
        Object oldValue = rows.get(target.oldIndex());
        Object newValue = rows.get(target.newIndex());
        RunConfigurableNodeKind oldKind = getKind(oldValue);
        boolean wasExpanded = expanded.contains(oldValue);
        if (isDropInto(oldValue, newValue)) {
            detach(oldValue);
            List<Object> folderChildren = getChildren(newValue);
            int index = folderChildren.size();
            if (oldKind.isConfiguration()) {
                int middleIndex = folderChildren.size();
                for (int i = 0; i < folderChildren.size(); i++) {
                    if (getKind(folderChildren.get(i)) == TEMPORARY_CONFIGURATION) {
                        middleIndex = i;
                        break;
                    }
                }
                if (target.position() != DropPosition.INTO) {
                    if (target.oldIndex() < target.newIndex()) {
                        index = oldKind == CONFIGURATION ? 0 : middleIndex;
                    }
                    else {
                        index = oldKind == CONFIGURATION ? middleIndex : folderChildren.size();
                    }
                }
                else {
                    index = oldKind == TEMPORARY_CONFIGURATION ? folderChildren.size() : middleIndex;
                }
            }
            insertInto(oldValue, newValue, index);
            myExpandedValues.add(newValue);
        }
        else {
            ConfigurationType type = getType(oldValue);
            Object newParent = getParent(newValue);
            boolean otherType = type != getType(newValue);
            if (type == null || newParent == null || otherType && !isRootType(type)) {
                return oldValue;
            }

            detach(oldValue);
            int index;
            if (otherType) {
                newParent = type;
                index = getChildren(type).size();
            }
            else {
                index = getIndex(newParent, newValue);
                if (target.position() == DropPosition.BELOW) {
                    index++;
                }
            }
            insertInto(oldValue, newParent, index);
        }

        if (wasExpanded) {
            myExpandedValues.add(oldValue);
        }
        return oldValue;
    }

    @RequiredUIAccess
    private void createFolder() {
        ConfigurationType type = getSelectedConfigurationType();
        if (type == null || !isRootType(type)) {
            return;
        }

        Object selected = mySelectedValue;
        String folderName = createUniqueName(type, ExecutionLocalize.runConfigurationNewFolderName().get(), FOLDER);
        Folder folder = new Folder(type, folderName);
        insertInto(folder, type, collect(type, FOLDER).size());

        if (selected != null && getKind(selected).isConfiguration()) {
            Set<Object> expanded = new HashSet<>(myExpandedValues);
            expanded.add(type);
            List<Object> rows = collectVisibleRows(expanded);
            int selectedRow = indexOf(rows, selected);
            int folderRow = indexOf(rows, folder);
            if (selectedRow >= 0 && folderRow >= 0 && canDrop(rows, expanded, selectedRow, folderRow, DropPosition.INTO)) {
                move(rows, expanded, new DropTarget(selectedRow, folderRow, DropPosition.INTO));
            }
        }

        myCreatedFolder = folder;
        myExpandedValues.add(type);
        refreshTree(folder);
    }

    private void onExternalChange(@RequiredUIAccess Runnable action) {
        if (myApplying || myDisposed) {
            return;
        }

        myProject.getUIAccess().give(() -> {
            if (!myApplying && !myDisposed && myTree != null) {
                action.run();
            }
        });
    }

    @RequiredUIAccess
    private void addExternalConfiguration(RunnerAndConfigurationSettings settings) {
        if (getParent(settings) != null) {
            return;
        }

        ConfigurationType type = settings.getType();
        if (!isRootType(type)) {
            add(ROOT, type);
            sortTypeNodes();
        }

        Object parent = type;
        String folderName = settings.getFolderName();
        if (folderName != null) {
            Folder folder = null;
            for (Object child : getChildren(type)) {
                if (child instanceof Folder candidate && folderName.equals(candidate.getName())) {
                    folder = candidate;
                    break;
                }
            }

            if (folder == null) {
                folder = new Folder(type, folderName);
                insert(type, folder, firstChildIndex(type, CONFIGURATION));
            }
            parent = folder;
        }

        if (settings.isTemporary()) {
            add(parent, settings);
        }
        else {
            insert(parent, settings, firstChildIndex(parent, TEMPORARY_CONFIGURATION));
        }

        refreshTree(null);
    }

    private int firstChildIndex(Object parent, RunConfigurableNodeKind kind) {
        List<Object> children = getChildren(parent);
        for (int i = 0; i < children.size(); i++) {
            if (getKind(children.get(i)) == kind) {
                return i;
            }
        }
        return children.size();
    }

    @RequiredUIAccess
    private void removeExternalConfiguration(RunnerAndConfigurationSettings settings) {
        Object parent = getParent(settings);
        if (parent == null) {
            return;
        }

        SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.remove(settings);
        if (configurable != null) {
            configurable.disposeUIResources();
        }

        removeValue(settings);
        if (parent instanceof ConfigurationType && getChildren(parent).isEmpty()) {
            removeValue(parent);
        }

        if (mySelectedValue == settings) {
            mySelectedValue = null;
            mySelectedConfigurable = null;
            showPressAddMessage(null);
        }

        refreshTree(null);
    }

    @RequiredUIAccess
    private void applyByType(RunManagerImpl manager, ConfigurationType type, @Nullable RunnerAndConfigurationSettings selectedSettings)
        throws ConfigurationException {
        int indexToMove = -1;

        List<RunConfigurationBean> stableConfigurations = new ArrayList<>();
        if (isRootType(type)) {
            Set<String> names = new HashSet<>();
            for (Object value : collect(type, CONFIGURATION, TEMPORARY_CONFIGURATION)) {
                RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings) value;
                SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
                RunConfigurationBean configurationBean;
                if (configurable != null) {
                    if (settings.isTemporary()) {
                        applyConfiguration(manager, configurable);
                    }
                    configurationBean = new RunConfigurationBean(configurable);
                }
                else {
                    configurationBean = new RunConfigurationBean(
                        settings,
                        manager.isConfigurationShared(settings),
                        manager.getBeforeRunTasks(settings.getConfiguration())
                    );
                }

                String nameText = configurable != null ? configurable.getNameText() : settings.getName();
                if (!names.add(nameText)) {
                    revealValue(value, true);
                    throw new ConfigurationException(type.getDisplayName() + " with name \'" + nameText + "\' already exists");
                }
                stableConfigurations.add(configurationBean);
                if (settings == selectedSettings) {
                    indexToMove = stableConfigurations.size() - 1;
                }
            }

            names.clear();
            for (Object value : collect(type, FOLDER)) {
                String folderName = ((Folder) value).getName();
                if (folderName.isEmpty()) {
                    revealValue(value, true);
                    throw new ConfigurationException(LocalizeValue.localizeTODO("Folder name shouldn't be empty"));
                }
                if (!names.add(folderName)) {
                    revealValue(value, true);
                    throw new ConfigurationException(LocalizeValue.localizeTODO("Folders name \'" + folderName + "\' is duplicated"));
                }
            }
        }

        for (RunConfigurationBean bean : stableConfigurations) {
            SingleConfigurationConfigurable<?> configurable = bean.getConfigurable();
            if (configurable != null) {
                applyConfiguration(manager, configurable);
            }
        }

        Set<RunnerAndConfigurationSettings> toDeleteSettings = new HashSet<>();
        for (RunConfiguration each : manager.getConfigurationsList(type)) {
            ContainerUtil.addIfNotNull(toDeleteSettings, manager.getSettings(each));
        }

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

    @RequiredUIAccess
    private void applyConfiguration(RunManagerImpl manager, SingleConfigurationConfigurable<?> configurable) throws ConfigurationException {
        try {
            configurable.apply();
            manager.fireRunConfigurationChanged(configurable.getSettings());
        }
        catch (ConfigurationException e) {
            revealValue(configurable.getSettings(), true);
            throw e;
        }
    }

    @RequiredUIAccess
    private boolean isTreeModified() {
        List<RunConfiguration> allConfigurations = myRunManager.getAllConfigurationsList();
        List<RunConfiguration> currentConfigurations = new ArrayList<>();
        for (Object value : getChildren(ROOT)) {
            if (value instanceof ConfigurationType type) {
                List<RunnerAndConfigurationSettings> configurationSettings = myRunManager.getConfigurationSettingsList(type);
                List<Object> configurationValues = collect(type, CONFIGURATION, TEMPORARY_CONFIGURATION);
                if (configurationSettings.size() != configurationValues.size()) {
                    return true;
                }
                for (int j = 0; j < configurationValues.size(); j++) {
                    RunnerAndConfigurationSettings settings = (RunnerAndConfigurationSettings) configurationValues.get(j);
                    SingleConfigurationConfigurable<RunConfiguration> configurable = myConfigurables.get(settings);
                    if (configurable != null) {
                        if (!Comparing.strEqual(
                            configurationSettings.get(j).getConfiguration().getName(),
                            configurable.getConfiguration().getName()
                        )) {
                            return true;
                        }
                        if (configurable.isModified()) {
                            return true;
                        }
                    }
                    currentConfigurations.add(settings.getConfiguration());
                }
            }
        }
        return allConfigurations.size() != currentConfigurations.size() || !allConfigurations.containsAll(currentConfigurations);
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        resetGeneralSettings();
        setModified(false);
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        if (myTree == null) {
            return false;
        }

        if (super.isModified() || isTreeModified() || isGeneralSettingsModified()) {
            return true;
        }

        for (Configurable configurable : myTemplateConfigurables.values()) {
            if (configurable.isModified()) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        if (myTree == null) {
            return;
        }

        updateActiveConfigurationFromSelected();

        RunManagerImpl manager = myRunManager;
        myApplying = true;
        try {
            manager.fireBeginUpdate();

            List<ConfigurationType> configurationTypes = new ArrayList<>();
            for (Object value : getChildren(ROOT)) {
                if (value instanceof ConfigurationType type) {
                    configurationTypes.add(type);
                }
            }
            for (ConfigurationType type : manager.getConfigurationFactories()) {
                if (!configurationTypes.contains(type)) {
                    configurationTypes.add(type);
                }
            }

            for (ConfigurationType type : configurationTypes) {
                applyByType(manager, type, getSelectedSettings());
            }

            applyGeneralSettings(manager);

            for (Configurable configurable : myTemplateConfigurables.values()) {
                if (configurable.isModified()) {
                    configurable.apply();
                }
            }

            manager.saveOrder();
            setModified(false);
        }
        finally {
            manager.fireEndUpdate();
            myApplying = false;
        }

        refreshTree(mySelectedValue);
    }

    @Override
    public String getHelpTopic() {
        ConfigurationType type = getSelectedConfigurationType();
        if (type != null) {
            return "reference.dialogs.rundebug." + type.getId();
        }
        return "reference.dialogs.rundebug";
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        myDisposed = true;

        for (Configurable configurable : myTemplateConfigurables.values()) {
            configurable.disposeUIResources();
        }
        myTemplateConfigurables.clear();

        for (SingleConfigurationConfigurable<RunConfiguration> configurable : myConfigurables.values()) {
            configurable.disposeUIResources();
        }
        myConfigurables.clear();

        for (UnnamedConfigurable each : myAdditionalSettings) {
            each.disposeUIResources();
        }
        myAdditionalSettings.clear();

        myChildren.clear();
        myParents.clear();
        myTreeNodes.clear();
        myExpandedValues.clear();
        myTree = null;
        myToolbar = null;
        myUIDisposable = null;
        myRightPanel = null;
        myDefaultsPanel = null;
        myConfirmationBox = null;
        myRecentsLimitBox = null;
        mySelectedValue = null;
        mySelectedConfigurable = null;
        myActionState = ActionState.NONE;
    }

    private class ConfigurationDragAndDropHandler implements DragAndDropTransferHandler<TreeNode<Object>> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<TreeNode<Object>> items, boolean move) {
            if (!move || items.size() != 1) {
                return null;
            }

            Object value = items.get(0).getValue();
            if (!getKind(value).supportsDnD()) {
                return null;
            }
            return DataTransfer.of(getSpeedSearchText(value));
        }

        @RequiredUIAccess
        @Override
        public boolean drop(Component component, DropContext<TreeNode<Object>> context) {
            List<TreeNode<Object>> items = context.getItems();
            if (items.size() != 1) {
                return false;
            }

            Object dragged = items.get(0).getValue();
            Object target = context.getTarget().getValue();
            if (dragged == null || target == null || dragged == target) {
                return false;
            }

            Set<Object> expanded = new HashSet<>(myExpandedValues);
            List<Object> rows = collectVisibleRows(expanded);
            int oldIndex = indexOf(rows, dragged);
            int newIndex = indexOf(rows, target);
            DropPosition position = context.getPosition();
            if (oldIndex < 0 || newIndex < 0 || !canDrop(rows, expanded, oldIndex, newIndex, position)) {
                return false;
            }

            if (!context.isCheckOnly()) {
                refreshTree(move(rows, expanded, new DropTarget(oldIndex, newIndex, position)));
            }
            return true;
        }
    }

    private class AddAction extends DumbAwareAction {
        private AddAction() {
            super(
                ExecutionLocalize.addNewRunConfigurationAction2Name(),
                ExecutionLocalize.addNewRunConfigurationAction2Name(),
                PlatformIconGroup.generalAdd()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            showAddPopup(true, popup -> popup.showUnderneathOf(e));
        }
    }

    private class RemoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveAction() {
            super(
                ExecutionLocalize.removeRunConfigurationActionName(),
                ExecutionLocalize.removeRunConfigurationActionName(),
                PlatformIconGroup.generalRemove()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            removeSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myActionState.remove());
        }
    }

    private class CopyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CopyAction() {
            super(
                ExecutionLocalize.copyConfigurationActionName(),
                ExecutionLocalize.copyConfigurationActionName(),
                PlatformIconGroup.actionsCopy()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            copySelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myActionState.copy());
        }
    }

    private class SaveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private SaveAction() {
            super(ExecutionLocalize.actionNameSaveConfiguration(), LocalizeValue.empty(), PlatformIconGroup.actionsMenu_saveall());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            saveSelected();
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myActionState.save());
        }
    }

    private class EditDefaultsAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private EditDefaultsAction() {
            super(
                ExecutionLocalize.runConfigurationEditDefaultConfigurationSettingsText(),
                ExecutionLocalize.runConfigurationEditDefaultConfigurationSettingsDescription(),
                PlatformIconGroup.generalSettings()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Object templateNode = findTemplateNode(getSelectedConfigurationType());
            if (templateNode != null) {
                revealValue(templateNode, true);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myActionState.editDefaults());
        }
    }

    private class MoveAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final int myDirection;

        private MoveAction(LocalizeValue text, Image icon, int direction) {
            super(text, LocalizeValue.empty(), icon);
            myDirection = direction;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            moveSelected(myDirection);
        }

        @Override
        public void update(AnActionEvent e) {
            ActionState state = myActionState;
            e.getPresentation().setEnabled(myDirection < 0 ? state.moveUp() : state.moveDown());
        }
    }

    private class CreateFolderAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private CreateFolderAction() {
            super(
                ExecutionLocalize.runConfigurationCreateFolderText(),
                ExecutionLocalize.runConfigurationCreateFolderDescription(),
                PlatformIconGroup.nodesFolder()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            createFolder();
        }

        @Override
        public void update(AnActionEvent e) {
            ActionState state = myActionState;
            e.getPresentation().setText(
                state.createFolderMoves()
                    ? ExecutionLocalize.runConfigurationCreateFolderDescriptionMove()
                    : ExecutionLocalize.runConfigurationCreateFolderDescription()
            );
            e.getPresentation().setEnabled(state.createFolder());
        }
    }
}
