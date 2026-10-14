/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath;

import consulo.content.bundle.Sdk;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.openapi.module.impl.scopes.LibraryScope;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.actions.AnalyzeDependenciesOnSpecifiedTargetHandler;
import consulo.ide.impl.roots.ui.configuration.classpath.AddModuleDependencyListPopupStep;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.ide.setting.module.AddModuleDependencyContext;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.ClasspathTableItem;
import consulo.ide.setting.module.LibraryTableModifiableModelProvider;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleSourceOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.MessageBoxes;
import consulo.ui.RenderItem;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.UnifiedColoredTextContainer;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClasspathPanelImpl implements ClasspathPanel {
    private static final Key<CheckBox> EXPORT_CHECK_BOX = Key.create("ClasspathPanelImpl.exportCheckBox");

    private static final int EXPORT_COLUMN_WIDTH = 60;
    private static final int SCOPE_COLUMN_WIDTH = 100;

    private final ModuleConfigurationState myState;
    private final MutableFlatDataModel<ClasspathTableItem<?>> myModel;
    private final Map<ClasspathTableItem<?>, ClasspathTableItemPresentation> myPresentations = new ConcurrentHashMap<>();
    private final Table<ClasspathTableItem<?>> myTable;
    private final Component myComponent;
    private final NavigateAction myNavigateAction = new NavigateAction();

    @RequiredUIAccess
    public ClasspathPanelImpl(ModuleConfigurationState state, Disposable uiDisposable) {
        myState = state;
        myModel = FlatDataModel.of(createItems());

        Table<ClasspathTableItem<?>> table = Table.create(myModel);
        table.setAllowMultipleSelect(true);
        table.setSpeedSearchConverter(this::getSearchText);

        table.addColumn(ProjectLocalize.modulesOrderExportExportColumn(), ClasspathPanelImpl::getExportValue)
            .setWidth(EXPORT_COLUMN_WIDTH)
            .setResizable(false)
            .setRenderer(ComponentItemRenderer.reusable(ClasspathPanelImpl::createExportRenderer, ClasspathPanelImpl::bindExportRenderer))
            .setEditor(new ExportColumnEditor());

        table.addColumn(LocalizeValue.empty(), item -> item)
            .setRenderer((presentation, value) -> renderItem(presentation, value.getValue()));

        table.addColumn(ProjectLocalize.modulesOrderExportScopeColumn(), ClasspathPanelImpl::getScopeValue)
            .setWidth(SCOPE_COLUMN_WIDTH)
            .setResizable(false)
            .setRenderer((presentation, value) -> renderScope(presentation, value.getValue()))
            .setEditor(new ScopeColumnEditor());

        table.addDoubleClickListener(event -> navigate(createActionEvent(), true));
        table.addContextMenuListener(this::showContextMenu);
        myTable = table;

        AnAction editSourceAction = ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE);
        if (editSourceAction != null) {
            myNavigateAction.registerCustomShortcutSet(editSourceAction.getShortcutSet(), table, uiDisposable);
        }
        new ToggleExportAction().registerCustomShortcutSet(CustomShortcutSet.fromString("SPACE"), table, uiDisposable);

        myComponent = ToolbarDecoratorBuilderFactory.getInstance()
            .create(table)
            .addOrReplaceAction(new AddDependencyAction())
            .addOrReplaceAction(new RemoveDependencyAction())
            .addOrReplaceAction(new EditDependencyAction())
            .addOrReplaceAction(new MoveUpDependencyAction())
            .addOrReplaceAction(new MoveDownDependencyAction())
            .build();

        if (myModel.getSize() > 0) {
            table.select(myModel.get(0));
        }
    }

    private List<ClasspathTableItem<?>> createItems() {
        List<ClasspathTableItem<?>> items = new ArrayList<>();
        for (OrderEntry orderEntry : getRootModel().getOrderEntries()) {
            ClasspathTableItem<?> item = ClasspathTableItem.createItem(
                orderEntry,
                myState.getProject(),
                myState.getModulesConfigurator(),
                myState.getLibrariesConfigurator()
            );
            myPresentations.put(item, createPresentation(item));
            items.add(item);
        }
        return items;
    }

    private static ClasspathTableItemPresentation createPresentation(ClasspathTableItem<?> item) {
        UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
        OrderEntryAppearanceService.getInstance().getRendererForOrderEntry(item.getEntry()).accept(text);
        return new ClasspathTableItemPresentation(text, item.getTooltipText());
    }

    private String getSearchText(ClasspathTableItem<?> item) {
        ClasspathTableItemPresentation presentation = myPresentations.get(item);
        return presentation == null ? item.getEntry().getPresentableName() : presentation.getSearchText();
    }

    private void renderItem(TextItemPresentation presentation, @Nullable ClasspathTableItem<?> item) {
        ClasspathTableItemPresentation itemPresentation = item == null ? null : myPresentations.get(item);
        if (itemPresentation != null) {
            itemPresentation.render(presentation);
        }
    }

    private static void renderScope(TextItemPresentation presentation, @Nullable DependencyScope scope) {
        if (scope != null) {
            presentation.append(scope.getDisplayName());
        }
    }

    private static @Nullable Boolean getExportValue(ClasspathTableItem<?> item) {
        return item.isExportable() ? item.isExported() : null;
    }

    private static @Nullable DependencyScope getScopeValue(ClasspathTableItem<?> item) {
        return item.getScope();
    }

    @RequiredUIAccess
    private static DockLayout createExportRenderer() {
        CheckBox checkBox = CheckBox.create(LocalizeValue.empty());
        DockLayout layout = DockLayout.create(Space.NONE);
        layout.center(checkBox);
        layout.putUserData(EXPORT_CHECK_BOX, checkBox);
        return layout;
    }

    @RequiredUIAccess
    private static void bindExportRenderer(DockLayout layout, RenderItem<Boolean> item) {
        CheckBox checkBox = layout.getUserData(EXPORT_CHECK_BOX);
        if (checkBox == null) {
            return;
        }

        Boolean value = item.getValue();
        checkBox.setVisible(value != null);
        checkBox.setValue(Boolean.TRUE.equals(value), false);
    }

    @RequiredUIAccess
    private AnActionEvent createActionEvent() {
        DataContext dataContext = DataManager.getInstance().getDataContext(myTable);
        return AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext);
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        ActionGroup group = ActionGroup.newImmutableBuilder()
            .add(new EditContextAction())
            .add(new RemoveContextAction())
            .add(myNavigateAction)
            .add(new AnalyzeDependencyAction())
            .build();

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
        menu.setTargetComponent(myTable);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    @Override
    @RequiredUIAccess
    public @Nullable OrderEntry getSelectedEntry() {
        List<ClasspathTableItem<?>> selected = myTable.getSelectedItems();
        if (selected.size() != 1) {
            return null;
        }
        return selected.get(0).getEntry();
    }

    @Override
    @RequiredUIAccess
    public void navigate(AnActionEvent e, boolean openLibraryEditor) {
        OrderEntry entry = getSelectedEntry();
        ProjectStructureSelector selector = e.getData(ProjectStructureSelector.KEY);
        if (entry == null || selector == null) {
            return;
        }

        if (entry instanceof ModuleOrderEntry moduleOrderEntry) {
            Module module = moduleOrderEntry.getModule();
            if (module != null) {
                selector.select(module.getName(), null, true);
            }
        }
        else if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
            if (!openLibraryEditor) {
                selector.select(libraryOrderEntry, true);
            }
            else {
                doEdit();
            }
        }
        else if (entry instanceof ModuleExtensionWithSdkOrderEntry withSdkOrderEntry) {
            Sdk sdk = withSdkOrderEntry.getSdk();
            if (sdk != null) {
                selector.select(sdk, true);
            }
        }
    }

    @RequiredUIAccess
    private void showAddPopup(AnActionEvent event) {
        SequencedMap<AddModuleDependencyActionProvider, AddModuleDependencyContext> contexts = new LinkedHashMap<>();

        AddModuleDependencyActionProvider.EP.forEachExtensionSafe(
            getProject().getApplication(),
            provider -> {
                @SuppressWarnings("unchecked")
                AddModuleDependencyContext context =
                    provider.createContext(this, myState.getModulesConfigurator(), myState.getLibrariesConfigurator());
                if (provider.isAvailable(context)) {
                    contexts.put(provider, context);
                }
            }
        );

        if (contexts.isEmpty()) {
            return;
        }

        if (contexts.size() == 1) {
            AddModuleDependencyListPopupStep.providerSelected(contexts.sequencedEntrySet().getFirst());
        }
        else {
            AddModuleDependencyListPopupStep step = new AddModuleDependencyListPopupStep(getRootModel(), contexts.entrySet());

            ListPopup popup = JBPopupFactory.getInstance().createListPopup(myState.getProject(), step);
            popup.showUnderneathOf(event);
        }
    }

    @RequiredUIAccess
    private boolean isRemoveActionEnabled() {
        List<ClasspathTableItem<?>> selected = myTable.getSelectedItems();
        if (selected.isEmpty()) {
            return false;
        }

        for (ClasspathTableItem<?> item : selected) {
            if (!item.isRemovable()) {
                return false;
            }
        }
        return true;
    }

    @RequiredUIAccess
    private @Nullable ClasspathTableItem<?> getEditableItem() {
        List<ClasspathTableItem<?>> selected = myTable.getSelectedItems();
        if (selected.size() != 1) {
            return null;
        }

        ClasspathTableItem<?> item = selected.get(0);
        return item.isEditable() ? item : null;
    }

    @RequiredUIAccess
    private void doEdit() {
        ClasspathTableItem<?> item = getEditableItem();
        if (item == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        item.doEdit(this).whenCompleteAsync((result, error) -> {
            if (error == null) {
                refreshItem(item);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void refreshItem(ClasspathTableItem<?> item) {
        if (myModel.indexOf(item) < 0) {
            return;
        }

        myPresentations.put(item, createPresentation(item));
        myModel.update(item);
    }

    @RequiredUIAccess
    private void removeSelectedItems() {
        List<ClasspathTableItem<?>> selected = List.copyOf(myTable.getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        int firstIndex = myModel.getSize();
        for (ClasspathTableItem<?> item : selected) {
            firstIndex = Math.min(firstIndex, myModel.indexOf(item));
        }

        for (ClasspathTableItem<?> item : selected) {
            getRootModel().removeOrderEntry(item.getEntry());
            myModel.remove(item);
            myPresentations.remove(item);
        }

        int size = myModel.getSize();
        if (size > 0) {
            ClasspathTableItem<?> toSelect = myModel.get(Math.max(0, Math.min(firstIndex, size - 1)));
            myTable.deselectAll();
            myTable.select(toSelect);
            myTable.scrollTo(toSelect);
        }
    }

    @Override
    public LibraryTableModifiableModelProvider getModifiableModelProvider(String tableLevel) {
        if (LibraryEx.MODULE_LEVEL.equals(tableLevel)) {
            LibraryTable moduleLibraryTable = getRootModel().getModuleLibraryTable();
            return moduleLibraryTable::getModifiableModel;
        }
        else {
            return myState.getLibrariesConfigurator().createModifiableModelProvider(tableLevel);
        }
    }

    @Override
    @RequiredUIAccess
    public void addItems(List<ClasspathTableItem<?>> toAdd) {
        if (toAdd.isEmpty()) {
            return;
        }

        for (ClasspathTableItem<?> item : toAdd) {
            myPresentations.put(item, createPresentation(item));
            myModel.add(item);
        }

        myTable.deselectAll();
        for (ClasspathTableItem<?> item : toAdd) {
            myTable.select(item);
        }
        myTable.scrollTo(toAdd.get(toAdd.size() - 1));
    }

    @Override
    public ModifiableRootModel getRootModel() {
        return myState.getRootModel();
    }

    @Override
    public Project getProject() {
        return myState.getProject();
    }

    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public ModuleConfigurationState getModuleConfigurationState() {
        return myState;
    }

    @RequiredUIAccess
    private void moveSelectedRows(int increment) {
        List<ClasspathTableItem<?>> selected = List.copyOf(myTable.getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        Set<ClasspathTableItem<?>> selectedSet = Collections.newSetFromMap(new IdentityHashMap<>());
        selectedSet.addAll(selected);

        List<ClasspathTableItem<?>> items = getItems();
        int size = items.size();
        if (increment < 0) {
            for (int index = 1; index < size; index++) {
                if (selectedSet.contains(items.get(index)) && !selectedSet.contains(items.get(index - 1))) {
                    Collections.swap(items, index, index - 1);
                }
            }
        }
        else {
            for (int index = size - 2; index >= 0; index--) {
                if (selectedSet.contains(items.get(index)) && !selectedSet.contains(items.get(index + 1))) {
                    Collections.swap(items, index, index + 1);
                }
            }
        }

        List<OrderEntry> entries = new ArrayList<>(size);
        for (ClasspathTableItem<?> item : items) {
            entries.add(item.getEntry());
        }
        getRootModel().rearrangeOrderEntries(entries.toArray(OrderEntry[]::new));

        myModel.replaceAll(items);

        myTable.deselectAll();
        for (ClasspathTableItem<?> item : selected) {
            myTable.select(item);
        }
        myTable.scrollTo(increment < 0 ? selected.get(0) : selected.get(selected.size() - 1));
    }

    private List<ClasspathTableItem<?>> getItems() {
        List<ClasspathTableItem<?>> items = new ArrayList<>(myModel.getSize());
        for (ClasspathTableItem<?> item : myModel) {
            items.add(item);
        }
        return items;
    }

    @RequiredUIAccess
    public void selectOrderEntry(OrderEntry entry) {
        for (ClasspathTableItem<?> item : myModel) {
            if (entry.getPresentableName().equals(item.getEntry().getPresentableName())) {
                myTable.deselectAll();
                myTable.select(item);
                myTable.scrollTo(item);
                return;
            }
        }
    }

    @RequiredUIAccess
    public void rootsChanged() {
        forceInitFromModel();
    }

    @RequiredUIAccess
    public void initFromModel() {
        forceInitFromModel();
    }

    @RequiredUIAccess
    public void forceInitFromModel() {
        List<Integer> selectedIndexes = new ArrayList<>();
        for (ClasspathTableItem<?> item : myTable.getSelectedItems()) {
            int index = myModel.indexOf(item);
            if (index >= 0) {
                selectedIndexes.add(index);
            }
        }

        myPresentations.clear();
        myModel.replaceAll(createItems());

        myTable.deselectAll();
        int size = myModel.getSize();
        for (int index : selectedIndexes) {
            if (index < size) {
                myTable.select(myModel.get(index));
            }
        }
    }

    @RequiredUIAccess
    private void toggleSelectedExport() {
        List<ClasspathTableItem<?>> selected = List.copyOf(myTable.getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        boolean allExported = true;
        for (ClasspathTableItem<?> item : selected) {
            if (!item.isExportable()) {
                return;
            }
            allExported &= item.isExported();
        }

        for (ClasspathTableItem<?> item : selected) {
            item.setExported(!allExported);
            myModel.update(item);
        }
    }

    private class ExportColumnEditor implements TableItemEditor<ClasspathTableItem<?>, Boolean> {
        @Override
        @RequiredUIAccess
        public ValueComponent<Boolean> createComponent(ClasspathTableItem<?> item) {
            return CheckBox.create(LocalizeValue.empty(), item.isExported());
        }

        @Override
        @RequiredUIAccess
        public void commit(ClasspathTableItem<?> item, @Nullable Boolean value) {
            item.setExported(Boolean.TRUE.equals(value));
            myModel.update(item);
        }

        @Override
        public boolean isEditable(ClasspathTableItem<?> item) {
            return item.isExportable();
        }
    }

    private class ScopeColumnEditor implements TableItemEditor<ClasspathTableItem<?>, DependencyScope> {
        @Override
        @RequiredUIAccess
        public ValueComponent<DependencyScope> createComponent(ClasspathTableItem<?> item) {
            return ComboBox.<DependencyScope>builder().fillByEnum(DependencyScope.class, DependencyScope::getDisplayName).build();
        }

        @Override
        @RequiredUIAccess
        public void commit(ClasspathTableItem<?> item, @Nullable DependencyScope value) {
            if (value != null) {
                item.setScope(value);
                myModel.update(item);
            }
        }

        @Override
        public boolean isEditable(ClasspathTableItem<?> item) {
            return item.getScope() != null;
        }
    }

    private class AddDependencyAction extends AddAction<ClasspathTableItem<?>> {
        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
            showAddPopup(e);
        }
    }

    private class RemoveDependencyAction extends RemoveAction<ClasspathTableItem<?>> {
        @Override
        @RequiredUIAccess
        public void updateAtUI(AnActionEvent e) {
            super.updateAtUI(e);
            if (!isRemoveActionEnabled()) {
                e.getPresentation().setEnabled(false);
            }
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (isRemoveActionEnabled()) {
                removeSelectedItems();
            }
        }
    }

    private class EditDependencyAction extends EditAction<ClasspathTableItem<?>> {
        @Override
        @RequiredUIAccess
        public void updateAtUI(AnActionEvent e) {
            super.updateAtUI(e);
            if (getEditableItem() == null) {
                e.getPresentation().setEnabled(false);
            }
        }

        @Override
        @RequiredUIAccess
        protected void doEdit(ClasspathTableItem<?> value, AnActionEvent e) {
            ClasspathPanelImpl.this.doEdit();
        }
    }

    private class MoveUpDependencyAction extends UpMoveAction<ClasspathTableItem<?>> {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            moveSelectedRows(-1);
        }
    }

    private class MoveDownDependencyAction extends DownMoveAction<ClasspathTableItem<?>> {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            moveSelectedRows(1);
        }
    }

    private class EditContextAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private EditContextAction() {
            super(ProjectLocalize.moduleClasspathButtonEdit(), LocalizeValue.empty(), PlatformIconGroup.actionsEdit());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            doEdit();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getEditableItem() != null);
        }
    }

    private class RemoveContextAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private RemoveContextAction() {
            super(CommonLocalize.buttonRemove(), LocalizeValue.empty(), PlatformIconGroup.generalRemove());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (isRemoveActionEnabled()) {
                removeSelectedItems();
            }
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(isRemoveActionEnabled());
        }
    }

    private class NavigateAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private NavigateAction() {
            super(ProjectLocalize.classpathPanelNavigateActionText());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            navigate(e, false);
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            OrderEntry entry = getSelectedEntry();
            e.getPresentation().setEnabled(entry != null && entry.isValid() && !(entry instanceof ModuleSourceOrderEntry));
        }
    }

    private class ToggleExportAction extends DumbAwareAction {
        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            toggleSelectedExport();
        }
    }

    private class AnalyzeDependencyAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private AnalyzeDependencyAction() {
            super(ProjectLocalize.classpathPanelAnalyzeThisDependencyActionText());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            GlobalSearchScope targetScope = getTargetScope(getSelectedEntry());
            if (targetScope == null) {
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            new AnalyzeDependenciesOnSpecifiedTargetHandler(
                getProject(),
                new AnalysisScope(getRootModel().getModule()),
                targetScope
            ) {
                @Override
                protected boolean canStartInBackground() {
                    return false;
                }

                @Override
                @RequiredUIAccess
                protected boolean shouldShowDependenciesPanel(List<DependenciesBuilder> builders) {
                    for (DependenciesBuilder builder : builders) {
                        for (Set<PsiFile> files : builder.getDependencies().values()) {
                            if (!files.isEmpty()) {
                                MessageBoxes.okInfo(ProjectLocalize.classpathPanelAnalyzeDependenciesCollected(ToolWindowId.DEPENDENCIES))
                                    .title(FindLocalize.findPointcutApplicationsNotFoundTitle())
                                    .showAsync(myTable);
                                return true;
                            }
                        }
                    }

                    MessageBoxes.okCancel()
                        .asWarning()
                        .title(CommonLocalize.titleWarning())
                        .text(ProjectLocalize.classpathPanelAnalyzeNoDependenciesFound())
                        .showAsync(myTable)
                        .whenCompleteAsync((remove, error) -> {
                            if (error == null && Boolean.TRUE.equals(remove)) {
                                removeSelectedItems();
                            }
                        }, uiAccess);
                    return false;
                }
            }.analyze();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            OrderEntry entry = getSelectedEntry();
            Presentation presentation = e.getPresentation();
            presentation.setVisible(
                entry instanceof ModuleOrderEntry moduleOrderEntry && moduleOrderEntry.getModule() != null
                    || entry instanceof LibraryOrderEntry libraryOrderEntry && libraryOrderEntry.getLibrary() != null
            );
        }

        private @Nullable GlobalSearchScope getTargetScope(@Nullable OrderEntry entry) {
            if (entry instanceof ModuleOrderEntry moduleOrderEntry) {
                Module module = moduleOrderEntry.getModule();
                return module == null ? null : GlobalSearchScope.moduleScope(module);
            }
            if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                Library library = libraryOrderEntry.getLibrary();
                return library == null ? null : new LibraryScope(getProject(), library);
            }
            return null;
        }
    }
}
