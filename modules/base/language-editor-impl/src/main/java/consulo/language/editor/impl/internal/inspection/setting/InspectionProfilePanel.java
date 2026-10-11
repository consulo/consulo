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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.InspectionProjectProfileManager;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.ToolsImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.inspection.scheme.ModifiableModel;
import consulo.language.editor.inspection.scheme.ProfileManager;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.TreeNode;
import consulo.ui.TriStateCheckBoxTree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.TreeCheckEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author anna
 * @since 2006-05-31
 */
public class InspectionProfilePanel implements Disposable {
    private static final long FILTER_DELAY = 300;
    private static final int TREE_PROPORTION = 50;
    private static final int DESCRIPTION_PROPORTION = 50;

    private final Application myApplication;
    private final InspectionProjectProfileManager myProjectProfileManager;
    private final Project myProject;
    private final Predicate<InspectionToolWrapper> myToolFilter;
    private final InspectionsFilter myInspectionsFilter = new InspectionsFilter();
    private final List<ToolDescriptors> myInitialToolDescriptors = new ArrayList<>();
    private final Map<String, InspectionToolWrapper> myToolWrappers = new ConcurrentHashMap<>();
    private final Set<String> myShownOptions = new HashSet<>();

    private InspectionProfileImpl mySelectedProfile;
    private String myCurrentProfileName;
    private boolean myShareProfile;
    private String @Nullable [] myInitialScopesOrder;
    private boolean myToolStatesInitialized;

    private @Nullable InspectionTreeModel myModel;
    private @Nullable TriStateCheckBoxTree<InspectionTreeNode> myTree;
    private @Nullable TextBox myFilter;
    private @Nullable HtmlView myDescription;
    private @Nullable InspectionToolDetailsPanel myDetails;
    private @Nullable Component myRoot;

    private @Nullable ScheduledFuture<?> myQueuedFilter;
    private @Nullable String myPendingSelection;
    private @Nullable String myAppliedFilter;
    private List<String> mySelectedToolIds = List.of();
    private boolean mySyncQueued;
    private boolean mySelectionQueued;
    private boolean myTreeLoaded;
    private int myDescriptionStamp;
    private boolean myDisposed;

    public InspectionProfilePanel(
        Application application,
        InspectionProjectProfileManager projectProfileManager,
        String inspectionProfileName,
        InspectionProfileImpl profile,
        Predicate<InspectionToolWrapper> toolFilter
    ) {
        myApplication = application;
        myProjectProfileManager = projectProfileManager;
        myProject = projectProfileManager.getProject();
        mySelectedProfile = profile;
        myCurrentProfileName = inspectionProfileName;
        myShareProfile = profile.getProfileManager() == projectProfileManager;
        myToolFilter = toolFilter;
        myInspectionsFilter.setChangeListener(this::onInspectionsFilterChanged);
    }

    public InspectionsFilter getInspectionsFilter() {
        return myInspectionsFilter;
    }

    public InspectionProfileImpl getSelectedProfile() {
        return mySelectedProfile;
    }

    public boolean isProfileShared() {
        return myShareProfile;
    }

    public void setProfileShared(boolean profileShared) {
        myShareProfile = profileShared;
    }

    public String getCurrentProfileName() {
        return myCurrentProfileName;
    }

    public void setCurrentProfileName(String currentProfileName) {
        myCurrentProfileName = currentProfileName;
    }

    public boolean isUIBuilt() {
        return myRoot != null;
    }

    @RequiredUIAccess
    public Component getComponent() {
        Component root = myRoot;
        if (root == null) {
            root = buildUI();
            myRoot = root;
        }
        return root;
    }

    public @Nullable Component getPreferredFocusedComponent() {
        return myTree;
    }

    @RequiredUIAccess
    public void setFilter(String filter) {
        TextBox filterBox = myFilter;
        if (filterBox != null) {
            filterBox.setValue(filter);
        }
    }

    @RequiredUIAccess
    public void selectInspectionTool(String shortName) {
        myPendingSelection = shortName;
        if (myTreeLoaded) {
            selectPending();
        }
    }

    @RequiredUIAccess
    private Component buildUI() {
        initToolStates();

        InspectionTreeModel model = new InspectionTreeModel(this::loadDescription);
        myModel = model;

        TriStateCheckBoxTree<InspectionTreeNode> tree = TriStateCheckBoxTree.create(
            model,
            myApplication.getInstance(ApplicationTreeExecutorFactory.class).forBackgroundThreadWithoutReadAction(this)
        );
        tree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
        tree.setAllowMultipleSelect(true);
        tree.addCheckListener(this::onCheck);
        tree.addSelectListener(event -> queueSelectionUpdate());
        tree.addDoubleClickListener(event -> onDoubleClick(event.getValue()));
        tree.addContextMenuListener(this::showContextMenu);
        myTree = tree;

        TextBox filter = TextBox.create();
        filter.setPlaceholder(InspectionLocalize.inspectionSettingsFilterPlaceholder());
        filter.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        filter.setPrefixComponent(searchIcon);
        filter.addValueListener(event -> queueFilter(StringUtil.notNullize(event.getValue())));
        myFilter = filter;

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.UNKNOWN, createToolbarGroup(), ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(tree);

        DockLayout top = DockLayout.create(Space.SMALL);
        top.center(filter);
        top.right(toolbar.getUIComponent());

        DockLayout left = DockLayout.create(Space.SMALL);
        left.top(top);
        left.center(ScrollableLayout.create(tree));
        left.paddingBuilder().allSet(Space.MEDIUM).apply();

        HtmlView description = HtmlView.create();
        myDescription = description;

        InspectionToolDetailsPanel details = new InspectionToolDetailsPanel(
            myProject,
            mySelectedProfile,
            this,
            this::onToolsChanged,
            this::onStructureChanged,
            this::onOptionsShown
        );
        myDetails = details;

        TwoComponentSplitLayout right = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        right.setFirstComponent(LabeledLayout.create(InspectionLocalize.inspectionDescriptionTitle(), description));
        right.setSecondComponent(details.getComponent());
        right.setProportion(DESCRIPTION_PROPORTION);

        DockLayout rightHolder = DockLayout.create(Space.NONE);
        rightHolder.center(right);
        rightHolder.paddingBuilder().allSet(Space.MEDIUM).apply();

        TwoComponentSplitLayout root = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        root.setFirstComponent(left);
        root.setSecondComponent(rightHolder);
        root.setProportion(TREE_PROPORTION);

        reloadRows();
        return root;
    }

    private ActionGroup createToolbarGroup() {
        ActionGroup.Builder actions = ActionGroup.newImmutableBuilder();
        actions.add(new InspectionFilterAction(mySelectedProfile, myInspectionsFilter, myProject));
        actions.addSeparator();
        actions.add(new DumbAwareAction(ActionLocalize.actionExpandallText(), LocalizeValue.empty(), PlatformIconGroup.actionsExpandall()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
                if (tree != null) {
                    tree.expandAll();
                }
            }
        });
        actions.add(new DumbAwareAction(ActionLocalize.actionCollapseallText(), LocalizeValue.empty(), PlatformIconGroup.actionsCollapseall()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
                if (tree != null) {
                    tree.collapseAll();
                }
            }
        });
        actions.add(new RestoreDefaultsAction());
        actions.add(createAdvancedGroup());
        return actions.build();
    }

    private ActionGroup createAdvancedGroup() {
        DefaultActionGroup group = new DefaultActionGroup(
            InspectionLocalize.inspectionSettingsAdvanced(),
            LocalizeValue.empty(),
            PlatformIconGroup.generalGearplain()
        );
        group.setPopup(true);
        group.add(new ToggleAction(InspectionLocalize.inspectionSettingsDisableNewInspections()) {
            @Override
            public boolean isSelected(AnActionEvent e) {
                return mySelectedProfile.isProfileLocked();
            }

            @RequiredUIAccess
            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                mySelectedProfile.lockProfile(state);
            }

            @Override
            public boolean isDumbAware() {
                return true;
            }
        });
        group.add(new ResetToEmptyAction());
        return group;
    }

    private void initToolStates() {
        myInitialToolDescriptors.clear();
        myToolWrappers.clear();
        for (ScopeToolState state : mySelectedProfile.getDefaultStates(myProject)) {
            InspectionToolWrapper toolWrapper = state.getTool();
            if (toolWrapper.getDefaultLevel() == HighlightDisplayLevel.NON_SWITCHABLE_ERROR || !myToolFilter.test(toolWrapper)) {
                continue;
            }
            myInitialToolDescriptors.add(ToolDescriptors.fromScopeToolState(state, mySelectedProfile, myProject));
            myToolWrappers.put(toolWrapper.getShortName(), toolWrapper);
        }
        myInitialScopesOrder = mySelectedProfile.getScopesOrder();
        myToolStatesInitialized = true;
    }

    private @Nullable String loadDescription(String shortName) {
        InspectionToolWrapper toolWrapper = myToolWrappers.get(shortName);
        return toolWrapper == null ? null : toolWrapper.loadDescription();
    }

    @RequiredUIAccess
    private void reloadRows() {
        UIAccess uiAccess = UIAccess.current();
        List<ToolDescriptors> descriptors = List.copyOf(myInitialToolDescriptors);
        CompletableFuture.supplyAsync(() -> createRows(descriptors), AppExecutorUtil.getAppExecutorService())
            .whenCompleteAsync((rows, error) -> {
                InspectionTreeModel model = myModel;
                if (myDisposed || model == null || rows == null) {
                    return;
                }
                model.setRows(rows);
                refilter(myFilter == null ? "" : StringUtil.notNullize(myFilter.getValue()));
            }, uiAccess);
    }

    private List<InspectionRow> createRows(List<ToolDescriptors> descriptors) {
        boolean emptyFilter = myInspectionsFilter.isEmptyFilter();
        List<InspectionRow> rows = new ArrayList<>(descriptors.size());
        for (ToolDescriptors toolDescriptors : descriptors) {
            String toolId = toolDescriptors.getDefaultDescriptor().getKey().toString();
            if (!emptyFilter && !myInspectionsFilter.matches(mySelectedProfile.getTools(toolId, myProject))) {
                continue;
            }
            rows.add(createRow(toolDescriptors.getDefaultDescriptor()));
        }
        return rows;
    }

    private InspectionRow createRow(Descriptor descriptor) {
        String toolId = descriptor.getKey().toString();
        ToolsImpl tools = mySelectedProfile.getTools(toolId, myProject);

        Boolean previous = null;
        boolean mixed = false;
        for (ScopeToolState state : tools.getTools()) {
            if (previous == null) {
                previous = state.isEnabled();
            }
            else if (previous != state.isEnabled()) {
                mixed = true;
            }
        }
        ThreeState enabled = mixed ? ThreeState.UNSURE : Boolean.TRUE.equals(previous) ? ThreeState.YES : ThreeState.NO;

        MultiColoredHighlightSeverityIconSink sink = new MultiColoredHighlightSeverityIconSink();
        if (tools.isEnabled()) {
            sink.put(mySelectedProfile.getToolDefaultState(toolId, myProject), mySelectedProfile.getNonDefaultTools(toolId, myProject));
        }

        InspectionToolWrapper toolWrapper = descriptor.getToolWrapper();
        boolean batchOnly = !(toolWrapper instanceof LocalInspectionToolWrapper)
            && !(toolWrapper instanceof GlobalInspectionToolWrapper globalTool && !globalTool.worksInBatchModeOnly());

        return new InspectionRow(
            toolId,
            descriptor.getText(),
            List.of(descriptor.getGroup()),
            enabled,
            sink.constructIcon(mySelectedProfile),
            mySelectedProfile.isProperSetting(toolId),
            batchOnly
        );
    }

    @RequiredUIAccess
    private void onInspectionsFilterChanged() {
        if (myModel != null) {
            reloadRows();
        }
    }

    @RequiredUIAccess
    private void queueFilter(String text) {
        ScheduledFuture<?> queued = myQueuedFilter;
        if (queued != null) {
            queued.cancel(false);
        }

        myQueuedFilter = UIAccess.current().getScheduler().schedule(() -> {
            myQueuedFilter = null;
            if (!myDisposed) {
                refilter(text);
            }
        }, FILTER_DELAY, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void refilter(String text) {
        InspectionTreeModel model = myModel;
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        if (model == null || tree == null) {
            return;
        }

        model.setFilter(text);
        String filter = model.getFilter();
        String previousFilter = myAppliedFilter;
        myAppliedFilter = filter;

        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll().thenComposeAsync(ignored -> {
            TreeNode<InspectionTreeNode> root = tree.getRootNode();
            return root == null ? CompletableFuture.completedFuture(null) : root.findChild(value -> true);
        }, uiAccess).whenCompleteAsync((value, error) -> {
            if (myDisposed) {
                return;
            }

            myTreeLoaded = true;
            syncChecks();

            if (filter != null) {
                tree.expandAll();
            }
            else if (previousFilter != null) {
                tree.collapseAll();
            }

            selectPending();
            updateSelection();
        }, uiAccess);
    }

    @RequiredUIAccess
    private void selectPending() {
        String shortName = myPendingSelection;
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        if (shortName == null || tree == null) {
            return;
        }

        TreeNode<InspectionTreeNode> root = tree.getRootNode();
        if (root == null) {
            return;
        }

        myPendingSelection = null;
        UIAccess uiAccess = UIAccess.current();
        root.findChildDeep(value -> value instanceof InspectionLeafNode leaf && leaf.shortName().equals(shortName))
            .whenCompleteAsync((node, error) -> {
                if (node != null && !myDisposed) {
                    tree.select(node);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void onCheck(TreeCheckEvent<InspectionTreeNode> event) {
        if (event.getInputDetails() instanceof ProgrammaticInputDetails) {
            return;
        }

        InspectionTreeModel model = myModel;
        if (model == null) {
            return;
        }

        boolean enabled = event.getState() != ThreeState.NO;
        List<String> toolIds = new ArrayList<>();
        for (InspectionRow row : model.getVisibleRows(event.getValue())) {
            setToolEnabled(row.shortName(), enabled);
            toolIds.add(row.shortName());
        }
        onToolsChanged(toolIds);
        queueSelectionUpdate();
    }

    @RequiredUIAccess
    private void onDoubleClick(TreeNode<InspectionTreeNode> node) {
        InspectionTreeModel model = myModel;
        if (model == null || !(node.getValue() instanceof InspectionLeafNode leaf)) {
            return;
        }

        InspectionRow row = model.getRow(leaf.shortName());
        if (row == null) {
            return;
        }

        setToolEnabled(row.shortName(), row.enabled() != ThreeState.YES);
        onToolsChanged(List.of(row.shortName()));
        queueSelectionUpdate();
    }

    private void setToolEnabled(String toolId, boolean enabled) {
        if (enabled) {
            mySelectedProfile.enableTool(toolId, myProject);
        }
        else {
            mySelectedProfile.disableTool(toolId, myProject);
        }
        for (ScopeToolState state : mySelectedProfile.getTools(toolId, myProject).getTools()) {
            state.setEnabled(enabled);
        }
    }

    @RequiredUIAccess
    private void onToolsChanged(Collection<String> toolIds) {
        InspectionTreeModel model = myModel;
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        if (model == null || tree == null) {
            return;
        }

        List<InspectionRow> rows = new ArrayList<>();
        for (ToolDescriptors descriptors : myInitialToolDescriptors) {
            Descriptor descriptor = descriptors.getDefaultDescriptor();
            if (toolIds.contains(descriptor.getKey().toString())) {
                rows.add(createRow(descriptor));
            }
        }
        model.updateRows(rows);

        Set<InspectionTreeNode> toRefresh = new LinkedHashSet<>();
        for (InspectionRow row : rows) {
            toRefresh.add(row.toNode());
            List<LocalizeValue> path = row.groupPath();
            for (int i = 1; i <= path.size(); i++) {
                toRefresh.add(new InspectionGroupNode(List.copyOf(path.subList(0, i))));
            }
        }
        for (InspectionTreeNode value : toRefresh) {
            TreeNode<InspectionTreeNode> node = model.findNode(value);
            if (node != null) {
                tree.refreshItem(node);
            }
        }

        queueSync();
    }

    @RequiredUIAccess
    private void onStructureChanged() {
        onToolsChanged(mySelectedToolIds);
        InspectionToolDetailsPanel details = myDetails;
        if (details != null) {
            details.show(mySelectedToolIds);
        }
    }

    private void onOptionsShown(String toolId) {
        myShownOptions.add(toolId);
        for (ToolDescriptors descriptors : myInitialToolDescriptors) {
            if (descriptors.getDefaultDescriptor().getKey().toString().equals(toolId)) {
                descriptors.getDefaultDescriptor().loadConfig();
                for (Descriptor descriptor : descriptors.getNonDefaultDescriptors()) {
                    descriptor.loadConfig();
                }
            }
        }
    }

    @RequiredUIAccess
    private void queueSync() {
        if (mySyncQueued) {
            return;
        }

        mySyncQueued = true;
        UIAccess.current().execute(() -> {
            mySyncQueued = false;
            if (!myDisposed) {
                syncChecks();
            }
        });
    }

    @RequiredUIAccess
    private void syncChecks() {
        InspectionTreeModel model = myModel;
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        if (model == null || tree == null) {
            return;
        }

        Map<InspectionGroupNode, boolean[]> groups = new LinkedHashMap<>();
        for (InspectionRow row : model.getVisible()) {
            tree.setCheckState(row.toNode(), row.enabled());

            List<LocalizeValue> path = row.groupPath();
            for (int i = 1; i <= path.size(); i++) {
                boolean[] state = groups.computeIfAbsent(new InspectionGroupNode(List.copyOf(path.subList(0, i))), group -> new boolean[2]);
                if (row.enabled() != ThreeState.NO) {
                    state[0] = true;
                }
                if (row.enabled() != ThreeState.YES) {
                    state[1] = true;
                }
            }
        }

        for (Map.Entry<InspectionGroupNode, boolean[]> entry : groups.entrySet()) {
            boolean[] state = entry.getValue();
            ThreeState groupState = state[0] && state[1] ? ThreeState.UNSURE : state[0] ? ThreeState.YES : ThreeState.NO;
            tree.setCheckState(entry.getKey(), groupState);
        }
    }

    @RequiredUIAccess
    private void queueSelectionUpdate() {
        if (mySelectionQueued) {
            return;
        }

        mySelectionQueued = true;
        UIAccess.current().execute(() -> {
            mySelectionQueued = false;
            if (!myDisposed) {
                updateSelection();
            }
        });
    }

    @RequiredUIAccess
    private void updateSelection() {
        InspectionTreeModel model = myModel;
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        HtmlView description = myDescription;
        InspectionToolDetailsPanel details = myDetails;
        if (model == null || tree == null || description == null || details == null) {
            return;
        }

        List<TreeNode<InspectionTreeNode>> nodes = tree.getSelectedNodes();
        Set<String> toolIds = new LinkedHashSet<>();
        for (TreeNode<InspectionTreeNode> node : nodes) {
            InspectionTreeNode value = node.getValue();
            if (value != null) {
                for (InspectionRow row : model.getVisibleRows(value)) {
                    toolIds.add(row.shortName());
                }
            }
        }

        List<String> selected = List.copyOf(toolIds);
        boolean changed = !selected.equals(mySelectedToolIds);
        mySelectedToolIds = selected;

        int stamp = ++myDescriptionStamp;
        if (selected.isEmpty()) {
            description.render(new HtmlView.RenderData(""));
        }
        else if (nodes.size() == 1 && nodes.get(0).getValue() instanceof InspectionLeafNode leaf) {
            String filter = model.getFilter();
            UIAccess uiAccess = UIAccess.current();
            CompletableFuture.supplyAsync(() -> model.loadDescription(leaf.shortName()), AppExecutorUtil.getAppExecutorService())
                .whenCompleteAsync((html, error) -> {
                    if (myDisposed || stamp != myDescriptionStamp) {
                        return;
                    }
                    String text = html == null
                        ? InspectionLocalize.inspectionSettingsNoDescription().get()
                        : SearchHighlightUtil.markupHtml(html, filter, "<b><font color=\"#d14\">", "</font></b>");
                    description.render(new HtmlView.RenderData(text));
                }, uiAccess);
        }
        else {
            description.render(new HtmlView.RenderData(InspectionLocalize.inspectionSettingsMultipleSelected().get()));
        }

        if (changed) {
            details.show(selected);
        }
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        TriStateCheckBoxTree<InspectionTreeNode> tree = myTree;
        if (tree == null || tree.getSelectedNodes().isEmpty()) {
            return;
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        SeverityRegistrarImpl registrar = InspectionSeverities.getOwnRegistrar(mySelectedProfile);
        for (HighlightSeverity severity : InspectionSeverities.getSeverities(registrar)) {
            HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
            group.add(new DumbAwareAction(InspectionSeverities.renderSeverity(severity), LocalizeValue.empty(), level.getIcon()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(AnActionEvent e) {
                    setNewHighlightingLevel(level);
                }
            });
        }

        InputDetails inputDetails = event.getInputDetails();
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group.build());
        menu.setTargetComponent(tree);
        menu.show(event.getComponent(), inputDetails == null ? 0 : inputDetails.getX(), inputDetails == null ? 0 : inputDetails.getY());
    }

    @RequiredUIAccess
    private void setNewHighlightingLevel(HighlightDisplayLevel level) {
        List<String> toolIds = mySelectedToolIds;
        for (String toolId : toolIds) {
            mySelectedProfile.setErrorLevel(mySelectedProfile.getToolDefaultState(toolId, myProject).getTool().getHighlightDisplayKey(), level, null, myProject);
        }
        onStructureChanged();
    }

    private void loadDescriptorsConfigs(boolean onlyModified) {
        for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
            loadDescriptorConfig(toolDescriptors.getDefaultDescriptor(), onlyModified);
            for (Descriptor descriptor : toolDescriptors.getNonDefaultDescriptors()) {
                loadDescriptorConfig(descriptor, onlyModified);
            }
        }
    }

    private void loadDescriptorConfig(Descriptor descriptor, boolean onlyModified) {
        if (!onlyModified || mySelectedProfile.isProperSetting(descriptor.getKey().toString())) {
            descriptor.loadConfig();
        }
    }

    @RequiredUIAccess
    private void postProcessModification() {
        for (ScopeToolState state : mySelectedProfile.getAllTools(myProject)) {
            state.resetConfigPanel();
        }
        mySelectedToolIds = List.of();
        reloadRows();
    }

    @RequiredUIAccess
    public boolean isModified() {
        if (mySelectedProfile.isChanged()) {
            return true;
        }
        if (myShareProfile != (mySelectedProfile.getProfileManager() == myProjectProfileManager)) {
            return true;
        }
        if (!Comparing.strEqual(myCurrentProfileName, mySelectedProfile.getName())) {
            return true;
        }
        if (!myToolStatesInitialized) {
            return false;
        }
        if (!Arrays.equals(myInitialScopesOrder, mySelectedProfile.getScopesOrder())) {
            return true;
        }
        if (descriptorsAreChanged() || wereToolSettingsModified()) {
            return true;
        }
        for (ScopeToolState state : mySelectedProfile.getAllTools(myProject)) {
            if (state.isModified()) {
                return true;
            }
        }
        return false;
    }

    private boolean wereToolSettingsModified() {
        for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
            Descriptor defaultDescriptor = toolDescriptors.getDefaultDescriptor();
            if (!myShownOptions.contains(defaultDescriptor.getKey().toString())) {
                continue;
            }
            if (wereToolSettingsModified(defaultDescriptor, true)) {
                return true;
            }
            for (Descriptor descriptor : toolDescriptors.getNonDefaultDescriptors()) {
                if (wereToolSettingsModified(descriptor, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean wereToolSettingsModified(Descriptor descriptor, boolean isDefault) {
        if (!mySelectedProfile.isToolEnabled(descriptor.getKey(), descriptor.getScope(), myProject)) {
            return false;
        }
        Element oldConfig = descriptor.getConfig();
        if (oldConfig == null) {
            return false;
        }

        String toolId = descriptor.getKey().toString();
        ScopeToolState state = null;
        if (isDefault) {
            state = mySelectedProfile.getToolDefaultState(toolId, myProject);
        }
        else {
            for (ScopeToolState candidate : mySelectedProfile.getNonDefaultTools(toolId, myProject)) {
                if (Objects.equals(candidate.getScopeId(), descriptor.getScopeName())) {
                    state = candidate;
                    break;
                }
            }
        }

        if (state == null) {
            return true;
        }

        Element newConfig = Descriptor.createConfigElement(state.getTool());
        return !JDOMUtil.areElementsEqual(oldConfig, newConfig);
    }

    private boolean descriptorsAreChanged() {
        for (ToolDescriptors toolDescriptors : myInitialToolDescriptors) {
            Descriptor desc = toolDescriptors.getDefaultDescriptor();
            if (mySelectedProfile.isToolEnabled(desc.getKey(), null, myProject) != desc.isEnabled()) {
                return true;
            }
            if (mySelectedProfile.getErrorLevel(desc.getKey(), desc.getScope(), myProject) != desc.getLevel()) {
                return true;
            }
            List<Descriptor> descriptors = toolDescriptors.getNonDefaultDescriptors();
            for (Descriptor descriptor : descriptors) {
                if (mySelectedProfile.isToolEnabled(descriptor.getKey(), descriptor.getScope(), myProject) != descriptor.isEnabled()) {
                    return true;
                }
                if (mySelectedProfile.getErrorLevel(descriptor.getKey(), descriptor.getScope(), myProject) != descriptor.getLevel()) {
                    return true;
                }
            }

            List<ScopeToolState> tools = mySelectedProfile.getNonDefaultTools(desc.getKey().toString(), myProject);
            if (tools.size() != descriptors.size()) {
                return true;
            }
            for (int i = 0; i < tools.size(); i++) {
                if (!Objects.equals(tools.get(i).getScope(myProject), descriptors.get(i).getScope())) {
                    return true;
                }
            }
        }
        return false;
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        if (!isModified()) {
            return;
        }

        for (ScopeToolState state : mySelectedProfile.getAllTools(myProject)) {
            state.applyConfigPanel();
        }

        ModifiableModel selectedProfile = mySelectedProfile;
        if (!Comparing.equal(myCurrentProfileName, selectedProfile.getName())) {
            selectedProfile.getProfileManager().deleteProfile(selectedProfile.getName());
            selectedProfile.setName(myCurrentProfileName);
            selectedProfile.getProfileManager().updateProfile(selectedProfile);
        }

        ProfileManager profileManager = myShareProfile ? myProjectProfileManager : InspectionProfileManager.getInstance();
        selectedProfile.setProjectLevel(myShareProfile);
        if (selectedProfile.getProfileManager() != profileManager) {
            if (selectedProfile.getProfileManager().getProfile(selectedProfile.getName(), false) != null) {
                selectedProfile.getProfileManager().deleteProfile(selectedProfile.getName());
            }
            copyUsedSeveritiesIfUndefined(mySelectedProfile, profileManager);
            selectedProfile.setProfileManager(profileManager);
        }

        InspectionProfile parentProfile = selectedProfile.getParentProfile();
        try {
            selectedProfile.commit();
        }
        catch (IOException e) {
            throw new ConfigurationException(LocalizeValue.of(StringUtil.notNullize(e.getMessage())));
        }

        if (parentProfile != null) {
            mySelectedProfile = (InspectionProfileImpl) parentProfile.getModifiableModel();
            mySelectedProfile.setModified(false);
        }
    }

    private static void copyUsedSeveritiesIfUndefined(InspectionProfileImpl selectedProfile, ProfileManager profileManager) {
        SeverityRegistrarImpl registrar = (SeverityRegistrarImpl) ((SeverityProvider) profileManager).getSeverityRegistrar();
        Set<HighlightSeverity> severities = selectedProfile.getUsedSeverities();
        for (Iterator<HighlightSeverity> iterator = severities.iterator(); iterator.hasNext(); ) {
            HighlightSeverity severity = iterator.next();
            if (registrar.isSeverityValid(severity.getName())) {
                iterator.remove();
            }
        }

        if (severities.isEmpty()) {
            return;
        }

        SeverityRegistrarImpl oppositeRegister = InspectionSeverities.getOwnRegistrar(selectedProfile);
        for (HighlightSeverity severity : severities) {
            TextAttributesKey attributesKey = TextAttributesKey.find(severity.getName());
            TextAttributes textAttributes = oppositeRegister.getTextAttributesBySeverity(severity);
            if (textAttributes == null) {
                continue;
            }
            HighlightInfoTypeImpl info = new HighlightInfoTypeImpl(severity, attributesKey);
            registrar.registerSeverity(
                new SeverityRegistrarImpl.SeverityBasedTextAttributes(textAttributes.clone(), info),
                textAttributes.getErrorStripeColor()
            );
        }
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myDisposed = true;

        ScheduledFuture<?> queued = myQueuedFilter;
        if (queued != null) {
            queued.cancel(false);
        }

        if (myToolStatesInitialized) {
            for (ScopeToolState state : mySelectedProfile.getAllTools(myProject)) {
                state.disposeUIResources();
            }
        }
    }

    private class RestoreDefaultsAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RestoreDefaultsAction() {
            super(InspectionLocalize.inspectionSettingsRestoreDefaults(), LocalizeValue.empty(), PlatformIconGroup.generalReset());
        }

        @Override
        public void update(AnActionEvent e) {
            InspectionTreeModel model = myModel;
            boolean modified = false;
            if (model != null) {
                for (InspectionRow row : model.getRows()) {
                    if (row.modified()) {
                        modified = true;
                        break;
                    }
                }
            }
            e.getPresentation().setEnabled(modified);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            mySelectedProfile.resetToBase(myProject);
            loadDescriptorsConfigs(true);
            postProcessModification();
        }
    }

    private class ResetToEmptyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private ResetToEmptyAction() {
            super(InspectionLocalize.inspectionSettingsResetToEmpty());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(mySelectedProfile.isExecutable(myProject));
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            mySelectedProfile.resetToEmpty(myProject);
            loadDescriptorsConfigs(false);
            postProcessModification();
        }
    }
}
