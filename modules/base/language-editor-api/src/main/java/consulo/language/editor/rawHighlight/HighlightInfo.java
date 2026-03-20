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
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2022-02-13
 */
public interface HighlightInfo extends Segment {
    public interface FixBuilderBase<THIS extends FixBuilderBase<THIS>> {
        
        THIS options(List<IntentionAction> options);

        
        THIS displayName(LocalizeValue displayName);

        
        THIS fixRange(TextRange fixRange);

        
        @SuppressWarnings("unchecked")
        default THIS optionalFixRange(@Nullable TextRange fixRange) {
            return fixRange == null ? (THIS) this : fixRange(fixRange);
        }

        
        THIS key(HighlightDisplayKey key);

        
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
            
            Builder register();
        }

        // only one 'range' call allowed
        
        default Builder range(TextRange textRange) {
            return range(textRange.getStartOffset(), textRange.getEndOffset());
        }

        
        @RequiredReadAction
        default Builder range(ASTNode node) {
            return range(node.getPsi());
        }

        
        @RequiredReadAction
        Builder range(PsiElement element);

        
        @RequiredReadAction
        default HighlightInfo.Builder range(PsiElement element, TextRange rangeInElement) {
            TextRange absoluteRange = rangeInElement.shiftRight(element.getTextRange().getStartOffset());
            return range(element, absoluteRange.getStartOffset(), absoluteRange.getEndOffset());
        }

        
        Builder range(PsiElement element, int start, int end);

        
        Builder range(int start, int end);

        
        Builder gutterIconRenderer(GutterIconRenderer gutterIconRenderer);

        
        Builder problemGroup(ProblemGroup problemGroup);

        
        Builder inspectionToolId(String inspectionTool);

        
        Builder description(LocalizeValue description);

        
        @SuppressWarnings("deprecation")
        default Builder descriptionAndTooltip(LocalizeValue description) {
            return description(description).unescapedToolTip(description);
        }

        // only one allowed
        
        Builder textAttributes(TextAttributes attributes);

        
        Builder textAttributes(TextAttributesKey attributesKey);

        
        Builder unescapedToolTip(LocalizeValue unescapedToolTip);

        
        Builder escapedToolTip(LocalizeValue escapedToolTip);

        
        Builder endOfLine();

        
        Builder needsUpdateOnTyping(boolean update);

        
        Builder severity(HighlightSeverity severity);

        
        Builder fileLevelAnnotation();

        
        Builder navigationShift(int navigationShift);

        
        Builder group(int group);

        
        FixBuilder newFix(IntentionAction action);

        
        default Builder registerFix(IntentionAction action) {
            return registerFix(action, null, LocalizeValue.empty(), null, null);
        }

        @Deprecated
        @DeprecationInfo("Use HighlightInfo.Builder.newFix()...register()")
        
        Builder registerFix(
            IntentionAction action,
            @Nullable List<IntentionAction> options,
            LocalizeValue displayName,
            @Nullable TextRange fixRange,
            @Nullable HighlightDisplayKey key
        );

        /**
         * @return null means filtered out
         */
        @Nullable HighlightInfo create();

        
        HighlightInfo createUnconditionally();

        // region deprecated stuff

        
        @Deprecated
        @DeprecationInfo("Use #description(LocalizeValue)")
        default Builder description(String description) {
            return description(LocalizeValue.of(description));
        }

        
        @Deprecated
        @DeprecationInfo("Use #descriptionAndTooltip(LocalizeValue)")
        @SuppressWarnings("deprecation")
        default HighlightInfo.Builder descriptionAndTooltip(String description) {
            return descriptionAndTooltip(LocalizeValue.of(description));
        }

        // only one allowed
        
        @Deprecated
        @DeprecationInfo("Use #unescapedToolTip(LocalizeValue)")
        default Builder unescapedToolTip(String unescapedToolTip) {
            return unescapedToolTip(LocalizeValue.of(unescapedToolTip));
        }

        
        @Deprecated
        @DeprecationInfo("use #escapedToolTip(LocalizeValue)")
        default Builder escapedToolTip(String escapedToolTip) {
            return escapedToolTip(LocalizeValue.of(escapedToolTip));
        }

        // endregion
    }

    
    static Builder newHighlightInfo(HighlightInfoType type) {
        return HighlightInfoFactory.getInstance().createBuilder(type);
    }

    static @Nullable HighlightInfo fromRangeHighlighter(RangeHighlighter highlighter) {
        return highlighter.getErrorStripeTooltip() instanceof HighlightInfo highlightInfo ? highlightInfo : null;
    }

    
    HighlightSeverity getSeverity();

    @Nullable RangeHighlighter getHighlighter();

    int getActualStartOffset();

    int getActualEndOffset();

    
    LocalizeValue getDescription();

    HighlightInfoType getType();

    @Nullable PsiElement getPsiElement();

    
    LocalizeValue getToolTip();

    @Nullable TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme);

    @Nullable GutterMark getGutterIconRenderer();

    @Deprecated
    @DeprecationInfo("Use HighlightInfo.Builder.newFix()...register()")
    
    FixBuilder newFix(IntentionAction action);

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
        LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    );

    boolean isFileLevelAnnotation();

    void unregisterQuickFix(Predicate<? super IntentionAction> condition);

    void forEachQuickFix(BiConsumer<IntentionAction, TextRange> consumer);
}
