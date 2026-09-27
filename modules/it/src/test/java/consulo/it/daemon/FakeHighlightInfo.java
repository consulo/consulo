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
package consulo.it.daemon;

import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.GutterMark;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Only what {@link ExpectedHighlight#matches} looks at is answered; everything else throws, so extending the
 * comparison without extending this double fails loudly instead of quietly comparing nulls.
 *
 * @author VISTALL
 */
final class FakeHighlightInfo implements HighlightInfo {
    private final HighlightSeverity mySeverity;
    private final int myStartOffset;
    private final int myEndOffset;
    private final @Nullable String myDescription;
    private final boolean myAfterEndOfLine;

    private FakeHighlightInfo(
        HighlightSeverity severity,
        int startOffset,
        int endOffset,
        @Nullable String description,
        boolean afterEndOfLine
    ) {
        mySeverity = severity;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myDescription = description;
        myAfterEndOfLine = afterEndOfLine;
    }

    static FakeHighlightInfo of(HighlightSeverity severity, int startOffset, int endOffset, @Nullable String description) {
        return new FakeHighlightInfo(severity, startOffset, endOffset, description, false);
    }

    static FakeHighlightInfo afterEndOfLine(HighlightSeverity severity, int startOffset, int endOffset, String description) {
        return new FakeHighlightInfo(severity, startOffset, endOffset, description, true);
    }

    @Override
    public HighlightSeverity getSeverity() {
        return mySeverity;
    }

    @Override
    public int getStartOffset() {
        return myStartOffset;
    }

    @Override
    public int getEndOffset() {
        return myEndOffset;
    }

    @Override
    public boolean isAfterEndOfLine() {
        return myAfterEndOfLine;
    }

    @Override
    public LocalizeValue getDescription() {
        return myDescription == null ? null : LocalizeValue.of(myDescription);
    }

    @Override
    public @Nullable RangeHighlighter getHighlighter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getActualStartOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getActualEndOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HighlightInfoType getType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable PsiElement getPsiElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalizeValue getToolTip() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme scheme) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable GutterMark getGutterIconRenderer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FixBuilder newFix(IntentionAction action) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void registerFix(
        @Nullable IntentionAction action,
        @Nullable List<IntentionAction> options,
        LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFileLevelAnnotation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregisterQuickFix(Predicate<? super IntentionAction> condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachQuickFix(BiConsumer<IntentionAction, TextRange> consumer) {
        throw new UnsupportedOperationException();
    }
}
