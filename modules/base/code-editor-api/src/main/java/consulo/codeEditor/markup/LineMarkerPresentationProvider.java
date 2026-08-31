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
package consulo.codeEditor.markup;

import consulo.codeEditor.Editor;
import consulo.localize.LocalizeValue;
import consulo.ui.event.details.InputDetails;

import java.util.List;
import java.util.Set;

/**
 * Declarative replacement for {@link LineMarkerRenderer}: instead of painting, a provider returns
 * semantic {@link LineMarkerPresentation}s and each platform renders them natively.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public interface LineMarkerPresentationProvider {
    /**
     * Gutter bands this provider may emit presentations into.
     * <p>
     * Declared at provider level rather than discovered from the presentations, because gutter width
     * is calculated during <em>layout</em>, before any painting, while
     * {@link #buildPresentations(LineMarkerPresentationContext)} needs a visible-line range that
     * only exists afterwards. A platform may additionally skip a band when nothing registers a
     * painter for this provider's presentations.
     */
    Set<EditorGutterArea> getUsedAreas();

    /**
     * Builds the presentations for the current state. Must be a pure function of {@code context}
     * and the provider's own model — it is called both to paint and to hit-test, and the two must
     * not be able to disagree.
     * <p>
     * Called on the paint path, so it must not mutate state or touch the view model.
     */
    List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context);

    /**
     * Tooltip shown while the pointer rests over the presentation.
     */
    default LocalizeValue getTooltipValue(LineMarkerPresentation presentation) {
        return LocalizeValue.empty();
    }

    /**
     * @return true if {@link #doAction} should be called for this presentation. The platform has
     * already established that the pointer is within the presentation's bounds, so this is only for
     * further conditions of the provider's own.
     */
    default boolean canDoAction(LineMarkerPresentation presentation, InputDetails details) {
        return false;
    }

    /**
     * Handles a click on the presentation.
     * <p>
     * Takes {@link InputDetails} rather than a toolkit event so providers stay implementable on
     * every editor; each platform converts its own native event.
     */
    default void doAction(Editor editor, LineMarkerPresentation presentation, InputDetails details) {
    }
}
