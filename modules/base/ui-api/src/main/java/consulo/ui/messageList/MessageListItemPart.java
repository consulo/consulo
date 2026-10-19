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
package consulo.ui.messageList;

import consulo.ui.Component;
import org.jspecify.annotations.Nullable;

/**
 * A part of a {@link MessageListItem}: text, rendered the way the text of its list is, or a component.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class MessageListItemPart {
    private final @Nullable String myText;
    private final @Nullable Component myComponent;

    private MessageListItemPart(@Nullable String text, @Nullable Component component) {
        myText = text;
        myComponent = component;
    }

    public static MessageListItemPart text(String text) {
        return new MessageListItemPart(text, null);
    }

    public static MessageListItemPart component(Component component) {
        return new MessageListItemPart(null, component);
    }

    /**
     * @return the text of a text part, {@code null} for a component part
     */
    public @Nullable String getText() {
        return myText;
    }

    /**
     * @return the component of a component part, {@code null} for a text part
     */
    public @Nullable Component getComponent() {
        return myComponent;
    }
}
