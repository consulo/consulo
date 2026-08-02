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

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.LightPopupCloseEvent;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * An area which floats over the frame, like a {@link Window} but light - it carries no chrome of its own, it is
 * anchored to something rather than placed, and it goes away as soon as it loses interest.
 * <p/>
 * This is only the surface. What is drawn on it, and what a close means, belongs to whoever builds it.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface LightPopup extends Component, Disposable {
    static LightPopup create(LightPopupOptions options) {
        return UIInternal.get()._LightPopup_create(options);
    }

    /**
     * A caption over the content, shown only while there is one to show.
     */
    @RequiredUIAccess
    void setTitle(@Nullable String title);

    @RequiredUIAccess
    void setContent(Component content);

    /**
     * Opens the popup against {@code target}, which is where it stays should the frame move under it.
     */
    @RequiredUIAccess
    void showBy(Component target);

    /**
     * Opens the popup against the frame rather than any one component, for a popup raised by something with no
     * place on screen - a menu action, a shortcut.
     */
    @RequiredUIAccess
    void showInCenterOf(@Nullable Window window);

    /**
     * Closes the popup. A popup dismissed by the user closes the same way, so a close listener sees every close -
     * whether one was a choice or an abandonment is for the caller to track.
     */
    @RequiredUIAccess
    void close();

    boolean isVisible();

    default Disposable addCloseListener(ComponentEventListener<LightPopup, LightPopupCloseEvent> listener) {
        return addListener(LightPopupCloseEvent.class, listener);
    }
}
