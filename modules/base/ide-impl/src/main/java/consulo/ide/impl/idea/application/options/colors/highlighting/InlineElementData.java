// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.application.options.colors.highlighting;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.ide.impl.idea.codeInsight.hints.HintRenderer;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Map;

public final class InlineElementData extends HighlightData {
    private final String myText;
    private final boolean myAddBorder;

    public InlineElementData(int offset, TextAttributesKey attributesKey, String text, EditorColorKey additionalColorKey) {
        this(offset, attributesKey, text, false, additionalColorKey);
    }

    private InlineElementData(int offset, TextAttributesKey attributesKey, String text, boolean highlighted, EditorColorKey additionalColorKey) {
        super(offset, offset, attributesKey, additionalColorKey);
        myText = text;
        myAddBorder = highlighted;
    }

    public String getText() {
        return myText;
    }

    @Override
    public void addHighlToView(Editor view, EditorColorsScheme scheme, Map<TextAttributesKey, LocalizeValue> displayText) {
        int offset = getStartOffset();
        RendererWrapper renderer = new RendererWrapper(new HintRenderer(myText) {
            @Override
            protected @Nullable TextAttributes getTextAttributes(@Nonnull Editor editor) {
                return editor.getColorsScheme().getAttributes(getHighlightKey());
            }
        }, myAddBorder);
        view.getInlayModel().addInlineElement(offset, false, renderer);
    }

    @Override
    public void addToCollection(@Nonnull Collection<? super HighlightData> list, boolean highlighted) {
        list.add(new InlineElementData(getStartOffset(), getHighlightKey(), myText, highlighted, getAdditionalColorKey()));
    }
}
