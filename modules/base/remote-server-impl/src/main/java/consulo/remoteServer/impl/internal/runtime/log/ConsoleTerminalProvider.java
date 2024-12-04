// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.io.OutputStream;

@Singleton
@ServiceImpl
public class ConsoleTerminalProvider extends CloudTerminalProvider {

    @Override
    public @Nonnull TerminalHandlerBase createTerminal(@Nonnull String presentableName,
                                                       @Nonnull Project project,
                                                       @Nonnull InputStream terminalOutput,
                                                       @Nonnull OutputStream terminalInput) {
        return new ConsoleTerminalHandlerImpl(presentableName, project, terminalOutput, terminalInput);
    }

    @Override
    public boolean isTtySupported() {
        return false;
    }
}
