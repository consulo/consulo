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
 * A failing tool is still a successful JSON-RPC call — the failure is carried by {@code isError}.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public final class ToolsCallResult {
    public List<ContentBlock> content;
    public boolean isError;

    public ToolsCallResult(List<ContentBlock> content, boolean isError) {
        this.content = content;
        this.isError = isError;
    }
}
