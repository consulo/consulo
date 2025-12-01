// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.rawHighlight;

import consulo.application.Application;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.HighlighterColors;
import consulo.codeEditor.markup.GutterMark;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.RangeMarker;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.Pass;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemHighlightTypeInspectionRuler;
import consulo.language.editor.intention.HintAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.InspectionCache;
import consulo.language.editor.internal.InspectionCacheService;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.rawHighlight.*;
import consulo.language.editor.util.HighlightTypeUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.util.collection.Lists;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class HighlightInfoImpl implements HighlightInfo {
    private static final Logger LOG = Logger.getInstance(HighlightInfoImpl.class);

    private static final byte BIJECTIVE_MASK = 0x1;
    private static final byte HAS_HINT_MASK = 0x2;
    private static final byte FROM_INJECTION_MASK = 0x4;
    private static final byte AFTER_END_OF_LINE_MASK = 0x8;
    private static final byte FILE_LEVEL_ANNOTATION_MASK = 0x10;
    private static final byte NEEDS_UPDATE_ON_TYPING_MASK = 0x20;

    @MagicConstant(intValues = {
        BIJECTIVE_MASK,
        HAS_HINT_MASK,
        FROM_INJECTION_MASK,
        AFTER_END_OF_LINE_MASK,
        FILE_LEVEL_ANNOTATION_MASK,
        NEEDS_UPDATE_ON_TYPING_MASK
    })
    private @interface FlagConstant {
    }

    public final TextAttributes myForcedTextAttributes;
    public final TextAttributesKey myForcedTextAttributesKey;
    @Nonnull
    private final HighlightInfoType myType;
    private final int myStartOffset;
    private final int myEndOffset;

    public List<Pair<IntentionActionDescriptor, TextRange>> myQuickFixActionRanges;
    public List<Pair<IntentionActionDescriptor, RangeMarker>> myQuickFixActionMarkers;

    @Nonnull
    private final LocalizeValue myDescription;
    @Nonnull
    private final LocalizeValue myToolTip;
    @Nonnull
    private final HighlightSeverity mySeverity;
    private final GutterMark myGutterIconRenderer;
    private final ProblemGroup myProblemGroup;
    private final String myInspectionToolId;

    private int myGroup;
    private int myFixStartOffset;
    private int myFixEndOffset;
    private volatile byte myFlags;  // see `@FlagConstant`

    private final int myNavigationShift;
    public JComponent myFileLevelComponent;

    /**
     * null means it the same as highlighter
     */
    @Nullable
    public RangeMarker myFixMarker;
    protected volatile RangeHighlighterEx myHighlighter; // modified in EDT only
    private PsiElement myPsiElement;

    /**
     * Returns the HighlightInfo instance from which the given range highlighter was created, or null if there isn't any.
     */
    @Nullable
    public static HighlightInfoImpl fromRangeHighlighter(@Nonnull RangeHighlighter highlighter) {
        return highlighter.getErrorStripeTooltip() instanceof HighlightInfoImpl highlightInfo ? highlightInfo : null;
    }

    @Nonnull
    public ProperTextRange getFixTextRange() {
        return new ProperTextRange(myFixStartOffset, myFixEndOffset);
    }

    public void setFromInjection(@SuppressWarnings("SameParameterValue") boolean fromInjection) {
        setFlag(FROM_INJECTION_MASK, fromInjection);
    }

    @Override
    public PsiElement getPsiElement() {
        return myPsiElement;
    }

    public int getNavigationShift() {
        return myNavigationShift;
    }

    @Nonnull
    @Override
    public LocalizeValue getToolTip() {
        return myToolTip;
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return myDescription;
    }

    @Nonnull
    @Override
    public HighlightInfoType getType() {
        return myType;
    }

    @Nullable
    public String getInspectionToolId() {
        return myInspectionToolId;
    }

    private boolean isFlagSet(@FlagConstant byte mask) {
        return BitUtil.isSet(myFlags, mask);
    }

    private void setFlag(@FlagConstant byte mask, boolean value) {
        myFlags = BitUtil.set(myFlags, mask, value);
    }

    @Override
    public boolean isFileLevelAnnotation() {
        return isFlagSet(FILE_LEVEL_ANNOTATION_MASK);
    }

    public boolean isBijective() {
        return isFlagSet(BIJECTIVE_MASK);
    }

    public void setBijective(boolean bijective) {
        setFlag(BIJECTIVE_MASK, bijective);
    }

    @Nonnull
    @Override
    public HighlightSeverity getSeverity() {
        return mySeverity;
    }

    @Override
    public RangeHighlighterEx getHighlighter() {
        return myHighlighter;
    }

    /**
     * Modified only in EDT.
     */
    @RequiredUIAccess
    public void setHighlighter(@Nullable RangeHighlighterEx highlighter) {
        UIAccess.assertIsUIThread();
        myHighlighter = highlighter;
    }

    public boolean isAfterEndOfLine() {
        return isFlagSet(AFTER_END_OF_LINE_MASK);
    }

    @Nullable
    @Override
    public TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme) {
        if (myForcedTextAttributes != null) {
            return myForcedTextAttributes;
        }

        EditorColorsScheme colorsScheme = getColorsScheme(editorColorsScheme);

        if (myForcedTextAttributesKey != null) {
            return colorsScheme.getAttributes(myForcedTextAttributesKey);
        }

        return SeverityRegistrarUtil.getAttributesByType(element, myType, colorsScheme);
    }

    /**
     * @param element
     * @param colorsScheme when null, a global scheme will be used
     * @return
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public ColorValue getErrorStripeMarkColor(@Nonnull PsiElement element, @Nullable EditorColorsScheme colorsScheme) {
        if (myForcedTextAttributes != null) {
            return myForcedTextAttributes.getErrorStripeColor();
        }

        EditorColorsScheme scheme = getColorsScheme(colorsScheme);
        if (myForcedTextAttributesKey != null) {
            TextAttributes forcedTextAttributes = scheme.getAttributes(myForcedTextAttributesKey);
            if (forcedTextAttributes != null) {
                ColorValue errorStripeColor = forcedTextAttributes.getErrorStripeColor();
                // let's copy above behaviour of forcedTextAttributes stripe color, but I'm not sure the behaviour is correct in general
                if (errorStripeColor != null) {
                    return errorStripeColor;
                }
            }
        }

        if (getSeverity() == HighlightSeverity.ERROR) {
            return scheme.getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.WARNING) {
            return scheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.INFO) {
            return scheme.getAttributes(CodeInsightColors.INFO_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.WEAK_WARNING) {
            return scheme.getAttributes(CodeInsightColors.WEAK_WARNING_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) {
            return scheme.getAttributes(CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING).getErrorStripeColor();
        }

        TextAttributes attributes = SeverityRegistrarUtil.getAttributesByType(element, myType, scheme);
        return attributes == null ? null : attributes.getErrorStripeColor();
    }

    @Nonnull
    private static EditorColorsScheme getColorsScheme(@Nullable EditorColorsScheme customScheme) {
        return customScheme != null ? customScheme : EditorColorsManager.getInstance().getGlobalScheme();
    }

    @Nullable
    static String htmlEscapeToolTip(@Nullable String unescapedTooltip) {
        return unescapedTooltip == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(unescapedTooltip));
    }

    public boolean needUpdateOnTyping() {
        return isFlagSet(NEEDS_UPDATE_ON_TYPING_MASK);
    }

    public HighlightInfoImpl(
        @Nullable TextAttributes forcedTextAttributes,
        @Nullable TextAttributesKey forcedTextAttributesKey,
        @Nonnull HighlightInfoType type,
        int startOffset,
        int endOffset,
        @Nonnull LocalizeValue escapedDescription,
        @Nonnull LocalizeValue escapedToolTip,
        @Nonnull HighlightSeverity severity,
        boolean afterEndOfLine,
        @Nullable Boolean needsUpdateOnTyping,
        boolean isFileLevelAnnotation,
        int navigationShift,
        ProblemGroup problemGroup,
        @Nullable String inspectionToolId,
        GutterMark gutterIconRenderer,
        int group
    ) {
        if (startOffset < 0 || startOffset > endOffset) {
            LOG.error(
                "Incorrect highlightInfo bounds. description=" + escapedDescription + ";" +
                    " startOffset=" + startOffset + ";" +
                    " endOffset=" + endOffset + ";" +
                    "type=" + type
            );
        }
        myForcedTextAttributes = forcedTextAttributes;
        myForcedTextAttributesKey = forcedTextAttributesKey;
        myType = type;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myFixStartOffset = startOffset;
        myFixEndOffset = endOffset;
        myDescription = escapedDescription;
        // optimization: do not retain extra memory if can recompute
        myToolTip = escapedToolTip;
        mySeverity = severity;
        setFlag(AFTER_END_OF_LINE_MASK, afterEndOfLine);
        setFlag(NEEDS_UPDATE_ON_TYPING_MASK, calcNeedUpdateOnTyping(needsUpdateOnTyping, type));
        setFlag(FILE_LEVEL_ANNOTATION_MASK, isFileLevelAnnotation);
        myNavigationShift = navigationShift;
        myProblemGroup = problemGroup;
        myGutterIconRenderer = gutterIconRenderer;
        myInspectionToolId = inspectionToolId;
        myGroup = group;
    }

    private static boolean calcNeedUpdateOnTyping(@Nullable Boolean needsUpdateOnTyping, HighlightInfoType type) {
        if (needsUpdateOnTyping != null) {
            return needsUpdateOnTyping;
        }
        //noinspection SimplifiableIfStatement
        if (type instanceof HighlightInfoType.UpdateOnTypingSuppressible updateOnTypingSuppressible) {
            return updateOnTypingSuppressible.needsUpdateOnTyping();
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || obj instanceof HighlightInfoImpl that
            && that.getSeverity() == getSeverity()
            && that.myStartOffset == myStartOffset
            && that.myEndOffset == myEndOffset
            && Objects.equals(that.myType, myType)
            && Objects.equals(that.myGutterIconRenderer, myGutterIconRenderer)
            && Objects.equals(that.myForcedTextAttributes, myForcedTextAttributes)
            && Objects.equals(that.myForcedTextAttributesKey, myForcedTextAttributesKey)
            && that.getDescription().equals(getDescription());
    }

    public boolean equalsByActualOffset(@Nonnull HighlightInfoImpl info) {
        return info == this
            || info.getSeverity() == getSeverity()
            && info.getActualStartOffset() == getActualStartOffset()
            && info.getActualEndOffset() == getActualEndOffset()
            && Objects.equals(info.myType, myType)
            && Objects.equals(info.myGutterIconRenderer, myGutterIconRenderer)
            && Objects.equals(info.myForcedTextAttributes, myForcedTextAttributes)
            && Objects.equals(info.myForcedTextAttributesKey, myForcedTextAttributesKey)
            && info.getDescription().equals(getDescription());
    }

    @Override
    public int hashCode() {
        return myStartOffset;
    }

    @Override
    public String toString() {
        String s = "HighlightInfo(" + myStartOffset + "," + myEndOffset + ")";
        if (getActualStartOffset() != myStartOffset || getActualEndOffset() != myEndOffset) {
            s += "; actual: (" + getActualStartOffset() + "," + getActualEndOffset() + ")";
        }
        if (myHighlighter != null) {
            s += " text='" + getText() + "'";
        }
        if (getDescription() != LocalizeValue.empty()) {
            s += ", description='" + getDescription() + "'";
        }
        s += " severity=" + getSeverity();
        s += " group=" + getGroup();
        if (myQuickFixActionRanges != null) {
            s += "; quickFixes: " + myQuickFixActionRanges;
        }
        if (myGutterIconRenderer != null) {
            s += "; gutter: " + myGutterIconRenderer;
        }
        return s;
    }

    @Nonnull
    public static Builder newHighlightInfo(@Nonnull HighlightInfoType type) {
        return new HighlightInfoBuilder(type);
    }

    public void setGroup(int group) {
        myGroup = group;
    }

    static boolean isAcceptedByFilters(@Nonnull HighlightInfoImpl info, @Nullable PsiElement psiElement) {
        PsiFile file = psiElement == null ? null : psiElement.getContainingFile();
        boolean acceptedByFilters = Application.get().getExtensionPoint(HighlightInfoFilter.class)
            .allMatchSafe(filter -> filter.accept(info, file));
        if (acceptedByFilters) {
            info.myPsiElement = psiElement;
        }
        return acceptedByFilters;
    }

    @Override
    public GutterMark getGutterIconRenderer() {
        return myGutterIconRenderer;
    }

    @Nullable
    public ProblemGroup getProblemGroup() {
        return myProblemGroup;
    }

    @Nonnull
    public static HighlightInfoImpl fromAnnotation(@Nonnull Annotation annotation) {
        return fromAnnotation(annotation, false);
    }

    @Nonnull
    public static HighlightInfoImpl fromAnnotation(@Nonnull Annotation annotation, boolean batchMode) {
        TextAttributes forcedAttributes = annotation.getEnforcedTextAttributes();
        TextAttributesKey key = annotation.getTextAttributes();
        TextAttributesKey forcedAttributesKey = forcedAttributes == null && key != HighlighterColors.NO_HIGHLIGHTING ? key : null;

        HighlightInfoImpl info = new HighlightInfoImpl(
            forcedAttributes,
            forcedAttributesKey,
            convertType(annotation),
            annotation.getStartOffset(),
            annotation.getEndOffset(),
            annotation.getMessage(),
            annotation.getTooltip(),
            annotation.getSeverity(),
            annotation.isAfterEndOfLine(),
            annotation.needsUpdateOnTyping(),
            annotation.isFileLevelAnnotation(),
            0,
            annotation.getProblemGroup(),
            null,
            annotation.getGutterIconRenderer(),
            Pass.UPDATE_ALL
        );

        List<? extends Annotation.QuickFixInfo> fixes = batchMode ? annotation.getBatchFixes() : annotation.getQuickFixes();
        if (fixes != null) {
            InspectionCache cache = InspectionCacheService.getInstance().get();

            for (Annotation.QuickFixInfo quickFixInfo : fixes) {
                TextRange range = quickFixInfo.textRange;
                HighlightDisplayKey k =
                    quickFixInfo.key != null ? quickFixInfo.key : cache.find(ANNOTATOR_INSPECTION_SHORT_NAME);
                info.registerFix(quickFixInfo.quickFix, null, cache.getDisplayNameByKey(k), range, k);
            }
        }

        return info;
    }

    private static final String ANNOTATOR_INSPECTION_SHORT_NAME = "Annotator";

    @Nonnull
    private static HighlightInfoType convertType(@Nonnull Annotation annotation) {
        ProblemHighlightType type = annotation.getHighlightType();
        return switch (type) {
            case LIKE_UNKNOWN_SYMBOL -> HighlightInfoType.WRONG_REF;
            default -> {
                if (ProblemHighlightTypeInspectionRuler.REGISTRY.containsKey(type)) {
                    InspectionCache cache = InspectionCacheService.getInstance().get();
                    yield cache.getControlledHighlightType(annotation.getLanguage(), type);
                }
                yield HighlightTypeUtil.convertSeverity(annotation.getSeverity());
            }
        };
    }

    public boolean hasHint() {
        return isFlagSet(HAS_HINT_MASK);
    }

    public void setHint(boolean hasHint) {
        setFlag(HAS_HINT_MASK, hasHint);
    }

    @Override
    public int getActualStartOffset() {
        RangeHighlighter h = myHighlighter;
        return h == null || !h.isValid() ? myStartOffset : h.getStartOffset();
    }

    @Override
    public int getActualEndOffset() {
        RangeHighlighter h = myHighlighter;
        return h == null || !h.isValid() ? myEndOffset : h.getEndOffset();
    }

    @Override
    public int getStartOffset() {
        return getActualStartOffset();
    }

    @Override
    public int getEndOffset() {
        return getActualEndOffset();
    }

    public int getGroup() {
        return myGroup;
    }

    public boolean isFromInjection() {
        return isFlagSet(FROM_INJECTION_MASK);
    }

    @Nonnull
    public String getText() {
        if (isFileLevelAnnotation()) {
            return "";
        }
        RangeHighlighter highlighter = myHighlighter;
        if (highlighter == null) {
            throw new RuntimeException("info not applied yet");
        }
        if (!highlighter.isValid()) {
            return "";
        }
        return highlighter.getDocument().getText(TextRange.create(highlighter));
    }

    @Override
    public void registerFix(
        @Nullable IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nonnull LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
        if (action == null) {
            return;
        }
        if (fixRange == null) {
            fixRange = new TextRange(myStartOffset, myEndOffset);
        }
        if (myQuickFixActionRanges == null) {
            myQuickFixActionRanges = Lists.newLockFreeCopyOnWriteList();
        }
        IntentionActionDescriptor desc =
            new IntentionActionDescriptor(action, options, displayName, null, key, getProblemGroup(), getSeverity());
        myQuickFixActionRanges.add(Pair.create(desc, fixRange));
        myFixStartOffset = Math.min(myFixStartOffset, fixRange.getStartOffset());
        myFixEndOffset = Math.max(myFixEndOffset, fixRange.getEndOffset());
        if (action instanceof HintAction) {
            setHint(true);
        }
    }

    @Override
    public void unregisterQuickFix(@Nonnull Predicate<? super IntentionAction> condition) {
        if (myQuickFixActionRanges != null) {
            myQuickFixActionRanges.removeIf(pair -> condition.test(pair.first.getAction()));
        }
    }

    @Override
    public void forEachQuickFix(@Nonnull BiConsumer<IntentionAction, TextRange> consumer) {
        if (myQuickFixActionRanges != null) {
            for (Pair<IntentionActionDescriptor, TextRange> range : myQuickFixActionRanges) {
                IntentionAction action = range.getFirst().getAction();

                consumer.accept(action, range.getSecond());
            }
        }
    }
}