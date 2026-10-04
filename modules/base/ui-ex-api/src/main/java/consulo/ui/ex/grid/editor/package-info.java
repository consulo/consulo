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
 * The cell editors of the data grid: the editor factories ({@link consulo.ui.ex.grid.editor.GridCellEditorFactoryImpl}), the
 * default editor helper and the formatters which turn the text of a cell editor into a value and back. The contracts the grid calls
 * are in {@code consulo.ui.grid.editor}; the editors here describe the widget a frontend shows with
 * {@link consulo.ui.grid.editor.GridCellEditorPresentation} instead of building a widget.
 */
@NullMarked
package consulo.ui.ex.grid.editor;

import org.jspecify.annotations.NullMarked;
