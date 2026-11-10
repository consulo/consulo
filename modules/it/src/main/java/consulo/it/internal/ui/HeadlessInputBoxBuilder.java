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
import consulo.ui.Component;
import consulo.ui.DialogCancelledException;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Window;
import consulo.ui.impl.BaseInputBoxBuilder;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * An input box answered by its id from {@link HeadlessAnswers}: a value confirms it, null dismisses it.
 */
public class HeadlessInputBoxBuilder<V, C extends Component> extends BaseInputBoxBuilder<V, C> {
    private @Nullable String myId;

    @Override
    public InputBoxBuilder<V, C> id(String id) {
        myId = id;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        HeadlessAnswers answers = HeadlessUIAccess.INSTANCE.getAnswers();
        HeadlessAnswers.ValueAnswer answer = answers == null ? null : answers.inputBoxAnswer(myId);
        if (answer == null) {
            return CompletableFuture.failedFuture(HeadlessAnswers.unexpected(answers, "input box", myId, myText));
        }
        V value = normalize((V)answer.value());
        return value != null ? CompletableFuture.completedFuture(value) : CompletableFuture.failedFuture(new DialogCancelledException());
    }
}
