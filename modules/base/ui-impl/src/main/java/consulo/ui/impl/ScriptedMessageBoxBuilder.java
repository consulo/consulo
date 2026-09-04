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

import consulo.ui.MessageButtonRole;
import consulo.ui.Window;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A message box for a frontend with no display: it is answered from {@link ScriptedDialogs}.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class ScriptedMessageBoxBuilder<V> extends BaseMessageBoxBuilder<V> {
    @Override
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        prepare();

        V remembered = rememberedValue();
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        ScriptedDialogs dialogs = ScriptedDialogs.installed();
        if (dialogs == null) {
            return failed("no dialog script is installed");
        }

        dialogs.record(this);

        Object answer = dialogs.nextAnswer();
        if (answer instanceof MessageButtonRole role) {
            for (ButtonImpl<V> button : myButtons) {
                if (button.myRole == role) {
                    V value = button.myValue.get();
                    storeRemembered(button, true, value);
                    return CompletableFuture.completedFuture(value);
                }
            }
            return failed("no button with role " + role + " in " + myText.get());
        }

        if (dialogs.unexpected() == ScriptedDialogs.Unexpected.DISMISS) {
            V value = exitValueOrNull();
            storeRemembered(null, true, value);
            return CompletableFuture.completedFuture(value);
        }

        return failed("unexpected message box: " + myText.get());
    }

    /**
     * The answers this box offered, for a test to assert on.
     */
    public List<MessageButtonRole> roles() {
        List<MessageButtonRole> roles = new ArrayList<>();
        for (ButtonImpl<V> button : myButtons) {
            roles.add(button.myRole);
        }
        return roles;
    }

    private CompletableFuture<V> failed(String message) {
        CompletableFuture<V> result = new CompletableFuture<>();
        result.completeExceptionally(new IllegalStateException(message));
        return result;
    }
}
