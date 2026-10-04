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

import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditorFactory.ValueParser;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.grid.editor.UnparsedValue.ParsingError;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.util.function.BiFunction;

import static consulo.util.lang.StringUtil.equalsIgnoreCase;

/**
 * A {@link ValueParser} over a {@link Formatter}. {@code <null>} gives NULL whether the column is nullable or not, and {@code unset}
 * gives COMPUTED.
 */
public final class ValueParserWrapper implements ValueParser {
    private final Formatter myParser;
    private final boolean myNullable;
    private final @Nullable ReservedCellValue myEmptyValue;
    private final BiFunction<String, @Nullable ParsingError, UnparsedValue> myUnparsedValueCreator;

    public ValueParserWrapper(Formatter parser,
                              boolean nullable,
                              @Nullable ReservedCellValue emptyValue,
                              BiFunction<String, @Nullable ParsingError, UnparsedValue> unparsedValueCreator) {
        myParser = parser;
        myNullable = nullable;
        myEmptyValue = emptyValue;
        myUnparsedValueCreator = unparsedValueCreator;
    }

    @Override
    public Object parse(String text) {
        if (myEmptyValue != null && StringUtil.isEmptyOrSpaces(text)) {
            return myEmptyValue;
        }
        if (myNullable && equalsIgnoreCase(text, "null") || equalsIgnoreCase(text, "<null>")) {
            return ReservedCellValue.NULL;
        }
        if (equalsIgnoreCase(text, "default") || equalsIgnoreCase(text, "<default>")) {
            return ReservedCellValue.DEFAULT;
        }
        else if (equalsIgnoreCase(text, "generated") || equalsIgnoreCase(text, "<generated>")) {
            return ReservedCellValue.GENERATED;
        }
        else if (equalsIgnoreCase(text, "computed") || equalsIgnoreCase(text, "<computed>")) {
            return ReservedCellValue.COMPUTED;
        }
        else if (equalsIgnoreCase(text, "unset") || equalsIgnoreCase(text, "<unset>")) {
            return ReservedCellValue.COMPUTED;
        }
        try {
            return myParser.parse(text);
        }
        catch (ParseException e) {
            int offset = e.getErrorOffset() != -1 ? e.getErrorOffset() : 0;
            return myUnparsedValueCreator.apply(text, new ParsingError(StringUtil.notNullize(e.getLocalizedMessage()), offset));
        }
    }
}
