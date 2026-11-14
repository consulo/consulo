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
package consulo.execution.impl.internal.terminal;

import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import consulo.process.internal.PlatformPtyProcess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

/**
 * The terminal of a process of any machine. The output is decoded by one reader - a character split between two chunks of
 * the output stays whole.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class PlatformPtyTtyConnector implements TtyConnector {
    private final PlatformPtyProcess myProcess;
    private final Charset myCharset;
    private final String myName;
    private final InputStreamReader myReader;

    public PlatformPtyTtyConnector(PlatformPtyProcess process, Charset charset, String name) {
        myProcess = process;
        myCharset = charset;
        myName = name;
        myReader = new InputStreamReader(process.getInputStream(), charset);
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        return myReader.read(buffer, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        OutputStream output = myProcess.getOutputStream();
        output.write(bytes);
        output.flush();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(myCharset));
    }

    @Override
    public boolean isConnected() {
        return myProcess.isAlive();
    }

    @Override
    public void resize(TermSize termSize) {
        if (isConnected()) {
            myProcess.resize(termSize.getColumns(), termSize.getRows());
        }
    }

    @Override
    public int waitFor() throws InterruptedException {
        try {
            return myProcess.onExit().get();
        }
        catch (ExecutionException e) {
            return -1;
        }
    }

    @Override
    public boolean ready() throws IOException {
        return myReader.ready();
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public void close() {
        myProcess.destroy();
    }
}
