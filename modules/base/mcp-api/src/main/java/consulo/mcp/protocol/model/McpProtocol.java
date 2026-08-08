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
package consulo.mcp.protocol.model;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpProtocol {
    public static final String LATEST_VERSION = "2025-06-18";

    /**
     * Ordered newest first. A client asking for any of these gets that exact version echoed back.
     */
    public static final List<String> SUPPORTED_VERSIONS = List.of(LATEST_VERSION, "2025-03-26");

    public static final String METHOD_INITIALIZE = "initialize";
    public static final String METHOD_INITIALIZED = "notifications/initialized";
    public static final String METHOD_PING = "ping";
    public static final String METHOD_TOOLS_LIST = "tools/list";
    public static final String METHOD_TOOLS_CALL = "tools/call";

    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";
    public static final String PROTOCOL_VERSION_HEADER = "Mcp-Protocol-Version";

    private McpProtocol() {
    }
}
