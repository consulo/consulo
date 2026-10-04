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
import consulo.localize.LocalizeValue;
import consulo.ui.grid.ActualGridCellRequest;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory.ValueFormatter;
import consulo.ui.grid.editor.GridCellEditorFactory.ValueParser;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.grid.editor.UnparsedValue.ParsingError;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * The editor has no component: its {@link GridCellEditorTextField} describes the text field a frontend shows, and the error of a
 * refused {@link #stop()} is kept for {@link #getError()}, which the frontend shows as a red outline, an error highlight from the
 * offset to the end of the text and a tooltip.
 * <p/>
 * The field has no completion ({@code TextCompletionProvider}), and no editor has a picker, a browse button or an out-of-range
 * popup - they need a popup component.
 */
public class FormatBasedGridCellEditor extends GridCellEditor.Adapter {
    private final Formatter myFormat;
    private final ValueParser myValueParser;
    private final @Nullable ReservedCellValue myNullValue;
    private final GridCellEditorTextField myTextField;
    private final ActualGridCellRequest<GridRow, GridColumn> myOriginalRequest;
    private @Nullable ParsingError myError;

    public FormatBasedGridCellEditor(GridCellRequest<GridRow, GridColumn> request,
                                     Formatter format,
                                     @Nullable ReservedCellValue nullValue,
                                     GridEditInitiator initiator,
                                     ValueParser valueParser,
                                     ValueFormatter valueFormatter,
                                     boolean multiline) {
        myFormat = format;
        myOriginalRequest = GridCellRequest.actual(request);
        myNullValue = nullValue;
        myValueParser = valueParser;

        myTextField = new GridCellEditorTextField(request, multiline, initiator, valueFormatter);
        myTextField.addDocumentListener(editorText -> {
            setError(null, 0);
            fireEditing(StringUtil.isEmpty(editorText) ? myNullValue : editorText);
        });
        if (nullValue != null) {
            myTextField.setPlaceholder(nullValue.getDisplayName());
        }
        Disposer.register(this, myTextField);
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

    @Override
    public Object getValue() {
        return myValueParser.parse(myTextField.getText());
    }

    @Override
    public boolean stop() {
        String text = myTextField.getText();
        Object parsed = myValueParser.parse(text);
        if (!(parsed instanceof UnparsedValue e)) {
            return true;
        }
        ParsingError error = e.getError();
        setError(error == null ? LocalizeValue.localizeTODO("Failed to parse data").get() : error.message(),
            error == null ? 0 : error.offset());
        return false;
    }

    @Override
    public @Nullable ParsingError getError() {
        return myError;
    }

    private void setError(@Nullable String error, int offset) {
        myError = error == null ? null : new ParsingError(error, offset);
    }

    protected GridCellEditorTextField getTextField() {
        return myTextField;
    }

    protected @Nullable ReservedCellValue getNullValue() {
        return myNullValue;
    }

    protected final DataGrid getGrid() {
        return (DataGrid) myOriginalRequest.getGrid();
    }

    protected final ModelIndex<GridColumn> getColumnIdx() {
        return myOriginalRequest.getColumnIdx();
    }

    protected final @Nullable GridColumn getColumn() {
        return myOriginalRequest.getColumn();
    }

    protected final ActualGridCellRequest<GridRow, GridColumn> getOriginalCellRequest() {
        return myOriginalRequest;
    }

    protected final Formatter getFormat() {
        return myFormat;
    }
}
