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

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * A popup which is placed rather than anchored, for one raised by something with no place on screen - a menu action,
 * a shortcut. It may dim what is behind it and take the frame over while it is up, which is why anything with a
 * target is better off as a {@link LightPopup}.
 *
 * @author VISTALL
 */
public non-sealed interface HeavyPopup extends Popup {
    static HeavyPopup create(PopupOptions options) {
        return UIInternal.get()._HeavyPopup_create(options);
    }

    /**
     * Opens the popup over the middle of {@code window}, or of the frame when none is given.
     */
    @RequiredUIAccess
    void showInCenterOf(@Nullable Window window);
}
