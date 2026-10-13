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

import com.google.protobuf.ByteString;
import consulo.remoteServer.agent.impl.internal.protocol.Empty;
import consulo.remoteServer.agent.impl.internal.protocol.ExecInput;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

class RemoteAgentProcessInput extends OutputStream {
    private final Consumer<ExecInput> mySender;

    private boolean myClosed;

    RemoteAgentProcessInput(Consumer<ExecInput> sender) {
        mySender = sender;
    }

    @Override
    public void write(int value) throws IOException {
        write(new byte[]{(byte) value}, 0, 1);
    }

    @Override
    public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
        if (myClosed) {
            throw new IOException("Process input is closed");
        }
        if (length > 0) {
            mySender.accept(ExecInput.newBuilder().setStdin(ByteString.copyFrom(bytes, offset, length)).build());
        }
    }

    @Override
    public synchronized void close() {
        if (!myClosed) {
            myClosed = true;
            mySender.accept(ExecInput.newBuilder().setCloseStdin(Empty.getDefaultInstance()).build());
        }
    }
}
