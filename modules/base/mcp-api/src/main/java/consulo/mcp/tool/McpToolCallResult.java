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

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpToolCallResult {
    public static McpToolCallResult text(String text) {
        return new McpToolCallResult(List.of(text), false);
    }

    public static McpToolCallResult success() {
        return text("[success]");
    }

    public static McpToolCallResult error(String message) {
        return new McpToolCallResult(List.of(message), true);
    }

    private final List<String> myContent;
    private final boolean myError;

    private McpToolCallResult(List<String> content, boolean error) {
        myContent = content;
        myError = error;
    }

    public List<String> getContent() {
        return myContent;
    }

    public boolean isError() {
        return myError;
    }
}
