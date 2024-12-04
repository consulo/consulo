// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.runtime.deployment;

import consulo.project.Project;
import consulo.remoteServer.runtime.log.LoggingHandler;
import consulo.remoteServer.runtime.log.TerminalHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.io.InputStream;
import java.io.OutputStream;

public interface DeploymentLogManager {
    @Nonnull
    Project getProject();

    @Nonnull
    LoggingHandler getMainLoggingHandler();

    @Nonnull
    LoggingHandler addAdditionalLog(@Nonnull String presentableName);

    void removeAdditionalLog(@Nonnull String presentableName);

    boolean isTtySupported();

    @Nullable
    TerminalHandler addTerminal(@Nonnull @Nls String presentableName, InputStream terminalOutput, OutputStream terminalInput);
}
