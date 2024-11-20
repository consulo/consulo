// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.application.ApplicationManager;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.process.NopProcessHandler;
import consulo.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConsoleTerminalHandlerImpl extends TerminalHandlerBase {

    private static final Logger LOG = Logger.getInstance(ConsoleTerminalHandlerImpl.class);

    private final LoggingHandlerImpl myLoggingHandler;

    public ConsoleTerminalHandlerImpl(String presentableName,
                                      Project project,
                                      final InputStream terminalOutput,
                                      final OutputStream terminalInput) {
        super(presentableName);

        myLoggingHandler = new LoggingHandlerImpl(presentableName, project);
        myLoggingHandler.attachToProcess(new NopProcessHandler() {

            @Override
            @Nullable
            public OutputStream getProcessInput() {
                return terminalInput;
            }
        });

        Disposer.register(this, myLoggingHandler);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(terminalOutput, StandardCharsets.UTF_8))) {
                while (!isClosed()) {
                    String line = outputReader.readLine();
                    if (line == null) {
                        break;
                    }
                    myLoggingHandler.print(line + "\n");
                }
            }
            catch (IOException e) {
                LOG.debug(e);
            }
        });
    }

    @Override
    public JComponent getComponent() {
        return myLoggingHandler.getComponent();
    }
}
