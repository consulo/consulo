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
package consulo.desktop.qt.editor.impl.internal.benchmark;

import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.gui.QPaintEvent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class DesktopQtEditorBenchmarkPaintWatcher extends QObject {
    private boolean myPainted;
    private long myPaintedPixels;

    public void reset() {
        myPainted = false;
        myPaintedPixels = 0;
    }

    public boolean isPainted() {
        return myPainted;
    }

    public long getPaintedPixels() {
        return myPaintedPixels;
    }

    @Override
    public boolean eventFilter(@Nullable QObject watched, @Nullable QEvent event) {
        if (event == null || event.type() != QEvent.Type.Paint) {
            return false;
        }

        myPainted = true;
        if (event instanceof QPaintEvent paintEvent) {
            QRect rect = paintEvent.rect();
            myPaintedPixels += (long) rect.width() * rect.height();
        }
        return false;
    }
}
