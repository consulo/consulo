/*
 * Copyright 2013-2026 consulo.io
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

import consulo.ui.ex.dialog.DialogService;
import consulo.content.scope.NamedScope;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.ToolsImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TriStateCheckBox;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class InspectionToolDetailsPanel {
    private static final int TABLE_PROPORTION = 40;

    private final Project myProject;
    private final InspectionProfileImpl myProfile;
    private final Disposable myDisposable;
    private final Consumer<List<String>> myToolsChanged;
    private final Runnable myStructureChanged;
    private final Consumer<String> myOptionsShown;

    private final DockLayout myRoot;

    private List<String> myToolIds = List.of();
    private @Nullable DockLayout myOptionsHolder;
    private @Nullable MutableFlatDataModel<ScopeSeverityRow> myTableModel;
    private List<String> myScopeIds = List.of();

    @RequiredUIAccess
    public InspectionToolDetailsPanel(
        Project project,
        InspectionProfileImpl profile,
        Disposable disposable,
        Consumer<List<String>> toolsChanged,
        Runnable structureChanged,
        Consumer<String> optionsShown
    ) {
        myProject = project;
        myProfile = profile;
        myDisposable = disposable;
        myToolsChanged = toolsChanged;
        myStructureChanged = structureChanged;
        myOptionsShown = optionsShown;

        myRoot = DockLayout.create(Space.SMALL);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void clear() {
        show(List.of());
    }

    @RequiredUIAccess
    public void show(List<String> toolIds) {
        DockLayout oldOptions = myOptionsHolder;
        if (oldOptions != null) {
            oldOptions.removeAll();
        }

        myRoot.removeAll();
        myRoot.setEnabledRecursive(true);
        myToolIds = List.copyOf(toolIds);
        myOptionsHolder = null;
        myTableModel = null;

        if (toolIds.isEmpty()) {
            return;
        }

        Set<String> scopeIds = new LinkedHashSet<>();
        boolean anyEnabled = false;
        for (String toolId : toolIds) {
            for (ScopeToolState state : myProfile.getNonDefaultTools(toolId, myProject)) {
                scopeIds.add(state.getScopeId());
            }
            anyEnabled |= myProfile.getTools(toolId, myProject).isEnabled();
        }

        List<String> sortedScopeIds = new ArrayList<>(scopeIds);
        sortedScopeIds.sort(new ScopeOrderComparator(myProfile));
        myScopeIds = List.copyOf(sortedScopeIds);

        DockLayout options = DockLayout.create(Space.NONE);
        myOptionsHolder = options;

        String singleToolId = toolIds.size() == 1 ? toolIds.get(0) : null;
        if (scopeIds.isEmpty()) {
            myRoot.top(createSeverityRow());
            myRoot.center(options);

            if (singleToolId != null) {
                showOptions(myProfile.getToolDefaultState(singleToolId, myProject));
            }
        }
        else {
            List<ScopeSeverityRow> rows = buildScopeRows();
            MutableFlatDataModel<ScopeSeverityRow> model = FlatDataModel.of(rows);
            myTableModel = model;

            TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
            split.setFirstComponent(LabeledLayout.create(InspectionLocalize.inspectionSettingsSeverityByScope(), createScopeTable(model)));
            split.setSecondComponent(options);
            split.setProportion(TABLE_PROPORTION);
            myRoot.center(split);

            ScopeSeverityRow first = rows.get(0);
            showOptions(first.singleState());
        }

        if (!anyEnabled) {
            myRoot.setEnabledRecursive(false);
        }
    }

    @RequiredUIAccess
    private Component createSeverityRow() {
        SeverityRegistrarImpl registrar = InspectionSeverities.getOwnRegistrar(myProfile);

        List<ScopeToolState> defaultStates = new ArrayList<>();
        for (String toolId : myToolIds) {
            defaultStates.add(myProfile.getToolDefaultState(toolId, myProject));
        }
        HighlightSeverity current = InspectionSeverities.getSeverity(defaultStates);

        List<HighlightSeverity> severities = new ArrayList<>(InspectionSeverities.getSeverities(registrar));
        if (current == InspectionSeverities.MIXED_FAKE_SEVERITY) {
            severities.add(0, current);
        }

        ComboBox<HighlightSeverity> severityBox = ComboBox.create(severities);
        severityBox.setRenderer((presentation, item) -> renderSeverity(presentation, item.getValue(), false));
        if (current != null) {
            severityBox.setValue(current);
        }
        severityBox.addValueListener(event -> {
            HighlightSeverity severity = event.getValue();
            if (severity != null && severity != InspectionSeverities.MIXED_FAKE_SEVERITY) {
                setDefaultSeverity(severity);
            }
        });

        Button editButton = Button.create(LocalizeValue.empty());
        editButton.setIcon(PlatformIconGroup.actionsEdit());
        editButton.addStyle(ButtonStyle.INPLACE);
        editButton.setToolTipText(InspectionLocalize.inspectionSettingsEditSeverities());
        editButton.addClickListener(event -> {
            UIAccess uiAccess = UIAccess.current();
            SeverityEditorDialogDescriptor descriptor = new SeverityEditorDialogDescriptor(registrar, severityBox.getValue());
            myProject.getApplication().getInstance(DialogService.class).build(severityBox, descriptor).showAsync().whenCompleteAsync((value, error) -> {
                if (value instanceof SeverityEditorResult result && result.severity() != null) {
                    setDefaultSeverity(result.severity());
                }
                myStructureChanged.run();
            }, uiAccess);
        });
        severityBox.setSuffixComponent(editButton);

        LocalizeValue allScopes = InspectionLocalize.inspectionSettingsScopeAll();
        ComboBoxWithCustomPopup<LocalizeValue> scopeBox = ComboBoxWithCustomPopup.create(FlatDataModel.of(List.of(allScopes)));
        scopeBox.setRenderer((presentation, item) -> {
            LocalizeValue value = item.getValue();
            if (value != null) {
                presentation.append(value);
            }
        });
        scopeBox.setValue(allScopes, false);
        scopeBox.addPopupHandler(onClosed -> createAddScopePopup(scopeBox, Set.of(), onClosed));

        HorizontalLayout row = HorizontalLayout.create(Space.SMALL);
        row.add(Label.create(InspectionLocalize.inspectionSettingsSeverityLabel()));
        row.add(severityBox);
        row.add(scopeBox);
        return row;
    }

    @RequiredUIAccess
    private void setDefaultSeverity(HighlightSeverity severity) {
        HighlightDisplayLevel level = HighlightDisplayLevel.find(severity);
        for (String toolId : myToolIds) {
            myProfile.setErrorLevel(getKey(toolId), level, null, myProject);
        }
        myToolsChanged.accept(myToolIds);
    }

    @RequiredUIAccess
    private Component createScopeTable(MutableFlatDataModel<ScopeSeverityRow> model) {
        Table<ScopeSeverityRow> table = Table.create(model);

        table.addColumn(LocalizeValue.empty(), ScopeSeverityRow::enabled)
            .setWidth(30)
            .setResizable(false)
            .setRenderer(ComponentItemRender.reusable(
                () -> TriStateCheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(item.getValue(), false)
            ))
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<ThreeState> createComponent(ScopeSeverityRow row) {
                    return TriStateCheckBox.create(LocalizeValue.empty(), row.enabled());
                }

                @RequiredUIAccess
                @Override
                public void commit(ScopeSeverityRow row, @Nullable ThreeState value) {
                    setScopeEnabled(row, value);
                }
            });

        table.addColumn(InspectionLocalize.inspectionSettingsScopeColumn(), ScopeSeverityRow::name)
            .setRenderer((presentation, item) -> {
                LocalizeValue name = item.getValue();
                if (name != null) {
                    presentation.append(name);
                }
            });

        table.addColumn(InspectionLocalize.inspectionSettingsSeverityColumn(), row -> row)
            .setRenderer((presentation, item) -> {
                ScopeSeverityRow row = item.getValue();
                if (row != null) {
                    renderSeverity(presentation, row.severity(), row.enabled() == ThreeState.NO);
                }
            })
            .setEditor(new TableItemEditor<>() {
                @Override
                public boolean isEditable(ScopeSeverityRow row) {
                    return row.severityEditable();
                }

                @RequiredUIAccess
                @Override
                public ValueComponent<ScopeSeverityRow> createComponent(ScopeSeverityRow row) {
                    List<ScopeSeverityRow> choices = new ArrayList<>();
                    for (HighlightSeverity severity : InspectionSeverities.getSeverities(InspectionSeverities.getOwnRegistrar(myProfile))) {
                        ScopeSeverityRow choice = withSeverity(row, severity);
                        choices.add(severity.equals(row.severity()) ? row : choice);
                    }
                    ComboBox<ScopeSeverityRow> box = ComboBox.create(choices);
                    box.setRenderer((presentation, item) -> {
                        ScopeSeverityRow value = item.getValue();
                        if (value != null) {
                            renderSeverity(presentation, value.severity(), false);
                        }
                    });
                    box.setValue(row);
                    return box;
                }

                @RequiredUIAccess
                @Override
                public void commit(ScopeSeverityRow row, @Nullable ScopeSeverityRow value) {
                    if (value != null && !value.severity().equals(row.severity())) {
                        setScopeSeverity(row, value.severity());
                    }
                }
            });

        table.addSelectListener(event -> {
            ScopeSeverityRow row = table.getSelectedItem();
            if (row != null) {
                showOptions(row.singleState());
            }
        });

        List<ScopeSeverityRow> rows = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            rows.add(model.get(i));
        }
        if (!rows.isEmpty()) {
            table.select(rows.get(0));
        }

        return ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .disableAction(EditAction.class)
            .addOrReplaceAction(new AddAction<>() {
                @RequiredUIAccess
                @Override
                protected void doAdd(AnActionEvent e) {
                    createAddScopePopup(table, Set.copyOf(myScopeIds), null).showUnderneathOf(e);
                }
            })
            .addOrReplaceAction(new RemoveAction<>() {
                @RequiredUIAccess
                @Override
                public void updateAtUI(AnActionEvent e) {
                    super.updateAtUI(e);
                    ScopeSeverityRow row = getSelectedValue(e);
                    if (row == null || row.isDefault()) {
                        e.getPresentation().setEnabled(false);
                    }
                }

                @RequiredUIAccess
                @Override
                protected void doRemove(ScopeSeverityRow row, AnActionEvent e) {
                    String scopeId = row.scopeId();
                    if (scopeId != null) {
                        myProfile.removeScopes(myToolIds, scopeId, myProject);
                        myStructureChanged.run();
                    }
                }
            })
            .addOrReplaceAction(new UpMoveAction<>() {
                @RequiredUIAccess
                @Override
                public void updateAtUI(AnActionEvent e) {
                    super.updateAtUI(e);
                    if (!canMove(getSelectedValue(e), -1)) {
                        e.getPresentation().setEnabled(false);
                    }
                }

                @RequiredUIAccess
                @Override
                protected void doUp(ScopeSeverityRow row, AnActionEvent e) {
                    moveScope(row, -1);
                }
            })
            .addOrReplaceAction(new DownMoveAction<>() {
                @RequiredUIAccess
                @Override
                public void updateAtUI(AnActionEvent e) {
                    super.updateAtUI(e);
                    if (!canMove(getSelectedValue(e), 1)) {
                        e.getPresentation().setEnabled(false);
                    }
                }

                @RequiredUIAccess
                @Override
                protected void doDown(ScopeSeverityRow row, AnActionEvent e) {
                    moveScope(row, 1);
                }
            })
            .build();
    }

    private boolean canMove(@Nullable ScopeSeverityRow row, int direction) {
        if (row == null || row.scopeId() == null) {
            return false;
        }
        int index = myScopeIds.indexOf(row.scopeId());
        int target = index + direction;
        return index >= 0 && target >= 0 && target < myScopeIds.size();
    }

    @RequiredUIAccess
    private void moveScope(ScopeSeverityRow row, int direction) {
        if (!canMove(row, direction)) {
            return;
        }

        String scopeId = Objects.requireNonNull(row.scopeId());
        String otherScopeId = myScopeIds.get(myScopeIds.indexOf(scopeId) + direction);

        List<String> order = new ArrayList<>(InspectionScopes.getOrderedScopeIds(myProfile, myProject));
        for (String id : myScopeIds) {
            if (!order.contains(id)) {
                order.add(id);
            }
        }

        int index = order.indexOf(scopeId);
        int otherIndex = order.indexOf(otherScopeId);
        Collections.swap(order, index, otherIndex);

        myProfile.setScopesOrder(order.toArray(String[]::new));
        myStructureChanged.run();
    }

    @RequiredUIAccess
    private void setScopeEnabled(ScopeSeverityRow row, @Nullable ThreeState value) {
        if (value == null || value == ThreeState.UNSURE || value == row.enabled()) {
            return;
        }

        NamedScope scope = row.scope();
        if (value == ThreeState.YES) {
            for (String toolId : myToolIds) {
                myProfile.enableTool(toolId, myProject);
            }
            if (row.isDefault()) {
                myProfile.enableToolsByDefault(myToolIds, myProject);
            }
            else if (scope != null) {
                myProfile.enableTools(myToolIds, scope, myProject);
            }
        }
        else if (row.isDefault()) {
            myProfile.disableToolByDefault(myToolIds, myProject);
        }
        else if (scope != null) {
            myProfile.disableTools(myToolIds, scope, myProject);
        }

        afterScopeRowChange(row);
    }

    @RequiredUIAccess
    private void setScopeSeverity(ScopeSeverityRow row, HighlightSeverity severity) {
        List<HighlightDisplayKey> keys = new ArrayList<>(myToolIds.size());
        for (String toolId : myToolIds) {
            keys.add(getKey(toolId));
        }
        myProfile.setErrorLevel(keys, HighlightDisplayLevel.find(severity), row.scopeId(), myProject);

        afterScopeRowChange(row);
    }

    @RequiredUIAccess
    private void afterScopeRowChange(ScopeSeverityRow row) {
        myToolsChanged.accept(myToolIds);

        MutableFlatDataModel<ScopeSeverityRow> model = myTableModel;
        if (model == null) {
            return;
        }

        UIAccess.current().execute(() -> {
            if (myTableModel != model) {
                return;
            }

            List<ScopeSeverityRow> rows = buildScopeRows();
            model.replaceAll(rows);
            for (ScopeSeverityRow newRow : rows) {
                if (Objects.equals(newRow.scopeId(), row.scopeId())) {
                    showOptions(newRow.singleState());
                    break;
                }
            }
        });
    }

    private List<ScopeSeverityRow> buildScopeRows() {
        List<ScopeSeverityRow> rows = new ArrayList<>(myScopeIds.size() + 1);
        for (String scopeId : myScopeIds) {
            rows.add(buildScopeRow(scopeId));
        }
        rows.add(buildScopeRow(null));
        return rows;
    }

    private ScopeSeverityRow buildScopeRow(@Nullable String scopeId) {
        List<ScopeToolState> existed = new ArrayList<>();
        int missing = 0;
        for (String toolId : myToolIds) {
            ScopeToolState state = findState(toolId, scopeId);
            if (state != null) {
                existed.add(state);
            }
            else {
                missing++;
            }
        }

        Boolean previous = null;
        boolean mixed = false;
        for (ScopeToolState state : existed) {
            if (previous == null) {
                previous = state.isEnabled();
            }
            else if (previous != state.isEnabled()) {
                mixed = true;
            }
        }

        ThreeState enabled;
        if (mixed || previous == null || (missing > 0 && previous)) {
            enabled = ThreeState.UNSURE;
        }
        else {
            enabled = previous ? ThreeState.YES : ThreeState.NO;
        }

        HighlightSeverity severity = missing > 0 ? InspectionSeverities.MIXED_FAKE_SEVERITY : InspectionSeverities.getSeverity(existed);
        if (severity == null) {
            severity = InspectionSeverities.MIXED_FAKE_SEVERITY;
        }

        NamedScope scope = existed.isEmpty() ? null : existed.get(0).getScope(myProject);

        LocalizeValue name;
        if (scopeId == null) {
            name = InspectionLocalize.inspectionSettingsScopeEverywhereElse();
        }
        else if (scope != null) {
            name = scope.getPresentableName();
        }
        else {
            name = LocalizeValue.of(scopeId);
        }

        boolean severityEditable = enabled != ThreeState.NO && missing == 0;
        ScopeToolState singleState = myToolIds.size() == 1 && existed.size() == 1 ? existed.get(0) : null;
        return new ScopeSeverityRow(scopeId, name, scope, enabled, severity, severityEditable, singleState);
    }

    private @Nullable ScopeToolState findState(String toolId, @Nullable String scopeId) {
        if (scopeId == null) {
            return myProfile.getToolDefaultState(toolId, myProject);
        }

        for (ScopeToolState state : myProfile.getNonDefaultTools(toolId, myProject)) {
            if (scopeId.equals(state.getScopeId())) {
                return state;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void showOptions(@Nullable ScopeToolState state) {
        DockLayout holder = myOptionsHolder;
        if (holder == null) {
            return;
        }

        holder.removeAll();
        if (state == null || !state.isEnabled()) {
            return;
        }

        Component component = state.getConfigurablePanel(myDisposable);
        if (component == null) {
            return;
        }

        myOptionsShown.accept(state.getTool().getShortName());
        holder.center(LabeledLayout.create(InspectionLocalize.inspectionSettingsOptionsTitle(), ScrollableLayout.create(component)));
    }

    @RequiredUIAccess
    private ListPopup createAddScopePopup(Component owner, Set<String> excluded, @Nullable Runnable onClosed) {
        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        addScopeActions(group, InspectionScopes.getPredefinedScopes(myProject), excluded);
        group.addSeparator();
        addScopeActions(group, InspectionScopes.getCustomScopes(myProject), excluded);

        return JBPopupFactory.getInstance().createActionGroupPopup(
            InspectionLocalize.inspectionSettingsScopePopupTitle().get(),
            group.build(),
            DataManager.getInstance().getDataContext(owner),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false,
            onClosed,
            -1
        );
    }

    private void addScopeActions(ActionGroup.Builder group, List<NamedScope> scopes, Set<String> excluded) {
        for (NamedScope scope : scopes) {
            if (excluded.contains(scope.getScopeId())) {
                continue;
            }

            group.add(DumbAwareAction.create(scope.getPresentableName(), e -> addScope(scope)));
        }
    }

    @RequiredUIAccess
    private void addScope(NamedScope scope) {
        for (String toolId : myToolIds) {
            ToolsImpl tools = myProfile.getTools(toolId, myProject);
            ScopeToolState defaultState = tools.getDefaultState();
            myProfile.addScope(defaultState.getTool().createCopy(), scope, defaultState.getLevel(), true, myProject);
        }
        myToolsChanged.accept(myToolIds);
        myStructureChanged.run();
    }

    private HighlightDisplayKey getKey(String toolId) {
        return myProfile.getToolDefaultState(toolId, myProject).getTool().getHighlightDisplayKey();
    }

    private static ScopeSeverityRow withSeverity(ScopeSeverityRow row, HighlightSeverity severity) {
        return new ScopeSeverityRow(row.scopeId(), row.name(), row.scope(), row.enabled(), severity, row.severityEditable(), row.singleState());
    }

    private static void renderSeverity(TextItemPresentation presentation, @Nullable HighlightSeverity severity, boolean disabled) {
        if (severity == null) {
            return;
        }
        presentation.withIcon(InspectionSeverities.getIcon(severity));
        presentation.append(InspectionSeverities.renderSeverity(severity), disabled ? TextAttribute.GRAYED : TextAttribute.REGULAR);
    }
}
