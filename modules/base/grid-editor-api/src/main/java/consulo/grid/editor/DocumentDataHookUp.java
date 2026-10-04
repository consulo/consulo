// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.grid.editor;

import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.GridDataHookUpBase;
import consulo.ui.ex.grid.HookUpVirtualFileProvider;
import consulo.ui.ex.grid.SimpleErrorInfo;
import consulo.ui.ex.grid.csv.TypeMerger;
import consulo.ui.grid.CellMutation;
import consulo.ui.grid.DataGridListModel;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataType;
import consulo.ui.grid.GridLoader;
import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridMutationModel;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridPagingModel;
import consulo.ui.grid.GridPagingModelImpl;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridStorageAndModelUpdater;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.LongActionRequestPlace;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ModelIndexSet;
import consulo.ui.grid.MoveColumnsRequestPlace;
import consulo.ui.grid.RowMutation;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A data source of the grid over a document, or over a range of it. The text is parsed into a {@link DataMarkup} on a sequential pool
 * thread, from an immutable snapshot, and every request of the grid is written back as one command of minimal document changes, so the
 * document undo reverts it as one step.
 * <p/>
 * Threading: the grid sends its requests on the UI thread, and every answer is reported there, through the {@link UIAccess} given to
 * the constructor. All the state of the data source is used on the UI thread only; {@link #buildMarkup} runs on the pool thread.
 * <p/>
 * Requests:
 * <ul>
 * <li>Every request is answered exactly once. A request which changes the text is answered by the parse of the changed text, after the
 * rows and columns of the grid were updated in one UI task - so a callback of the request already sees the new rows and columns. A
 * request which fails is answered at once, with its error.</li>
 * <li>A request which comes while the text changed and is not parsed yet waits for that parse, as the offsets of the old markup are stale;
 * the waiting requests run in the order they came. A request is never refused because a parse is pending - but a request of rows or
 * columns the grid showed is refused when, by the time it runs, rows or columns may have moved to other indices: a request before it
 * deleted, inserted, moved or renamed some, the format changed, or the text was changed elsewhere.</li>
 * <li>A request of a {@link LongActionRequestPlace} shows the loading state of the grid from the moment it comes until the parse of its
 * change is shown, so no edit starts on the rows and columns it is about to replace.</li>
 * </ul>
 * Changes of the document made elsewhere - another editor, an undo, a reload from disk - are parsed again while the grid listens to the
 * data source and the owner keeps it {@link #setActive active}; such parses are at least {@value #RELOAD_DELAY_MILLIS} ms apart.
 * Otherwise the data is only marked dirty, and parsed once the data source becomes active again.
 */
public abstract class DocumentDataHookUp extends GridDataHookUpBase<GridRow, GridColumn> implements Disposable, HookUpVirtualFileProvider {
    private static final Logger LOG = Logger.getInstance(DocumentDataHookUp.class);

    /**
     * The shortest time between two parses of changes made elsewhere.
     */
    public static final long RELOAD_DELAY_MILLIS = 300;

    private static final LocalizeValue UPDATE_VALUES_COMMAND_NAME = LocalizeValue.localizeTODO("Update Values");
    private static final LocalizeValue CANNOT_UPDATE_DOCUMENT = LocalizeValue.localizeTODO("Cannot update document");
    private static final LocalizeValue DATA_CHANGED = LocalizeValue.localizeTODO("The data changed meanwhile, try again");

    private final @Nullable Project myProject;
    private final Document myDocument;
    private final UIAccess myUIAccess;
    private @Nullable TextRange myRange;

    private @Nullable DataMarkup myCurrentMarkup;
    /**
     * The modification stamp of the document the current markup was parsed from - also when the parse gave no markup - or {@code -1}
     * before the first parse.
     */
    private long myMarkupStamp = -1;
    /**
     * Consulo: counts the changes after which a row or a column may sit at another index - every change of the text made elsewhere,
     * every change of the format, and every request which deletes, inserts before, moves or renames - but not the values or the
     * appended rows and columns a request wrote.
     */
    private long myStructureGeneration;
    /**
     * The {@link #myStructureGeneration} of the text the current markup was parsed from, or {@code -1} before the first parse.
     */
    private long myMarkupGeneration = -1;

    private final DataGridListModel myModel;
    private final GridStorageAndModelUpdater myModelUpdater;
    private final GridMutationModel myMutationModel;
    private final GridPagingModel<GridRow, GridColumn> myPageModel;
    private final DocumentDataLoader myLoader;
    private final DocumentDataMutator myMutator;

    private final MyDocumentListener myDocumentListener;

    /**
     * The update requests which wait for their turn: for the parse of the current text, or for the request before them.
     */
    private final Deque<PendingUpdate> myPendingUpdates = new ArrayDeque<>();
    /**
     * An update request runs and must not be overtaken: it asks whether the file may be written, or it prepares its text in the background.
     */
    private boolean myUpdateRunning;
    private boolean myActive = true;
    private volatile boolean myDisposed;

    /**
     * @param project  the project the document undo and the read-only check belong to, or {@code null} for a document of no project
     * @param range    the part of the document which holds the data, or {@code null} for the whole document. It follows the changes of
     *                 the document
     * @param uiAccess the UI thread of the grid, where the data source reports its answers
     */
    protected DocumentDataHookUp(@Nullable Project project, Document document, @Nullable TextRange range, UIAccess uiAccess) {
        myProject = project;
        myDocument = document;
        myRange = range;
        myUIAccess = uiAccess;

        myModel = new DataGridListModel(GridCellEditorHelper::areValuesEqual);
        myMutationModel = new GridMutationModel(this);
        myModelUpdater = new GridStorageAndModelUpdater(myModel, myMutationModel, null);
        myPageModel = new GridPagingModelImpl.SinglePage<>(myMutationModel);
        myLoader = new DocumentDataLoader(getConcurrency(project));
        Disposer.register(this, myLoader);
        myMutator = createDataMutator();

        myDocumentListener = new MyDocumentListener();
        myDocument.addDocumentListener(myDocumentListener, this);
    }

    @SuppressWarnings("deprecation")
    private static ApplicationConcurrency getConcurrency(@Nullable Project project) {
        Application application = project != null ? project.getApplication() : Application.get();
        return application.getInstance(ApplicationConcurrency.class);
    }

    protected DocumentDataMutator createDataMutator() {
        return new DocumentDataMutator();
    }

    /**
     * Waits until the parse which runs now is done; its result is applied later, on the UI thread. For tests.
     */
    public void awaitParsingFinished() {
        myLoader.awaitParsingFinished();
    }

    public Document getDocument() {
        return myDocument;
    }

    public @Nullable Project getProject() {
        return myProject;
    }

    /**
     * @return the UI thread of the grid, where the data source reports its answers
     */
    protected UIAccess getUIAccess() {
        return myUIAccess;
    }

    @Override
    public @Nullable VirtualFile getVirtualFile() {
        return FileDocumentManager.getInstance().getFile(myDocument);
    }

    @Override
    public GridModel<GridRow, GridColumn> getDataModel() {
        return myModel;
    }

    @Override
    public GridModel<GridRow, GridColumn> getMutationModel() {
        return myMutationModel;
    }

    @Override
    public GridPagingModel<GridRow, GridColumn> getPageModel() {
        return myPageModel;
    }

    @Override
    public GridLoader getLoader() {
        return myLoader;
    }

    @Override
    public @Nullable GridMutator<GridRow, GridColumn> getMutator() {
        return myMutator;
    }

    @Override
    public boolean isReadOnly() {
        return !myDocument.isWritable();
    }

    /**
     * Called on the UI thread. The requests nobody will answer any more are rejected - their callbacks only, the grid went before.
     */
    @Override
    public void dispose() {
        myDisposed = true;
        for (PendingUpdate update : myPendingUpdates) {
            update.reject();
        }
        myPendingUpdates.clear();
        myLoader.rejectSources();
    }

    public @Nullable TextRange getRange() {
        return myRange;
    }

    /**
     * Consulo: the markup the rows and columns of the grid come from now, for example for a subclass which is read-only while its markup
     * has errors.
     *
     * @return the markup, or {@code null} before the first parse and when the last parse gave none. Used on the UI thread
     */
    public @Nullable DataMarkup getCurrentMarkup() {
        return myCurrentMarkup;
    }

    /**
     * Consulo: whether the owner shows the grid now. While the data source is not active, a change of the document made elsewhere is not
     * parsed, it only marks the data dirty - a file editor deactivates its data source when another editor of the file is selected.
     * Becoming active parses the text again when it changed meanwhile. The requests of the grid and the loads it asks for are served
     * either way. A data source is active until it is told otherwise.
     */
    @RequiredUIAccess
    public void setActive(boolean active) {
        if (myActive == active) {
            return;
        }
        myActive = active;
        if (active && !myDisposed) {
            myLoader.reloadIfDirty();
        }
    }

    public boolean isActive() {
        return myActive;
    }

    /**
     * Consulo: the rows or the columns of the next parse may sit at other indices although the text did not change - for one, the
     * format of the text changed. A request which was resolved against the rows and columns the grid shows now, and still waits to
     * run, is refused once that parse is shown, instead of writing to whatever sits at its indices then.
     */
    @RequiredUIAccess
    protected void markStructureChanged() {
        myStructureGeneration++;
    }

    /**
     * Parses the text. Called on a pool thread, one call at a time.
     *
     * @param sequence an immutable snapshot of the text of the data range
     * @return the markup of the text, or {@code null} when the text cannot be shown - the grid is then emptied
     */
    protected abstract @Nullable DataMarkup buildMarkup(CharSequence sequence, GridRequestSource requestSource);

    public static class UpdateSession {
        private final Document myDocument;
        private int myRightShift;

        protected UpdateSession(Document document, int rightShift) {
            myDocument = document;
            myRightShift = rightShift;
        }

        public void insert(CharSequence sequence, int atOffset) {
            myDocument.insertString(atOffset + myRightShift, sequence);
            myRightShift += sequence.length();
        }

        public void replace(TextRange range, CharSequence sequence) {
            TextRange shifted = range.shiftRight(myRightShift);
            myDocument.replaceString(shifted.getStartOffset(), shifted.getEndOffset(), sequence);
            myRightShift += sequence.length() - shifted.getLength();
        }

        public void delete(TextRange range) {
            TextRange shifted = range.shiftRight(myRightShift);
            myDocument.deleteString(shifted.getStartOffset(), shifted.getEndOffset());
            myRightShift -= range.getLength();
        }

        public char charAt(int offset) {
            return myDocument.getCharsSequence().charAt(offset + myRightShift);
        }

        public boolean isValidOffset(int offset) {
            int shifted = offset + myRightShift;
            return shifted >= 0 && shifted < myDocument.getTextLength();
        }

        public String getText() {
            return myDocument.getText();
        }
    }

    /**
     * The parsed text: the columns and rows of the grid, and the ranges of the text behind them. Every operation writes its change through
     * the {@link UpdateSession}, in ascending order of offsets, against the ranges of the text the markup was parsed from.
     */
    public abstract static class DataMarkup {
        public static final TypeMerger STRING_MERGER = new TypeMerger.StringMerger("TEXT");
        public static final TypeMerger INTEGER_MERGER = new TypeMerger.IntegerMerger("INT");
        public static final TypeMerger BIG_INTEGER_MERGER = new TypeMerger.BigIntegerMerger("BIGINT");
        public static final TypeMerger DOUBLE_MERGER = new TypeMerger.DoubleMerger("DOUBLE");
        public static final TypeMerger BOOLEAN_MERGER = new TypeMerger.BooleanMerger("BOOLEAN");

        public final List<GridColumn> columns;
        public final List<GridRow> rows;

        public DataMarkup(List<GridColumn> columns, List<GridRow> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        /**
         * @return the type of a column whose values the merger accepts, named after the merger
         */
        public static GridDataType getType(TypeMerger merger) {
            GridTypeKind kind = merger == INTEGER_MERGER ? GridTypeKind.INTEGER :
                merger == DOUBLE_MERGER ? GridTypeKind.FLOAT :
                    merger == BIG_INTEGER_MERGER ? GridTypeKind.INTEGER :
                        merger == BOOLEAN_MERGER ? GridTypeKind.BOOLEAN :
                            GridTypeKind.TEXT;
            return GridDataType.of(kind, merger.getName());
        }

        protected abstract boolean deleteRows(UpdateSession session, List<GridRow> sortedRows);

        protected abstract boolean insertRow(UpdateSession session);

        protected abstract boolean cloneRow(UpdateSession session, GridRow row);

        protected abstract boolean deleteColumns(UpdateSession session, List<GridColumn> sortedColumns);

        protected abstract boolean insertColumn(UpdateSession session, @Nullable String name);

        protected abstract boolean cloneColumn(UpdateSession session, GridColumn column);

        protected abstract boolean update(UpdateSession session, List<RowMutation> infos);

        protected abstract boolean renameColumn(UpdateSession session, ModelIndex<GridColumn> column, String name);

        /**
         * @return the whole new text of the data range with the column moved, or an empty text when the markup cannot move columns. It
         * is computed on a pool thread, and used only by a markup whose {@link #moveColumn(UpdateSession, GridColumn, ModelIndex)}
         * returns {@code false}
         */
        protected String prepareMoveColumn(GridColumn fromColumn, ModelIndex<GridColumn> toColumn) {
            return "";
        }

        /**
         * Consulo: inserts an empty row before a row. The default appends it in any case.
         *
         * @param before the row the new one goes before, or {@code null} to append it
         */
        protected boolean insertRow(UpdateSession session, @Nullable GridRow before) {
            return insertRow(session);
        }

        /**
         * Consulo: inserts empty rows before a row, in the one session of the request. The default inserts them one by one with
         * {@link #insertRow(UpdateSession, GridRow)}: a markup whose append depends on the text before it overrides it.
         *
         * @param before the row the new ones go before, or {@code null} to append them
         */
        protected boolean insertRows(UpdateSession session, @Nullable GridRow before, int amount) {
            for (int i = 0; i < amount; i++) {
                if (!insertRow(session, before)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Consulo: inserts an empty column before a column. The default appends it in any case.
         *
         * @param before the column the new one goes before, or {@code null} to append it
         * @param name   the name of the new column, or {@code null} for a generated one
         */
        protected boolean insertColumn(UpdateSession session, @Nullable GridColumn before, @Nullable String name) {
            return insertColumn(session, name);
        }

        /**
         * Consulo: moves a column by changing only the ranges of the moved values. The default cannot: the column is then moved by
         * replacing the whole text with {@link #prepareMoveColumn}.
         *
         * @param toColumn the index the column gets; the columns between shift by one
         * @return {@code false} - without any change made - when the markup cannot move the column this way
         */
        protected boolean moveColumn(UpdateSession session, GridColumn fromColumn, ModelIndex<GridColumn> toColumn) {
            return false;
        }
    }

    public class DocumentDataMutator
        implements GridMutator.RowsMutator<GridRow, GridColumn>, GridMutator.ColumnsMutator<GridRow, GridColumn> {

        @Override
        public boolean hasUnparsedValues() {
            return false;
        }

        @Override
        @RequiredUIAccess
        public void deleteRows(GridRequestSource source, ModelIndexSet<GridRow> rows) {
            List<GridRow> rowsToDelete = sortedRows(myModel.getRows(rows));
            if (isReadOnly() || rowsToDelete.isEmpty()) {
                notifyRequestFinished(source, !isReadOnly());
                return;
            }

            updateDocument(source, UpdateKind.STRUCTURE, session -> requireMarkup().deleteRows(session, rowsToDelete));
        }

        /**
         * Consulo: the rows are inserted in one session - one command and one undo step.
         */
        @Override
        @RequiredUIAccess
        public void insertRows(GridRequestSource source, int amount) {
            insertRows(source, null, amount);
        }

        /**
         * Consulo: the rows are inserted in one session - one command and one undo step.
         */
        @Override
        @RequiredUIAccess
        public void insertRows(GridRequestSource source, @Nullable ModelIndex<GridRow> before, int amount) {
            if (isReadOnly() || amount <= 0) {
                notifyRequestFinished(source, !isReadOnly() && amount == 0);
                return;
            }

            GridRow beforeRow = before != null ? myModel.getRow(before) : null;
            updateDocument(source, beforeRow == null ? UpdateKind.APPEND : UpdateKind.STRUCTURE, session -> {
                DataMarkup markup = requireMarkup();
                return beforeRow == null && amount == 1 ? markup.insertRow(session) : markup.insertRows(session, beforeRow, amount);
            });
        }

        @Override
        @RequiredUIAccess
        public void cloneRow(GridRequestSource source, ModelIndex<GridRow> toClone) {
            GridRow row = myModel.getRow(toClone);
            if (isReadOnly() || row == null) {
                notifyRequestFinished(source, false);
                return;
            }

            updateDocument(source, UpdateKind.CLONE, session -> requireMarkup().cloneRow(session, row));
        }

        @Override
        public boolean isDeletedRow(ModelIndex<GridRow> row) {
            return false;
        }

        @Override
        public boolean isDeletedRows(ModelIndexSet<GridRow> rows) {
            return false;
        }

        @Override
        public boolean isInsertedRow(ModelIndex<GridRow> row) {
            return false;
        }

        @Override
        public boolean isDeletedColumn(ModelIndex<GridColumn> idx) {
            return false;
        }

        @Override
        public int getInsertedRowsCount() {
            return 0;
        }

        @Override
        public int getInsertedColumnsCount() {
            return 0;
        }

        @Override
        @RequiredUIAccess
        public void deleteColumns(GridRequestSource source, ModelIndexSet<GridColumn> columns) {
            List<GridColumn> columnsToDelete = sortedColumns(myModel.getColumns(columns));
            if (isReadOnly() || columnsToDelete.isEmpty()) {
                notifyRequestFinished(source, !isReadOnly());
                return;
            }

            updateDocument(source, UpdateKind.STRUCTURE, session -> requireMarkup().deleteColumns(session, columnsToDelete));
        }

        @Override
        @RequiredUIAccess
        public void insertColumn(GridRequestSource source, @Nullable String name) {
            if (isReadOnly()) {
                notifyRequestFinished(source, false);
                return;
            }

            updateDocument(source, UpdateKind.APPEND, session -> requireMarkup().insertColumn(session, name));
        }

        /**
         * Consulo: positional insert through {@link DataMarkup#insertColumn(UpdateSession, GridColumn, String)}.
         */
        @Override
        @RequiredUIAccess
        public void insertColumn(GridRequestSource source, @Nullable ModelIndex<GridColumn> before, @Nullable String name) {
            if (isReadOnly()) {
                notifyRequestFinished(source, false);
                return;
            }

            GridColumn beforeColumn = before != null ? myModel.getColumn(before) : null;
            updateDocument(source, beforeColumn == null ? UpdateKind.APPEND : UpdateKind.STRUCTURE, session -> {
                DataMarkup markup = requireMarkup();
                return beforeColumn == null ? markup.insertColumn(session, name) : markup.insertColumn(session, beforeColumn, name);
            });
        }

        /**
         * The markup moves the column by ranges ({@link DataMarkup#moveColumn(UpdateSession, GridColumn, ModelIndex)}); a markup which
         * cannot gets the whole new text computed on a pool thread ({@link DataMarkup#prepareMoveColumn}).
         */
        @Override
        @RequiredUIAccess
        public void moveColumn(
            GridRequestSource source,
            ModelIndex<GridColumn> from,
            ModelIndex<GridColumn> to
        ) {
            GridColumn fromColumn = myModel.getColumn(from);
            if (isReadOnly() || fromColumn == null) {
                notifyRequestFinished(source, false);
                return;
            }

            updateDocument(source, UpdateKind.STRUCTURE, new MoveColumnAction(fromColumn, to));
        }

        @Override
        @RequiredUIAccess
        public void cloneColumn(GridRequestSource source, ModelIndex<GridColumn> toClone) {
            GridColumn column = myModel.getColumn(toClone);
            if (isReadOnly() || column == null) {
                notifyRequestFinished(source, false);
                return;
            }

            updateDocument(source, UpdateKind.CLONE, session -> requireMarkup().cloneColumn(session, column));
        }

        @Override
        public ModelIndex<GridRow> getLastInsertedRow() {
            return ModelIndex.forRow(myModel, -1);
        }

        @Override
        public ModelIndexSet<GridRow> getAffectedRows() {
            return ModelIndexSet.forRows(myModel, -1);
        }

        @Override
        public JBIterable<ModelIndex<GridRow>> getInsertedRows() {
            return JBIterable.empty();
        }

        @Override
        public JBIterable<ModelIndex<GridColumn>> getInsertedColumns() {
            return JBIterable.empty();
        }

        @Override
        public boolean isInsertedColumn(ModelIndex<GridColumn> idx) {
            return false;
        }

        @Override
        public @Nullable GridColumn getInsertedColumn(ModelIndex<GridColumn> idx) {
            return null;
        }

        /**
         * Consulo: a request which cannot run is answered as well.
         */
        @Override
        @RequiredUIAccess
        public void renameColumn(GridRequestSource source,
                                 ModelIndex<GridColumn> idx,
                                 String newName) {
            GridColumn column = myModel.getColumn(idx);
            if (isReadOnly() || column == null) {
                notifyRequestFinished(source, false);
                return;
            }

            // a name may need a header record the text did not have
            updateDocument(source, UpdateKind.STRUCTURE, session -> requireMarkup().renameColumn(session, idx, newName));
        }

        @Override
        @RequiredUIAccess
        public void mutate(GridRequestSource source,
                           ModelIndexSet<GridRow> rows,
                           ModelIndexSet<GridColumn> columns,
                           @Nullable Object newValue,
                           boolean allowImmediateUpdate) {
            mutate(source, GridUtilCore.createMutations(rows, columns, newValue), allowImmediateUpdate);
        }

        @Override
        @RequiredUIAccess
        public void mutate(GridRequestSource source, List<CellMutation> mutations, boolean allowImmediateUpdate) {
            List<RowMutation> rowMutations = GridUtilCore.mergeAll(mutations, myModel);
            if (isReadOnly() || mutations.isEmpty() || rowMutations.isEmpty() || myModel.allValuesEqualTo(mutations)) {
                notifyRequestFinished(source, !isReadOnly());
                return;
            }

            updateDocument(source, UpdateKind.VALUES, session -> requireMarkup().update(session, rowMutations));
        }

        @Override
        public boolean isUpdateSafe(ModelIndexSet<GridRow> rowIndices,
                                    ModelIndexSet<GridColumn> columnIndices,
                                    @Nullable Object newValue) {
            return true;
        }

        @Override
        public boolean hasPendingChanges() {
            return false;
        }

        @Override
        public boolean isUpdateImmediately() {
            return true;
        }

        /**
         * Called inside the command of the request, after its change - before the text is parsed again.
         */
        protected void finishSession(UpdateSession session, boolean success) {
        }

        protected UpdateSession createSession() {
            TextRange range = myRange;
            return new UpdateSession(myDocument, range != null ? range.getStartOffset() : 0);
        }

        private static List<GridColumn> sortedColumns(List<GridColumn> columns) {
            return ContainerUtil.sorted(columns, Comparator.comparingInt(GridColumn::getColumnNumber));
        }

        private static List<GridRow> sortedRows(List<GridRow> rows) {
            return ContainerUtil.sorted(rows, Comparator.comparingInt(GridRow::getRowNum));
        }
    }

    private DataMarkup requireMarkup() {
        // the requests run only while the markup is the one of the current text
        return Objects.requireNonNull(myCurrentMarkup, "No markup for the current text");
    }

    // region update requests (Consulo: queue, one command per request, answers after the parse)

    /**
     * @param kind how the request depends on the indices of the rows and columns the grid shows now, and changes them
     */
    @RequiredUIAccess
    private void updateDocument(GridRequestSource source, UpdateKind kind, UpdateAction action) {
        // the rows and columns of the request were resolved against the model, which shows the current markup
        PendingUpdate update = new PendingUpdate(source, kind, myMarkupGeneration, action, openLoadingUI(source));
        myUIAccess.giveIfNeed(() -> {
            if (myDisposed) {
                update.reject();
                return;
            }
            myPendingUpdates.addLast(update);
            processPendingUpdates();
        });
    }

    /**
     * Runs the waiting requests in order while the markup is the one of the current text. A request whose change waits for the parse of
     * the changed text stops the queue; the parse goes on with it.
     */
    @RequiredUIAccess
    private void processPendingUpdates() {
        while (!myDisposed && !myUpdateRunning && !myPendingUpdates.isEmpty()) {
            if (myMarkupStamp != myDocument.getModificationStamp()) {
                // the offsets of the markup are stale: the requests wait for the parse of the current text
                myLoader.loadForPendingUpdates();
                return;
            }

            PendingUpdate update = myPendingUpdates.removeFirst();
            if (myCurrentMarkup == null) {
                // the current text gave no markup to change
                notifyRequestError(update.mySource, SimpleErrorInfo.create(CANNOT_UPDATE_DOCUMENT.get()));
                update.finish(false);
                continue;
            }
            if (update.myKind.myPositional && update.myGeneration != myMarkupGeneration) {
                // Consulo: rows or columns moved since the request was resolved - its indices point at others now
                notifyRequestError(update.mySource, SimpleErrorInfo.create(DATA_CHANGED.get()));
                update.finish(false);
                continue;
            }
            runUpdate(update);
        }
    }

    @RequiredUIAccess
    private void runUpdate(PendingUpdate update) {
        DataMarkup markup = myCurrentMarkup;
        long stamp = myMarkupStamp;

        boolean writable;
        myUpdateRunning = true;
        try {
            // may ask the user, and let other UI tasks run meanwhile
            writable = ensureWritable();
        }
        finally {
            myUpdateRunning = false;
        }

        if (myDisposed) {
            update.reject();
            return;
        }
        if (!writable) {
            notifyRequestError(update.mySource, SimpleErrorInfo.create(CANNOT_UPDATE_DOCUMENT.get()));
            update.finish(false);
            return;
        }
        if (markup != myCurrentMarkup || stamp != myDocument.getModificationStamp()) {
            // the text changed while the user was asked: run again once it is parsed
            myPendingUpdates.addFirst(update);
            return;
        }

        execute(update);
    }

    private boolean ensureWritable() {
        VirtualFile file = FileDocumentManager.getInstance().getFile(myDocument);
        if (myProject != null && file != null) {
            return ReadonlyStatusHandler.ensureFilesWritable(myProject, file);
        }
        return myDocument.isWritable();
    }

    /**
     * One command: the listener is muted, the change is made in bulk mode, and the session is finished - then the change is parsed with
     * the source of the request, which answers it.
     */
    @RequiredUIAccess
    private void execute(PendingUpdate update) {
        UpdateSession session = myMutator.createSession();
        UpdateOutcome outcome = new UpdateOutcome();
        try {
            CommandProcessor.getInstance()
                .newCommand()
                .project(myProject)
                .document(myDocument)
                .name(UPDATE_VALUES_COMMAND_NAME)
                .inWriteAction()
                .run(() -> perform(update, session, outcome));
        }
        catch (RuntimeException e) {
            if (outcome.myError == null) {
                outcome.myError = e;
            }
        }

        GridRequestSource source = update.mySource;
        Throwable error = outcome.myError;
        if (error != null) {
            LOG.warn(error);
            notifyRequestError(source, SimpleErrorInfo.create(error));
            update.finish(false);
        }
        else if (!outcome.myRan || !outcome.mySuccess) {
            notifyRequestError(source, SimpleErrorInfo.create(CANNOT_UPDATE_DOCUMENT.get()));
            update.finish(false);
        }
        else if (update.myAction instanceof MoveColumnAction move && move.myFallback) {
            runMoveFallback(update, move);
        }
        else if (!outcome.myChanged) {
            // nothing to parse again: a move which changed nothing did not move the column
            update.finish(!(source.place instanceof MoveColumnsRequestPlace));
        }
        else {
            // the parse of the change, already requested with the source, answers it - a loading state stays until then, so no edit
            // starts on the rows and columns the change is about to replace
            myLoader.closeLoadingWhenAnswered(source, update.takeLoading());
        }
    }

    private void perform(PendingUpdate update, UpdateSession session, UpdateOutcome outcome) {
        outcome.myRan = true;
        myDocumentListener.muteChangeEvents();
        try {
            DocumentUtil.executeInBulk(myDocument, true, () -> {
                try {
                    outcome.mySuccess = update.myAction.perform(session);
                }
                catch (Exception e) {
                    outcome.myError = e;
                }
            });
            if (outcome.myError == null) {
                myMutator.finishSession(session, outcome.mySuccess);
            }
        }
        catch (RuntimeException e) {
            outcome.myError = e;
        }
        finally {
            if (update.myKind.myShifting && myDocumentListener.myChangesOccurredWhileMuted) {
                // before the parse of the change takes its snapshot
                myStructureGeneration++;
            }
            boolean answeredByReload = outcome.myError == null
                && outcome.mySuccess
                && !(update.myAction instanceof MoveColumnAction move && move.myFallback);
            outcome.myChanged = myDocumentListener.unmuteChangeEvents(answeredByReload ? update.mySource : null);
        }
    }

    /**
     * The markup cannot move the column by ranges: the whole new text is computed on the pool thread, then written in one command - when
     * the text did not change meanwhile; otherwise the request waits for the parse and runs again.
     */
    @RequiredUIAccess
    private void runMoveFallback(PendingUpdate update, MoveColumnAction move) {
        DataMarkup markup = requireMarkup();
        long stamp = myMarkupStamp;
        myUpdateRunning = true;
        try {
            myLoader.submit(() -> {
                String text = "";
                Exception error = null;
                try {
                    text = markup.prepareMoveColumn(move.myFromColumn, move.myTo);
                }
                catch (Exception e) {
                    error = e;
                }
                String preparedText = text;
                Exception prepareError = error;
                myUIAccess.give(() -> finishMoveFallback(update, markup, stamp, preparedText, prepareError));
            });
        }
        catch (RejectedExecutionException e) {
            myUpdateRunning = false;
            update.reject();
        }
    }

    @RequiredUIAccess
    private void finishMoveFallback(PendingUpdate update, DataMarkup markup, long stamp, String text, @Nullable Exception error) {
        myUpdateRunning = false;
        if (myDisposed) {
            update.reject();
            return;
        }

        if (error != null) {
            LOG.warn(error);
            notifyRequestError(update.mySource, SimpleErrorInfo.create(error));
            update.finish(false);
        }
        else if (markup != myCurrentMarkup || stamp != myDocument.getModificationStamp()) {
            myPendingUpdates.addFirst(update);
        }
        else if (text.isEmpty()) {
            // the markup cannot move columns
            update.finish(false);
        }
        else {
            update.myAction = session -> {
                TextRange range = myRange;
                if (range == null) {
                    myDocument.setText(text);
                }
                else {
                    // Consulo: only the data range is replaced
                    myDocument.replaceString(range.getStartOffset(), range.getEndOffset(), text);
                }
                return true;
            };
            execute(update);
        }
        processPendingUpdates();
    }

    private @Nullable AutoCloseable openLoadingUI(GridRequestSource source) {
        if (source.place instanceof LongActionRequestPlace place) {
            return place.getLoadingUI().get();
        }
        return null;
    }

    private static void closeQuietly(@Nullable AutoCloseable loading) {
        if (loading != null) {
            try {
                loading.close();
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }
    }

    /**
     * Consulo: how an update request depends on the indices of the rows and columns it was resolved against, and how it changes them.
     */
    private enum UpdateKind {
        /**
         * Writes values into rows and columns the grid showed.
         */
        VALUES(true, false),
        /**
         * Appends rows or a column: it names none, and the others keep their indices.
         */
        APPEND(false, false),
        /**
         * Appends a copy of a row or a column the grid showed.
         */
        CLONE(true, false),
        /**
         * Deletes, inserts before, moves or renames rows or columns the grid showed: the others may get other indices.
         */
        STRUCTURE(true, true);

        /**
         * The request runs only against a markup of the generation it was resolved against.
         */
        private final boolean myPositional;
        /**
         * The change of the request starts a new generation.
         */
        private final boolean myShifting;

        UpdateKind(boolean positional, boolean shifting) {
            myPositional = positional;
            myShifting = shifting;
        }
    }

    private final class PendingUpdate {
        private final GridRequestSource mySource;
        private final UpdateKind myKind;
        /**
         * The generation of the markup the rows and columns of the request were resolved against.
         */
        private final long myGeneration;
        private UpdateAction myAction;
        private @Nullable AutoCloseable myLoading;

        private PendingUpdate(GridRequestSource source,
                              UpdateKind kind,
                              long generation,
                              UpdateAction action,
                              @Nullable AutoCloseable loading) {
            mySource = source;
            myKind = kind;
            myGeneration = generation;
            myAction = action;
            myLoading = loading;
        }

        private void closeLoading() {
            closeQuietly(takeLoading());
        }

        /**
         * @return the loading state of the request, which the caller closes from now on
         */
        private @Nullable AutoCloseable takeLoading() {
            AutoCloseable loading = myLoading;
            myLoading = null;
            return loading;
        }

        @RequiredUIAccess
        private void finish(boolean success) {
            closeLoading();
            notifyRequestFinished(mySource, success);
        }

        /**
         * The data source is disposed: only the callback of the request is told.
         */
        private void reject() {
            closeLoading();
            mySource.requestComplete(false);
        }
    }

    private static final class UpdateOutcome {
        private boolean myRan;
        private boolean mySuccess;
        private boolean myChanged;
        private @Nullable Throwable myError;
    }

    private final class MoveColumnAction implements UpdateAction {
        private final GridColumn myFromColumn;
        private final ModelIndex<GridColumn> myTo;
        /**
         * The markup cannot move the column by ranges, nothing was changed.
         */
        private boolean myFallback;

        private MoveColumnAction(GridColumn fromColumn, ModelIndex<GridColumn> to) {
            myFromColumn = fromColumn;
            myTo = to;
        }

        @Override
        public boolean perform(UpdateSession session) {
            myFallback = !requireMarkup().moveColumn(session, myFromColumn, myTo);
            return true;
        }
    }

    // endregion

    public class MyDocumentListener implements DocumentListener {
        private boolean myMuteChangeEvents;
        private boolean myChangesOccurredWhileMuted;
        private boolean myChangeReported;

        @Override
        public void beforeDocumentChange(DocumentEvent event) {
            adjustRange(event);
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            if (myMuteChangeEvents) {
                myChangesOccurredWhileMuted = true;
                return;
            }
            // Consulo: a change made elsewhere may move rows and columns anywhere
            myStructureGeneration++;
            if (!myChangeReported) {
                // Consulo: parsed after the command which made the change, one report for all its changes
                myChangeReported = true;
                myUIAccess.give(() -> {
                    myChangeReported = false;
                    myLoader.documentChanged();
                });
            }
        }

        public void muteChangeEvents() {
            myMuteChangeEvents = true;
            myChangesOccurredWhileMuted = false;
        }

        /**
         * Consulo: parses the changes made while muted, at once.
         *
         * @param source the request the parse answers, or {@code null} when the request is answered otherwise
         * @return whether the document changed while muted
         */
        @RequiredUIAccess
        public boolean unmuteChangeEvents(@Nullable GridRequestSource source) {
            myMuteChangeEvents = false;
            if (myChangesOccurredWhileMuted) {
                myLoader.loadNow(source);
                return true;
            }
            return false;
        }

        private void adjustRange(DocumentEvent e) {
            TextRange range = myRange;
            if (range == null) {
                return;
            }

            int lengthDelta = e.getNewLength() - e.getOldLength();
            if (range.containsRange(e.getOffset(), e.getOffset() + e.getOldLength())) {
                myRange = new TextRange(range.getStartOffset(), range.getEndOffset() + lengthDelta);
            }
            else if (range.getStartOffset() > e.getOffset() + e.getOldLength()) {
                myRange = range.shiftRight(lengthDelta);
            }
            else if (range.intersects(e.getOffset(), e.getOffset() + e.getOldLength())) {
                // expand our range to cover both previous range and appended/prepended region, or contract accordingly
                int startOffset = Math.min(e.getOffset(), range.getStartOffset());
                int endOffset = Math.max(e.getOffset() + e.getNewLength(), range.getEndOffset() + lengthDelta);
                myRange = new TextRange(startOffset, endOffset);
            }
        }
    }

    /**
     * Parses the text on a sequential pool thread. At most one parse runs; requests for a load meanwhile are answered by one more parse
     * after it, which takes a new snapshot - unless the text is still the one the running parse parses, which then answers them too. A
     * parse answers every request which came before its snapshot was taken.
     */
    private class DocumentDataLoader implements GridLoader, Disposable {
        private final ScheduledExecutorService myExecutor;

        /**
         * The requests the next parse answers.
         */
        private final List<GridRequestSource> myWaitingSources = new ArrayList<>();
        /**
         * The requests the running parse answers.
         */
        private List<GridRequestSource> myParsingSources = Collections.emptyList();
        private boolean myParsing;
        /**
         * The modification stamp and the {@link #myStructureGeneration} of the snapshot the running parse parses.
         */
        private long myParsingStamp = -1;
        private long myParsingGeneration = -1;
        /**
         * Consulo: the loading states of the requests whose change is done, kept until the parse which answers the request is shown.
         */
        private final Map<GridRequestSource, AutoCloseable> myAnswerLoadings = new IdentityHashMap<>();
        /**
         * One more parse is wanted after the running one; {@link #myReloadPendingNow}: without the delay of live reloads.
         */
        private boolean myReloadPending;
        private boolean myReloadPendingNow;
        /**
         * The document changed while the data source did not parse changes made elsewhere.
         */
        private boolean myDirty;
        private long myLastParseStartNanos;
        private @Nullable Future<?> myDelayedReload;
        private @Nullable Object myDelayedReloadMarker;
        private @Nullable Future<?> myLastExtractorTask = null;

        private DocumentDataLoader(ApplicationConcurrency concurrency) {
            myExecutor = concurrency.createBoundedScheduledExecutorService(getClass().getSimpleName(), 1);
            // the first change is parsed at once
            myLastParseStartNanos = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(RELOAD_DELAY_MILLIS);
        }

        @Override
        @RequiredUIAccess
        public void reloadCurrentPage(GridRequestSource source) {
            load(source, 0);
        }

        @Override
        @RequiredUIAccess
        public void loadNextPage(GridRequestSource source) {
            load(source, 0);
        }

        @Override
        @RequiredUIAccess
        public void loadPreviousPage(GridRequestSource source) {
            load(source, 0);
        }

        @Override
        @RequiredUIAccess
        public void loadLastPage(GridRequestSource source) {
            load(source, 0);
        }

        @Override
        @RequiredUIAccess
        public void loadFirstPage(GridRequestSource source) {
            load(source, 0);
        }

        @Override
        @RequiredUIAccess
        public void load(GridRequestSource source, int offset) {
            try {
                doLoadData(source);
            }
            catch (Exception e) {
                if (myWaitingSources.remove(source)) {
                    notifyRequestError(source, SimpleErrorInfo.create(e));
                    answer(source, false);
                }
            }
        }

        /**
         * Closes the loading state of a request once the parse which answers it is shown; at once when no parse is to answer it.
         */
        @RequiredUIAccess
        private void closeLoadingWhenAnswered(GridRequestSource source, @Nullable AutoCloseable loading) {
            if (loading == null) {
                return;
            }
            if (containsSource(myWaitingSources, source) || containsSource(myParsingSources, source)) {
                myAnswerLoadings.put(source, loading);
            }
            else {
                closeQuietly(loading);
            }
        }

        private static boolean containsSource(List<GridRequestSource> sources, GridRequestSource source) {
            for (GridRequestSource each : sources) {
                if (each == source) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Answers a request a parse was to answer: its loading state closes first.
         */
        @RequiredUIAccess
        private void answer(GridRequestSource source, boolean success) {
            closeQuietly(myAnswerLoadings.remove(source));
            notifyRequestFinished(source, success);
        }

        @Override
        @RequiredUIAccess
        public void updateTotalRowCount(GridRequestSource source) {
            notifyRequestFinished(source, false);
        }

        @Override
        @RequiredUIAccess
        public void applyFilterAndSorting(GridRequestSource source) {
            notifyRequestFinished(source, false);
        }

        @Override
        public void updateIsTotalRowCountUpdateable() {
        }

        /**
         * Consulo: the grid shows the text as it is - every change of it is parsed again.
         */
        @Override
        public boolean isReloadable() {
            return false;
        }

        @RequiredUIAccess
        private void doLoadData(GridRequestSource source) {
            loadNow(source);
        }

        /**
         * Parses the current text as soon as possible.
         *
         * @param source the request the parse answers, or {@code null}
         */
        @RequiredUIAccess
        private void loadNow(@Nullable GridRequestSource source) {
            if (myDisposed) {
                if (source != null) {
                    myWaitingSources.add(source);
                }
                rejectSources();
                return;
            }
            if (myParsing && isParsingCurrentText()) {
                // Consulo: the running parse is of this very text - it answers the request as well
                if (source != null) {
                    myParsingSources.add(source);
                }
                return;
            }
            if (source != null) {
                myWaitingSources.add(source);
            }
            if (myParsing) {
                myReloadPending = true;
                myReloadPendingNow = true;
                return;
            }
            startParsing();
        }

        /**
         * @return whether the running parse parses the current text, and nothing since its snapshot may have moved rows or columns
         */
        private boolean isParsingCurrentText() {
            return myParsingStamp == myDocument.getModificationStamp() && myParsingGeneration == myStructureGeneration;
        }

        /**
         * The waiting update requests need the markup of the current text.
         */
        @RequiredUIAccess
        private void loadForPendingUpdates() {
            if (myParsing) {
                // the requests are checked again when its result lands
                return;
            }
            loadNow(null);
        }

        /**
         * A change made elsewhere: parsed while the grid listens and the data source is active - the first change after a quiet while
         * at once, later ones at most every {@link #RELOAD_DELAY_MILLIS} ms - otherwise only remembered.
         */
        @RequiredUIAccess
        private void documentChanged() {
            if (myDisposed) {
                return;
            }
            if (!myMutationModel.hasListeners() || !myActive) {
                myDirty = true;
                return;
            }
            if (myParsing) {
                myReloadPending = true;
            }
            else if (myDelayedReload == null) {
                long delay = getReloadDelayMillis();
                if (delay == 0) {
                    startParsing();
                }
                else {
                    scheduleReload(delay);
                }
            }
            // else: the scheduled parse takes this change too
        }

        @RequiredUIAccess
        private void reloadIfDirty() {
            if (!myMutationModel.hasListeners() || (myMarkupStamp == -1 && !myDirty)) {
                // Consulo: before the first load the load the grid asks for parses the text
                return;
            }
            if (myParsing) {
                // Consulo: the running parse is enough when it parses the current text; otherwise one more follows it
                if (!isParsingCurrentText()) {
                    myReloadPending = true;
                }
                return;
            }
            if (myDirty || myMarkupStamp != myDocument.getModificationStamp() || myMarkupGeneration != myStructureGeneration) {
                loadNow(null);
            }
        }

        /**
         * @return how long a parse of changes made elsewhere waits, so it starts {@link #RELOAD_DELAY_MILLIS} ms after the last parse
         */
        private long getReloadDelayMillis() {
            long sinceLastParse = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - myLastParseStartNanos);
            return Math.max(0, Math.min(RELOAD_DELAY_MILLIS, RELOAD_DELAY_MILLIS - sinceLastParse));
        }

        @RequiredUIAccess
        private void scheduleReload(long delayMillis) {
            if (myDelayedReload != null) {
                return;
            }
            Object marker = new Object();
            myDelayedReloadMarker = marker;
            try {
                Runnable reload = () -> myUIAccess.give(() -> delayedReload(marker));
                myDelayedReload = myExecutor.schedule(reload, delayMillis, TimeUnit.MILLISECONDS);
            }
            catch (RejectedExecutionException e) {
                myDelayedReloadMarker = null;
            }
        }

        @RequiredUIAccess
        private void delayedReload(Object marker) {
            if (marker != myDelayedReloadMarker) {
                return;
            }
            myDelayedReloadMarker = null;
            myDelayedReload = null;
            if (myDisposed) {
                return;
            }
            if (!myMutationModel.hasListeners() || !myActive) {
                myDirty = true;
            }
            else if (myParsing) {
                myReloadPending = true;
            }
            else {
                startParsing();
            }
        }

        @RequiredUIAccess
        private void cancelDelayedReload() {
            myDelayedReloadMarker = null;
            Future<?> delayedReload = myDelayedReload;
            myDelayedReload = null;
            if (delayedReload != null) {
                delayedReload.cancel(false);
            }
        }

        @RequiredUIAccess
        private void startParsing() {
            cancelDelayedReload();
            myDirty = false;
            myReloadPending = false;
            myReloadPendingNow = false;
            myLastParseStartNanos = System.nanoTime();

            // the stamp is read before the text: a change in between makes the markup look stale, never newer than it is
            long stamp = myDocument.getModificationStamp();
            long generation = myStructureGeneration;
            CharSequence text = myDocument.getImmutableCharSequence();
            TextRange range = myRange;
            CharSequence sequence = range != null ? range.subSequence(text) : text;

            List<GridRequestSource> sources = new ArrayList<>(myWaitingSources);
            myWaitingSources.clear();
            GridRequestSource requestSource = sources.isEmpty() ? new GridRequestSource(null) : sources.get(0);

            myParsing = true;
            myParsingSources = sources;
            myParsingStamp = stamp;
            myParsingGeneration = generation;
            try {
                myLastExtractorTask = myExecutor.submit(() -> {
                    DataMarkup markup = null;
                    Exception error = null;
                    try {
                        markup = buildMarkup(sequence, requestSource);
                    }
                    catch (Exception e) {
                        error = e;
                    }
                    DataMarkup result = markup;
                    Exception buildError = error;
                    myUIAccess.give(() -> applyMarkup(result, buildError, stamp, generation, sources));
                });
            }
            catch (RejectedExecutionException e) {
                myParsing = false;
                myParsingSources = Collections.emptyList();
                for (GridRequestSource source : sources) {
                    closeQuietly(myAnswerLoadings.remove(source));
                    source.requestComplete(false);
                }
            }
        }

        /**
         * Shows the parsed text: the rows and columns of the grid change in this one UI task, then the requests are answered.
         */
        @RequiredUIAccess
        private void applyMarkup(@Nullable DataMarkup markup,
                                 @Nullable Exception error,
                                 long stamp,
                                 long generation,
                                 List<GridRequestSource> sources) {
            if (myDisposed) {
                // the requests were rejected by the disposal
                return;
            }
            myParsing = false;
            myParsingSources = Collections.emptyList();
            myParsingStamp = -1;
            myParsingGeneration = -1;

            myCurrentMarkup = markup;
            myMarkupStamp = stamp;
            myMarkupGeneration = generation;
            GridRequestSource firstSource = sources.isEmpty() ? new GridRequestSource(null) : sources.get(0);

            if (markup == null) {
                if (error != null) {
                    LOG.warn(error);
                    for (GridRequestSource source : sources) {
                        notifyRequestError(source, SimpleErrorInfo.create(error));
                    }
                }
                myModelUpdater.removeRows(0, myModel.getRowCount());
                myModelUpdater.setColumns(Collections.emptyList());
                for (GridRequestSource source : sources) {
                    answer(source, false);
                }
            }
            else {
                List<GridColumn> columns = markup.columns;
                List<GridRow> rows = markup.rows;

                boolean moveColumns = ContainerUtil.exists(sources, source -> source.place instanceof MoveColumnsRequestPlace);
                if (!sameColumns(columns, myModel.getColumns()) || moveColumns) {
                    myModelUpdater.removeRows(0, myModel.getRowCount());
                    myModelUpdater.setColumns(columns);
                    myModelUpdater.addRows(rows);

                    for (GridRequestSource source : sources) {
                        if (source.place instanceof MoveColumnsRequestPlace info) {
                            info.adjustColumnsUI();
                        }
                    }
                }
                else {
                    int oldRowCount = myModel.getRowCount();
                    int newRowCount = rows.size();
                    myModelUpdater.setRows(0, rows, firstSource);
                    if (oldRowCount > newRowCount) {
                        myModelUpdater.removeRows(newRowCount, oldRowCount - newRowCount);
                    }
                }

                for (GridRequestSource source : sources) {
                    answer(source, true);
                }
            }

            if (myReloadPending) {
                boolean now = myReloadPendingNow || !myWaitingSources.isEmpty();
                myReloadPending = false;
                myReloadPendingNow = false;
                if (now) {
                    loadNow(null);
                }
                else {
                    scheduleReload(getReloadDelayMillis());
                }
            }
            processPendingUpdates();
        }

        private void submit(Runnable task) {
            myLastExtractorTask = myExecutor.submit(task);
        }

        /**
         * The data source is disposed: the requests waiting for a parse are rejected - their callbacks only.
         */
        private void rejectSources() {
            List<GridRequestSource> sources = new ArrayList<>(myParsingSources);
            sources.addAll(myWaitingSources);
            myParsingSources = Collections.emptyList();
            myWaitingSources.clear();
            for (GridRequestSource source : sources) {
                closeQuietly(myAnswerLoadings.remove(source));
                source.requestComplete(false);
            }
            for (AutoCloseable loading : myAnswerLoadings.values()) {
                closeQuietly(loading);
            }
            myAnswerLoadings.clear();
        }

        private static boolean sameColumns(List<GridColumn> columns1, List<GridColumn> columns2) {
            if (columns1.size() != columns2.size()) {
                return false;
            }
            for (int i = 0; i < columns1.size(); i++) {
                if (!Objects.equals(columns1.get(i), columns2.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public void awaitParsingFinished() {
            Future<?> task = myLastExtractorTask;
            if (task == null) {
                return;
            }
            try {
                task.get(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("DocumentDataLoader task termination interrupted", e);
            }
        }

        @Override
        public void dispose() {
            myExecutor.shutdownNow();
        }
    }

    public interface UpdateAction {
        boolean perform(UpdateSession session) throws Exception;
    }
}
