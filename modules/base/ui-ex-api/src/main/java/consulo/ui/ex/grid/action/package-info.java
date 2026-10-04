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
 * The row and column actions of the data grid - add, insert, clone, delete, rename, clear - for its context menus and shortcuts.
 * They find the grid in the data context, and ask its data source what it allows through {@link consulo.ui.grid.GridHelper} and
 * its mutator, so they work on every frontend.
 */
@NullMarked
package consulo.ui.ex.grid.action;

import org.jspecify.annotations.NullMarked;
