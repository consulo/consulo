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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import consulo.ui.util.TraverseUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Asks the user for a single value. Generic in the value and in the editor which collects it, so
 * the editor is configured through its own API rather than through a second vocabulary repeated
 * here.
 * <p>
 * Showing never blocks the calling frame. The result is empty when the box was dismissed.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public interface InputBoxBuilder<V, C extends Component> {
    /**
     * One line of text.
     */
    static InputBoxBuilder<String, TextBox> text() {
        return UIInternal.get()._InputBox_text();
    }

    /**
     * A whole number.
     */
    static InputBoxBuilder<Integer, IntBox> integer() {
        return UIInternal.get()._InputBox_integer();
    }

    /**
     * One of a fixed set.
     */
    static <V> InputBoxBuilder<V, ComboBox<V>> items(Collection<? extends V> items) {
        return UIInternal.get()._InputBox_items(items);
    }

    /**
     * A secret. The same value type as {@link #text()} with a different editor, which is what the
     * second type parameter is for.
     */
    static InputBoxBuilder<String, PasswordBox> password() {
        return UIInternal.get()._InputBox_password();
    }

    /**
     * Configures the editor through its own API, before the box opens.
     * <p>
     * The instance is not guaranteed to be a live component: a frontend which renders this box
     * itself may hand over one which only records what was set. Configuration always applies;
     * anything else - listeners, reading the value back - moves the box onto the frontend's general
     * path, where a real component exists.
     */
    InputBoxBuilder<V, C> setupComponent(Consumer<C> setup);

    /**
     * Empty - the default - lets the frontend use the application name.
     */
    InputBoxBuilder<V, C> title(LocalizeValue title);

    /**
     * The prompt shown above the editor.
     */
    InputBoxBuilder<V, C> text(LocalizeValue text);

    InputBoxBuilder<V, C> asPlain();

    InputBoxBuilder<V, C> asInfo();

    InputBoxBuilder<V, C> asWarning();

    InputBoxBuilder<V, C> asError();

    InputBoxBuilder<V, C> asQuestion();

    /**
     * Overrides the image the severity would have chosen. Prefer stating the severity.
     */
    InputBoxBuilder<V, C> icon(Image icon);

    InputBoxBuilder<V, C> value(V initialValue);

    InputBoxBuilder<V, C> validator(InputValidator<V> validator);

    InputBoxBuilder<V, C> confirmText(LocalizeValue text);

    InputBoxBuilder<V, C> cancelText(LocalizeValue text);

    /**
     * Adds a help affordance. Whether the box stays open while it runs is the frontend's, so the
     * action must be written to work either way.
     */
    InputBoxBuilder<V, C> help(@RequiredUIAccess Runnable action);

    /**
     * Shows the box. The result carries a value only when the user confirmed one, so the rest of a
     * chain can be written without a null check.
     * <p>
     * A dismissal fails the result with {@link DialogCancelledException}; a box which could not be
     * shown at all fails it with whatever went wrong. The two stay apart by type.
     *
     * @return the confirmed value, never null. A text value is trimmed.
     */
    @RequiredUIAccess
    CompletableFuture<V> showAsync(@Nullable Window owner);

    @RequiredUIAccess
    default CompletableFuture<V> showAsync() {
        return showAsync((Window)null);
    }

    @RequiredUIAccess
    default CompletableFuture<V> showAsync(@Nullable Component component) {
        return showAsync(TraverseUtil.getWindowAncestor(component));
    }

    @RequiredUIAccess
    default CompletableFuture<V> showAsync(@Nullable WindowOwner owner) {
        return showAsync(owner != null ? owner.getWindow() : null);
    }
}
