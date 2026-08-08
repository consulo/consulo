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
package consulo.web.internal.execution.terminal;

import com.jediterm.terminal.Terminal;
import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.model.TerminalTextBuffer;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.logging.Logger;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.model.RangeModel;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * Terminal console backed by xterm.js. The characters coming from the pty are written to the browser and to a
 * headless jediterm mirror, so that {@link #getTerminal()} and {@link #getTerminalTextBuffer()} answer for the
 * very content the user is looking at.
 *
 * @author VISTALL
 * @since 2026-08-08
 */
public class WebTerminalConsole implements JediTerminalConsole {
    private static final Logger LOG = Logger.getInstance(WebTerminalConsole.class);

    private static final int READ_BUFFER_SIZE = 8192;

    private final String mySessionName;
    private final WebTerminalComponent myComponent;
    private final WebTerminalMirror myMirror;
    private final TtyConnector myConnector;

    private volatile boolean myDisposed;

    private final BiFunction<TerminalDataStream, Terminal, JediEmulator> myEmulatorFactory;

    public WebTerminalConsole(String sessionName,
                              TtyConnector connector,
                              BiFunction<TerminalDataStream, Terminal, JediEmulator> emulatorFactory,
                              int columns,
                              int rows) {
        mySessionName = sessionName;
        myConnector = connector;
        myEmulatorFactory = emulatorFactory;
        myMirror = new WebTerminalMirror(columns, rows);
        myComponent = new WebTerminalComponent(this::onUserInput, this::onUserResize);
    }

    public void start() {
        myMirror.start(myEmulatorFactory);

        Thread thread = new Thread(this::readLoop, "Web Terminal " + mySessionName);
        thread.setDaemon(true);
        thread.start();
    }

    private void readLoop() {
        char[] buffer = new char[READ_BUFFER_SIZE];
        while (!myDisposed) {
            try {
                int read = myConnector.read(buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }

                myMirror.write(buffer, 0, read);

                String text = new String(buffer, 0, read);
                myComponent.write(text);
            }
            catch (IOException e) {
                break;
            }
            catch (Exception e) {
                LOG.warn("terminal read loop stopped", e);
                break;
            }
        }
    }

    private void onUserInput(String data) {
        try {
            myConnector.write(data);
        }
        catch (IOException e) {
            LOG.warn("failed to write to terminal", e);
        }
    }

    private void onUserResize(TerminalSize size) {
        myMirror.resize(size.columns(), size.rows());
        myConnector.resize(new com.jediterm.core.util.TermSize(size.columns(), size.rows()));
    }

    public void updateTheme() {
        myComponent.updateTheme();
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
        return myMirror.getTerminal();
    }

    @Override
    public TerminalTextBuffer getTerminalTextBuffer() {
        return myMirror.getTextBuffer();
    }

    @Override
    public RangeModel getTerminalVerticalScrollModel() {
        return myComponent.getVerticalScrollModel(myMirror.getTextBuffer());
    }

    @Override
    public boolean isShowing() {
        UIAccess uiAccess = myComponent.getUIAccess();
        return uiAccess != null;
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myMirror.stop();
        myConnector.close();
    }

    public record TerminalSize(int columns, int rows) {
    }
}
