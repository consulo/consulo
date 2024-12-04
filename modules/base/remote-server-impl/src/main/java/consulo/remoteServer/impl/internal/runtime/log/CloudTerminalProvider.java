// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import java.io.InputStream;
import java.io.OutputStream;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CloudTerminalProvider {
    @Nonnull
    public static CloudTerminalProvider getInstance() {
        return ApplicationManager.getApplication().getInstance(CloudTerminalProvider.class);
    }

    public abstract @Nonnull TerminalHandlerBase createTerminal(@Nonnull @Nls String presentableName,
                                                                @Nonnull Project project,
                                                                @Nonnull InputStream terminalOutput,
                                                                @Nonnull OutputStream terminalInput);

    public abstract boolean isTtySupported();
}
