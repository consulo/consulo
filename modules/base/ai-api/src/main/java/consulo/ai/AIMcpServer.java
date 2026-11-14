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

import java.util.Map;

/**
 * An MCP server reached over http which an agent gets for its sessions.
 *
 * @param headers sent with every request - the authorization above all
 * @author VISTALL
 * @since 2026-11-14
 */
public record AIMcpServer(String name, String url, Map<String, String> headers) {
}
