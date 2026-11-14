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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.List;

/**
 * Contributes the MCP servers every agent session gets - the server of the IDE, which lets an agent search, navigate
 * and refactor with the indexes of the IDE. The dependency runs MCP to AI, like {@link AIToolProvider}.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIMcpServerProvider {
    /**
     * Empty while the servers do not run.
     */
    List<AIMcpServer> getServers();
}
