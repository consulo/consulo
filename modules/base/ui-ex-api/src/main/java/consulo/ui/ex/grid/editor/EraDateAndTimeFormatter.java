// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.editor;

import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.text.ParsePosition;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class EraDateAndTimeFormatter implements Formatter {
    private final DateAndTimeFormatter<?, ?> myRegularFormatter;
    private final DateAndTimeFormatter<?, ?> myEraFormatter;

    public EraDateAndTimeFormatter(DateAndTimeFormatter<?, ?> regularFormatter, DateAndTimeFormatter<?, ?> eraFormatter) {
        myRegularFormatter = regularFormatter;
        myEraFormatter = eraFormatter;
    }

    @Override
    public Object parse(String value) throws ParseException {
        return myEraFormatter.parse(value);
    }

    @Override
    public @Nullable Object parse(String value, ParsePosition position) {
        return myEraFormatter.parse(value, position);
    }

    @Override
    public String format(Object value) {
        return getFormatter(value).format(value);
    }

    @Override
    public String toString() {
        return myRegularFormatter.toString();
    }

    private Formatter getFormatter(Object value) {
        TemporalAccessor accessor = myRegularFormatter.getTemporalAccessor(value);
        int era = accessor.isSupported(ChronoField.ERA) ? accessor.get(ChronoField.ERA) : 1;
        return era == 0 ? myEraFormatter : myRegularFormatter;
    }
}
