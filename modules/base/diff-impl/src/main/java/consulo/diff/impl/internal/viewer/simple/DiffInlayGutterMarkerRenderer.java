// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.viewer.simple;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.InlayFillColorLineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.LineMarkerPresentationProvider;
import consulo.diff.util.TextDiffType;
import consulo.ui.color.ColorValue;

import java.util.List;
import java.util.Set;

class DiffInlayGutterMarkerRenderer implements LineMarkerPresentationProvider {
    private final TextDiffType myType;
    private final Editor myEditor;
    private final Inlay<?> myInlay;

    DiffInlayGutterMarkerRenderer(TextDiffType type, Editor editor, Inlay<?> inlay) {
        myType = type;
        myEditor = editor;
        myInlay = inlay;
    }

    @Override
    public Set<EditorGutterArea> getUsedAreas() {
        return Set.of(EditorGutterArea.WHOLE_GUTTER);
    }

    @Override
    public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
        ColorValue color = AlignableChange.getAlignedChangeColor(myType, myEditor);
        if (color == null || !myInlay.isValid()) {
            return List.of();
        }
        int line = myEditor.getDocument().getLineNumber(myInlay.getOffset());
        return List.of(new InlayFillColorLineMarkerPresentation(line, EditorGutterArea.WHOLE_GUTTER, color, myInlay));
    }
}
