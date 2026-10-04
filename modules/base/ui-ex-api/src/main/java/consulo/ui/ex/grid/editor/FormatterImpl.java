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

import java.text.ParseException;
import java.text.ParsePosition;

/**
 * A formatter whose {@link #parse(String)} takes the whole text, or throws with its error message, a {@link LocalizeValue}.
 */
public abstract class FormatterImpl implements Formatter {
    @Override
    public Object parse(String value) throws ParseException {
        ParsePosition position = new ParsePosition(0);
        Object res = parse(value, position);
        int errIdx = position.getErrorIndex();
        if (errIdx == -1 && position.getIndex() != value.length()) {
            errIdx = position.getIndex();
        }
        if (errIdx != -1) {
            throw new ParseException(getErrorMessage().get(), errIdx);
        }
        if (res == null) {
            // a formatter which consumed the whole text without a result
            throw new ParseException(getErrorMessage().get(), 0);
        }
        return res;
    }

    protected abstract LocalizeValue getErrorMessage();
}
