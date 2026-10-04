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
package consulo.ui.grid;

/**
 * Kind of the values a grid column holds - what renderers, editors, formatters and sorting work from.
 * <p/>
 * A data source maps its own native types to a kind: JDBC types, BSON types, Android cursor types. A source which is
 * typed per value rather than per column - a MongoDB field, an SQLite or Android cursor column - uses {@link #ANY} and
 * lets each cell value decide.
 *
 * @since 2026-10-03
 */
public enum GridTypeKind {
    TEXT,
    INTEGER,
    /**
     * Exact decimal number, like SQL {@code DECIMAL} or {@code NUMERIC}.
     */
    DECIMAL,
    /**
     * Approximate floating point number.
     */
    FLOAT,
    BOOLEAN,
    DATE,
    TIME,
    TIMESTAMP,
    TIMESTAMP_TZ,
    INTERVAL,
    BINARY,
    UUID,
    JSON,
    XML,
    /**
     * Nested object, like a MongoDB document.
     */
    DOCUMENT,
    ARRAY,
    /**
     * The column has no fixed type - each cell value decides.
     */
    ANY,
    OTHER
}
