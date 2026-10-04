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
package consulo.sandboxPlugin.ui.tab;

import consulo.ui.grid.DataConsumer;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataType;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic rows for the grid tester - every value is computed from the row index, so each frontend shows the same
 * table. The columns cover every common kind of value, with nulls, empty and multi-line text, non-latin text, NaN and
 * infinity, and a column whose values change their type from row to row. The {@code java.sql} timestamp and time are made
 * in the system zone, the one the formatter shows them in, so they read the same in any zone - the hours of the timestamp
 * stay away from the night, when the clocks are moved.
 *
 * @since 2026-10-03
 */
final class SampleGridData {
    static final int TOTAL = 2000;

    static final List<GridColumn> COLUMNS = List.of(
        column(0, "id", GridTypeKind.INTEGER, "bigint"),
        column(1, "name", GridTypeKind.TEXT, "varchar(64)"),
        column(2, "quantity", GridTypeKind.INTEGER, "integer"),
        column(3, "price", GridTypeKind.DECIMAL, "numeric(12,2)"),
        column(4, "ratio", GridTypeKind.FLOAT, "double precision"),
        column(5, "active", GridTypeKind.BOOLEAN, "boolean"),
        column(6, "created", GridTypeKind.DATE, "date"),
        column(7, "updated", GridTypeKind.TIMESTAMP, "timestamp"),
        column(8, "uid", GridTypeKind.UUID, "uuid"),
        column(9, "payload", GridTypeKind.JSON, "jsonb"),
        column(10, "blob", GridTypeKind.BINARY, "bytea"),
        column(11, "misc", GridTypeKind.ANY, ""),
        // the scale of the column gives the digits of the fraction of a second
        column(12, "logged_at", GridTypeKind.TIMESTAMP, "timestamp(6)", 26, 6),
        column(13, "alarm", GridTypeKind.TIME, "time", 8, 0),
        // no scale - the fraction has as many digits as the value needs
        column(14, "event_at", GridTypeKind.TIMESTAMP_TZ, "timestamptz")
    );

    private static final ZoneOffset[] OFFSETS = {
        ZoneOffset.UTC,
        ZoneOffset.ofHoursMinutes(5, 30),
        ZoneOffset.ofHours(-8),
        ZoneOffset.ofHours(3)
    };

    /**
     * The binary values, made once. A reloaded row keeps its pending changes only while each of its values {@code equals} the one
     * loaded before, which a new array never does - a database row holds a LOB value which compares by content instead.
     */
    private static final byte[][] BLOBS = createBlobs();

    private SampleGridData() {
    }

    /**
     * @param start the index of the first row, starting from 0
     * @return the rows {@code [start, start + count)}, numbered from {@code start + 1}
     */
    static List<GridRow> fetch(int start, int count) {
        List<GridRow> rows = new ArrayList<>(Math.max(count, 0));
        for (int k = 0; k < count; k++) {
            int index = start + k;
            rows.add(DataConsumer.Row.create(index, values(index)));
        }
        return rows;
    }

    private static @Nullable Object[] values(int i) {
        @Nullable Object[] values = new @Nullable Object[COLUMNS.size()];
        values[0] = (long) i;
        values[1] = name(i);
        values[2] = i % 13 == 0 ? null : (i * 37) % 2000 - 1000;
        values[3] = i % 11 == 0 ? null : BigDecimal.valueOf((long) i * 1379 % 10_000_000, 2);
        values[4] = ratio(i);
        values[5] = active(i);
        values[6] = i % 29 == 0 ? null : LocalDate.of(2020, 1, 1).plusDays(i);
        values[7] = i % 31 == 0 ? null : LocalDateTime.of(2024, 1, 1, 0, 0).plusMinutes(i * 37L);
        values[8] = i % 37 == 0 ? null : uuid("row-" + i);
        values[9] = i % 41 == 0 ? null : "{\"id\":" + i + ",\"ok\":" + (i % 2 == 0) + "}";
        values[10] = i % 43 == 0 ? null : BLOBS[i];
        values[11] = misc(i);
        values[12] = loggedAt(i);
        values[13] = i % 53 == 0 ? null : Time.valueOf(LocalTime.of(i * 5 % 24, i * 11 % 60, i * 17 % 60));
        values[14] = eventAt(i);
        return values;
    }

    private static @Nullable String name(int i) {
        if (i % 17 == 0) {
            return null;
        }
        if (i % 23 == 0) {
            return "";
        }
        if (i % 50 == 0) {
            return "Ünïcödé ✓ 日本語 — " + i;
        }
        if (i % 97 == 0) {
            return "line one\nline two";
        }
        return String.format(Locale.ROOT, "Item %04d", i);
    }

    private static @Nullable Boolean active(int i) {
        return switch (i % 3) {
            case 0 -> Boolean.TRUE;
            case 1 -> Boolean.FALSE;
            default -> null;
        };
    }

    private static @Nullable Double ratio(int i) {
        if (i % 19 == 0) {
            return null;
        }
        if (i == 3) {
            return Double.NaN;
        }
        if (i == 4) {
            return Double.POSITIVE_INFINITY;
        }
        return i / 7.0;
    }

    private static byte[][] createBlobs() {
        byte[][] blobs = new byte[TOTAL][];
        for (int i = 0; i < TOTAL; i++) {
            blobs[i] = bytes(i);
        }
        return blobs;
    }

    private static byte[] bytes(int i) {
        byte[] bytes = new byte[i % 16];
        for (int k = 0; k < bytes.length; k++) {
            bytes[k] = (byte) (i + k);
        }
        return bytes;
    }

    private static @Nullable Object misc(int i) {
        return switch (i % 9) {
            case 0 -> i;
            case 1 -> "text " + i;
            case 2 -> i % 2 == 0;
            // never the same whole part as an integer of this column - a number of another type is compared by its whole
            // part when sorting, and a tie between an integer and a double would make the order inconsistent
            case 3 -> i + 0.25;
            case 4 -> LocalDate.of(2020, 1, 1).plusDays(i);
            case 5 -> uuid("misc-" + i);
            case 6 -> null;
            case 7 -> List.of(1, 2, 3);
            default -> Map.of("k", "v");
        };
    }

    private static @Nullable Timestamp loggedAt(int i) {
        if (i % 47 == 0) {
            return null;
        }
        // whole seconds now and then, which still show all six digits
        int nanos = i % 5 == 0 ? 0 : i * 7919 % 1_000_000 * 1000;
        LocalDateTime dateTime = LocalDate.of(2024, 1, 1).plusDays(i / 4).atTime(8 + i % 12, i * 7 % 60, i * 13 % 60, nanos);
        return Timestamp.valueOf(dateTime);
    }

    private static @Nullable OffsetDateTime eventAt(int i) {
        if (i % 59 == 0) {
            return null;
        }
        int nanos = switch (i / 4 % 4) {
            case 0 -> 0;
            case 1 -> 500_000_000;
            case 2 -> 123_000_000;
            default -> (int) (i * 1_000_003L % 1_000_000_000);
        };
        return LocalDateTime.of(2025, 6, 1, 0, 0).plusSeconds(i * 4321L).withNano(nanos).atOffset(OFFSETS[i % OFFSETS.length]);
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static GridColumn column(int columnNum, String name, GridTypeKind kind, String typeName) {
        return new DataConsumer.Column(columnNum, name, GridDataType.of(kind, typeName));
    }

    private static GridColumn column(int columnNum, String name, GridTypeKind kind, String typeName, int precision, int scale) {
        return new DataConsumer.Column(columnNum, name, GridDataType.of(kind, typeName), precision, scale);
    }
}
