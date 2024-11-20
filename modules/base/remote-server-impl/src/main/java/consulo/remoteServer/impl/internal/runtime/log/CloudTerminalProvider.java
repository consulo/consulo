// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CloudTerminalProvider {
    @NotNull
    public static CloudTerminalProvider getInstance() {
        return ApplicationManager.getApplication().getInstance(CloudTerminalProvider.class);
    }

    public abstract @NotNull TerminalHandlerBase createTerminal(@NotNull @Nls String presentableName,
                                                                @NotNull Project project,
                                                                @NotNull InputStream terminalOutput,
                                                                @NotNull OutputStream terminalInput);

    public abstract boolean isTtySupported();
}
