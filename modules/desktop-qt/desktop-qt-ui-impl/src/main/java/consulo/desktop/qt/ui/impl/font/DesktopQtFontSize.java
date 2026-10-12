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
package consulo.desktop.qt.ui.impl.font;

import io.qt.gui.QFont;
import io.qt.widgets.QApplication;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class DesktopQtFontSize {
    private static final float PIXELS_PER_POINT = 4f / 3f;

    private DesktopQtFontSize() {
    }

    public static QFont withPointDelta(QFont font, int pointDelta) {
        QFont base = QApplication.font();
        QFont result = new QFont(font);
        if (base.pointSizeF() > 0) {
            result.setPointSizeF(Math.max(1.0, base.pointSizeF() + pointDelta));
        }
        else {
            result.setPixelSize(Math.max(1, base.pixelSize() + Math.round(pointDelta * PIXELS_PER_POINT)));
        }
        return result;
    }
}
