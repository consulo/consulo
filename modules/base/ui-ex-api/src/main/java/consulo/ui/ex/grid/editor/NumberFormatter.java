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

import java.text.DecimalFormat;
import java.text.ParsePosition;

public class NumberFormatter extends FormatterImpl {
    private final DecimalFormat myFormat;

    public NumberFormatter(DecimalFormat format) {
        myFormat = format;
    }

    @Override
    public String format(Object value) {
        return myFormat.format(value);
    }

    @Override
    protected LocalizeValue getErrorMessage() {
        return LocalizeValue.localizeTODO("Invalid number");
    }

    @Override
    public @Nullable Object parse(String value, ParsePosition position) {
        return myFormat.parseObject(value, position);
    }

    public void setParseIntegerOnly(boolean value) {
        myFormat.setParseIntegerOnly(value);
    }

    public void setParseBigDecimal(boolean value) {
        myFormat.setParseBigDecimal(value);
    }

    public void setMinimumFractionDigits(int value) {
        myFormat.setMinimumFractionDigits(value);
    }

    public void setMaximumFractionDigits(int value) {
        myFormat.setMaximumFractionDigits(value);
    }

    public void setMinimumIntegerDigits(int value) {
        myFormat.setMinimumIntegerDigits(value);
    }
}
