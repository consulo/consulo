// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.internal;

import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;

/**
 * Attributes created as a result of merging a list of {@link TextAttributesKey}s.
 * Used in combination with {@link consulo.language.editor.highlight.SyntaxHighlighter#getTokenHighlights}
 */
public final class LayeredTextAttributes extends TextAttributes {
    public static LayeredTextAttributes create(EditorColorsScheme scheme, TextAttributesKey [] keys) {
        TextAttributes result = new TextAttributes();

        for (TextAttributesKey key : keys) {
            TextAttributes attributes = scheme.getAttributes(key);
            if (attributes != null) {
                result = TextAttributes.merge(result, attributes);
            }
        }

        return new LayeredTextAttributes(keys, result);
    }

    private final TextAttributesKey[] myKeys;

    private LayeredTextAttributes(TextAttributesKey [] keys, TextAttributes attributes) {
        super(attributes.getForegroundColor(),
            attributes.getBackgroundColor(),
            attributes.getEffectColor(),
            attributes.getEffectType(),
            attributes.getFontType());
        myKeys = keys;
    }

    
    public TextAttributesKey [] getKeys() {
        return myKeys;
    }
}
