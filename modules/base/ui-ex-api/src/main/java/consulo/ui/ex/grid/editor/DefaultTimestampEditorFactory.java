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

import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.editor.GridCellEditorHelper;
import org.jspecify.annotations.Nullable;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

import static consulo.ui.ex.grid.editor.FormatterCreator.getDateKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getTimeKey;
import static consulo.ui.ex.grid.editor.FormatterCreator.getTimestampKey;

/**
 * Notes:
 * <ul>
 * <li>the mode is a {@link Mode} of this class;</li>
 * <li>the expected type is a {@link GridTypeKind};</li>
 * <li>the editor is a plain text field: a date and time picker would need a popup component.</li>
 * </ul>
 */
public class DefaultTimestampEditorFactory extends DefaultTemporalEditorFactory {
    /**
     * Which part of a timestamp the editor edits.
     */
    public enum Mode {
        DATE,
        TIME,
        DATETIME
    }

    private final Mode myCalendarMode;

    public DefaultTimestampEditorFactory(Mode mode) {
        myCalendarMode = mode;
    }

    protected final Mode getCalendarMode() {
        return myCalendarMode;
    }

    protected final GridTypeKind getExpectedTypeKind() {
        return switch (myCalendarMode) {
            case DATE -> GridTypeKind.DATE;
            case TIME -> GridTypeKind.TIME;
            case DATETIME -> GridTypeKind.TIMESTAMP;
        };
    }

    protected final FormatterCreator.FormatterKey<? extends Formatter> getFormatterKey(DataGrid grid, @Nullable GridColumn c) {
        FormatsCache formatsCache = FormatsCache.get(grid);
        return switch (myCalendarMode) {
            case DATE -> getDateKey(c, null, formatsCache);
            case TIME -> getTimeKey(c, null, formatsCache);
            case DATETIME -> getTimestampKey(c, null, formatsCache);
        };
    }

    @Override
    protected Formatter getFormatInner(GridCellRequest<GridRow, GridColumn> request) {
        GridColumn c = Objects.requireNonNull(request.getColumn());
        return FormatterCreator.get(request.getGrid()).create(getFormatterKey((DataGrid) request.getGrid(), c));
    }

    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request) == getExpectedTypeKind()
            ? SUITABILITY_MIN
            : SUITABILITY_UNSUITABLE;
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        ValueParser parser = super.getValueParser(request);
        return text -> {
            Object v = parser.parse(text);
            if (!(v instanceof Date date)) {
                return v;
            }
            return switch (myCalendarMode) {
                case DATE -> date instanceof java.sql.Date ? date : new java.sql.Date(date.getTime());
                case TIME -> date instanceof Time ? date : new Time(date.getTime());
                case DATETIME -> date instanceof Timestamp ? date : new Timestamp(date.getTime());
            };
        };
    }
}
