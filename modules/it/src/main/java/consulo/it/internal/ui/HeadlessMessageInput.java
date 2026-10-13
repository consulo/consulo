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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.image.Image;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageInputStyle;
import consulo.ui.messageList.MessageInputSubmitEvent;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link MessageInput}: a test sends a message with {@link #submit(String)}.
 *
 * @author VISTALL
 */
public class HeadlessMessageInput extends HeadlessComponentBase implements MessageInput {
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private LocalizeValue mySendText = LocalizeValue.empty();
    private @Nullable Image mySendIcon;
    private @Nullable Component mySuffixComponent;

    @Override
    public void setPlaceholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;
    }

    public LocalizeValue getPlaceholder() {
        return myPlaceholder;
    }

    @Override
    public void setSendText(LocalizeValue sendText) {
        mySendText = sendText;
    }

    public LocalizeValue getSendText() {
        return mySendText;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }

    @Override
    public void addStyle(MessageInputStyle style) {
    }

    @Override
    public void setSendIcon(@Nullable Image icon) {
        mySendIcon = icon;
    }

    public @Nullable Image getSendIcon() {
        return mySendIcon;
    }

    /**
     * Sends like the user would: a blank text or a disabled input sends nothing.
     */
    public void submit(String text) {
        if (!isEnabled() || text.isBlank()) {
            return;
        }
        getListenerDispatcher(MessageInputSubmitEvent.class).onEvent(new MessageInputSubmitEvent(this, text));
    }
}
