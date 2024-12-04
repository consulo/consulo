// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.agent.shared.util;

import consulo.remoteServer.agent.shared.TerminalListener;
import consulo.remoteServer.agent.shared.util.log.LogListener;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author michael.golubev
 */
public interface CloudAgentLoggingHandler {
    String getProjectHash();

    void println(String message);

    LogListener getOrCreateLogListener(String pipeName);

    boolean isTtySupported();

    TerminalListener createTerminal(String pipeName, OutputStream terminalInput, InputStream terminalOutput, InputStream stderr);
}
