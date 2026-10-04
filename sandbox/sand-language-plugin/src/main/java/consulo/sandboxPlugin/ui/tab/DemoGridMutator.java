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
package consulo.sandboxPlugin.ui.tab;

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.ErrorInfo;
import consulo.ui.ex.grid.SimpleErrorInfo;
import consulo.ui.grid.CellMutation;
import consulo.ui.grid.ColumnDescriptor;
import consulo.ui.grid.DataConsumer;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataType;
import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridPagingModel;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridStorageAndModelUpdater;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.LongActionRequestPlace;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import consulo.ui.grid.MoveColumnsRequestPlace;
import consulo.ui.grid.MutationData;
import consulo.ui.grid.MutationRow;
import consulo.ui.grid.MutationType;
import consulo.ui.grid.MutationsStorage;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.SizeProvider;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.util.collection.JBIterable;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Keeps the edits of the grid as pending changes until Submit, the way a database result does: a changed cell is a mutation in
 * the {@link MutationsStorage}, and a value written back to what the source holds drops it. Submit answers after the delay of
 * every request of {@link DemoGridHookUp} - or fails, when the source is told to fail its next request; the changes then stay
 * pending and show as failed until a Submit succeeds. The submitted values go into the rows and are kept by the hook-up, which puts
 * them back into the rows of every later load.
 * <p/>
 * Rows and columns are inserted - before a row or a column, or at the end -, deleted, cloned, renamed and moved at once, without
 * Submit, the way a text source changes. The page shown is laid out again in one UI task - its rows removed, the new columns set, the
 * rows added - and the pending changes move along with their cells; the request is answered only then, so a callback of it sees the
 * new rows and columns. A move shows the loading state of the grid while it runs, and the grid adjusts its columns before the answer.
 * <ul>
 * <li>Columns: the layout lives in the columns of the model ({@link DemoColumn}), each reading its values from the column of
 * {@link SampleGridData} it was made from, or from the values kept here for a column added through the grid. The loader keeps the
 * columns of the model while there are any, so the layout stays for Reload and the other pages. The last column cannot be deleted.</li>
 * <li>Rows: the loader computes every page from {@link SampleGridData} again, so the rows inserted, cloned or deleted stay until a page
 * is loaded again - with the values submitted into an inserted row -, and a load does not ask before it drops them; the tester tab
 * tells so while the page holds such rows ({@link #hasUnsavedRows()}). An inserted row is numbered after the sample rows.</li>
 * </ul>
 * A change of the rows or columns is refused while a Submit is on its way, as the Submit finds its cells again by their rows and
 * columns, and by the mutations it took. Only the inserted columns of the {@link MutationsStorage} are pending inserts - a column added
 * here is a column of the model at once -, so the context menu, which offers Rename for a pending insert or over CSV text only, does
 * not offer it here; {@link #renameColumn} is served all the same. Everything runs on the UI thread.
 *
 * @since 2026-10-04
 */
final class DemoGridMutator implements GridMutator.DatabaseMutator<GridRow, GridColumn> {
    private static final Logger LOG = Logger.getInstance(DemoGridMutator.class);

    private static final LocalizeValue FAILURE_MESSAGE =
        LocalizeValue.localizeTODO("Simulated failure: the sample source refused to submit the changes");
    private static final LocalizeValue UNPARSED_MESSAGE =
        LocalizeValue.localizeTODO("Values which could not be parsed cannot be submitted");
    private static final LocalizeValue SUBMIT_RUNNING_MESSAGE =
        LocalizeValue.localizeTODO("The rows and columns cannot change while the changes are being submitted");
    private static final LocalizeValue LAST_COLUMN_MESSAGE = LocalizeValue.localizeTODO("The last column cannot be deleted");
    private static final LocalizeValue NAME_TAKEN_MESSAGE = LocalizeValue.localizeTODO("There is a column named \"%s\" already");
    private static final LocalizeValue EMPTY_NAME_MESSAGE = LocalizeValue.localizeTODO("The column name is empty");
    private static final LocalizeValue NO_ROW_MESSAGE = LocalizeValue.localizeTODO("The row is not loaded any more");
    private static final LocalizeValue NO_COLUMN_MESSAGE = LocalizeValue.localizeTODO("The column does not exist any more");
    private static final LocalizeValue MOVE_REFUSED_MESSAGE = LocalizeValue.localizeTODO("The column cannot be moved there");

    /**
     * The columns of {@link SampleGridData}: a column key below it is the index of a sample column, a bigger one is the key of a
     * column added through the grid.
     */
    private static final int SAMPLE_COLUMN_COUNT = SampleGridData.COLUMNS.size();

    /**
     * The columns of {@link SampleGridData} with {@code java.time} values, which the temporal editors give a {@code java.sql} value
     * when the cell was NULL - see {@link #toColumnClass}.
     */
    private static final int CREATED_COLUMN = sampleColumn("created");
    private static final int UPDATED_COLUMN = sampleColumn("updated");

    private static final GridDataType ADDED_COLUMN_TYPE = GridDataType.of(GridTypeKind.TEXT, "text");
    private static final String COPY_SUFFIX = "_copy";

    private final DemoGridHookUp myHookUp;
    private final MutationsStorage myStorage;
    private final UIAccess myUIAccess;

    /**
     * The values of the columns added through the grid: by the key of the column, then by the number of the row. A row without an entry
     * holds NULL. Kept for every row of the source, so an added column has its values on every page.
     */
    private final Map<Integer, Map<Integer, @Nullable Object>> myAddedColumnValues = new HashMap<>();
    private int myNextColumnKey = SAMPLE_COLUMN_COUNT;
    private int myNextRowNum = SampleGridData.TOTAL + 1;
    /**
     * The row inserted or cloned last, by identity - it is gone once its page is loaded again.
     */
    private @Nullable GridRow myLastInsertedRow;
    private int mySubmitsInFlight;
    private @Nullable GridStorageAndModelUpdater myModelUpdater;

    private boolean myFailed;

    /**
     * @param uiAccess the UI thread of the grid, where a Submit reports its answer
     */
    DemoGridMutator(DemoGridHookUp hookUp, MutationsStorage storage, UIAccess uiAccess) {
        myHookUp = hookUp;
        myStorage = storage;
        myUIAccess = uiAccess;
    }

    // region GridMutator

    @Override
    public boolean isUpdateSafe(ModelIndexSet<GridRow> rowIndices, ModelIndexSet<GridColumn> columnIndices, @Nullable Object newValue) {
        return true;
    }

    @Override
    public boolean hasPendingChanges() {
        return myStorage.hasChanges();
    }

    @Override
    public boolean hasUnparsedValues() {
        return myStorage.hasUnparsedValues();
    }

    @Override
    public boolean isUpdateImmediately() {
        return false;
    }

    @Override
    public void mutate(GridRequestSource source,
                       ModelIndexSet<GridRow> row,
                       ModelIndexSet<GridColumn> column,
                       @Nullable Object newValue,
                       boolean allowImmediateUpdate) {
        mutate(source, GridUtilCore.createMutations(row, column, newValue), allowImmediateUpdate);
    }

    @Override
    public void mutate(GridRequestSource source, List<CellMutation> mutations, boolean allowImmediateUpdate) {
        if (!myStorage.hasChanges()) {
            // the changes of the failed Submit are gone - reverted, or dropped by a reload
            myFailed = false;
        }

        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Set<Integer> rows = new TreeSet<>();
        for (CellMutation mutation : mutations) {
            ModelIndex<GridRow> rowIdx = mutation.getRow();
            ModelIndex<GridColumn> columnIdx = mutation.getColumn();
            GridColumn column = model.getColumn(columnIdx);
            if (column == null || !rowIdx.isValid(model)) {
                continue;
            }
            Object value = toColumnClass(originOf(column), emptyTextToNull(column, mutation.getValue()));
            CellMutation stored = value == mutation.getValue()
                ? mutation
                : new CellMutation(rowIdx, columnIdx, value).withMetadata(mutation.getMetadata());
            // a value the source already holds is no change
            boolean same = GridCellEditorHelper.areValuesEqual(value, model.getValueAt(rowIdx, columnIdx));
            myStorage.set(rowIdx, columnIdx, same ? null : stored);
            rows.add(rowIdx.asInteger());
        }

        notifyRowsUpdated(rows, source);
        source.requestComplete(true);
    }

    // endregion

    // region DatabaseMutator

    /**
     * Sends the pending changes: the request answers after the delay of the source, so the grid is busy meanwhile, and it ends
     * with {@link DemoGridHookUp#notifyRequestFinished}, which the grid needs to finish the request and to clear the error of a
     * failed one. The values are taken now; a cell changed again meanwhile keeps its newer change.
     */
    @Override
    @RequiredUIAccess
    public void submit(GridRequestSource source, boolean includeInserted) {
        myHookUp.notifyRequestStarted(source);

        List<PendingChange> pendingChanges = collectPendingChanges();
        mySubmitsInFlight++;
        CompletableFuture.runAsync(
            () -> myUIAccess.give(() -> finishSubmit(source, pendingChanges)),
            CompletableFuture.delayedExecutor(myHookUp.getRequestDelayMillis(), TimeUnit.MILLISECONDS)
        );
    }

    @RequiredUIAccess
    private void finishSubmit(GridRequestSource source, List<PendingChange> pendingChanges) {
        mySubmitsInFlight = Math.max(0, mySubmitsInFlight - 1);
        if (myHookUp.isDisposed()) {
            return;
        }

        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Map<GridRow, Integer> rowIndices = indexRows(model);
        Map<Integer, Integer> columnIndices = indexColumns(model);
        Set<Integer> rows = new TreeSet<>();
        boolean unparsed = false;
        for (PendingChange change : pendingChanges) {
            Integer row = rowIndices.get(change.row());
            if (row != null) {
                rows.add(row);
            }
            unparsed |= change.data().getValue() instanceof UnparsedValue;
        }

        // the grid does not submit while a value is unparsed - a database would refuse it anyway
        LocalizeValue failure = myHookUp.consumeFailNext() ? FAILURE_MESSAGE : unparsed ? UNPARSED_MESSAGE : null;
        if (failure != null) {
            myFailed = true;
            myHookUp.notifyRequestError(source, SimpleErrorInfo.create(failure.get()));
            // the pending cells turn to the colour of the failure
            notifyRowsUpdated(rows, source);
            myHookUp.notifyRequestFinished(source, false);
            return;
        }

        for (PendingChange change : pendingChanges) {
            Integer row = rowIndices.get(change.row());
            Integer column = columnIndices.get(change.columnKey());
            if (row != null && column != null) {
                ModelIndex<GridRow> rowIdx = ModelIndex.forRow(model, row);
                ModelIndex<GridColumn> columnIdx = ModelIndex.forColumn(model, column);
                // the storage makes a new MutationData for every change - the same one means the cell was not changed since. It is
                // dropped before the source holds the value: the storage counts a modified row by comparing the change with the value
                // of the source, and would not count the change off once they are the same object
                if (myStorage.get(rowIdx, columnIdx) == change.data()) {
                    myStorage.set(rowIdx, columnIdx, null);
                }
            }
            // a row loaded again meanwhile is another object - the value still reaches the source, and the next load shows it
            writeValue(change.row(), change.columnKey(), toStoredValue(change.data().getValue()));
        }
        myFailed = false;

        notifyRowsUpdated(rows, source);
        myHookUp.notifyRequestFinished(source, true);
    }

    @Override
    public String getPendingChanges() {
        int count = myStorage.getModifiedRowsCount();
        return String.format(Locale.ROOT, count == 1 ? "%d row modified" : "%d rows modified", count);
    }

    @Override
    public @Nullable MutationType getMutationType(ModelIndex<GridRow> row) {
        if (myStorage.isInsertedRow(row)) {
            return MutationType.INSERT;
        }
        if (myStorage.isDeletedRow(row)) {
            return MutationType.DELETE;
        }
        return myStorage.isModified(row) ? MutationType.MODIFY : null;
    }

    @Override
    public boolean hasUnparsedValues(ModelIndex<GridRow> row) {
        return myStorage.hasUnparsedValues(row);
    }

    @Override
    public @Nullable MutationData getMutation(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        return myStorage.get(row, column);
    }

    @Override
    public @Nullable MutationType getMutationType(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
        if (myStorage.isInsertedRow(row)) {
            return MutationType.INSERT;
        }
        if (myStorage.isDeletedRow(row)) {
            return MutationType.DELETE;
        }
        return myStorage.get(row, column) != null ? MutationType.MODIFY : null;
    }

    /**
     * The last Submit failed, and its changes are still pending.
     */
    @Override
    public boolean isFailed() {
        return myFailed && myStorage.hasChanges();
    }

    @Override
    public boolean hasMutatedRows(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
        for (ModelIndex<GridRow> row : rows.asIterable()) {
            if (myStorage.isInsertedRow(row) || myStorage.isDeletedRow(row)) {
                return true;
            }
            if (!myStorage.isModified(row)) {
                continue;
            }
            for (ModelIndex<GridColumn> column : columns.asIterable()) {
                if (myStorage.get(row, column) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Drops the pending changes of the cells. The rows and columns, which change at once, stay as they are.
     */
    @Override
    public void revert(GridRequestSource source, ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
        Set<Integer> reverted = new TreeSet<>();
        for (ModelIndex<GridRow> row : rows.asIterable()) {
            if (!myStorage.isModified(row)) {
                continue;
            }
            for (ModelIndex<GridColumn> column : columns.asIterable()) {
                if (myStorage.get(row, column) != null) {
                    myStorage.set(row, column, null);
                    reverted.add(row.asInteger());
                }
            }
        }
        notifyRowsUpdated(reverted, source);
        source.requestComplete(true);
    }

    // endregion

    // region RowsMutator - rows change at once, until the page is loaded again

    @Override
    @RequiredUIAccess
    public void deleteRows(GridRequestSource source, ModelIndexSet<GridRow> rows) {
        runStructureRequest(source, () -> doDeleteRows(source, rows));
    }

    @Override
    @RequiredUIAccess
    public void insertRows(GridRequestSource source, int amount) {
        insertRows(source, null, amount);
    }

    @Override
    @RequiredUIAccess
    public void insertRows(GridRequestSource source, @Nullable ModelIndex<GridRow> before, int amount) {
        runStructureRequest(source, () -> doInsertRows(source, before, amount));
    }

    /**
     * Inserts the copy right below the row. The copy holds what the row holds in the source, and the pending changes of the row as
     * pending changes of its own, so it shows the same values.
     */
    @Override
    @RequiredUIAccess
    public void cloneRow(GridRequestSource source, ModelIndex<GridRow> toClone) {
        runStructureRequest(source, () -> doCloneRow(source, toClone));
    }

    @Override
    public boolean isDeletedRow(ModelIndex<GridRow> row) {
        return myStorage.isDeletedRow(row);
    }

    @Override
    public boolean isDeletedRows(ModelIndexSet<GridRow> rows) {
        return myStorage.isDeletedRows(rows);
    }

    @Override
    public boolean isInsertedRow(ModelIndex<GridRow> row) {
        return myStorage.isInsertedRow(row);
    }

    @Override
    public int getInsertedRowsCount() {
        return myStorage.getInsertedRowsCount();
    }

    /**
     * The row inserted or cloned last, while it is loaded - the insert actions select it once their request is done.
     */
    @Override
    public @Nullable ModelIndex<GridRow> getLastInsertedRow() {
        ModelIndex<GridRow> pendingRow = myStorage.getLastInsertedRow();
        GridRow lastInserted = myLastInsertedRow;
        if (pendingRow != null || lastInserted == null) {
            return pendingRow;
        }
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Integer index = indexRows(model).get(lastInserted);
        return index == null ? null : ModelIndex.forRow(model, index);
    }

    /**
     * The page shown holds rows which the next load does not give back - inserted or cloned rows -, or misses rows which it does -
     * deleted rows: a load gives the sample rows numbered from the start to the end of the page, one after another.
     */
    boolean hasUnsavedRows() {
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        List<GridRow> rows = myHookUp.getDataModel().getRows();
        int pageStart = pageModel.getPageStart();
        if (rows.size() != Math.max(0, pageModel.getPageEnd() - pageStart + 1)) {
            return true;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getRowNum() != pageStart + i) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ModelIndexSet<GridRow> getAffectedRows() {
        JBIterable<ModelIndex<GridRow>> rows = JBIterable.from(myStorage.getModifiedRows())
            .append(myStorage.getInsertedRows())
            .append(myStorage.getDeletedRows())
            .unique();
        return ModelIndexSet.forRows(myHookUp.getDataModel(), rows);
    }

    @Override
    public JBIterable<ModelIndex<GridRow>> getInsertedRows() {
        return myStorage.getInsertedRows();
    }

    // endregion

    // region ColumnsMutator - columns change at once, and stay for Reload and the other pages

    /**
     * Refuses to delete every column: the loader would put the sample columns back with the next load.
     */
    @Override
    @RequiredUIAccess
    public void deleteColumns(GridRequestSource source, ModelIndexSet<GridColumn> columns) {
        runStructureRequest(source, () -> doDeleteColumns(source, columns));
    }

    @Override
    @RequiredUIAccess
    public void insertColumn(GridRequestSource source, @Nullable String name) {
        insertColumn(source, null, name);
    }

    /**
     * Inserts a text column of NULL values. The name must not be taken, ignoring case, as in a database table.
     */
    @Override
    @RequiredUIAccess
    public void insertColumn(GridRequestSource source, @Nullable ModelIndex<GridColumn> before, @Nullable String name) {
        runStructureRequest(source, () -> doInsertColumn(source, before, name));
    }

    /**
     * Appends a copy of the column, named after it, with the values the column holds in the source for every row, and the pending
     * changes of the column as pending changes of its own.
     */
    @Override
    @RequiredUIAccess
    public void cloneColumn(GridRequestSource source, ModelIndex<GridColumn> toClone) {
        runStructureRequest(source, () -> doCloneColumn(source, toClone));
    }

    @Override
    @RequiredUIAccess
    public void moveColumn(GridRequestSource source, ModelIndex<GridColumn> from, ModelIndex<GridColumn> to) {
        runStructureRequest(source, () -> doMoveColumn(source, from, to));
    }

    @Override
    @RequiredUIAccess
    public void renameColumn(GridRequestSource source, ModelIndex<GridColumn> idx, String newName) {
        runStructureRequest(source, () -> doRenameColumn(source, idx, newName));
    }

    @Override
    public int getInsertedColumnsCount() {
        return myStorage.getInsertedColumnsCount();
    }

    @Override
    public JBIterable<ModelIndex<GridColumn>> getInsertedColumns() {
        return myStorage.getInsertedColumns();
    }

    /**
     * A pending insert only, as {@link #getInsertedColumns()} and {@link #getInsertedColumn} report - a column added through the grid
     * is a column of the model at once, no change which Revert could drop.
     */
    @Override
    public boolean isInsertedColumn(ModelIndex<GridColumn> idx) {
        return myStorage.isInsertedColumn(idx);
    }

    @Override
    public boolean isDeletedColumn(ModelIndex<GridColumn> idx) {
        return myStorage.isDeletedColumn(idx);
    }

    @Override
    public @Nullable GridColumn getInsertedColumn(ModelIndex<GridColumn> idx) {
        return myStorage.getInsertedColumn(idx);
    }

    // endregion

    // region changes of the rows and columns

    /**
     * Runs a change of the rows or columns and answers its request exactly once, after the model events of the change: done, or
     * refused with an error which the grid shows. A request of a {@link LongActionRequestPlace} shows the loading state of the grid
     * while the change runs.
     *
     * @param change makes the change and returns {@code null} - also when there was nothing to change -, or returns why it was refused,
     *               before anything changed
     */
    @RequiredUIAccess
    private void runStructureRequest(GridRequestSource source, Supplier<@Nullable LocalizeValue> change) {
        if (myHookUp.isDisposed()) {
            source.requestComplete(false);
            return;
        }

        myHookUp.notifyRequestStarted(source);
        ErrorInfo error;
        if (mySubmitsInFlight > 0) {
            error = SimpleErrorInfo.create(SUBMIT_RUNNING_MESSAGE.get());
        }
        else {
            error = changeShowingLoading(source, change);
        }

        if (error != null) {
            myHookUp.notifyRequestError(source, error);
            myHookUp.notifyRequestFinished(source, false);
            return;
        }
        myHookUp.notifyRequestFinished(source, true);
    }

    @RequiredUIAccess
    private static @Nullable ErrorInfo changeShowingLoading(GridRequestSource source, Supplier<@Nullable LocalizeValue> change) {
        AutoCloseable loading = source.place instanceof LongActionRequestPlace place ? place.getLoadingUI().get() : null;
        try {
            LocalizeValue refusal = change.get();
            return refusal == null ? null : SimpleErrorInfo.create(refusal.get());
        }
        catch (RuntimeException e) {
            // the request is answered all the same - the grid must not stay busy
            LOG.error(e);
            return SimpleErrorInfo.create(e);
        }
        finally {
            if (loading != null) {
                try {
                    loading.close();
                }
                catch (Exception e) {
                    LOG.warn(e);
                }
            }
        }
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doInsertRows(GridRequestSource source, @Nullable ModelIndex<GridRow> before, int amount) {
        if (amount <= 0) {
            return null;
        }
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        List<GridRow> rows = new ArrayList<>(model.getRows());
        int at = before != null && before.isValid(model) ? before.asInteger() : rows.size();
        List<GridRow> inserted = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            inserted.add(newRow(new @Nullable Object[SAMPLE_COLUMN_COUNT]));
        }
        rows.addAll(at, inserted);

        reload(source, rows, null, collectPendingChanges());
        myLastInsertedRow = inserted.get(inserted.size() - 1);
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doDeleteRows(GridRequestSource source, ModelIndexSet<GridRow> rows) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Set<Integer> deleted = new HashSet<>();
        for (ModelIndex<GridRow> row : rows.asIterable()) {
            if (row.isValid(model)) {
                deleted.add(row.asInteger());
            }
        }
        if (deleted.isEmpty()) {
            return null;
        }

        List<GridRow> current = model.getRows();
        List<GridRow> kept = new ArrayList<>(current.size());
        for (int i = 0; i < current.size(); i++) {
            GridRow row = current.get(i);
            if (!deleted.contains(i)) {
                kept.add(row);
            }
            else if (!isSampleRow(row)) {
                // an inserted row never comes back - its values in the added columns go
                for (Map<Integer, @Nullable Object> values : myAddedColumnValues.values()) {
                    values.remove(row.getRowNum());
                }
            }
        }

        reload(source, kept, null, collectPendingChanges());
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doCloneRow(GridRequestSource source, ModelIndex<GridRow> toClone) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        GridRow original = model.getRow(toClone);
        if (original == null) {
            return NO_ROW_MESSAGE;
        }

        GridRow clone = newRow(GridRow.getValues(original));
        for (Map<Integer, @Nullable Object> values : myAddedColumnValues.values()) {
            if (values.containsKey(original.getRowNum())) {
                values.put(clone.getRowNum(), values.get(original.getRowNum()));
            }
        }
        List<PendingChange> pendingChanges = collectPendingChanges();
        int pendingCount = pendingChanges.size();
        for (int i = 0; i < pendingCount; i++) {
            PendingChange change = pendingChanges.get(i);
            if (change.row() == original) {
                pendingChanges.add(new PendingChange(clone, change.columnKey(), change.data()));
            }
        }
        List<GridRow> rows = new ArrayList<>(model.getRows());
        rows.add(toClone.asInteger() + 1, clone);

        reload(source, rows, null, pendingChanges);
        myLastInsertedRow = clone;
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doInsertColumn(GridRequestSource source,
                                                   @Nullable ModelIndex<GridColumn> before,
                                                   @Nullable String name) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        String columnName = name != null && !StringUtil.isEmptyOrSpaces(name) ? name : GridUtilCore.generateColumnName(model);
        if (isNameTaken(model, columnName, -1)) {
            return nameTaken(columnName);
        }

        List<GridColumn> columns = new ArrayList<>(model.getColumns());
        int at = before != null && before.isValid(model) ? before.asInteger() : columns.size();
        int key = myNextColumnKey++;
        myAddedColumnValues.put(key, new HashMap<>());
        columns.add(at, new DemoColumn(at, key, key, columnName, ADDED_COLUMN_TYPE, -1, -1, Set.of()));

        reload(source, new ArrayList<>(model.getRows()), layOut(columns), collectPendingChanges());
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doDeleteColumns(GridRequestSource source, ModelIndexSet<GridColumn> columns) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Set<Integer> deleted = new HashSet<>();
        for (ModelIndex<GridColumn> column : columns.asIterable()) {
            if (column.isValid(model)) {
                deleted.add(column.asInteger());
            }
        }
        if (deleted.isEmpty()) {
            return null;
        }
        if (deleted.size() >= model.getColumnCount()) {
            return LAST_COLUMN_MESSAGE;
        }

        List<GridColumn> current = model.getColumns();
        List<GridColumn> kept = new ArrayList<>(current.size());
        for (int i = 0; i < current.size(); i++) {
            GridColumn column = current.get(i);
            if (!deleted.contains(i)) {
                kept.add(column);
            }
            else if (keyOf(column) >= SAMPLE_COLUMN_COUNT) {
                myAddedColumnValues.remove(keyOf(column));
            }
        }

        reload(source, new ArrayList<>(model.getRows()), layOut(kept), collectPendingChanges());
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doCloneColumn(GridRequestSource source, ModelIndex<GridColumn> toClone) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        GridColumn original = model.getColumn(toClone);
        if (original == null) {
            return NO_COLUMN_MESSAGE;
        }

        int originalKey = keyOf(original);
        int key = myNextColumnKey++;
        myAddedColumnValues.put(key, copyColumnValues(originalKey));
        List<PendingChange> pendingChanges = collectPendingChanges();
        int pendingCount = pendingChanges.size();
        for (int i = 0; i < pendingCount; i++) {
            PendingChange change = pendingChanges.get(i);
            if (change.columnKey() == originalKey) {
                pendingChanges.add(new PendingChange(change.row(), key, change.data()));
            }
        }
        List<GridColumn> columns = new ArrayList<>(model.getColumns());
        String name = uniqueName(model, original.getName() + COPY_SUFFIX);
        columns.add(createColumn(original, columns.size(), key, originOf(original), name));

        reload(source, new ArrayList<>(model.getRows()), layOut(columns), pendingChanges);
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doMoveColumn(GridRequestSource source, ModelIndex<GridColumn> from, ModelIndex<GridColumn> to) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        if (!from.isValid(model) || !to.isValid(model)) {
            return MOVE_REFUSED_MESSAGE;
        }

        // laid out again even when the column stays: the grid rebuilds its view of the move from the new columns
        List<GridColumn> columns = new ArrayList<>(model.getColumns());
        GridColumn moved = columns.remove(from.asInteger());
        columns.add(to.asInteger(), moved);

        reload(source, new ArrayList<>(model.getRows()), layOut(columns), collectPendingChanges());
        return null;
    }

    @RequiredUIAccess
    private @Nullable LocalizeValue doRenameColumn(GridRequestSource source, ModelIndex<GridColumn> idx, String newName) {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        GridColumn column = model.getColumn(idx);
        if (column == null) {
            return NO_COLUMN_MESSAGE;
        }
        if (StringUtil.isEmptyOrSpaces(newName)) {
            return EMPTY_NAME_MESSAGE;
        }
        if (newName.equals(column.getName())) {
            return null;
        }
        if (isNameTaken(model, newName, idx.asInteger())) {
            return nameTaken(newName);
        }

        List<GridColumn> columns = new ArrayList<>(model.getColumns());
        columns.set(idx.asInteger(), createColumn(column, idx.asInteger(), keyOf(column), originOf(column), newName));

        reload(source, new ArrayList<>(model.getRows()), layOut(columns), collectPendingChanges());
        return null;
    }

    /**
     * Lays the page out again, the way a load does, in this UI task: its rows removed, the columns set when they changed, the rows added.
     * The pending changes go back to their cells - the rows by identity, the columns by key - and a column move adjusts the columns of
     * the grid, before the request is answered.
     *
     * @param rows           the rows of the page, a copy - the list of the model is emptied first
     * @param columns        the new columns, or {@code null} when they stay
     * @param pendingChanges the pending changes of the page, taken before
     */
    @RequiredUIAccess
    private void reload(GridRequestSource source,
                        List<GridRow> rows,
                        @Nullable List<GridColumn> columns,
                        List<PendingChange> pendingChanges) {
        GridStorageAndModelUpdater updater = getModelUpdater();
        // the storage forgets the changes of the removed rows - they are put back below
        updater.removeRows(0, myHookUp.getDataModel().getRowCount());
        if (columns != null) {
            updater.setColumns(columns);
        }
        updater.addRows(rows);
        updater.afterLastRowAdded();

        restorePendingChanges(pendingChanges, source);

        if (source.place instanceof MoveColumnsRequestPlace place) {
            place.adjustColumnsUI();
        }
    }

    /**
     * The pending changes of the loaded rows, by the row object and the key of the column, which stay while the indices move.
     */
    private List<PendingChange> collectPendingChanges() {
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        List<PendingChange> pendingChanges = new ArrayList<>();
        for (ModelIndex<GridRow> rowIdx : model.getRowIndices().asIterable()) {
            GridRow row = model.getRow(rowIdx);
            if (row == null) {
                continue;
            }
            for (ModelIndex<GridColumn> columnIdx : model.getColumnIndices().asIterable()) {
                MutationData data = myStorage.get(rowIdx, columnIdx);
                GridColumn column = model.getColumn(columnIdx);
                if (data != null && column != null) {
                    pendingChanges.add(new PendingChange(row, keyOf(column), data));
                }
            }
        }
        return pendingChanges;
    }

    /**
     * Puts the pending changes back at the cells their rows and columns have now; the change of a row or column which is gone is
     * dropped. The rows are painted again.
     */
    private void restorePendingChanges(List<PendingChange> pendingChanges, GridRequestSource source) {
        if (pendingChanges.isEmpty()) {
            return;
        }
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        Map<GridRow, Integer> rowIndices = indexRows(model);
        Map<Integer, Integer> columnIndices = indexColumns(model);
        Set<Integer> rows = new TreeSet<>();
        for (PendingChange change : pendingChanges) {
            Integer row = rowIndices.get(change.row());
            Integer column = columnIndices.get(change.columnKey());
            if (row == null || column == null) {
                continue;
            }
            ModelIndex<GridRow> rowIdx = ModelIndex.forRow(model, row);
            ModelIndex<GridColumn> columnIdx = ModelIndex.forColumn(model, column);
            MutationData data = change.data();
            myStorage.set(rowIdx, columnIdx, new CellMutation(rowIdx, columnIdx, data.getValue()).withMetadata(data.getMetadata()));
            rows.add(row);
        }
        notifyRowsUpdated(rows, source);
    }

    /**
     * The columns at their new indices: the number of a column is its index in the model, which a row with pending changes reads its
     * values by - see {@link MutationRow}.
     */
    private List<GridColumn> layOut(List<GridColumn> columns) {
        List<GridColumn> laidOut = new ArrayList<>(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            GridColumn column = columns.get(i);
            laidOut.add(createColumn(column, i, keyOf(column), originOf(column), column.getName()));
        }
        return laidOut;
    }

    private DemoColumn createColumn(GridColumn column, int index, int key, int origin, String name) {
        int precision = column instanceof SizeProvider sizeProvider ? sizeProvider.getSize() : -1;
        int scale = column instanceof SizeProvider sizeProvider ? sizeProvider.getScale() : -1;
        return new DemoColumn(index, key, origin, name, column.getType(), precision, scale, column.getAttributes());
    }

    /**
     * The values a column holds in the source for every row: the sample rows, with the values submitted so far, and the rows inserted
     * into the page shown.
     */
    @RequiredUIAccess
    private Map<Integer, @Nullable Object> copyColumnValues(int key) {
        Map<Integer, @Nullable Object> values = new HashMap<>();
        if (key >= SAMPLE_COLUMN_COUNT) {
            Map<Integer, @Nullable Object> added = myAddedColumnValues.get(key);
            if (added != null) {
                values.putAll(added);
            }
            return values;
        }

        List<GridRow> sampleRows = SampleGridData.fetch(0, SampleGridData.TOTAL);
        myHookUp.applyCommittedValues(sampleRows);
        for (GridRow row : sampleRows) {
            values.put(row.getRowNum(), row.getValue(key));
        }
        for (GridRow row : myHookUp.getDataModel().getRows()) {
            if (!isSampleRow(row)) {
                values.put(row.getRowNum(), row.getValue(key));
            }
        }
        return values;
    }

    /**
     * A row of the source numbered after the sample rows. It holds a value for every sample column; the added columns keep theirs here.
     */
    private GridRow newRow(@Nullable Object[] values) {
        int rowNum = myNextRowNum++;
        return DataConsumer.Row.create(rowNum - 1, values);
    }

    private GridStorageAndModelUpdater getModelUpdater() {
        GridStorageAndModelUpdater updater = myModelUpdater;
        if (updater == null) {
            // the kind of updater the loader fills the model with: the storage follows the rows
            updater = new GridStorageAndModelUpdater(myHookUp.getDataModel(), myHookUp.getMutationModel(), myStorage);
            myModelUpdater = updater;
        }
        return updater;
    }

    private static LocalizeValue nameTaken(String name) {
        return NAME_TAKEN_MESSAGE.map(text -> String.format(Locale.ROOT, text, name));
    }

    /**
     * @param except the index of the column which is renamed, or {@code -1}
     */
    private static boolean isNameTaken(GridModel<GridRow, GridColumn> model, String name, int except) {
        List<GridColumn> columns = model.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i != except && columns.get(i).getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String uniqueName(GridModel<GridRow, GridColumn> model, String base) {
        String name = base;
        int number = 2;
        while (isNameTaken(model, name, -1)) {
            name = base + number;
            number++;
        }
        return name;
    }

    private static Map<GridRow, Integer> indexRows(GridModel<GridRow, GridColumn> model) {
        // rows are told apart by identity - two rows may hold the same values
        Map<GridRow, Integer> indices = new IdentityHashMap<>();
        List<GridRow> rows = model.getRows();
        for (int i = 0; i < rows.size(); i++) {
            indices.put(rows.get(i), i);
        }
        return indices;
    }

    private Map<Integer, Integer> indexColumns(GridModel<GridRow, GridColumn> model) {
        Map<Integer, Integer> indices = new HashMap<>();
        List<GridColumn> columns = model.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            indices.put(keyOf(columns.get(i)), i);
        }
        return indices;
    }

    // endregion

    // region values of the columns

    /**
     * @return the key of the column: the index of the sample column it reads, or the key of a column added through the grid
     */
    private int keyOf(GridColumn column) {
        // the columns of the first load are those of SampleGridData, numbered by their index
        return column instanceof DemoColumn demoColumn ? demoColumn.myKey : column.getColumnNumber();
    }

    /**
     * @return the key of the column the values of the column came from first - a copy comes from the column it copies
     */
    private int originOf(GridColumn column) {
        return column instanceof DemoColumn demoColumn ? demoColumn.myOrigin : column.getColumnNumber();
    }

    private @Nullable Object readValue(GridRow row, int columnKey) {
        if (columnKey < SAMPLE_COLUMN_COUNT) {
            return row.getValue(columnKey);
        }
        Map<Integer, @Nullable Object> values = myAddedColumnValues.get(columnKey);
        return values == null ? null : values.get(row.getRowNum());
    }

    /**
     * Writes a submitted value into the source: into the row, and for a sample row into the hook-up, which puts it back into the rows
     * of every later load; the value of an added column is kept here. A column deleted meanwhile keeps nothing.
     */
    @RequiredUIAccess
    private void writeValue(GridRow row, int columnKey, @Nullable Object value) {
        if (columnKey >= SAMPLE_COLUMN_COUNT) {
            Map<Integer, @Nullable Object> values = myAddedColumnValues.get(columnKey);
            if (values != null) {
                values.put(row.getRowNum(), value);
            }
            return;
        }
        // DataGridListModel.injectValue takes no NULL
        row.setValue(columnKey, value);
        if (isSampleRow(row)) {
            myHookUp.putCommittedValue(row.getRowNum(), columnKey, value);
        }
    }

    private static boolean isSampleRow(GridRow row) {
        return row.getRowNum() <= SampleGridData.TOTAL;
    }

    // endregion

    /**
     * Repaints the rows - every column of them, the row header shows the change of the row too.
     */
    private void notifyRowsUpdated(Set<Integer> rows, GridRequestSource source) {
        if (rows.isEmpty()) {
            return;
        }
        GridModel<GridRow, GridColumn> model = myHookUp.getDataModel();
        int[] rowIndices = rows.stream().mapToInt(Integer::intValue).toArray();
        myHookUp.getMutationModel().notifyCellsUpdated(
            ModelIndexSet.forRows(model, rowIndices),
            model.getColumnIndices(),
            source.place
        );
    }

    /**
     * The value as the source keeps it in its column. The temporal editors parse to the {@code java.time} class of the value the
     * cell held, but give a cell which held NULL a {@code java.sql} value; the columns of {@link SampleGridData} with
     * {@code java.time} values - and their copies - keep them. The {@code java.sql} values are made in the system zone, so their wall
     * time is the one typed.
     *
     * @param origin the key of the column the values of the column came from, see {@link #originOf}
     */
    private static @Nullable Object toColumnClass(int origin, @Nullable Object value) {
        if (origin == CREATED_COLUMN && value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (origin == UPDATED_COLUMN && value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return value;
    }

    /**
     * Clear Cells - and the delete key, which clears the selected cells in the tester tab - writes an empty text. A column which is
     * not text takes NULL instead: a number, a date or a UUID has no empty value.
     */
    private static @Nullable Object emptyTextToNull(GridColumn column, @Nullable Object value) {
        return "".equals(value) && column.getType().getKind() != GridTypeKind.TEXT ? ReservedCellValue.NULL : value;
    }

    /**
     * The value a submitted change leaves in the source: the sample source has neither column defaults nor generated columns,
     * so every reserved value ({@code <null>}, {@code <default>}, ...) stores NULL.
     */
    private static @Nullable Object toStoredValue(@Nullable Object value) {
        return value instanceof ReservedCellValue ? null : value;
    }

    private static int sampleColumn(String name) {
        for (GridColumn column : SampleGridData.COLUMNS) {
            if (column.getName().equals(name)) {
                return column.getColumnNumber();
            }
        }
        throw new IllegalStateException("No sample column " + name);
    }

    /**
     * A column of the model as laid out here: its number is its index in the model, and it reads its values from the sample column
     * or the added column of its key - so the rows of {@link SampleGridData}, loaded again, show them in this layout.
     */
    private final class DemoColumn extends DataConsumer.Column {
        private final int myKey;
        private final int myOrigin;

        private DemoColumn(int index,
                           int key,
                           int origin,
                           String name,
                           GridDataType type,
                           int precision,
                           int scale,
                           Set<ColumnDescriptor.Attribute> attributes) {
            super(index, name, type, precision, scale, attributes);
            myKey = key;
            myOrigin = origin;
        }

        @Override
        public @Nullable Object getValue(GridRow row) {
            // a row with pending changes reads every value by the index of the column, and comes back here with the row of the source
            return row instanceof MutationRow ? row.getValue(getColumnNumber()) : readValue(row, myKey);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return super.equals(o) && o instanceof DemoColumn column && column.myKey == myKey;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), myKey);
        }
    }

    /**
     * A pending change by the row object and the key of its column, which stay while the indices of its cell move; the mutation is
     * kept by identity.
     */
    private record PendingChange(GridRow row, int columnKey, MutationData data) {
    }
}
