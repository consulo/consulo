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

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A message box: a title, a body, an image chosen by severity, and a row of answers the platform
 * already has names for. Each answer carries the value it stands for, so the result is the caller's
 * own type rather than a code to translate.
 * <p>
 * Showing never blocks the calling frame. Consume the result with
 * {@link CompletableFuture#whenComplete}, which reports failure as well as success.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public interface MessageBoxBuilder<V extends @Nullable Object> {
    static <V extends @Nullable Object> MessageBoxBuilder<V> create() {
        return UIInternal.get()._MessageBox_create();
    }

    /**
     * Empty - the default - lets the frontend use the application name.
     */
    MessageBoxBuilder<V> title(LocalizeValue title);

    MessageBoxBuilder<V> text(LocalizeValue text);

    /**
     * Secondary text, shown only if the reader asks for it. A frontend with nowhere to put it may
     * show it inline instead.
     */
    MessageBoxBuilder<V> detail(LocalizeValue detail);

    MessageBoxBuilder<V> asPlain();

    MessageBoxBuilder<V> asInfo();

    MessageBoxBuilder<V> asWarning();

    MessageBoxBuilder<V> asError();

    MessageBoxBuilder<V> asQuestion();

    /**
     * Overrides the image the severity would have chosen. Prefer stating the severity.
     */
    MessageBoxBuilder<V> icon(Image icon);

    MessageBoxBuilder<V> richText();

    /**
     * Rich text whose links are to be reported back. A frontend which cannot report activation
     * still renders the body.
     */
    MessageBoxBuilder<V> richText(MessageLinkHandler linkHandler);

    default MessageBoxBuilder<V> button(MessageButtonRole role, V value) {
        return button(role, () -> value);
    }

    MessageBoxBuilder<V> button(MessageButtonRole role, Supplier<V> valueGetter);

    default MessageBoxBuilder<V> button(MessageButtonRole role, LocalizeValue text, V value) {
        return button(role, text, () -> value);
    }

    /**
     * A standard role wearing a different label. It keeps its role, so it is still placed and
     * treated as that answer - this is how a button is reworded, not how one is invented.
     */
    MessageBoxBuilder<V> button(MessageButtonRole role, LocalizeValue text, Supplier<V> valueGetter);

    /**
     * Marks the last added button as the one activated by default. Any previous default is cleared.
     */
    MessageBoxBuilder<V> asDefaultButton();

    /**
     * Marks the last added button as the one a dismissal answers with.
     */
    MessageBoxBuilder<V> asExitButton();

    default MessageBoxBuilder<V> exitValue(V value) {
        return exitValue(() -> value);
    }

    MessageBoxBuilder<V> exitValue(Supplier<V> valueGetter);

    /**
     * Adds a help affordance. Whether the box stays open while it runs is the frontend's, so the
     * action must be written to work either way.
     */
    MessageBoxBuilder<V> help(@RequiredUIAccess Runnable action);

    MessageBoxBuilder<V> remember(MessageBoxRemember<V> remember);

    /**
     * Cancelling the returned result closes the box, so a caller which no longer needs an answer -
     * the process it was asking about has already exited, say - takes the question back rather than
     * leaving it on screen.
     *
     * @return a result which answers with the value of the button that was pressed
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
