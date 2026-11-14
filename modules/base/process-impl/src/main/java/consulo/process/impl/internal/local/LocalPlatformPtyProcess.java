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
package consulo.process.impl.internal.local;

import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import consulo.process.internal.PlatformPtyProcess;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * A process of a pseudo terminal of the IDE machine.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public final class LocalPlatformPtyProcess implements PlatformPtyProcess {
    private final PtyProcess myProcess;

    public LocalPlatformPtyProcess(PtyProcess process) {
        myProcess = process;
    }

    @Override
    public InputStream getInputStream() {
        return myProcess.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() {
        return myProcess.getOutputStream();
    }

    @Override
    public void resize(int columns, int rows) {
        if (myProcess.isAlive()) {
            myProcess.setWinSize(new WinSize(columns, rows));
        }
    }

    @Override
    public boolean isAlive() {
        return myProcess.isAlive();
    }

    @Override
    public CompletableFuture<Integer> onExit() {
        return myProcess.onExit().thenApply(Process::exitValue);
    }

    @Override
    public void destroy() {
        myProcess.destroy();
    }
}
