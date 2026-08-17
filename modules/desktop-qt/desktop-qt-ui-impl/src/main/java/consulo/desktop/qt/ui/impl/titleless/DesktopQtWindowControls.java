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
package consulo.desktop.qt.ui.impl.titleless;

import io.qt.core.Qt;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * The minimize, maximize and close buttons of a titleless window, and the only place the actions behind them are
 * written.
 * <p/>
 * It is a widget of its own rather than three widgets in a header, because a window which carries no header shows
 * the same three controls floating over its content - the welcome screen is one - and both have to answer a click
 * the same way.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtWindowControls extends QWidget {
    private final List<DesktopQtWindowButton> myButtons = new ArrayList<>();

    private final DesktopQtWindowButton myMaximizeButton;

    public DesktopQtWindowControls(QWidget parent) {
        super(parent);

        // the cells sit against each other, the way the decoration of the desktop draws them, so the widget covers
        // the buttons and nothing else - what is left over of a header is the window itself, and a press on it is
        // what drags the window
        QHBoxLayout layout = new QHBoxLayout();
        layout.setContentsMargins(0, 0, 0, 0);
        layout.setSpacing(0);
        setLayout(layout);

        setAttribute(Qt.WidgetAttribute.WA_NoSystemBackground, true);

        addButton(layout, new DesktopQtWindowButton(this, DesktopQtWindowButton.Kind.MINIMIZE, this::minimize));

        myMaximizeButton = new DesktopQtWindowButton(this, DesktopQtWindowButton.Kind.MAXIMIZE, this::toggleMaximized);
        addButton(layout, myMaximizeButton);

        addButton(layout, new DesktopQtWindowButton(this, DesktopQtWindowButton.Kind.CLOSE, this::closeWindow));
    }

    private void addButton(QHBoxLayout layout, DesktopQtWindowButton button) {
        myButtons.add(button);
        layout.addWidget(button);
    }

    /**
     * Follows the state of the window: the button offers to restore a maximized frame rather than to maximize it
     * again, and every button is drawn dimmed while the window does not hold the focus.
     */
    public void updateWindowState() {
        myMaximizeButton.setKind(
            window().isMaximized() ? DesktopQtWindowButton.Kind.RESTORE : DesktopQtWindowButton.Kind.MAXIMIZE
        );

        for (DesktopQtWindowButton button : myButtons) {
            button.update();
        }
    }

    public void minimize() {
        window().showMinimized();
    }

    public void toggleMaximized() {
        QWidget window = window();

        window.setWindowState(window.isMaximized() ? Qt.WindowState.WindowNoState : Qt.WindowState.WindowMaximized);

        updateWindowState();
    }

    public void closeWindow() {
        window().close();
    }
}
