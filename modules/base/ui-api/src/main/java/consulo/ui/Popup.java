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
import consulo.ui.event.PopupCloseEvent;
import org.jspecify.annotations.Nullable;

/**
 * An area which floats over the frame. What it is drawn with, and what a close means, belongs to whoever builds it.
 * <p/>
 * There are two of these because a popup with something to point at and a popup without one are not the same thing:
 * {@link LightPopup} hangs off a target and is as unobtrusive as the frontend can make it, {@link HeavyPopup} is
 * placed rather than anchored and may take the frame over while it is up.
 *
 * @author VISTALL
 */
public sealed interface Popup extends Component, Disposable permits LightPopup, HeavyPopup {
    /**
     * A caption over the content, shown only while there is one to show.
     */
    @RequiredUIAccess
    void setTitle(@Nullable String title);

    @RequiredUIAccess
    void setContent(Component content);

    /**
     * Closes the popup. A popup dismissed by the user closes the same way, so a close listener sees every close -
     * whether one was a choice or an abandonment is for the caller to track.
     */
    @RequiredUIAccess
    void close();

    boolean isVisible();

    default Disposable addCloseListener(ComponentEventListener<Popup, PopupCloseEvent> listener) {
        return addListener(PopupCloseEvent.class, listener);
    }
}
