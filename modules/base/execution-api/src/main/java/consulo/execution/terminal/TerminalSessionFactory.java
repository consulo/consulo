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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.platform.Platform;

import java.util.function.Supplier;

@ServiceAPI(ComponentScope.APPLICATION)
public interface TerminalSessionFactory {
    /**
     * A shell in a terminal of the machine of the platform.
     *
     * @param workingDirectory a path of that machine
     * @param shellPathGetter  the shell to start - empty for {@link #findDefaultShellPath(Platform)}
     */
    TerminalSession create(Platform platform, String connectorName, String workingDirectory, Supplier<String> shellPathGetter);

    /**
     * The shell a terminal of the machine starts when none is configured. Blocking - asks that machine.
     */
    String findDefaultShellPath(Platform platform);
}
