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

import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.editor.GridCellEditorHelper;

import java.util.Date;
import java.util.Objects;

import static consulo.ui.ex.grid.editor.FormatterCreator.getDateKey;

/**
 * The editor is a plain text field: a date picker would need a popup component.
 */
public final class DefaultDateEditorFactory extends DefaultTemporalEditorFactory {
    @Override
    protected Formatter getFormatInner(GridCellRequest<GridRow, GridColumn> request) {
        GridColumn c = Objects.requireNonNull(request.getColumn());
        return FormatterCreator.get(request.getGrid()).create(getDateKey(c, null, FormatsCache.get(request.getGrid())));
    }

    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request) == GridTypeKind.DATE
            ? SUITABILITY_MIN
            : SUITABILITY_UNSUITABLE;
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        ValueParser parser = super.getValueParser(request);
        return text -> {
            Object v = parser.parse(text);
            return v instanceof Date ? new java.sql.Date(((Date) v).getTime()) : v;
        };
    }
}
