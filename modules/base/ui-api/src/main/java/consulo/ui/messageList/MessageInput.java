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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.HasComponentStyle;
import consulo.ui.HasSuffixComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * Where a message for a {@link MessageList} is written: a text field which grows with the text and a send button.
 * Enter sends, Shift+Enter starts a new line, and the field empties once a message is sent. Nothing is sent while the
 * text is blank or the input is disabled.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public interface MessageInput extends HasSuffixComponent, HasComponentStyle<MessageInputStyle> {
    static MessageInput create() {
        return UIInternal.get()._Components_messageInput();
    }

    @RequiredUIAccess
    void setPlaceholder(LocalizeValue placeholder);

    /**
     * The text of the send button, or its tooltip once the button shows an {@linkplain #setSendIcon icon}.
     */
    @RequiredUIAccess
    void setSendText(LocalizeValue sendText);

    /**
     * Puts the text where the caret is, in place of the selected text, and moves the focus into the input.
     */
    @RequiredUIAccess
    void insertText(String text);

    /**
     * Shown inside the field before the send button - what the message goes to, a chooser of a model for example.
     */
    @Override
    @RequiredUIAccess
    void setSuffixComponent(@Nullable Component suffixComponent);

    /**
     * Shows the image instead of the text on the send button.
     */
    @RequiredUIAccess
    void setSendIcon(@Nullable Image icon);

    default Disposable addSubmitListener(ComponentEventListener<MessageInput, MessageInputSubmitEvent> listener) {
        return addListener(MessageInputSubmitEvent.class, listener);
    }

    /**
     * Told about the text while it is written. A frontend which learns about the text late may report several edits
     * at once.
     */
    default Disposable addTextChangeListener(ComponentEventListener<MessageInput, MessageInputTextChangeEvent> listener) {
        return addListener(MessageInputTextChangeEvent.class, listener);
    }
}
