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

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.CursorShape;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.Terminal;
import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.TerminalDisplay;
import com.jediterm.terminal.TtyBasedArrayDataStream;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.emulator.mouse.MouseFormat;
import com.jediterm.terminal.emulator.mouse.MouseMode;
import com.jediterm.terminal.model.JediTerminal;
import com.jediterm.terminal.model.StyleState;
import com.jediterm.terminal.model.TerminalSelection;
import com.jediterm.terminal.model.TerminalTextBuffer;
import consulo.desktop.qt.ui.impl.DesktopQtUIAccess;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.model.RangeModel;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * A terminal session drawn by qt. The emulator, the buffer and the screen model are jediterm's, the same ones the
 * awt frontend runs on - what differs is that the screen is painted by {@link DesktopQtTerminalWidget} instead of
 * by the swing panel of jediterm, which cannot be put inside a qt window.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtTerminalConsole implements JediTerminalConsole {
    private static final Logger LOG = Logger.getInstance(DesktopQtTerminalConsole.class);

    private final String mySessionName;
    private final TtyConnector myConnector;
    private final BiFunction<TerminalDataStream, Terminal, JediEmulator> myEmulatorFactory;

    private final TerminalTextBuffer myTextBuffer;
    private final JediTerminal myTerminal;
    private final TerminalDataStream myDataStream;

    private final DesktopQtTerminalComponent myComponent;

    private volatile boolean myDisposed;

    public DesktopQtTerminalConsole(
        String sessionName,
        TtyConnector connector,
        BiFunction<TerminalDataStream, Terminal, JediEmulator> emulatorFactory,
        int columns,
        int rows
    ) {
        mySessionName = sessionName;
        myConnector = connector;
        myEmulatorFactory = emulatorFactory;
        myDataStream = new TtyBasedArrayDataStream(connector);

        StyleState styleState = new StyleState();
        myTextBuffer = new TerminalTextBuffer(columns, rows, styleState);
        myTerminal = new JediTerminal(new QtDisplay(), myTextBuffer, styleState);

        myComponent = new DesktopQtTerminalComponent(myTextBuffer, myTerminal, this::write, this::onResize);
    }

    public void start() {
        JediEmulator emulator = myEmulatorFactory.apply(myDataStream, myTerminal);

        startThread("Qt Terminal Emulator " + mySessionName, () -> {
            while (!myDisposed) {
                try {
                    if (!emulator.hasNext()) {
                        break;
                    }

                    emulator.next();

                    repaint();
                }
                catch (IOException e) {
                    break;
                }
                catch (Exception e) {
                    LOG.warn("terminal emulator stopped", e);
                    break;
                }
            }
        });
    }

    private static void startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * The emulator runs off the ui thread, and a widget may only be told to repaint on it.
     */
    private void repaint() {
        DesktopQtTerminalComponent component = myComponent;
        if (component == null) {
            return;
        }

        DesktopQtUIAccess.INSTANCE.give(component::repaintScreen);
    }

    private void write(String data) {
        try {
            myConnector.write(data);
        }
        catch (IOException e) {
            LOG.warn("failed to write to terminal", e);
        }
    }

    private void onResize(int columns, int rows) {
        if (columns <= 0 || rows <= 0) {
            return;
        }

        // a drag raises a resize for every pixel, and reflowing the buffer for a size it already has drops content
        if (columns == myTextBuffer.getWidth() && rows == myTextBuffer.getHeight()) {
            return;
        }

        TermSize size = new TermSize(columns, rows);

        myTerminal.resize(size, RequestOrigin.User);
        myConnector.resize(size);
    }

    @Override
    public String getSessionName() {
        return mySessionName;
    }

    @Override
    public Component getUIComponent() {
        return myComponent;
    }

    @Override
    public Terminal getTerminal() {
        return myTerminal;
    }

    @Override
    public TerminalTextBuffer getTerminalTextBuffer() {
        return myTextBuffer;
    }

    @Override
    public RangeModel getTerminalVerticalScrollModel() {
        return new RangeModel() {
            @Override
            public int getValue() {
                DesktopQtTerminalWidget widget = myComponent.getTerminalWidget();
                return widget == null ? 0 : widget.getScrollOrigin();
            }

            @Override
            public void setValue(int value) {
                DesktopQtTerminalWidget widget = myComponent.getTerminalWidget();
                if (widget != null) {
                    widget.setScrollOrigin(value);
                }
            }

            @Override
            public int getMinimum() {
                return -myTextBuffer.getHistoryLinesCount();
            }

            @Override
            public int getMaximum() {
                return 0;
            }

            @Override
            public int getExtent() {
                return myTextBuffer.getHeight();
            }
        };
    }

    @Override
    public boolean isShowing() {
        DesktopQtTerminalWidget widget = myComponent.getTerminalWidget();
        return widget != null && !widget.isDisposed() && widget.isVisible();
    }

    @Override
    public void dispose() {
        myDisposed = true;

        myConnector.close();
    }

    /**
     * What the emulator tells the screen. Everything it reports is held by the widget or by the buffer, so the
     * display itself only has to answer the questions the emulator asks back.
     */
    private class QtDisplay implements TerminalDisplay {
        private String myWindowTitle = "";

        @Override
        public void setCursor(int x, int y) {
            repaint();
        }

        @Override
        public void setCursorShape(CursorShape cursorShape) {
        }

        @Override
        public void beep() {
        }

        @Override
        public void scrollArea(int scrollRegionTop, int dy, int scrollRegionBottom) {
            repaint();
        }

        @Override
        public void setCursorVisible(boolean isCursorVisible) {
        }

        @Override
        public void useAlternateScreenBuffer(boolean useAlternateScreenBuffer) {
            repaint();
        }

        @Override
        public String getWindowTitle() {
            return myWindowTitle;
        }

        @Override
        public void setWindowTitle(String windowTitle) {
            myWindowTitle = windowTitle;
        }

        @Override
        public @Nullable TerminalSelection getSelection() {
            DesktopQtTerminalWidget widget = myComponent.getTerminalWidget();
            return widget == null ? null : widget.getSelection();
        }

        @Override
        public void terminalMouseModeSet(MouseMode mouseMode) {
        }

        @Override
        public void setMouseFormat(MouseFormat mouseFormat) {
        }

        @Override
        public boolean ambiguousCharsAreDoubleWidth() {
            return false;
        }

        @Override
        public com.jediterm.core.@Nullable Color getWindowForeground() {
            return null;
        }

        @Override
        public com.jediterm.core.@Nullable Color getWindowBackground() {
            return null;
        }
    }
}
