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

/**
 * Comma separated values for the data grid: the record format ({@link consulo.ui.ex.grid.csv.CsvFormat}), its parser and
 * formatter, the stored formats, and the seam ({@link consulo.ui.ex.grid.csv.CsvFormatHookUp}) through which grid actions reach a
 * data source parsed with a format. It needs no documents or files: the data sources which edit a document are in
 * {@code consulo.grid.editor}.
 */
@NullMarked
package consulo.ui.ex.grid.csv;

import org.jspecify.annotations.NullMarked;
