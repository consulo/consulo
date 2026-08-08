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

import java.util.ArrayList;
import java.util.List;

/**
 * One turn of a conversation.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public record AIMessage(AIRole role, List<AIContent> content) {
    public AIMessage {
        content = List.copyOf(content);
    }

    public static AIMessage user(String text) {
        return new AIMessage(AIRole.USER, List.of(AIContent.text(text)));
    }

    public static AIMessage assistant(String text) {
        return new AIMessage(AIRole.ASSISTANT, List.of(AIContent.text(text)));
    }

    public static AIMessage of(AIRole role, AIContent... content) {
        return new AIMessage(role, List.of(content));
    }

    /**
     * Concatenated text blocks, ignoring images and tool traffic. Convenient for display and logging.
     */
    public String getText() {
        StringBuilder builder = new StringBuilder();
        for (AIContent block : content) {
            if (block instanceof AIContent.Text text) {
                builder.append(text.text());
            }
        }
        return builder.toString();
    }

    public List<AIContent.ToolUse> getToolUses() {
        List<AIContent.ToolUse> toolUses = new ArrayList<>();
        for (AIContent block : content) {
            if (block instanceof AIContent.ToolUse toolUse) {
                toolUses.add(toolUse);
            }
        }
        return toolUses;
    }
}
