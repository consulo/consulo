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
package consulo.language.editor.ui.internal;

import consulo.ui.Point2D;
import org.jspecify.annotations.Nullable;

/**
 * A shown ctrl-hover quick-doc tooltip, see {@link EditorDocTooltipService#show}.
 */
public interface EditorDocTooltip {
    boolean isVisible();

    void hide();

    /**
     * Replaces the shown html, resizing or re-showing the surface as the frontend requires.
     */
    void updateText(String html);

    void addHideListener(Runnable runnable);

    /**
     * Whether a pointer move to {@code screenLocation} should keep the tooltip alive - the pointer is over it
     * or heading towards it.
     */
    boolean shouldSuppressMove(@Nullable Point2D prevScreenLocation, Point2D screenLocation);
}
