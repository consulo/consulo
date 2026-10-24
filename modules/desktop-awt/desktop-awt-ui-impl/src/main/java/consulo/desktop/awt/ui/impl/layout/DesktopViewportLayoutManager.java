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
package consulo.desktop.awt.ui.impl.layout;

import java.awt.*;

/**
 * A layout manager whose height follows the given width, with a preferred viewport of a few rows.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
interface DesktopViewportLayoutManager extends LayoutManager {
    Dimension preferredViewportSize(Container parent, int rows);
}
