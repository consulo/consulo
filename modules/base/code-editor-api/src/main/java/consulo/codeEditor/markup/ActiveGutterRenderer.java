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

import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Handles mouse events over a gutter marker.
 * <p>
 * Painting is not part of this contract: what the marker looks like is described by
 * {@link LineMarkerPresentationProvider}.
 *
 * @author max
 * @deprecated Kept for source parity with IntelliJ. Ties gutter interaction to
 * {@link MouseEvent} and {@link Rectangle}, so it cannot be implemented by editors other than the
 * AWT one. Use the action methods on {@link LineMarkerPresentationProvider} instead
 * ({@link LineMarkerPresentationProvider#getTooltipValue},
 * {@link LineMarkerPresentationProvider#canDoAction},
 * {@link LineMarkerPresentationProvider#doAction}), which take a toolkit-free
 * {@link consulo.ui.event.details.InputDetails} and address the presentation that was clicked
 * rather than the whole highlighter.
 */
@Deprecated
@DeprecationInfo("Use action methods of LineMarkerPresentationProvider")
public interface ActiveGutterRenderer extends LineMarkerRenderer, SimpleAccessible {
    /**
     * Returns the text of the tooltip displayed when the mouse is over the renderer area.
     *
     * @return the tooltip text, or null if no tooltip is required.
     */
    //TODO: rename into getToolTip() after deprecation deletion
    
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
    default @Nullable String getTooltipText() {
        return getTooltipValue().getNullIfEmpty();
    }

    /**
     * Processes a mouse released event on the marker.
     * <p>
     * Implementations must extend one of {@link #canDoAction} methods, otherwise the action will never be called.
     *
     * @param editor the editor to which the marker belongs.
     * @param e      the mouse event instance.
     */
    void doAction(Editor editor, MouseEvent e);

    /**
     * @return true if {@link #doAction(Editor, MouseEvent)} should be called
     */
    default boolean canDoAction(Editor editor, MouseEvent e) {
        return canDoAction(e);
    }

    default boolean canDoAction(MouseEvent e) {
        return false;
    }

    
    @Override
    default LocalizeValue getAccessibleNameValue() {
        return LocalizeValue.localizeTODO("marker: unknown");
    }

    
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
    default @Nullable Rectangle calcBounds(Editor editor, int lineNum, Rectangle preferredBounds) {
        return preferredBounds;
    }
}
