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
package consulo.ide.impl.diff;

import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.LineMarkerPresentationProvider;
import consulo.colorScheme.TextAttributes;
import consulo.diff.util.DiffChunkPresentation;
import consulo.diff.util.TextDiffType;
import consulo.diff.util.TextDiffTypeFactory;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DiffLineMarkerRenderer implements LineMarkerPresentationProvider {

    private final TextDiffType myDiffType;
    private final boolean myIgnoredFoldingOutline;
    private final boolean myResolved;
    private final boolean myHideWithoutLineNumbers;

    private final boolean myEmptyRange;
    private final boolean myLastLine;

    public DiffLineMarkerRenderer(
        TextDiffType diffType,
        boolean ignoredFoldingOutline,
        boolean resolved,
        boolean hideWithoutLineNumbers,
        boolean isEmptyRange,
        boolean isLastLine
    ) {
        myDiffType = diffType;
        myIgnoredFoldingOutline = ignoredFoldingOutline;
        myResolved = resolved;
        myHideWithoutLineNumbers = hideWithoutLineNumbers;
        myEmptyRange = isEmptyRange;
        myLastLine = isLastLine;
    }

    @Override
    public Set<EditorGutterArea> getUsedAreas() {
        return Set.of(
            EditorGutterArea.WHOLE_GUTTER,
            EditorGutterArea.BEFORE_ANNOTATIONS,
            EditorGutterArea.AFTER_ANNOTATIONS,
            EditorGutterArea.AFTER_ANNOTATIONS_TO_SEPARATOR,
            EditorGutterArea.BEFORE_WHITESPACE_SEPARATOR,
            EditorGutterArea.FROM_WHITESPACE_SEPARATOR
        );
    }

    @Override
    public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
        TextAttributes attributes = context.getAttributes(myDiffType.getKey());
        ColorValue borderColor = attributes.getBackgroundColor();
        ColorValue fillColor = myResolved ? null : borderColor;

        int startLine;
        int endLine;
        if (myEmptyRange && myLastLine) {
            startLine = context.lineCount();
            endLine = startLine;
        }
        else {
            startLine = context.startLine();
            endLine = myEmptyRange ? startLine : context.endLine();
        }

        List<DiffChunkPresentation> result = new ArrayList<>();

        // Without line numbers there is nothing worth marking left of the folding outline.
        if (myHideWithoutLineNumbers && !context.isLineNumbersShown()) {
            add(result, startLine, endLine, EditorGutterArea.FROM_WHITESPACE_SEPARATOR, borderColor, fillColor);
            return result;
        }

        boolean annotationsShown = context.isAnnotationsShown();
        if (annotationsShown) {
            // the annotations area keeps its own background, so the chunk is split around it
            add(result, startLine, endLine, EditorGutterArea.BEFORE_ANNOTATIONS, borderColor, fillColor);
        }

        if (myIgnoredFoldingOutline) {
            // the folding outline shows the chunk as ignored, the rest as a normal change
            ColorValue ignoredFill = myResolved ? null : getIgnoredColor(attributes, context);
            add(result, startLine, endLine, EditorGutterArea.FROM_WHITESPACE_SEPARATOR, borderColor, ignoredFill);
            add(
                result,
                startLine,
                endLine,
                annotationsShown ? EditorGutterArea.AFTER_ANNOTATIONS_TO_SEPARATOR : EditorGutterArea.BEFORE_WHITESPACE_SEPARATOR,
                borderColor,
                fillColor
            );
        }
        else {
            add(
                result,
                startLine,
                endLine,
                annotationsShown ? EditorGutterArea.AFTER_ANNOTATIONS : EditorGutterArea.WHOLE_GUTTER,
                borderColor,
                fillColor
            );
        }

        return result;
    }

    private void add(
        List<DiffChunkPresentation> result,
        int startLine,
        int endLine,
        EditorGutterArea area,
        ColorValue borderColor,
        @Nullable ColorValue fillColor
    ) {
        result.add(new DiffChunkPresentation(startLine, endLine, area, borderColor, fillColor, myResolved, myDiffType));
    }

    /**
     * Mirrors {@code TextDiffType.getIgnoredColor}, which needs the editor background to blend
     * against when the scheme defines no explicit foreground.
     */
    private static ColorValue getIgnoredColor(TextAttributes attributes, LineMarkerPresentationContext context) {
        ColorValue color = attributes.getForegroundColor();
        if (color != null) {
            return color;
        }
        return TextDiffTypeFactory.getMiddleColor(attributes.getBackgroundColor(), context.getEditorBackgroundColor());
    }
}
