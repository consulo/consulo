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

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

import java.util.List;

/**
 * A conversation: messages one under another, each with its author, time and text. The list owns its messages -
 * callers hand over data and keep the returned {@link MessageListItem} to change a message later, for example to
 * stream an answer into it. The list draws the author picture, renders the text and keeps the newest message in
 * view.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public interface MessageList extends Component {
    static MessageList create() {
        return UIInternal.get()._Components_messageList();
    }

    /**
     * Adds a message after the last one.
     */
    @RequiredUIAccess
    MessageListItem add(LocalizeValue author, String text);

    @RequiredUIAccess
    void remove(MessageListItem item);

    @RequiredUIAccess
    void clear();

    List<MessageListItem> getItems();

    /**
     * Whether the text of the messages is markdown. Plain text, the default, is shown as written.
     */
    @RequiredUIAccess
    void setMarkdown(boolean markdown);

    boolean isMarkdown();
}
