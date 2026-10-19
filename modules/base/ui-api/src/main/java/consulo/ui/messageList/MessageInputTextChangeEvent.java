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

import consulo.ui.event.ComponentEvent;

/**
 * The text of a {@link MessageInput} changed - typed, inserted, or emptied once a message was sent.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class MessageInputTextChangeEvent extends ComponentEvent<MessageInput> {
    private final String myValue;

    public MessageInputTextChangeEvent(MessageInput component, String value) {
        super(component);
        myValue = value;
    }

    public String getValue() {
        return myValue;
    }
}
