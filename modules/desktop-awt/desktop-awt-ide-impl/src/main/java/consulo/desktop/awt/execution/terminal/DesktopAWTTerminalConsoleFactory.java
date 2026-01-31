/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.*;
import com.jediterm.terminal.emulator.JediEmulator;
import com.jediterm.terminal.model.JediTerminal;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.terminal.TerminalSession;
import consulo.execution.ui.terminal.JediTerminalConsole;
import consulo.execution.ui.terminal.TerminalConsoleFactory;
import consulo.execution.ui.terminal.TerminalConsoleSettings;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 15/04/2023
 */
@ServiceImpl
@Singleton
public class DesktopAWTTerminalConsoleFactory implements TerminalConsoleFactory {
    private final Application myApplication;

    @Inject
    public DesktopAWTTerminalConsoleFactory(Application application) {
        myApplication = application;
    }

    @Override
    @Nonnull
    public JediTerminalConsole create(TerminalSession session, TerminalConsoleSettings settings, Disposable parentDisposable) {
        JBTerminalSystemSettingsProvider provider = new JBTerminalSystemSettingsProvider(myApplication, settings, parentDisposable);

        JBTerminalWidget widget = new JBTerminalWidget(provider);
        Disposer.register(parentDisposable, widget);

        ((AbstractTerminalRunner) session).openSessionInDirectory(widget);

        return widget;
    }

    @Nonnull
    @Override
    public JediTerminalConsole createCustom(Disposable parentDisposable,
                                            BiFunction<TerminalDataStream, Terminal, JediEmulator> jediEmulatorFactory,
                                            TtyConnector connector) {
        JBTerminalSystemSettingsProvider provider =
            new JBTerminalSystemSettingsProvider(myApplication, TerminalConsoleSettings.DEFAULT, parentDisposable);

        JBTerminalWidget widget = new JBTerminalWidget(provider) {
            @Override
            protected TerminalStarter createTerminalStarter(JediTerminal terminal, TtyConnector connector) {
                return new TerminalStarter(terminal, connector, new TtyBasedArrayDataStream(connector)) {
                    @Override
                    protected JediEmulator createEmulator(TerminalDataStream dataStream, Terminal terminal) {
                        return jediEmulatorFactory.apply(dataStream, terminal);
                    }
                };
            }
        };

        widget.setTtyConnector(connector);
        widget.start();

        return widget;
    }
}
