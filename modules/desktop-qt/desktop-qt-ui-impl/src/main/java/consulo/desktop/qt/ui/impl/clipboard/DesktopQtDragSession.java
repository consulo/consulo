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
package consulo.desktop.qt.ui.impl.clipboard;

import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QDrag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class DesktopQtDragSession {
    private static int ourDepth;
    private static final List<Runnable> ourPending = new ArrayList<>();

    private DesktopQtDragSession() {
    }

    public static Qt.DropAction exec(QDrag drag, Qt.DropActions supportedActions, Qt.DropAction defaultAction) {
        ourDepth++;
        try {
            return drag.exec(supportedActions, defaultAction);
        }
        finally {
            ourDepth--;
            if (ourDepth == 0 && !ourPending.isEmpty()) {
                List<Runnable> pending = new ArrayList<>(ourPending);
                ourPending.clear();
                QTimer.singleShot(0, () -> pending.forEach(Runnable::run));
            }
        }
    }

    public static void runAfterDrag(Runnable action) {
        if (ourDepth > 0) {
            ourPending.add(action);
        }
        else {
            QTimer.singleShot(0, action::run);
        }
    }
}
