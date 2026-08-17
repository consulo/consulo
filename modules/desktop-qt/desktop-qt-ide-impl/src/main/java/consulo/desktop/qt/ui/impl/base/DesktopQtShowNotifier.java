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
package consulo.desktop.qt.ui.impl.base;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
public final class DesktopQtShowNotifier {
    private DesktopQtShowNotifier() {
    }

    public static void once(Component component, @RequiredUIAccess Runnable action) {
        if (!(component instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.whenBound(widget -> {
            if (widget.isVisible()) {
                action.run();
                return;
            }

            widget.installEventFilter(new QObject(widget) {
                @Override
                public boolean eventFilter(QObject watched, QEvent event) {
                    if (event.type() == QEvent.Type.Show) {
                        QWidget target = (QWidget) watched;

                        target.removeEventFilter(this);
                        disposeLater();

                        action.run();
                    }

                    return false;
                }
            });
        });
    }
}
