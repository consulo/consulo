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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2022-02-13
 */
public interface HighlightInfo extends Segment {
    public interface FixBuilderBase<THIS extends FixBuilderBase<THIS>> {
        @Nonnull
        THIS options(@Nonnull List<IntentionAction> options);

        @Nonnull
        THIS displayName(@Nonnull LocalizeValue displayName);

        @Nonnull
        THIS fixRange(@Nonnull TextRange fixRange);

        @Nonnull
        @SuppressWarnings("unchecked")
        default THIS optionalFixRange(@Nullable TextRange fixRange) {
            return fixRange == null ? (THIS) this : fixRange(fixRange);
        }

        @Nonnull
        THIS key(@Nonnull HighlightDisplayKey key);

        @Nonnull
        @SuppressWarnings("unchecked")
        default THIS optionalKey(@Nullable HighlightDisplayKey key) {
            return key == null ? (THIS) this : key(key);
        }
    }

    public interface FixBuilder extends FixBuilderBase<FixBuilder> {
        void register();
    }

    public interface Builder {
        public interface FixBuilder extends FixBuilderBase<FixBuilder> {
            @Nonnull
            Builder register();
        }

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
        @SuppressWarnings("deprecation")
        default Builder descriptionAndTooltip(@Nonnull LocalizeValue description) {
            return description(description).unescapedToolTip(description);
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
        FixBuilder newFix(@Nonnull IntentionAction action);

        @Nonnull
        default Builder registerFix(@Nonnull IntentionAction action) {
            return registerFix(action, null, LocalizeValue.empty(), null, null);
        }

        @Deprecated
        @DeprecationInfo("Use HighlightInfo.Builder.newFix()...register()")
        @Nonnull
        Builder registerFix(
            @Nonnull IntentionAction action,
            @Nullable List<IntentionAction> options,
            @Nonnull LocalizeValue displayName,
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
        @Deprecated
        @DeprecationInfo("Use #descriptionAndTooltip(LocalizeValue)")
        @SuppressWarnings("deprecation")
        default HighlightInfo.Builder descriptionAndTooltip(@Nonnull String description) {
            return descriptionAndTooltip(LocalizeValue.of(description));
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

    @Nonnull
    LocalizeValue getDescription();

    HighlightInfoType getType();

    @Nullable
    PsiElement getPsiElement();

    @Nonnull
    LocalizeValue getToolTip();

    @Nullable
    TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme);

    @Nullable
    GutterMark getGutterIconRenderer();

    @Deprecated
    @DeprecationInfo("Use HighlightInfo.Builder.newFix()...register()")
    @Nonnull
    FixBuilder newFix(@Nonnull IntentionAction action);

    @Deprecated
    @DeprecationInfo("Use HighlightInfo.Builder.registerFix() or HighlightInfo.Builder.newFix()...register()")
    @SuppressWarnings("deprecation")
    default void registerFix(@Nullable IntentionAction action) {
        registerFix(action, null, LocalizeValue.empty(), null, null);
    }

    @Deprecated
    @DeprecationInfo("Use HighlightInfo.Builder.registerFix() or HighlightInfo.Builder.newFix()...register()")
    void registerFix(
        @Nullable IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nonnull LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    );

    boolean isFileLevelAnnotation();

    void unregisterQuickFix(@Nonnull Predicate<? super IntentionAction> condition);

    void forEachQuickFix(@Nonnull BiConsumer<IntentionAction, TextRange> consumer);
}
