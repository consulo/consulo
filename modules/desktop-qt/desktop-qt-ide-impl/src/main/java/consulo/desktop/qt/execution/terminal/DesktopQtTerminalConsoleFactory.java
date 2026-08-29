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

import com.jediterm.terminal.Terminal;
import com.jediterm.terminal.TerminalDataStream;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.emulator.JediEmulator;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.terminal.TerminalSession;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.execution.ui.terminal.TerminalConsoleFactory;
import consulo.execution.ui.terminal.TerminalConsoleSettings;
import consulo.logging.Logger;
import consulo.ui.Alerts;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2026-08-17
 */
@ServiceImpl
@Singleton
public class DesktopQtTerminalConsoleFactory implements TerminalConsoleFactory {
    private static final Logger LOG = Logger.getInstance(DesktopQtTerminalConsoleFactory.class);

    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 24;

    @Override
    @RequiredUIAccess
    public @Nullable JediTerminalConsole create(TerminalSession session, TerminalConsoleSettings settings, Disposable parentDisposable) {
        TtyConnector connector;
        try {
            connector = session.connect();
        }
        catch (ExecutionException e) {
            LOG.warn("Error connecting terminal", e);
            Alerts.okError(e).showAsync();
            return null;
        }

        return start(
            new DesktopQtTerminalConsole(session.getConnectorName(), connector, JediEmulator::new, DEFAULT_COLUMNS, DEFAULT_ROWS),
            parentDisposable
        );
    }

    @Override
    public JediTerminalConsole createCustom(
        Disposable parentDisposable,
        BiFunction<TerminalDataStream, Terminal, JediEmulator> jediEmulatorFactory,
        TtyConnector connector
    ) {
        return start(
            new DesktopQtTerminalConsole(connector.getName(), connector, jediEmulatorFactory, DEFAULT_COLUMNS, DEFAULT_ROWS),
            parentDisposable
        );
    }

    private static JediTerminalConsole start(DesktopQtTerminalConsole console, Disposable parentDisposable) {
        Disposer.register(parentDisposable, console);

        console.start();

        return console;
    }
}
