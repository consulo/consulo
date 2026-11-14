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
package consulo.process.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * A process running in a pseudo terminal of its machine - a shell of a terminal. The terminal is its input and its output:
 * what it prints comes as one stream, escape sequences included.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public interface PlatformPtyProcess {
    /**
     * What the process prints - the bytes of the terminal.
     */
    InputStream getInputStream();

    /**
     * What is typed into the terminal.
     */
    OutputStream getOutputStream();

    /**
     * The terminal changed its size.
     */
    void resize(int columns, int rows);

    boolean isAlive();

    /**
     * Completes with the exit code once the process is gone.
     */
    CompletableFuture<Integer> onExit();

    /**
     * Ends the process and everything started in its terminal.
     */
    void destroy();
}
