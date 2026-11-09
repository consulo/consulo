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
package consulo.codeEditor.toolbar.floating;

import consulo.codeEditor.Editor;
import consulo.ui.Popup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents floating toolbar which is shown for selected text inside editor.
 * Toolbar is visible only if mouse was used for the selection.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public interface CodeFloatingToolbar {
    Key<CodeFloatingToolbar> KEY = Key.create("floating.codeToolbar");

    static @Nullable CodeFloatingToolbar getToolbar(@Nullable Editor editor) {
        return editor == null ? null : editor.getUserData(KEY);
    }

    @RequiredUIAccess
    boolean isShown();

    @RequiredUIAccess
    void scheduleShow();

    @RequiredUIAccess
    void scheduleHide();

    /**
     * Allow toolbar to be shown immediately even it was already closed before.
     */
    @RequiredUIAccess
    void allowInstantShowing();

    /**
     * Hides and disables the toolbar for the whole popup lifetime.
     */
    @RequiredUIAccess
    void hideWhilePopupVisible(JBPopup popup);

    /**
     * Hides and disables the toolbar for the whole popup lifetime.
     */
    @RequiredUIAccess
    void hideWhilePopupVisible(Popup popup);

    /**
     * Shows the toolbar for the current selection right away, even when it was closed before.
     *
     * @return completes with {@code true} once the toolbar is shown, {@code false} when it is not available here
     */
    @RequiredUIAccess
    CompletableFuture<Boolean> showAsync();

    /**
     * Opens the popup under the shown toolbar, as one opened from the button of {@code action}.
     *
     * @return {@code false} when the toolbar is not shown and nothing was opened
     */
    @RequiredUIAccess
    boolean showPopupUnderneath(AnAction action, JBPopup popup);

    /**
     * A popup opened from a button of the toolbar - closed together with the toolbar.
     */
    @RequiredUIAccess
    void attachPopup(AnAction action, JBPopup popup);

    @RequiredUIAccess
    void cancelActivePopup();

    /**
     * The action of the toolbar's button which follows (or precedes) the one whose popup is open.
     */
    @RequiredUIAccess
    @Nullable
    AnAction getNextMenuAction(boolean forward);
}
