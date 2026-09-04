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
package consulo.ui.impl;

import consulo.ui.Component;
import consulo.ui.DialogCancelledException;
import consulo.ui.Window;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * An input box for a frontend with no display: it is answered from {@link ScriptedDialogs}.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class ScriptedInputBoxBuilder<V, C extends Component> extends BaseInputBoxBuilder<V, C> {
    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        ScriptedDialogs dialogs = ScriptedDialogs.installed();
        if (dialogs == null) {
            return failed("no dialog script is installed");
        }

        dialogs.record(this);

        Object answer = dialogs.nextAnswer();
        if (answer instanceof ScriptedDialogs.InputAnswer input) {
            V value = normalize((V)input.value());
            return value != null ? CompletableFuture.completedFuture(value) : cancelled();
        }

        if (dialogs.unexpected() == ScriptedDialogs.Unexpected.DISMISS) {
            return cancelled();
        }

        return failed("unexpected input box: " + myText.get());
    }

    private CompletableFuture<V> cancelled() {
        CompletableFuture<V> result = new CompletableFuture<>();
        result.completeExceptionally(new DialogCancelledException());
        return result;
    }

    private CompletableFuture<V> failed(String message) {
        CompletableFuture<V> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException(message));
        return result;
    }
}
