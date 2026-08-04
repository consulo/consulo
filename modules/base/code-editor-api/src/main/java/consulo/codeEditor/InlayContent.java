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
package consulo.codeEditor;

import java.util.List;

/**
 * What an inlay says, never how it looks — the counterpart of
 * {@link consulo.codeEditor.markup.LineMarkerPresentation} for custom elements.
 * <p>
 * Contains no shapes and no pixel values. Colours are allowed, as colour scheme keys, since a
 * renderer knows what its parts mean and not what a frontend will make of them.
 * <p>
 * A renderer answering {@code null} from {@link EditorCustomElementRenderer#getContent(Inlay)} is
 * simply skipped by the frontends which cannot paint, rather than failing.
 *
 * @param segments    the runs of the hint, laid out in order
 * @param smallerFont whether the hint is set a little smaller than the editor font, the way the implicit type
 *                    hints are - a scale rather than a size, since only a frontend knows what it is scaling from
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public record InlayContent(List<InlayContentSegment> segments, boolean smallerFont) {
    public static InlayContent of(List<InlayContentSegment> segments) {
        return new InlayContent(segments, false);
    }
}
