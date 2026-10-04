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

import java.sql.Time;
import java.util.Date;

/**
 * In the list of {@link GridCellEditorFactoryImpl} it is never chosen: {@code DefaultTimestampEditorFactory(TIME)} comes first and is
 * as suitable.
 */
public class DefaultTimeEditorFactory extends DefaultTemporalEditorFactory {
    @Override
    protected Formatter getFormatInner(GridCellRequest<GridRow, GridColumn> request) {
        FormatsCache cache = FormatsCache.get(request.getGrid());
        return cache.get(FormatsCache.getTimeFormatProvider(null, null), FormatterCreator.get(request.getGrid()));
    }

    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request) == GridTypeKind.TIME
            ? SUITABILITY_MIN
            : SUITABILITY_UNSUITABLE;
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        ValueParser parser = super.getValueParser(request);
        return text -> {
            Object v = parser.parse(text);
            return v instanceof Date ? new Time(((Date) v).getTime()) : v;
        };
    }
}
