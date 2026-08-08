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

import java.util.List;

/**
 * A complete answer. When {@link #stopReason()} is {@link AIStopReason#TOOL_USE} the caller is
 * expected to run the requested tools and send the results back as the next message.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public record AIResponse(AIMessage message, AIStopReason stopReason, AIUsage usage) {
    public String getText() {
        return message.getText();
    }

    public List<AIContent.ToolUse> getToolUses() {
        return message.getToolUses();
    }
}
