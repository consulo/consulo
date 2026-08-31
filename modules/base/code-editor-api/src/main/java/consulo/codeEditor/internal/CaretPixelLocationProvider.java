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

import org.jspecify.annotations.Nullable;

/**
 * An editor which can say where its caret ended up on screen.
 * <p/>
 * {@code Editor#visualPositionToXY} answers the same question by laying the text out itself, which an editor that
 * hands the layout to the frontend cannot do - the browser is the only thing which knows where a character went. Such
 * an editor implements this instead, and whatever wants to open against the caret - the completion lookup above all -
 * asks through here.
 *
 * @author VISTALL
 */
public interface CaretPixelLocationProvider {
    /**
     * Pixels from the top left of the editor's own ui component, so it can be handed straight to a popup anchored
     * inside that component.
     *
     * @param x      distance from the left of the editor component
     * @param y      distance from its top, at the top of the caret line
     * @param height height of the caret line, so a popup with no room below it can go above the line
     * @param textX  where the text of a line begins, for what is put beside the line rather than beside the caret
     */
    record CaretPixelLocation(int x, int y, int height, int textX) {
    }

    /**
     * {@code null} while the caret has nowhere on screen to be - inside a collapsed region, or before the frontend has
     * reported anything back.
     */
    @Nullable
    CaretPixelLocation getCaretPixelLocation();
}
