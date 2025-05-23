/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.codeEditor.markup;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.util.SimpleAccessible;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Interface which should be implemented in order to paint custom markers in the line
 * marker area (over the folding area) and process mouse events on the markers.
 *
 * @author max
 */
public interface ActiveGutterRenderer extends LineMarkerRenderer, SimpleAccessible {
    /**
     * Returns the text of the tooltip displayed when the mouse is over the renderer area.
     *
     * @return the tooltip text, or null if no tooltip is required.
     */
    //TODO: rename into getToolTip() after deprecation deletion
    @Nonnull
    default LocalizeValue getTooltipValue() {
        return LocalizeValue.ofNullable(getTooltipText());
    }

    /**
     * Returns the text of the tooltip displayed when the mouse is over the renderer area.
     *
     * @return the tooltip text, or null if no tooltip is required.
     */
    @Deprecated
    @DeprecationInfo("Use getToolTipValue(int)")
    @Nullable
    default String getTooltipText() {
        LocalizeValue tooltip = getTooltipValue();
        return tooltip == LocalizeValue.empty() ? null : tooltip.get();
    }

    /**
     * Processes a mouse released event on the marker.
     * <p>
     * Implementations must extend one of {@link #canDoAction} methods, otherwise the action will never be called.
     *
     * @param editor the editor to which the marker belongs.
     * @param e      the mouse event instance.
     */
    void doAction(@Nonnull Editor editor, @Nonnull MouseEvent e);

    /**
     * @return true if {@link #doAction(Editor, MouseEvent)} should be called
     */
    default boolean canDoAction(@Nonnull Editor editor, @Nonnull MouseEvent e) {
        return canDoAction(e);
    }

    default boolean canDoAction(@Nonnull MouseEvent e) {
        return false;
    }

    @Nonnull
    @Override
    default LocalizeValue getAccessibleNameValue() {
        return LocalizeValue.localizeTODO("marker: unknown");
    }

    @Nonnull
    @Override
    default LocalizeValue getAccessibleTooltipValue() {
        return getTooltipValue();
    }

    /**
     * Calculates the rectangular bounds enclosing the marker.
     * Returns null if the marker is not rendered for the provided line.
     *
     * @param editor          the editor the renderer belongs to
     * @param lineNum         the line which the marker should intersect
     * @param preferredBounds the preferred bounds to take into account
     * @return the new calculated bounds or the preferred bounds or null
     */
    @Nullable
    default Rectangle calcBounds(@Nonnull Editor editor, int lineNum, @Nonnull Rectangle preferredBounds) {
        return preferredBounds;
    }
}
