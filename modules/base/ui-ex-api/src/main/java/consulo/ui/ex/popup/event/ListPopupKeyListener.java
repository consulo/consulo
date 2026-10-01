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
package consulo.ui.ex.popup.event;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.popup.ListPopup;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public interface ListPopupKeyListener {
    @RequiredUIAccess
    default boolean keyPressed(ListPopup popup, KeyboardInputDetails details) {
        return false;
    }

    @RequiredUIAccess
    default boolean keyReleased(ListPopup popup, KeyboardInputDetails details) {
        return false;
    }
}
