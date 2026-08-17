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
package consulo.desktop.qt.execution.terminal;

import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.TerminalTextBuffer;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The terminal screen as a component of the api, so it can be put in a tool window like anything else.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTerminalComponent extends QtComponentDelegate<QWidget> {
    private final TerminalTextBuffer myTextBuffer;
    private final JediTerminal myTerminal;

    private final Consumer<String> myInputConsumer;
    private final BiConsumer<Integer, Integer> myResizeConsumer;

    public DesktopQtTerminalComponent(
        TerminalTextBuffer textBuffer,
        JediTerminal terminal,
        Consumer<String> inputConsumer,
        BiConsumer<Integer, Integer> resizeConsumer
    ) {
        myTextBuffer = textBuffer;
        myTerminal = terminal;
        myInputConsumer = inputConsumer;
        myResizeConsumer = resizeConsumer;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        return new DesktopQtTerminalWidget(parent, myTextBuffer, myTerminal, myInputConsumer, myResizeConsumer);
    }

    /**
     * Asks the screen to be drawn again, which is what the emulator writing into the buffer means for the ui.
     */
    public void repaintScreen() {
        QWidget widget = myComponent;
        if (widget != null && !widget.isDisposed()) {
            widget.update();
        }
    }

    public @Nullable DesktopQtTerminalWidget getTerminalWidget() {
        return myComponent instanceof DesktopQtTerminalWidget widget ? widget : null;
    }
}
