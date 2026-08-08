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
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.terminal.TerminalSession;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.execution.ui.terminal.TerminalConsoleFactory;
import consulo.execution.ui.terminal.TerminalConsoleSettings;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2026-08-08
 */
@ServiceImpl
@Singleton
public class WebTerminalConsoleFactory implements TerminalConsoleFactory {
    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 24;

    private final Application myApplication;

    @Inject
    public WebTerminalConsoleFactory(Application application) {
        myApplication = application;
    }

    private void listenForThemeChange(WebTerminalConsole console, Disposable parentDisposable) {
        myApplication.getMessageBus()
            .connect(parentDisposable)
            .subscribe(EditorColorsListener.class, scheme -> console.updateTheme());
    }

    @Override
    public JediTerminalConsole create(TerminalSession session, TerminalConsoleSettings settings, Disposable parentDisposable) {
        TtyConnector connector;
        try {
            connector = session.connect();
        }
        catch (ExecutionException e) {
            Alerts.okError(LocalizeValue.of(e.getLocalizedMessage())).showAsync();
            return null;
        }

        WebTerminalConsole console = new WebTerminalConsole(
            session.getConnectorName(), connector, JediEmulator::new, DEFAULT_COLUMNS, DEFAULT_ROWS);
        Disposer.register(parentDisposable, console);
        listenForThemeChange(console, parentDisposable);
        console.start();
        return console;
    }

    /**
     * The supplied emulator drives the server side model only - characters reach xterm.js as the pty produced
     * them, so an emulator which rewrites what it prints will show one thing and record another.
     */
    @Override
    public JediTerminalConsole createCustom(Disposable parentDisposable,
                                            BiFunction<TerminalDataStream, Terminal, JediEmulator> jediEmulatorFactory,
                                            TtyConnector connector) {
        WebTerminalConsole console = new WebTerminalConsole(
            connector.getName(), connector, jediEmulatorFactory, DEFAULT_COLUMNS, DEFAULT_ROWS);
        Disposer.register(parentDisposable, console);
        listenForThemeChange(console, parentDisposable);
        console.start();
        return console;
    }
}
