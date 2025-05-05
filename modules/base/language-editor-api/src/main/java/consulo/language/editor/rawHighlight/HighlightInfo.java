/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.rawHighlight;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.HighlightInfoFactory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2022-02-13
 */
public interface HighlightInfo extends Segment {
    public interface Builder {
        // only one 'range' call allowed
        @Nonnull
        default Builder range(@Nonnull TextRange textRange) {
            return range(textRange.getStartOffset(), textRange.getEndOffset());
        }

        @Nonnull
        @RequiredReadAction
        default Builder range(@Nonnull ASTNode node) {
            return range(node.getPsi());
        }

        @Nonnull
        @RequiredReadAction
        Builder range(@Nonnull PsiElement element);

        @Nonnull
        @RequiredReadAction
        default HighlightInfo.Builder range(@Nonnull PsiElement element, @Nonnull TextRange rangeInElement) {
            TextRange absoluteRange = rangeInElement.shiftRight(element.getTextRange().getStartOffset());
            return range(element, absoluteRange.getStartOffset(), absoluteRange.getEndOffset());
        }

        @Nonnull
        Builder range(@Nonnull PsiElement element, int start, int end);

        @Nonnull
        Builder range(int start, int end);

        @Nonnull
        Builder gutterIconRenderer(@Nonnull GutterIconRenderer gutterIconRenderer);

        @Nonnull
        Builder problemGroup(@Nonnull ProblemGroup problemGroup);

        @Nonnull
        Builder inspectionToolId(@Nonnull String inspectionTool);

        @Nonnull
        Builder description(@Nonnull LocalizeValue description);

        @Nonnull
        default Builder descriptionAndTooltip(@Nonnull LocalizeValue description) {
            return descriptionAndTooltip(description.get());
        }

        // only one allowed
        @Nonnull
        Builder textAttributes(@Nonnull TextAttributes attributes);

        @Nonnull
        Builder textAttributes(@Nonnull TextAttributesKey attributesKey);

        @Nonnull
        Builder unescapedToolTip(@Nonnull LocalizeValue unescapedToolTip);

        @Nonnull
        Builder escapedToolTip(@Nonnull LocalizeValue escapedToolTip);

        @Nonnull
        Builder endOfLine();

        @Nonnull
        Builder needsUpdateOnTyping(boolean update);

        @Nonnull
        Builder severity(@Nonnull HighlightSeverity severity);

        @Nonnull
        Builder fileLevelAnnotation();

        @Nonnull
        Builder navigationShift(int navigationShift);

        @Nonnull
        Builder group(int group);

        @Nonnull
        default Builder registerFix(@Nonnull IntentionAction action) {
            return registerFix(action, null, null, null, null);
        }

        @Nonnull
        default Builder registerFix(@Nonnull IntentionAction action, @Nonnull TextRange textRange) {
            return registerFix(action, null, null, textRange, null);
        }

        @Nonnull
        Builder registerFix(
            @Nonnull IntentionAction action,
            @Nullable List<IntentionAction> options,
            @Nullable @Nls String displayName,
            @Nullable TextRange fixRange,
            @Nullable HighlightDisplayKey key
        );

        /**
         * @return null means filtered out
         */
        @Nullable
        HighlightInfo create();

        @Nonnull
        HighlightInfo createUnconditionally();

        // region deprecated stuff

        @Nonnull
        @Deprecated
        @DeprecationInfo("Use #description(LocalizeValue)")
        default Builder description(@Nonnull String description) {
            return description(LocalizeValue.of(description));
        }

        @Nonnull
        @DeprecationInfo("Use #descriptionAndTooltip(LocalizeValue)")
        default HighlightInfo.Builder descriptionAndTooltip(@Nonnull String description) {
            return description(description).unescapedToolTip(description);
        }

        // only one allowed
        @Nonnull
        @Deprecated
        @DeprecationInfo("Use #unescapedToolTip(LocalizeValue)")
        default Builder unescapedToolTip(@Nonnull String unescapedToolTip) {
            return unescapedToolTip(LocalizeValue.of(unescapedToolTip));
        }

        @Nonnull
        @Deprecated
        @DeprecationInfo("use #escapedToolTip(LocalizeValue)")
        default Builder escapedToolTip(@Nonnull String escapedToolTip) {
            return escapedToolTip(LocalizeValue.of(escapedToolTip));
        }

        // endregion
    }

    @Nonnull
    static Builder newHighlightInfo(@Nonnull HighlightInfoType type) {
        return HighlightInfoFactory.getInstance().createBuilder(type);
    }

    @Nullable
    static HighlightInfo fromRangeHighlighter(@Nonnull RangeHighlighter highlighter) {
        return highlighter.getErrorStripeTooltip() instanceof HighlightInfo highlightInfo ? highlightInfo : null;
    }

    @Nonnull
    HighlightSeverity getSeverity();

    @Nullable
    RangeHighlighter getHighlighter();

    int getActualStartOffset();

    int getActualEndOffset();

    String getDescription();

    HighlightInfoType getType();

    @Nullable
    PsiElement getPsiElement();

    @Nullable
    String getToolTip();

    @Nullable
    TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme);

    @Nullable
    GutterMark getGutterIconRenderer();

    @Deprecated
    @DeprecationInfo("HighlightInfo.Builder#registerFix")
    void registerFix(
        @Nullable IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nullable String displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    );

    void unregisterQuickFix(@Nonnull Predicate<? super IntentionAction> condition);

    void forEachQuickFix(@Nonnull BiConsumer<IntentionAction, TextRange> consumer);
}
