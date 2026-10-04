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
 * Data grids over documents. A document data source parses the text of a document into rows in the background and writes every
 * request of the grid back as one command of minimal document changes, so the document undo reverts it; a file editor shows such a
 * data source as a grid next to the text editor of the file. The comma separated values flavour parses with a
 * {@link consulo.ui.ex.grid.csv.CsvFormatHookUp format}.
 */
@NullMarked
package consulo.grid.editor;

import org.jspecify.annotations.NullMarked;
