/*
 * Copyright 2013-2023 consulo.io
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
package consulo.execution.terminal;

import com.jediterm.terminal.TtyConnector;

import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 15/04/2023
 */
public interface TerminalSession {
    String getConnectorName();

    /**
     * Starts the underlying process and returns the channel used to talk to it.
     */
    TtyConnector connect() throws ExecutionException;
}
