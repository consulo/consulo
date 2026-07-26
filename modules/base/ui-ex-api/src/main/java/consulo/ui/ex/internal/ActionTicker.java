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
package consulo.ui.ex.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;

/**
 * Periodically notifies {@link TimerListener}s so that actions bound to UI (toolbars, menu bars) can refresh their
 * presentations.
 * <p>
 * Ticking and the activity gating happen on a background thread; a listener is only run when there was user activity
 * since the previous tick, and never while the user is typing. Listeners of a single {@link UIAccess} are then run
 * together in one UI event on that {@code UIAccess} - a listener is bound to the UI it was registered with, since an
 * application can drive several independent UIs (AWT, SWT, and one per session for web).
 *
 * @author VISTALL
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ActionTicker {
    static ActionTicker getInstance() {
        return Application.get().getInstance(ActionTicker.class);
    }

    /**
     * Registers a listener to be notified on {@code uiAccess}. The listener is held strongly until the returned
     * {@link Disposable} is disposed, or until {@code uiAccess} becomes invalid.
     */
    Disposable addListener(UIAccess uiAccess, TimerListener listener);

    /**
     * Suppresses the next ticks for a short while, so that action updates do not compete with the editor while the
     * user is typing.
     */
    void notifyEditorTyping();
}
