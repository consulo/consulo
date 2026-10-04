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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.DocumentReferenceProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.action.TableResultColumnHeaderPopupGroup;
import consulo.ui.ex.grid.action.TableResultPopupGroup;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridRow;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * A file editor which shows a file as a grid, next to the text editor of the file.
 * <p/>
 * The document undo of the file works in the grid: every request of a document data source is one command of the document. While a
 * cell editor is open the editor has no document, so the undo does not change the document under the open editor.
 * <p/>
 * A document data source is told whether the grid is shown ({@link DocumentDataHookUp#setActive}): it parses changes made in other
 * editors only while the grid is selected, and catches up when it is selected again. A subclass which overrides {@link #selectNotify()}
 * or {@link #deselectNotify()} calls the super method.
 */
public abstract class TableFileEditor extends TableEditorBase implements DocumentReferenceProvider {
    private final VirtualFile myFile;

    private @Nullable DataGrid myGrid;
    private @Nullable DocumentDataHookUp myDocumentHookUp;
    private boolean mySelected;

    protected TableFileEditor(Project project, VirtualFile file) {
        super(project);
        myFile = file;
    }

    /**
     * Consulo: no document while a cell editor of the grid is open.
     */
    @Override
    public Collection<DocumentReference> getDocumentReferences() {
        DataGrid grid = myGrid;
        if (grid != null && grid.isEditing()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(DocumentReferenceManager.getInstance().create(myFile));
    }

    @Override
    public boolean isValid() {
        return myFile.isValid();
    }

    @Override
    public VirtualFile getFile() {
        return myFile;
    }

    /**
     * Configures the grid before its view is built: its appearance, cell editors and color layers.
     */
    protected abstract void configure(DataGrid grid, DataGridAppearance appearance);

    @RequiredUIAccess
    protected final DataGrid createDataGrid(GridDataHookUp<GridRow, GridColumn> hookUp) {
        return createDataGrid(hookUp, TableResultPopupGroup.ID, TableResultColumnHeaderPopupGroup.ID);
    }

    /**
     * Creates the grid of the data source, with the context menus of the groups, configured by {@link #configure}. The grid and the data
     * source - when it is disposable - are disposed with the editor.
     *
     * @param popupGroupId       the action group of the context menu of the cells
     * @param headerPopupGroupId the action group of the context menu of the column headers
     */
    @RequiredUIAccess
    protected final DataGrid createDataGrid(GridDataHookUp<GridRow, GridColumn> hookUp,
                                            String popupGroupId,
                                            String headerPopupGroupId) {
        // the data source is registered first, so it is disposed after the grid which listens to it
        if (hookUp instanceof Disposable disposableHookUp) {
            Disposer.register(this, disposableHookUp);
        }
        DataGrid grid = GridUtil.createDataGrid(hookUp, popupGroupId, headerPopupGroupId, this::configure);
        Disposer.register(this, grid);
        myGrid = grid;

        if (hookUp instanceof DocumentDataHookUp documentHookUp) {
            myDocumentHookUp = documentHookUp;
            documentHookUp.setActive(mySelected);
        }
        return grid;
    }

    @Override
    @RequiredUIAccess
    public void selectNotify() {
        mySelected = true;
        DocumentDataHookUp hookUp = myDocumentHookUp;
        if (hookUp != null) {
            hookUp.setActive(true);
        }
    }

    @Override
    @RequiredUIAccess
    public void deselectNotify() {
        mySelected = false;
        DocumentDataHookUp hookUp = myDocumentHookUp;
        if (hookUp != null) {
            hookUp.setActive(false);
        }
    }
}
