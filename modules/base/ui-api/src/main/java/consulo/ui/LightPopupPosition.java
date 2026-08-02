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

/**
 * Where a {@link LightPopup} sits relative to what it was opened against.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public enum LightPopupPosition {
    /**
     * Under the target, which is where a popup opened off a button or a menu belongs.
     */
    BOTTOM,
    /**
     * Beside the target with their tops aligned - a submenu next to the popup which owns it.
     */
    END
}
