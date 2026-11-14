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
package consulo.ai.impl.internal.chat;

import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What the user sent while the assistant still answered - sent one by one once it finished, unless the user sends one
 * at once, which stops the answer, or drops it.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
final class AIChatQueuePanel {
    private static final int SHOWN_CHARS = 80;

    private final VerticalLayout myRoot = VerticalLayout.create(Space.X_SMALL);
    private final Label myTitle = Label.create();
    private final VerticalLayout myRows = VerticalLayout.create(Space.X_SMALL);
    private final List<String> myQueue = new ArrayList<>();
    private final Consumer<String> mySendNow;

    /**
     * @param sendNow stops the answer and sends the message
     */
    @RequiredUIAccess
    AIChatQueuePanel(Consumer<String> sendNow) {
        mySendNow = sendNow;
        myTitle.addStyle(LabelStyle.FONT_BOLD);
        myRoot.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myRows.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myRoot.add(myTitle);
        myRoot.add(myRows);
        myRoot.paddingBuilder().topSet(Space.SMALL).bottomSet(Space.SMALL).apply();
        myRoot.setVisible(false);
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    void add(String text) {
        myQueue.add(text);
        update();
    }

    /**
     * The message next in line, taken out of the queue - null when nothing waits.
     */
    @RequiredUIAccess
    @Nullable String poll() {
        if (myQueue.isEmpty()) {
            return null;
        }
        String text = myQueue.removeFirst();
        update();
        return text;
    }

    @RequiredUIAccess
    void clear() {
        myQueue.clear();
        update();
    }

    @RequiredUIAccess
    private void update() {
        myRows.removeAll();
        for (String text : List.copyOf(myQueue)) {
            String line = text.strip().lines().findFirst().orElse("");
            Label label = Label.create(LocalizeValue.of(line.length() > SHOWN_CHARS ? line.substring(0, SHOWN_CHARS) + "…" : line));
            label.setToolTipText(LocalizeValue.of(text));

            Button sendNow = Button.create(AILocalize.buttonSendNow(), event -> {
                myQueue.remove(text);
                update();
                mySendNow.accept(text);
            });
            sendNow.addStyle(ButtonStyle.INPLACE);
            Button remove = Button.create(LocalizeValue.empty(), event -> {
                myQueue.remove(text);
                update();
            });
            remove.setIcon(PlatformIconGroup.actionsClose());
            remove.setToolTipText(AILocalize.buttonRemove());
            remove.addStyle(ButtonStyle.INPLACE);

            DockLayout row = DockLayout.create();
            row.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
            HorizontalLayout actions = HorizontalLayout.create(Space.X_SMALL);
            actions.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
            myRows.add(row.center(label).right(actions.add(sendNow).add(remove)));
        }
        myTitle.setText(AILocalize.labelQueued(myQueue.size()));
        myRoot.setVisible(!myQueue.isEmpty());
    }
}
