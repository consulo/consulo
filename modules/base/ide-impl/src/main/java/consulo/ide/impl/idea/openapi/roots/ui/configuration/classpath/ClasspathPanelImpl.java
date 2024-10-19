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
import consulo.dataContext.DataManager;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.module.impl.scopes.LibraryScope;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.FindUsagesInProjectStructureActionBase;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.LibraryProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ModuleProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.SdkProjectStructureElement;
import consulo.ide.impl.idea.packageDependencies.DependenciesBuilder;
import consulo.ide.impl.idea.packageDependencies.actions.AnalyzeDependenciesOnSpecifiedTargetHandler;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.roots.ui.configuration.classpath.AddModuleDependencyListPopupStep;
import consulo.ide.setting.ProjectStructureSelector;
import consulo.ide.setting.module.*;
import consulo.ide.ui.OrderEntryAppearanceService;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.*;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ColoredStringBuilder;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.table.ComboBoxTableRenderer;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class ClasspathPanelImpl extends JPanel implements ClasspathPanel {
    private static final Logger LOG = Logger.getInstance(ClasspathPanelImpl.class);
    private final JBTable myEntryTable;
    private final ClasspathTableModel myModel;
    private AnAction myEditButton;
    private final ModuleConfigurationState myState;

    public ClasspathPanelImpl(ModuleConfigurationState state) {
        super(new BorderLayout());

        myState = state;
        myModel = new ClasspathTableModel(state);
        myEntryTable = new JBTable(myModel);
        myEntryTable.setShowGrid(false);
        myEntryTable.setDragEnabled(false);
        myEntryTable.setIntercellSpacing(new Dimension(0, 0));

        myEntryTable.setDefaultRenderer(ClasspathTableItem.class, new TableItemRenderer());
        myEntryTable.setDefaultRenderer(Boolean.class, new ExportFlagRenderer(myEntryTable.getDefaultRenderer(Boolean.class)));

        JComboBox<DependencyScope> scopeEditor = new ComboBox<>(new EnumComboBoxModel<>(DependencyScope.class));
        myEntryTable.setDefaultEditor(DependencyScope.class, new DefaultCellEditor(scopeEditor));
        myEntryTable.setDefaultRenderer(DependencyScope.class, new ComboBoxTableRenderer<>(DependencyScope.values()) {
            @Override
            protected String getTextFor(@Nonnull final DependencyScope value) {
                return value.getDisplayName();
            }
        });

        myEntryTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        new SpeedSearchBase<>(myEntryTable) {
            @Override
            public int getSelectedIndex() {
                return myEntryTable.getSelectedRow();
            }

            @Override
            protected int convertIndexToModel(int viewIndex) {
                return myEntryTable.convertRowIndexToModel(viewIndex);
            }

            @Nonnull
            @Override
            public Object[] getAllElements() {
                final int count = myModel.getRowCount();
                Object[] elements = new Object[count];
                for (int idx = 0; idx < count; idx++) {
                    elements[idx] = myModel.getItemAt(idx);
                }
                return elements;
            }

            @Override
            public String getElementText(Object element) {
                Consumer<ColoredTextContainer> render = getRender((ClasspathTableItem<?>)element);
                ColoredStringBuilder b = new ColoredStringBuilder();
                render.accept(b);
                return b.toString();
            }

            @Override
            public void selectElement(Object element, String selectedText) {
                final int count = myModel.getRowCount();
                for (int row = 0; row < count; row++) {
                    if (element.equals(myModel.getItemAt(row))) {
                        final int viewRow = myEntryTable.convertRowIndexToView(row);
                        myEntryTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                        TableUtil.scrollSelectionToVisible(myEntryTable);
                        break;
                    }
                }
            }
        };
        setFixedColumnWidth(ClasspathTableModel.EXPORT_COLUMN, ClasspathTableModel.EXPORT_COLUMN_NAME);
        // leave space for combobox border
        setFixedColumnWidth(
            ClasspathTableModel.SCOPE_COLUMN,
            DependencyScope.COMPILE.toString() + "     "
        );

        myEntryTable.registerKeyboardAction(
            e -> {
                final int[] selectedRows = myEntryTable.getSelectedRows();
                boolean currentlyMarked = true;
                for (final int selectedRow : selectedRows) {
                    final ClasspathTableItem<?> item = myModel.getItemAt(myEntryTable.convertRowIndexToModel(selectedRow));
                    if (selectedRow < 0 || !item.isExportable()) {
                        return;
                    }
                    currentlyMarked &= item.isExported();
                }
                for (final int selectedRow : selectedRows) {
                    myModel.getItemAt(myEntryTable.convertRowIndexToModel(selectedRow)).setExported(!currentlyMarked);
                }
                myModel.fireTableDataChanged();
                TableUtil.selectRows(myEntryTable, selectedRows);
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            WHEN_FOCUSED
        );

        myEditButton = new DumbAwareAction(
            ProjectLocalize.moduleClasspathButtonEdit(),
            LocalizeValue.empty(),
            IconUtil.getEditIcon()
        ) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                doEdit();
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                final int[] selectedRows = myEntryTable.getSelectedRows();
                ClasspathTableItem<?> selectedItem = selectedRows.length == 1 ? myModel.getItemAt(selectedRows[0]) : null;
                e.getPresentation().setEnabled(selectedItem != null && selectedItem.isEditable());
            }
        };
        add(createTableWithButtons(), BorderLayout.CENTER);

        if (myEntryTable.getRowCount() > 0) {
            myEntryTable.getSelectionModel().setSelectionInterval(0, 0);
        }

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                navigate(
                    AnActionEvent.createFromInputEvent(
                        e,
                        ActionPlaces.UNKNOWN,
                        null,
                        DataManager.getInstance().getDataContext(myEntryTable)
                    ),
                    true
                );
                return true;
            }
        }.installOn(myEntryTable);

        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
        final AnAction navigateAction = new AnAction(ProjectLocalize.classpathPanelNavigateActionText()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                navigate(e, false);
            }

            @Override
            @RequiredUIAccess
            public void update(AnActionEvent e) {
                final Presentation presentation = e.getPresentation();
                presentation.setEnabled(false);
                final OrderEntry entry = getSelectedEntry();
                if (entry != null && entry.isValid()) {
                    if (!(entry instanceof ModuleSourceOrderEntry)) {
                        presentation.setEnabled(true);
                    }
                }
            }
        };
        navigateAction.registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet(),
            myEntryTable
        );
        actionGroup.add(myEditButton);
        actionGroup.add(new DumbAwareAction(CommonLocalize.buttonRemove(), LocalizeValue.empty(), IconUtil.getRemoveIcon()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                removeSelectedItems(TableUtil.removeSelectedItems(myEntryTable));
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                e.getPresentation().setEnabled(isRemoveActionEnabled());
            }
        });
        actionGroup.add(navigateAction);
        actionGroup.add(new MyFindUsagesAction());
        actionGroup.add(new AnalyzeDependencyAction());

        PopupHandler.installPopupHandler(
            myEntryTable,
            actionGroup.build(),
            ActionPlaces.UNKNOWN,
            ActionManager.getInstance()
        );
    }

    @Override
    @Nullable
    public OrderEntry getSelectedEntry() {
        if (myEntryTable.getSelectedRowCount() != 1) {
            return null;
        }
        return myModel.getItemAt(myEntryTable.getSelectedRow()).getEntry();
    }

    private void setFixedColumnWidth(final int columnIndex, final String textToMeasure) {
        final TableColumn column = myEntryTable.getTableHeader().getColumnModel().getColumn(columnIndex);
        column.setResizable(false);
        column.setMaxWidth(column.getPreferredWidth());
    }

    @Override
    @RequiredUIAccess
    public void navigate(AnActionEvent e, boolean openLibraryEditor) {
        final OrderEntry entry = getSelectedEntry();
        ProjectStructureSelector selector = e.getData(ProjectStructureSelector.KEY);

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
                myEditButton.actionPerformed(ActionUtil.createEmptyEvent());
            }
        }
        else if (entry instanceof ModuleExtensionWithSdkOrderEntry withSdkOrderEntry) {
            Sdk sdk = withSdkOrderEntry.getSdk();
            if (sdk != null) {
                selector.select(sdk, true);
            }
        }
    }

    private JComponent createTableWithButtons() {
        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myEntryTable);
        decorator.setPanelBorder(JBUI.Borders.empty());
        decorator.setAddAction(this::showAddPopup);
        decorator.setRemoveAction(button -> removeSelectedItems(TableUtil.removeSelectedItems(myEntryTable)));
        decorator.setRemoveActionUpdater(e -> isRemoveActionEnabled());

        decorator.setMoveUpAction(button -> moveSelectedRows(-1));
        decorator.setMoveDownAction(button -> moveSelectedRows(+1));
        decorator.addExtraAction(myEditButton);
        return decorator.createPanel();
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void showAddPopup(@Nonnull AnActionButton button) {
        Map<AddModuleDependencyActionProvider, AddModuleDependencyContext> contexts = new LinkedHashMap<>();

        AddModuleDependencyActionProvider.EP.forEachExtensionSafe(
            getProject().getApplication(),
            it -> {
                AddModuleDependencyContext context =
                    it.createContext(this, myState.getModulesConfigurator(), myState.getLibrariesConfigurator());
                if (it.isAvailable(context)) {
                    contexts.put(it, context);
                }
            }
        );

        // can't be empty - file adding avaliable always
        if (contexts.size() == 1) {
            Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> entry =
                ContainerUtil.getFirstItem(contexts.entrySet());

            AddModuleDependencyListPopupStep.providerSelected(entry);
        }
        else {
            AddModuleDependencyListPopupStep step = new AddModuleDependencyListPopupStep(getRootModel(), contexts.entrySet());
            ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
            popup.show(button.getPreferredPopupPoint());
        }
    }

    private boolean isRemoveActionEnabled() {
        final int[] selectedRows = myEntryTable.getSelectedRows();
        boolean removeButtonEnabled = true;
        int minRow = myEntryTable.getRowCount() + 1;
        int maxRow = -1;
        for (final int selectedRow : selectedRows) {
            minRow = Math.min(minRow, selectedRow);
            maxRow = Math.max(maxRow, selectedRow);
            final ClasspathTableItem<?> item = myModel.getItemAt(selectedRow);
            if (!item.isRemovable()) {
                removeButtonEnabled = false;
            }
        }
        return removeButtonEnabled && selectedRows.length > 0;
    }

    private void doEdit() {
        if (myEntryTable.getSelectedRowCount() != 1) {
            return;
        }
        ClasspathTableItem<?> itemAt = myModel.getItemAt(myEntryTable.getSelectedRow());
        itemAt.doEdit(this);
        myEntryTable.repaint();
    }

    private void removeSelectedItems(final List removedRows) {
        if (removedRows.isEmpty()) {
            return;
        }
        for (final Object removedRow : removedRows) {
            final ClasspathTableItem<?> item = (ClasspathTableItem<?>)((Object[])removedRow)[ClasspathTableModel.ITEM_COLUMN];
            final OrderEntry orderEntry = item.getEntry();

            getRootModel().removeOrderEntry(orderEntry);
        }
        final int[] selectedRows = myEntryTable.getSelectedRows();
        myModel.fireTableDataChanged();
        TableUtil.selectRows(myEntryTable, selectedRows);

        // TODO [VISTALL] context.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myState.getModulesConfigurator(), getRootModel().getModule()));
    }

    @Override
    @Nonnull
    public LibraryTableModifiableModelProvider getModifiableModelProvider(@Nonnull String tableLevel) {
        if (LibraryEx.MODULE_LEVEL.equals(tableLevel)) {
            final LibraryTable moduleLibraryTable = getRootModel().getModuleLibraryTable();
            return moduleLibraryTable::getModifiableModel;
        }
        else {
            return myState.getLibrariesConfigurator().createModifiableModelProvider(tableLevel);
        }
    }

    @Override
    public void addItems(List<ClasspathTableItem<?>> toAdd) {
        for (ClasspathTableItem<?> item : toAdd) {
            myModel.addItem(item);
        }
        myModel.fireTableDataChanged();
        final ListSelectionModel selectionModel = myEntryTable.getSelectionModel();
        selectionModel.setSelectionInterval(myModel.getRowCount() - toAdd.size(), myModel.getRowCount() - 1);
        TableUtil.scrollSelectionToVisible(myEntryTable);

        // TODO [VISTALL] context.getDaemonAnalyzer().queueUpdate(new ModuleProjectStructureElement(myState.getModulesConfigurator(), getRootModel().getModule()));
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
    public ModuleConfigurationState getModuleConfigurationState() {
        return myState;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    public void rootsChanged() {
        forceInitFromModel();
    }

    private void enableModelUpdate() {
        myInsideChange--;
    }

    private void disableModelUpdate() {
        myInsideChange++;
    }

    private void moveSelectedRows(int increment) {
        if (increment == 0) {
            return;
        }
        if (myEntryTable.isEditing()) {
            myEntryTable.getCellEditor().stopCellEditing();
        }
        final ListSelectionModel selectionModel = myEntryTable.getSelectionModel();
        for (int row = increment < 0 ? 0 : myModel.getRowCount() - 1; increment < 0 ? row < myModel.getRowCount() : row >= 0;
             row += increment < 0 ? +1 : -1) {
            if (selectionModel.isSelectedIndex(row)) {
                final int newRow = moveRow(row, increment);
                selectionModel.removeSelectionInterval(row, row);
                selectionModel.addSelectionInterval(newRow, newRow);
            }
        }

        List<OrderEntry> entries = getEntries();
        myState.getRootModel().rearrangeOrderEntries(entries.toArray(new OrderEntry[entries.size()]));

        myModel.fireTableRowsUpdated(0, myModel.getRowCount() - 1);
        Rectangle cellRect = myEntryTable.getCellRect(selectionModel.getMinSelectionIndex(), 0, true);
        myEntryTable.scrollRectToVisible(cellRect);
        myEntryTable.repaint();
    }

    public void selectOrderEntry(@Nonnull OrderEntry entry) {
        for (int row = 0; row < myModel.getRowCount(); row++) {
            final OrderEntry orderEntry = myModel.getItemAt(row).getEntry();
            if (entry.getPresentableName().equals(orderEntry.getPresentableName())) {
                myEntryTable.getSelectionModel().setSelectionInterval(row, row);
                TableUtil.scrollSelectionToVisible(myEntryTable);
            }
        }
        ProjectIdeFocusManager.getInstance(myState.getProject()).requestFocus(myEntryTable, true);
    }

    private int moveRow(final int row, final int increment) {
        int newIndex = Math.abs(row + increment) % myModel.getRowCount();
        final ClasspathTableItem<?> item = myModel.removeDataRow(row);
        myModel.addItemAt(item, newIndex);
        return newIndex;
    }

    public void stopEditing() {
        TableUtil.stopEditing(myEntryTable);
    }

    public List<OrderEntry> getEntries() {
        final int count = myModel.getRowCount();
        final List<OrderEntry> entries = new ArrayList<>(count);
        for (int row = 0; row < count; row++) {
            final OrderEntry entry = myModel.getItemAt(row).getEntry();
            entries.add(entry);
        }
        return entries;
    }

    private int myInsideChange = 0;

    public void initFromModel() {
        if (myInsideChange == 0) {
            forceInitFromModel();
        }
    }

    public void forceInitFromModel() {
        final int[] selection = myEntryTable.getSelectedRows();
        myModel.clear();
        myModel.init();
        myModel.fireTableDataChanged();
        TableUtil.selectRows(myEntryTable, selection);
    }

    private static Consumer<ColoredTextContainer> getRender(final ClasspathTableItem<?> item) {
        final OrderEntryAppearanceService service = OrderEntryAppearanceService.getInstance();
        final OrderEntry entry = item.getEntry();
        return service.getRenderForOrderEntry(entry);
    }

    private static class TableItemRenderer extends ColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(
            JTable table,
            Object value,
            boolean selected,
            boolean hasFocus,
            int row,
            int column
        ) {
            setPaintFocusBorder(false);
            setFocusBorderAroundIcon(true);
            setBorder(JBUI.Borders.empty(1));
            if (value instanceof ClasspathTableItem<?>) {
                final ClasspathTableItem<?> tableItem = (ClasspathTableItem<?>)value;
                getRender(tableItem).accept(this);
                setToolTipText(tableItem.getTooltipText());
            }
        }
    }

    private static class ExportFlagRenderer implements TableCellRenderer {
        private final TableCellRenderer myDelegate;
        private final JPanel myBlankPanel;

        public ExportFlagRenderer(TableCellRenderer delegate) {
            myDelegate = delegate;
            myBlankPanel = new JPanel();
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
            if (!table.isCellEditable(row, column)) {
                myBlankPanel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                return myBlankPanel;
            }
            return myDelegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    private class MyFindUsagesAction extends FindUsagesInProjectStructureActionBase {
        private MyFindUsagesAction() {
            super(myEntryTable);
        }

        @Override
        protected boolean isEnabled() {
            return getSelectedElement() != null;
        }

        @Override
        protected ProjectStructureElement getSelectedElement() {
            final OrderEntry entry = getSelectedEntry();
            if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                final Library library = libraryOrderEntry.getLibrary();
                if (library != null) {
                    return new LibraryProjectStructureElement(library);
                }
            }
            else if (entry instanceof ModuleOrderEntry moduleOrderEntry) {
                final Module module = moduleOrderEntry.getModule();
                if (module != null) {
                    return new ModuleProjectStructureElement(myState.getModulesConfigurator(), module);
                }
            }
            else if (entry instanceof ModuleExtensionWithSdkOrderEntry withSdkOrderEntry) {
                final Sdk sdk = withSdkOrderEntry.getSdk();
                if (sdk != null) {
                    return new SdkProjectStructureElement(sdk);
                }
            }
            return null;
        }

        @Override
        protected RelativePoint getPointToShowResults() {
            Rectangle rect = myEntryTable.getCellRect(myEntryTable.getSelectedRow(), 1, false);
            Point location = rect.getLocation();
            location.y += rect.height;
            return new RelativePoint(myEntryTable, location);
        }
    }

    private class AnalyzeDependencyAction extends AnAction {
        private AnalyzeDependencyAction() {
            super("Analyze This Dependency");
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            final OrderEntry selectedEntry = getSelectedEntry();
            GlobalSearchScope targetScope;
            if (selectedEntry instanceof ModuleOrderEntry moduleOrderEntry) {
                final Module module = moduleOrderEntry.getModule();
                LOG.assertTrue(module != null);
                targetScope = GlobalSearchScope.moduleScope(module);
            }
            else {
                Library library = ((LibraryOrderEntry)selectedEntry).getLibrary();
                LOG.assertTrue(library != null);
                targetScope = new LibraryScope(getProject(), library);
            }
            new AnalyzeDependenciesOnSpecifiedTargetHandler(
                getProject(),
                new AnalysisScope(myState.getRootModel().getModule()),
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
                                Messages.showInfoMessage(
                                    myEntryTable,
                                    "Dependencies were successfully collected in \"" + ToolWindowId.DEPENDENCIES + "\" toolwindow",
                                    FindLocalize.findPointcutApplicationsNotFoundTitle().get()
                                );
                                return true;
                            }
                        }
                    }
                    if (Messages.showOkCancelDialog(
                        myEntryTable,
                        "No code dependencies were found. Would you like to remove the dependency?",
                        CommonLocalize.titleWarning().get(),
                        UIUtil.getWarningIcon()
                    ) == DialogWrapper.OK_EXIT_CODE) {
                        removeSelectedItems(TableUtil.removeSelectedItems(myEntryTable));
                    }
                    return false;
                }
            }.analyze();
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            final OrderEntry entry = getSelectedEntry();
            e.getPresentation().setVisible(
                entry instanceof ModuleOrderEntry moduleOrderEntry && moduleOrderEntry.getModule() != null
                    || entry instanceof LibraryOrderEntry libraryOrderEntry && libraryOrderEntry.getLibrary() != null
            );
        }
    }
}
