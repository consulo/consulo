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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.QtMnemonic;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.LabeledLayoutStyle;
import consulo.ui.layout.LayoutConstraint;
import io.qt.core.Qt;
import io.qt.gui.QFont;
import io.qt.widgets.QFrame;
import io.qt.widgets.QLabel;
import io.qt.widgets.QLayout;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLabeledLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements LabeledLayout {
    private static final String FRAME_NAME = "consuloLabeledLayout";

    /**
     * The name is part of the selector because a bare rule would be inherited by every child of the frame, and
     * every row inside the section would then draw a border of its own.
     */
    private static final String STYLE_SHEET = """
        QFrame#%s {
            background: transparent;
            border: 1px solid palette(mid);
            border-radius: 4px;
        }
        """.formatted(FRAME_NAME);

    private final LocalizeValue myLabel;

    public DesktopQtLabeledLayoutImpl(LocalizeValue label) {
        myLabel = label;
    }

    /**
     * A section of the web frontend is a bordered box holding a bold caption row above its content, and not the
     * {@code QGroupBox} of qt, whose caption is cut into the top border and centered by some styles.
     */
    @Override
    protected QWidget createQt(QWidget parent) {
        QFrame frame = new QFrame(parent);
        frame.setObjectName(FRAME_NAME);
        // a plain widget draws nothing of its style sheet box until it is told to
        frame.setAttribute(Qt.WidgetAttribute.WA_StyledBackground, true);
        frame.setStyleSheet(STYLE_SHEET);

        QLayout layout = createLayout();
        layout.setContentsMargins(8, 6, 8, 8);
        frame.setLayout(layout);

        QLabel caption = new QLabel(QtMnemonic.plain(myLabel), frame);

        QFont captionFont = new QFont(caption.font());
        captionFont.setBold(true);
        caption.setFont(captionFont);

        caption.setAlignment(Qt.AlignmentFlag.AlignLeft, Qt.AlignmentFlag.AlignVCenter);

        layout.addWidget(caption);

        return frame;
    }

    @Override
    protected QLayout createLayout() {
        return new QVBoxLayout();
    }

    @Override
    @RequiredUIAccess
    public LabeledLayout set(Component component) {
        addImpl(component, null);
        return this;
    }

    @Override
    public void addStyle(LabeledLayoutStyle style) {
    }
}
