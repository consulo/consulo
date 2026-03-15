// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.runtime.log;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.project.Project;

import java.io.InputStream;
import java.io.OutputStream;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CloudTerminalProvider {
    
    public static CloudTerminalProvider getInstance() {
        return ApplicationManager.getApplication().getInstance(CloudTerminalProvider.class);
    }

    public abstract TerminalHandlerBase createTerminal(String presentableName,
                                                                Project project,
                                                                InputStream terminalOutput,
                                                                OutputStream terminalInput);

    public abstract boolean isTtySupported();
}
