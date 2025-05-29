// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.inlay.InlayPresentation;

import java.awt.*;

public class WithAttributesPresentation extends StaticDelegatePresentation {
    private final TextAttributesKey textAttributesKey;
    private final EditorColorsScheme colorsScheme;
    private final AttributesFlags flags;

    public WithAttributesPresentation(InlayPresentation presentation,
                                      TextAttributesKey textAttributesKey,
                                      Editor editor,
                                      AttributesFlags flags) {
        super(presentation);
        this.textAttributesKey = textAttributesKey;
        this.colorsScheme = editor.getColorsScheme();
        this.flags = flags;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        TextAttributes other = colorsScheme.getAttributes(textAttributesKey);
        if (other == null) {
            other = new TextAttributes();
        }
        if (flags.skipEffects) {
            other.setEffectType(null);
        }
        if (flags.skipBackground) {
            other.setBackgroundColor(null);
        }
        if (!flags.isDefault) {
            super.paint(g, other);
        }
        else {
            TextAttributes result = attributes.clone();
            if (other.getForegroundColor() != null && !other.getForegroundColor().equals(result.getForegroundColor())) {
                result.setForegroundColor(other.getForegroundColor());
            }
            if (other.getBackgroundColor() != null && !other.getBackgroundColor().equals(result.getBackgroundColor())) {
                result.setBackgroundColor(other.getBackgroundColor());
            }
            if (other.getEffectType() != null && !other.getEffectType().equals(result.getEffectType())) {
                result.setEffectType(other.getEffectType());
            }
            if (other.getEffectColor() != null && !other.getEffectColor().equals(result.getEffectColor())) {
                result.setEffectColor(other.getEffectColor());
            }
            super.paint(g, result);
        }
    }

    public static class AttributesFlags {
        public boolean skipEffects = false;
        public boolean skipBackground = false;
        public boolean isDefault = false;

        public AttributesFlags withSkipEffects(boolean skipEffects) {
            this.skipEffects = skipEffects;
            return this;
        }

        public AttributesFlags withSkipBackground(boolean skipBackground) {
            this.skipBackground = skipBackground;
            return this;
        }

        public AttributesFlags withIsDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }
    }
}
