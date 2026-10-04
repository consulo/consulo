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

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.text.ParsePosition;
import java.util.List;

/**
 * Parses with the first of its formatters which takes the whole text, and formats with the first one. The error message is a
 * {@link LocalizeValue}.
 */
public class CompositeFormatter extends FormatterImpl {
    private final Formatter[] myFormatters;
    private final Formatter myBaseFormatter;
    private final LocalizeValue myErrorMessage;

    public CompositeFormatter(LocalizeValue errorMessage, List<Formatter> formatters) {
        this(errorMessage, formatters.toArray(Formatter[]::new));
    }

    public CompositeFormatter(LocalizeValue errorMessage, Formatter... formatters) {
        if (formatters.length == 0) {
            throw new IllegalArgumentException("Formatters must contains at least one formatter");
        }
        myBaseFormatter = formatters[0];
        myFormatters = formatters;
        myErrorMessage = errorMessage;
    }

    @Override
    public @Nullable Object parse(String value, ParsePosition position) {
        ParsePosition internal = new ParsePosition(0);
        for (Formatter formatter : myFormatters) {
            internal.setIndex(0);
            internal.setErrorIndex(-1);
            Object result = formatter.parse(value, internal);
            if (internal.getErrorIndex() == -1 && internal.getIndex() == value.length()) {
                position.setIndex(internal.getIndex());
                return result;
            }
        }
        position.setErrorIndex(internal.getErrorIndex());
        return null;
    }

    @Override
    protected LocalizeValue getErrorMessage() {
        return myErrorMessage;
    }

    @Override
    public String format(Object value) {
        return myBaseFormatter.format(value);
    }

    @Override
    public String toString() {
        return myBaseFormatter.toString();
    }
}
