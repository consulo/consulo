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
package consulo.ui.ex.grid.editor;

import consulo.disposer.Disposer;
import consulo.ui.grid.ActualGridCellRequest;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorFactory.IsEditableChecker;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import org.jspecify.annotations.Nullable;

/**
 * The editor has no component: its multi-line {@link GridCellEditorTextField} describes the field a frontend shows. There is no
 * completion in the field.
 */
public abstract class GridTextCellEditorBase extends GridCellEditor.Adapter {
    private final IsEditableChecker myEditableChecker;
    private final ActualGridCellRequest<GridRow, GridColumn> myOriginalRequest;
    protected final GridCellEditorTextField myTextField;

    protected @Nullable Object myValue;

    protected GridTextCellEditorBase(GridCellRequest<GridRow, GridColumn> request,
                                     GridEditInitiator initiator,
                                     IsEditableChecker editableChecker,
                                     GridCellEditorFactory.ValueFormatter valueFormatter) {
        myEditableChecker = editableChecker;
        myOriginalRequest = GridCellRequest.actual(request);
        myValue = request.getValue();

        myTextField = new MyGridCellEditorTextField(initiator, request, valueFormatter);
        Disposer.register(this, myTextField);
    }

    public DataGrid getGrid() {
        return (DataGrid) myOriginalRequest.getGrid();
    }

    public ModelIndex<GridColumn> getColumnIdx() {
        return myOriginalRequest.getColumnIdx();
    }

    @Override
    public String getText() {
        return myTextField.getText();
    }

    @Override
    public void setText(String text) {
        myTextField.setText(text);
    }

    @Override
    public GridCellEditorPresentation getPresentation() {
        return myTextField.getPresentation();
    }

    protected boolean isValueEditable() {
        return myEditableChecker.isEditable(myValue, getGrid(), getColumnIdx());
    }

    private class MyGridCellEditorTextField extends GridCellEditorTextField {
        MyGridCellEditorTextField(GridEditInitiator initiator,
                                  GridCellRequest<GridRow, GridColumn> request,
                                  GridCellEditorFactory.ValueFormatter valueFormatter) {
            super(request, true, initiator, valueFormatter);
            addDocumentListener(text -> fireEditing(text));
        }

        @Override
        protected boolean isEditable() {
            return super.isEditable() && isValueEditable();
        }
    }
}
