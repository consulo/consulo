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

/**
 * A popup which hangs off something on screen - it carries no chrome of its own, it does not dim what is behind it,
 * and it goes away as soon as it loses interest. What a completion list, a hint or a submenu is shown with.
 * <p/>
 * It always has a target. A popup with nothing to point at is a {@link HeavyPopup}.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public non-sealed interface LightPopup extends Popup {
    static LightPopup create(PopupOptions options) {
        return UIInternal.get()._LightPopup_create(options);
    }

    /**
     * Opens the popup against {@code target}, which is where it stays should the frame move under it.
     */
    @RequiredUIAccess
    void showBy(Component target);

}
