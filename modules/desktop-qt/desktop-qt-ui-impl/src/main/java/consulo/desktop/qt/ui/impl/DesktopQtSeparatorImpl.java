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

import consulo.ui.Separator;
import consulo.ui.SeparatorStyle;
import io.qt.widgets.QFrame;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtSeparatorImpl extends QtComponentDelegate<QFrame> implements Separator {
    /** as in the web frontend, a toolbar divider is only as long as the icons standing beside it */
    private static final int LINE_LENGTH = 16;

    private final SeparatorStyle myStyle;

    public DesktopQtSeparatorImpl(SeparatorStyle style) {
        myStyle = style;
    }

    @Override
    protected QFrame createQt(QWidget parent) {
        return new QFrame(parent);
    }

    @Override
    protected void initialize(QFrame component) {
        component.setFrameShadow(QFrame.Shadow.Plain);

        if (myStyle == SeparatorStyle.VERTICAL) {
            component.setFrameShape(QFrame.Shape.VLine);
            component.setFixedWidth(1);
            component.setFixedHeight(LINE_LENGTH);
        }
        else {
            component.setFrameShape(QFrame.Shape.HLine);
            component.setFixedHeight(1);
            component.setFixedWidth(LINE_LENGTH);
        }

        // a rule stretched over the cross axis of the row would be as tall as the whole button box
        component.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);
    }

    @Override
    public SeparatorStyle getSeparatorStyle() {
        return myStyle;
    }
}
