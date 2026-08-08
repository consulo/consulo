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
package consulo.mcp.tool;

/**
 * Thrown by a tool to report a failure whose message is meant for the calling agent verbatim.
 * Other exception types are wrapped with a generic prefix instead.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public class McpToolException extends RuntimeException {
    public McpToolException(String message) {
        super(message);
    }
}
