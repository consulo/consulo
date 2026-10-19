/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.util;

import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorKey;
import consulo.diff.DiffColors;
import consulo.diff.util.DiffSeparatorGutterPresentation;
import consulo.diff.util.DiffSeparatorWavePresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class DiffLineSeparatorRenderer implements LineSeparatorPresentationProvider, LineMarkerPresentationProvider {

    private final BooleanSupplier myCondition;

    public DiffLineSeparatorRenderer() {
        this(() -> true);
    }

    public DiffLineSeparatorRenderer(BooleanSupplier condition) {
        myCondition = condition;
    }

    //
    // Editor content
    //

    @Override
    public @Nullable LineSeparatorPresentation buildPresentation(LineMarkerPresentationContext context) {
        if (!myCondition.getAsBoolean()) {
            return null;
        }

        return new DiffSeparatorWavePresentation(
            getBackgroundColor(context),
            context.getColor(TOP_BORDER),
            context.getColor(BOTTOM_BORDER)
        );
    }

    //
    // Gutter
    //

    @Override
    public Set<EditorGutterArea> getUsedAreas() {
        return Set.of(EditorGutterArea.WHOLE_GUTTER);
    }

    @Override
    public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
        if (!myCondition.getAsBoolean()) {
            return List.of();
        }

        return List.of(new DiffSeparatorGutterPresentation(
            context.startLine(),
            getBackgroundColor(context),
            context.getColor(TOP_BORDER),
            context.getColor(BOTTOM_BORDER),
            context.isAnnotationsShown() ? context.getColor(EditorColors.GUTTER_BACKGROUND) : null
        ));
    }

    //
    // Parameters
    //

    public static final EditorColorKey BACKGROUND = DiffColors.DIFF_SEPARATORS_BACKGROUND;
    private static final EditorColorKey TOP_BORDER = DiffColors.DIFF_SEPARATORS_TOP_BORDER;
    private static final EditorColorKey BOTTOM_BORDER = DiffColors.DIFF_SEPARATORS_BOTTOM_BORDER;

    private static ColorValue getBackgroundColor(LineMarkerPresentationContext context) {
        ColorValue color = context.getColor(BACKGROUND);
        return color != null ? color : new RGBColor(128, 128, 128);
    }
}
