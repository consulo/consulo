// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.text.ParsePosition;

public interface Formatter {
    Object parse(String value) throws ParseException;

    @Nullable
    Object parse(String value, ParsePosition position);

    String format(Object value);

    class Wrapper implements Formatter {
        private final Formatter myDelegate;

        public Wrapper(Formatter delegate) {
            myDelegate = delegate;
        }

        @Override
        public Object parse(String value) throws ParseException {
            return myDelegate.parse(value);
        }

        @Override
        public @Nullable Object parse(String value, ParsePosition position) {
            return myDelegate.parse(value, position);
        }

        @Override
        public String format(Object value) {
            return myDelegate.format(value);
        }
    }
}
