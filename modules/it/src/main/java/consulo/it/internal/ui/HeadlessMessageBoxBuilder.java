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

import consulo.it.HeadlessAnswers;
import consulo.it.internal.HeadlessUIAccess;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.Window;
import consulo.ui.impl.BaseMessageBoxBuilder;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A message box answered by its id from {@link HeadlessAnswers}, as a click on the button of the answered role would.
 * A remembered answer never shows the box.
 */
public class HeadlessMessageBoxBuilder<V> extends BaseMessageBoxBuilder<V> {
    private @Nullable String myId;

    @Override
    public MessageBoxBuilder<V> id(String id) {
        myId = id;
        return this;
    }

    @Override
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        prepare();

        V remembered = rememberedValue();
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        HeadlessAnswers answers = HeadlessUIAccess.INSTANCE.getAnswers();
        MessageButtonRole role = answers == null ? null : answers.messageBoxAnswer(myId);
        if (role == null) {
            return CompletableFuture.failedFuture(HeadlessAnswers.unexpected(answers, "message box", myId, myText));
        }
        for (ButtonImpl<V> button : myButtons) {
            if (button.myRole == role) {
                V value = button.myValue.get();
                storeRemembered(button, true, value);
                return CompletableFuture.completedFuture(value);
            }
        }
        return CompletableFuture.failedFuture(HeadlessAnswers.unexpected(answers, "button " + role + " of message box", myId, myText));
    }
}
