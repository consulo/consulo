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

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;
import java.util.function.Consumer;

/**
 * The box a chat message is written in: Enter sends, Shift+Enter breaks the line, the arrows on the first and the last
 * line bring back what was sent before, and {@code /} and {@code @} complete commands and files.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface AIChatInput {
    Component getComponent();

    @RequiredUIAccess
    void focus();

    /**
     * Shown inside the box before the send button - the choosers of the chat.
     */
    @RequiredUIAccess
    void setSuffixComponent(Component suffix);

    /**
     * Told the text when the user sends it - the box is empty again by then.
     */
    void addSubmitListener(Consumer<String> listener);

    /**
     * Told the text after every change of it - typing, pasting, a line break.
     */
    void addTextListener(Consumer<String> listener);

    /**
     * Told what the user attached in the box itself - a pasted screenshot, pasted image files.
     */
    void addAttachListener(Consumer<AIChatAttachment> listener);

    /**
     * What was sent before, the oldest first.
     */
    @RequiredUIAccess
    void setHistory(List<String> history);

    /**
     * Replaces the text of the box, the cursor at its end.
     */
    @RequiredUIAccess
    void setText(String text);

    /**
     * What is written in the box now.
     */
    String getText();

    /**
     * Asks for a box as wide as the given number of characters of its font.
     */
    @RequiredUIAccess
    void setVisibleLength(int columns);
}
