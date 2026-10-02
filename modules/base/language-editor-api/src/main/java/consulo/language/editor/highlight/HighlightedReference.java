// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight;

import consulo.language.psi.PsiReference;

/**
 * Marker interface to highlight references.
 * <p>
 * Use for non-soft references in non-obvious places like String literals which have some navigation target.
 * Highlighting will apply
 * {@link consulo.codeEditor.DefaultLanguageHighlighterColors#HIGHLIGHTED_REFERENCE} text attributes.
 */
public interface HighlightedReference extends PsiReference {
    default boolean isHighlightedWhenSoft() {
        return false;
    }
}
