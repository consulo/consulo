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

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import java.util.concurrent.CompletableFuture;

/**
 * Something the model waits on the user for, shown above the message box until it is answered or withdrawn.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
abstract class AIChatPrompt<T> {
    protected final CompletableFuture<T> myResult;
    protected final VerticalLayout myRoot = VerticalLayout.create(Space.SMALL);

    protected AIChatPrompt(CompletableFuture<T> result) {
        myResult = result;
        myRoot.paddingBuilder().allSet(Space.MEDIUM).apply();
    }

    Component getComponent() {
        return myRoot;
    }

    CompletableFuture<T> getResult() {
        return myResult;
    }

    @RequiredUIAccess
    protected static Label title(LocalizeValue text) {
        Label label = Label.create(text);
        label.addStyle(LabelStyle.FONT_BOLD);
        return label;
    }

    @RequiredUIAccess
    protected static HorizontalLayout buttonRow() {
        return HorizontalLayout.create(Space.SMALL);
    }
}
