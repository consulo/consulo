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

import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.editor.GridCellEditorHelper;
import org.jspecify.annotations.Nullable;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;

/**
 * The temporal formats read and make the {@link Date} values ({@code java.sql.Date}, {@link Time}, {@link Timestamp}) in UTC
 * ({@link DataGridFormattersUtilCore#getDefaultTimeZone()}). A data source of the grid makes them in the system zone, which
 * {@code consulo.ui.grid.BaseObjectFormatter} shows them in.
 * {@link #adaptFormatter} therefore puts a value of the cell into UTC with the same wall time before it is formatted, and takes a
 * parsed value back with the wall time it was parsed with: to the class of the database value of the cell when that is a
 * {@code java.time} value, otherwise to the class the format parses to, in the system zone. The date and timestamp formats refuse a
 * {@code java.time} value of the cell, so the factory formats it with the grid's object formatter.
 */
public abstract class DefaultTemporalEditorFactory extends FormatBasedGridCellEditorFactory {
    @Override
    protected boolean makeFormatterLenient(CoreGrid<GridRow, GridColumn> grid) {
        return GridCellEditorHelper.get(grid).useLenientFormatterForTemporalObjects(grid);
    }

    @Override
    protected Formatter adaptFormatter(Formatter formatter, GridCellRequest<GridRow, GridColumn> request) {
        return new CellValueFormatter(formatter, () -> request.isValid() ? GridCellRequest.getDatabaseValue(request) : null);
    }

    private static final class CellValueFormatter extends Formatter.Wrapper {
        private final Formatter myFormatter;
        private final Supplier<@Nullable Object> myDatabaseValue;

        CellValueFormatter(Formatter formatter, Supplier<@Nullable Object> databaseValue) {
            super(formatter);
            myFormatter = formatter;
            myDatabaseValue = databaseValue;
        }

        @Override
        public Object parse(String value) throws ParseException {
            return fromFormatterValue(super.parse(value));
        }

        @Override
        public @Nullable Object parse(String value, ParsePosition position) {
            Object parsed = super.parse(value, position);
            return parsed == null ? null : fromFormatterValue(parsed);
        }

        @Override
        public String format(Object value) {
            return super.format(value instanceof Date date ? moveWallTime(date, TimeZone.getDefault(), getFormatterTimeZone()) : value);
        }

        @Override
        public String toString() {
            return myFormatter.toString();
        }

        private Object fromFormatterValue(Object parsed) {
            if (!(parsed instanceof Date date)) {
                return parsed;
            }
            Object databaseValue = myDatabaseValue.get();
            if (databaseValue instanceof LocalDate || databaseValue instanceof LocalDateTime || databaseValue instanceof LocalTime) {
                LocalDateTime wallTime = getWallTime(date);
                return databaseValue instanceof LocalDate ? wallTime.toLocalDate()
                    : databaseValue instanceof LocalTime ? wallTime.toLocalTime()
                    : wallTime;
            }
            return moveWallTime(date, getFormatterTimeZone(), TimeZone.getDefault());
        }

        private static TimeZone getFormatterTimeZone() {
            return DataGridFormattersUtilCore.getDefaultTimeZone();
        }

        /**
         * @return a value of the class of the date which shows in {@code to} the wall time the date shows in {@code from}; the fields
         * are moved with calendars, like {@code SimpleDateFormat}s do, so a date before the Gregorian reform keeps its day
         */
        private static Date moveWallTime(Date date, TimeZone from, TimeZone to) {
            Calendar source = createCalendar(from);
            source.setTime(date);
            Calendar target = createCalendar(to);
            target.clear();
            for (int field : new int[]{Calendar.ERA, Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
                Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND}) {
                target.set(field, source.get(field));
            }
            long millis = target.getTimeInMillis();
            if (date instanceof Timestamp timestamp) {
                Timestamp result = new Timestamp(millis);
                result.setNanos(timestamp.getNanos());
                return result;
            }
            if (date instanceof Time) {
                return new Time(millis);
            }
            if (date instanceof java.sql.Date) {
                return new java.sql.Date(millis);
            }
            return new Date(millis);
        }

        /**
         * @return the wall time a parsed date shows in the zone of the formats
         */
        private static LocalDateTime getWallTime(Date date) {
            Calendar calendar = createCalendar(getFormatterTimeZone());
            calendar.setTime(date);
            int yearOfEra = calendar.get(Calendar.YEAR);
            int year = calendar.get(Calendar.ERA) == GregorianCalendar.BC ? 1 - yearOfEra : yearOfEra;
            int month = calendar.get(Calendar.MONTH) + 1;
            // a Julian day the ISO calendar does not have, like the 29th of February 1500, becomes the last day of the month
            int day = Math.min(calendar.get(Calendar.DAY_OF_MONTH), YearMonth.of(year, month).lengthOfMonth());
            int nanos = date instanceof Timestamp timestamp ? timestamp.getNanos() : calendar.get(Calendar.MILLISECOND) * 1_000_000;
            return LocalDateTime.of(year, month, day, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND), nanos);
        }

        private static Calendar createCalendar(TimeZone zone) {
            // a GregorianCalendar whatever the locale - Calendar.getInstance() may give a Buddhist or Japanese one
            return new GregorianCalendar(zone, Locale.ROOT);
        }
    }
}
