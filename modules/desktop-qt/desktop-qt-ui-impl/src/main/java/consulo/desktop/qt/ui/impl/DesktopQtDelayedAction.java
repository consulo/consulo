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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.base.DesktopQtImageWidget;
import consulo.ui.DelayedAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import io.qt.core.QPoint;
import io.qt.core.Qt;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

public final class DesktopQtDelayedAction implements DelayedAction {
    private @Nullable QWidget myHost;

    private DesktopQtDelayedAction(QWidget host) {
        myHost = host;
    }

    @RequiredUIAccess
    public static DelayedAction start(QPoint globalPosition) {
        QWidget host = new QWidget(null, Qt.WindowType.ToolTip, Qt.WindowType.FramelessWindowHint, Qt.WindowType.WindowStaysOnTopHint);
        host.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating, true);
        host.setAttribute(Qt.WidgetAttribute.WA_TransparentForMouseEvents, true);

        DesktopQtImageWidget busy = new DesktopQtImageWidget(host);
        busy.setImage(Image.busy());

        QHBoxLayout layout = new QHBoxLayout(host);
        layout.setContentsMargins(0, 0, 0, 0);
        layout.addWidget(busy);

        host.adjustSize();
        host.move(globalPosition.x() - host.width() / 2, globalPosition.y() - host.height() / 2);
        host.show();

        return new DesktopQtDelayedAction(host);
    }

    @RequiredUIAccess
    @Override
    public void stop() {
        QWidget host = myHost;
        myHost = null;

        if (host == null || host.isDisposed()) {
            return;
        }

        host.close();
        host.disposeLater();
    }
}
