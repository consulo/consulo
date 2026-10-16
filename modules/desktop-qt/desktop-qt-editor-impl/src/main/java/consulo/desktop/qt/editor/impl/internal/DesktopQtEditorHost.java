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
package consulo.desktop.qt.editor.impl.internal;

import io.qt.gui.QColor;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

interface DesktopQtEditorHost {
    QHBoxLayout getBoxLayout();

    void setOneLine(boolean oneLine);

    void watchFocus(QWidget widget);

    void setPaletteListener(@Nullable Runnable listener);

    QColor getBaseColor();
}
