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
package consulo.codeEditor.internal;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-11-09
 * @see EditorSelectionGestureSupport
 */
public interface EditorSelectionGestureListener {
    /**
     * The mouse button which made or changed the selection was released.
     *
     * @param clickCount 2 when the selection was made by a double click
     */
    @RequiredUIAccess
    default void mouseReleased(int clickCount) {
    }

    /**
     * The pointer went onto or off the selected text. Reported on the change only, not on every move.
     */
    @RequiredUIAccess
    default void mouseOverSelectionChanged(boolean overSelection) {
    }
}
