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
package consulo.remoteServer.agent.impl.internal.process;

import consulo.process.BinaryProcessHandler;
import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.util.io.BufferExposingByteArrayOutputStream;

import java.nio.charset.Charset;

/**
 * A remote process which output is collected as is - its error output is reported as text.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public class RemoteAgentBinaryProcessHandler extends RemoteAgentProcessHandler implements BinaryProcessHandler {
    private final BufferExposingByteArrayOutputStream myOutput = new BufferExposingByteArrayOutputStream();

    public RemoteAgentBinaryProcessHandler(RemoteAgentConnection connection, String commandLine, Charset charset) {
        super(connection, commandLine, charset);
    }

    @Override
    protected void onStdout(byte[] data) {
        synchronized (myOutput) {
            myOutput.write(data, 0, data.length);
        }
    }

    @Override
    public byte[] getOutput() {
        synchronized (myOutput) {
            return myOutput.toByteArray();
        }
    }
}
