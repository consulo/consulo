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

import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.GridDataHookUpBase;
import consulo.ui.ex.grid.MultiPageModelImpl;
import consulo.ui.ex.grid.MutationsStorageImpl;
import consulo.ui.grid.DataGridListModel;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridLoader;
import consulo.ui.grid.GridMutationModel;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridStorageAndModelUpdater;
import consulo.ui.grid.MutationsStorage;
import consulo.ui.grid.editor.GridCellEditorHelper;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An editable data source over {@link SampleGridData}, paged by 100 rows. Edits stay pending in {@link DemoGridMutator} until they
 * are submitted; the submitted values are kept here and put back into the rows of every load, so they survive Reload and paging.
 * It can be told to fail its next request - a load or a Submit - or to answer slowly, to show how the grid reports an error and a
 * request in progress.
 * <p/>
 * The requests are answered on a background thread; the answer is reported on the UI thread, through the {@link UIAccess} given by
 * whoever creates the grid there.
 *
 * @since 2026-10-03
 */
final class DemoGridHookUp extends GridDataHookUpBase<GridRow, GridColumn> implements Disposable {
    static final int PAGE_SIZE = 100;

    private static final long DELAY_MILLIS = 150;
    private static final long SLOW_DELAY_MILLIS = 2000;

    private final DataGridListModel myDataModel;
    private final MultiPageModelImpl<GridRow, GridColumn> myPageModel;
    private final GridMutationModel myMutationModel;
    private final DemoGridMutator myMutator;
    private final DemoGridLoader myLoader;
    /**
     * The submitted values by the number of their row and their column - the sample rows are computed again on every load.
     * Used on the UI thread only.
     */
    private final Map<Integer, Map<Integer, @Nullable Object>> myCommittedValues = new HashMap<>();

    private volatile boolean myFailNext;
    private volatile boolean mySlow;
    private volatile boolean myDisposed;

    /**
     * @param uiAccess the UI thread of the grid, where the loads and the Submits report their answers
     */
    DemoGridHookUp(UIAccess uiAccess) {
        // 5L and 5 are the same value - an edited number need not be of the class of the loaded one
        myDataModel = new DataGridListModel(GridCellEditorHelper::areValuesEqual);
        // without settings the page would hold 500 rows
        myPageModel = new MultiPageModelImpl<>(myDataModel, null);
        myPageModel.setPageSize(PAGE_SIZE);
        // reads the data model through the hook-up, so it is created after it
        myMutationModel = new GridMutationModel(this);
        // sized for a page while the model is still empty; it grows with a bigger page
        MutationsStorage storage = new MutationsStorageImpl(myDataModel, PAGE_SIZE, SampleGridData.COLUMNS.size());
        myMutator = new DemoGridMutator(this, storage, uiAccess);
        // a reloaded row keeps its pending changes, unless it changed or the user dropped them
        GridStorageAndModelUpdater updater = new GridStorageAndModelUpdater(myDataModel, myMutationModel, storage);
        myLoader = new DemoGridLoader(this, myPageModel, updater, uiAccess);
    }

    @Override
    public DataGridListModel getDataModel() {
        return myDataModel;
    }

    @Override
    public GridMutationModel getMutationModel() {
        return myMutationModel;
    }

    @Override
    public MultiPageModelImpl<GridRow, GridColumn> getPageModel() {
        return myPageModel;
    }

    @Override
    public GridLoader getLoader() {
        return myLoader;
    }

    @Override
    public DemoGridMutator getMutator() {
        return myMutator;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Keeps a submitted value for {@link #applyCommittedValues}.
     *
     * @param rowNum the number of the row in the source, starting from 1 ({@link GridRow#getRowNum()})
     */
    @RequiredUIAccess
    void putCommittedValue(int rowNum, int column, @Nullable Object value) {
        myCommittedValues.computeIfAbsent(rowNum, key -> new HashMap<>()).put(column, value);
    }

    /**
     * Puts the submitted values into freshly computed rows, as a database answers with what was written into it.
     */
    @RequiredUIAccess
    void applyCommittedValues(List<? extends GridRow> rows) {
        if (myCommittedValues.isEmpty()) {
            return;
        }
        for (GridRow row : rows) {
            Map<Integer, @Nullable Object> values = myCommittedValues.get(row.getRowNum());
            if (values == null) {
                continue;
            }
            for (Map.Entry<Integer, @Nullable Object> entry : values.entrySet()) {
                row.setValue(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @return how long a request - a load or a Submit - takes to answer
     */
    long getRequestDelayMillis() {
        return mySlow ? SLOW_DELAY_MILLIS : DELAY_MILLIS;
    }

    boolean isFailNext() {
        return myFailNext;
    }

    void setFailNext(boolean failNext) {
        myFailNext = failNext;
    }

    /**
     * @return whether the next request was to fail - the flag is cleared, so only one request fails
     */
    boolean consumeFailNext() {
        boolean failNext = myFailNext;
        myFailNext = false;
        return failNext;
    }

    void setSlow(boolean slow) {
        mySlow = slow;
    }

    boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }
}
