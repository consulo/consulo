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

import consulo.colorScheme.TextAttributesKey;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * One run of an {@link InlayContent}, carrying its own colour so the parts of a hint which are
 * painted differently stay apart - a collapsed presentation against the text around it, the counter
 * of a code vision lens against its label.
 *
 * @param text          the text of the run, empty for a run which is nothing but an image
 * @param attributesKey the scheme key the run is coloured with, {@code null} to inherit whatever the
 *                      frontend uses for hints
 * @param image         an image drawn ahead of the text, if the run carries one
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public record InlayContentSegment(String text, @Nullable TextAttributesKey attributesKey, @Nullable Image image) {
    public static InlayContentSegment of(String text) {
        return new InlayContentSegment(text, null, null);
    }

    public static InlayContentSegment of(String text, @Nullable TextAttributesKey attributesKey) {
        return new InlayContentSegment(text, attributesKey, null);
    }

    public static InlayContentSegment of(Image image) {
        return new InlayContentSegment("", null, image);
    }
}
