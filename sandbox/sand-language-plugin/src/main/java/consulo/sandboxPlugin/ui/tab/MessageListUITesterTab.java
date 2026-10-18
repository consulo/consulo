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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.messageList.MessageInput;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionImpl(id = "messageList", order = "after imageViewer")
public class MessageListUITesterTab implements UITesterTab {
    private static final LocalizeValue USER = LocalizeValue.of("Valery Semenchuk");
    private static final LocalizeValue ASSISTANT = LocalizeValue.of("Claude");

    private static final String ANSWER = """
        Here is a **markdown** answer, streamed word by word:

        1. a numbered item with `inline code`
        2. a second item with ~~strikethrough~~

        | Column | Value |
        |--------|-------|
        | first  | 1     |
        | second | 2     |

        ```java
        System.out.println("Hello");
        ```
        """;

    private final ApplicationConcurrency myApplicationConcurrency;

    @Inject
    public MessageListUITesterTab(ApplicationConcurrency applicationConcurrency) {
        myApplicationConcurrency = applicationConcurrency;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Message List");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        MessageList list = MessageList.create();
        list.setMarkdown(true);

        add(list, USER, "Show me what the list can do", MessageListItemStyle.OWN, Instant.now().minus(Duration.ofDays(1)));
        add(list, ASSISTANT, "# Heading\nA paragraph with a [link](https://consulo.io) and *emphasis*.", MessageListItemStyle.OTHER, Instant.now().minus(Duration.ofDays(1)));
        add(list, LocalizeValue.of("Tool"), "Called `read_file`", MessageListItemStyle.SYSTEM, Instant.now());
        add(list, ASSISTANT, "The request failed: connection refused", MessageListItemStyle.ERROR, Instant.now());

        MessageInput input = MessageInput.create();
        input.setPlaceholder(LocalizeValue.localizeTODO("Write a message"));
        input.setSendText(LocalizeValue.localizeTODO("Send"));
        input.setSendIcon(PlatformIconGroup.actionsSend());

        UIAccess uiAccess = UIAccess.current();
        input.addSubmitListener(event -> {
            add(list, USER, event.getValue(), MessageListItemStyle.OWN, Instant.now());
            MessageListItem answer = add(list, ASSISTANT, "", MessageListItemStyle.OTHER, Instant.now());
            stream(answer, uiAccess);
        });

        CheckBox markdown = CheckBox.create(LocalizeValue.localizeTODO("Markdown"), true);
        markdown.addValueListener(event -> list.setMarkdown(Boolean.TRUE.equals(event.getValue())));

        Button clear = Button.create(LocalizeValue.localizeTODO("Clear"), event -> list.clear());

        HorizontalLayout options = HorizontalLayout.create().add(markdown).add(clear);

        return DockLayout.create().top(options).center(list).bottom(input);
    }

    private void stream(MessageListItem answer, UIAccess uiAccess) {
        String[] words = ANSWER.split("(?<=\\s)");
        AtomicInteger index = new AtomicInteger();
        ScheduledFuture<?>[] task = new ScheduledFuture<?>[1];
        task[0] = myApplicationConcurrency.getScheduledExecutorService().scheduleWithFixedDelay(() -> {
            int next = index.getAndIncrement();
            if (next >= words.length) {
                task[0].cancel(false);
                return;
            }
            uiAccess.give(() -> answer.appendText(words[next]));
        }, 0, 40, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private static MessageListItem add(MessageList list, LocalizeValue author, String text, MessageListItemStyle style, Instant time) {
        MessageListItem item = list.add(author, text);
        item.setStyle(style);
        item.setTime(time);
        return item;
    }
}
