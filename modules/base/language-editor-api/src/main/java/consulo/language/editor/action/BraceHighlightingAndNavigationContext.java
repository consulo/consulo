// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.action;

/**
 * Describes a brace matching/navigation context computed by {@link BraceMatchingUtil#computeHighlightingAndNavigationContext}
 */
public record BraceHighlightingAndNavigationContext(int currentBraceOffset, int navigationOffset, boolean isCaretAfterBrace) {
}
