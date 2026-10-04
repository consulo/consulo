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
package consulo.ui.ex.grid.csv;

import consulo.ui.grid.GridRequestSource;

/**
 * A data source whose records are parsed with a {@link CsvFormat}. The grid actions which depend on the format - first row is
 * header, add and rename column - look for this interface on the data source of the grid, so they work with any such data source
 * without depending on the module which implements it.
 *
 * @since 2026-10-04
 */
public interface CsvFormatHookUp {
    /**
     * @return the format the records are parsed with now
     */
    CsvFormat getFormat();

    /**
     * Parses the records again with another format. The text of the data source does not change.
     *
     * @param source completes once the records of the new format are loaded
     */
    void setFormat(CsvFormat format, GridRequestSource source);
}
