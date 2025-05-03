// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.rawHighlight;

import consulo.application.ApplicationManager;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
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
import consulo.language.editor.annotation.SuppressableProblemGroup;
import consulo.language.editor.impl.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.impl.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.impl.internal.inspection.AnnotatorBasedInspection;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.intention.HintAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.*;
import consulo.language.editor.util.HighlightTypeUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.BitUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class HighlightInfoImpl implements HighlightInfo {
    private static final Logger LOG = Logger.getInstance(HighlightInfoImpl.class);

    // optimization: if tooltip contains this marker object, then it replaced with description field in getTooltip()
    private static final String DESCRIPTION_PLACEHOLDER = "\u0000";

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

    public final TextAttributes forcedTextAttributes;
    public final TextAttributesKey forcedTextAttributesKey;
    @Nonnull
    public final HighlightInfoType type;
    public final int startOffset;
    public final int endOffset;

    public List<Pair<IntentionActionDescriptor, TextRange>> quickFixActionRanges;
    public List<Pair<IntentionActionDescriptor, RangeMarker>> quickFixActionMarkers;

    private final String description;
    private final String toolTip;
    @Nonnull
    private final HighlightSeverity severity;
    private final GutterMark gutterIconRenderer;
    private final ProblemGroup myProblemGroup;
    private final String inspectionToolId;

    private int group;
    private int fixStartOffset;
    private int fixEndOffset;
    private volatile byte myFlags;  // see `@FlagConstant`

    final int navigationShift;
    public JComponent fileLevelComponent;
    /**
     * null means it the same as highlighter
     */
    @Nullable
    public RangeMarker fixMarker;
    protected volatile RangeHighlighterEx highlighter; // modified in EDT only
    PsiElement psiElement;

    /**
     * Returns the HighlightInfo instance from which the given range highlighter was created, or null if there isn't any.
     */
    @Nullable
    public static HighlightInfoImpl fromRangeHighlighter(@Nonnull RangeHighlighter highlighter) {
        Object errorStripeTooltip = highlighter.getErrorStripeTooltip();
        return errorStripeTooltip instanceof HighlightInfoImpl ? (HighlightInfoImpl)errorStripeTooltip : null;
    }

    @Nonnull
    public ProperTextRange getFixTextRange() {
        return new ProperTextRange(fixStartOffset, fixEndOffset);
    }

    public void setFromInjection(@SuppressWarnings("SameParameterValue") boolean fromInjection) {
        setFlag(FROM_INJECTION_MASK, fromInjection);
    }

    public PsiElement getPsiElement() {
        return psiElement;
    }

    public int getNavigationShift() {
        return navigationShift;
    }

    @Nullable
    public String getToolTip() {
        String toolTip = this.toolTip;
        String description = this.description;
        if (toolTip == null || description == null || !toolTip.contains(DESCRIPTION_PLACEHOLDER)) {
            return toolTip;
        }
        String decoded = StringUtil.replace(toolTip, DESCRIPTION_PLACEHOLDER, XmlStringUtil.escapeString(description));
        return XmlStringUtil.wrapInHtml(decoded);
    }

    /**
     * Encodes \p tooltip so that substrings equal to a \p description
     * are replaced with the special placeholder to reduce size of the
     * tooltip. If encoding takes place, <html></html> tags are
     * stripped of the tooltip.
     *
     * @param tooltip     - html text
     * @param description - plain text (not escaped)
     * @return encoded tooltip (stripped html text with one or more placeholder characters)
     * or tooltip without changes.
     */
    private static String encodeTooltip(String tooltip, String description) {
        if (tooltip == null || description == null || description.isEmpty()) {
            return tooltip;
        }

        String encoded = StringUtil.replace(tooltip, XmlStringUtil.escapeString(description), DESCRIPTION_PLACEHOLDER);
        //noinspection StringEquality
        if (encoded == tooltip) {
            return tooltip;
        }
        if (encoded.equals(DESCRIPTION_PLACEHOLDER)) {
            encoded = DESCRIPTION_PLACEHOLDER;
        }
        return XmlStringUtil.stripHtml(encoded);
    }

    public String getDescription() {
        return description;
    }

    @Override
    @Nonnull
    public HighlightInfoType getType() {
        return type;
    }

    @Nullable
    public String getInspectionToolId() {
        return inspectionToolId;
    }

    private boolean isFlagSet(@FlagConstant byte mask) {
        return BitUtil.isSet(myFlags, mask);
    }

    private void setFlag(@FlagConstant byte mask, boolean value) {
        myFlags = BitUtil.set(myFlags, mask, value);
    }

    public boolean isFileLevelAnnotation() {
        return isFlagSet(FILE_LEVEL_ANNOTATION_MASK);
    }

    public boolean isBijective() {
        return isFlagSet(BIJECTIVE_MASK);
    }

    public void setBijective(boolean bijective) {
        setFlag(BIJECTIVE_MASK, bijective);
    }

    @Override
    @Nonnull
    public HighlightSeverity getSeverity() {
        return severity;
    }

    public RangeHighlighterEx getHighlighter() {
        return highlighter;
    }

    /**
     * Modified only in EDT.
     */
    public void setHighlighter(@Nullable RangeHighlighterEx highlighter) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        this.highlighter = highlighter;
    }

    public boolean isAfterEndOfLine() {
        return isFlagSet(AFTER_END_OF_LINE_MASK);
    }

    @Nullable
    public TextAttributes getTextAttributes(@Nullable PsiElement element, @Nullable EditorColorsScheme editorColorsScheme) {
        if (forcedTextAttributes != null) {
            return forcedTextAttributes;
        }

        EditorColorsScheme colorsScheme = getColorsScheme(editorColorsScheme);

        if (forcedTextAttributesKey != null) {
            return colorsScheme.getAttributes(forcedTextAttributesKey);
        }

        return SeverityRegistrarUtil.getAttributesByType(element, type, colorsScheme);
    }

    /**
     * @param element
     * @param colorsScheme when null, a global scheme will be used
     * @return
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public ColorValue getErrorStripeMarkColor(@Nonnull PsiElement element, @Nullable EditorColorsScheme colorsScheme) {
        if (forcedTextAttributes != null) {
            return forcedTextAttributes.getErrorStripeColor();
        }

        EditorColorsScheme scheme = getColorsScheme(colorsScheme);
        if (forcedTextAttributesKey != null) {
            TextAttributes forcedTextAttributes = scheme.getAttributes(forcedTextAttributesKey);
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

        TextAttributes attributes = SeverityRegistrarUtil.getAttributesByType(element, type, scheme);
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
        @Nullable String escapedDescription,
        @Nullable String escapedToolTip,
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
        this.forcedTextAttributes = forcedTextAttributes;
        this.forcedTextAttributesKey = forcedTextAttributesKey;
        this.type = type;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        fixStartOffset = startOffset;
        fixEndOffset = endOffset;
        description = escapedDescription;
        // optimization: do not retain extra memory if can recompute
        toolTip = encodeTooltip(escapedToolTip, escapedDescription);
        this.severity = severity;
        setFlag(AFTER_END_OF_LINE_MASK, afterEndOfLine);
        setFlag(NEEDS_UPDATE_ON_TYPING_MASK, calcNeedUpdateOnTyping(needsUpdateOnTyping, type));
        setFlag(FILE_LEVEL_ANNOTATION_MASK, isFileLevelAnnotation);
        this.navigationShift = navigationShift;
        myProblemGroup = problemGroup;
        this.gutterIconRenderer = gutterIconRenderer;
        this.inspectionToolId = inspectionToolId;
        this.group = group;
    }

    private static boolean calcNeedUpdateOnTyping(@Nullable Boolean needsUpdateOnTyping, HighlightInfoType type) {
        if (needsUpdateOnTyping != null) {
            return needsUpdateOnTyping.booleanValue();
        }
        if (type instanceof HighlightInfoType.UpdateOnTypingSuppressible) {
            return ((HighlightInfoType.UpdateOnTypingSuppressible)type).needsUpdateOnTyping();
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HighlightInfoImpl)) {
            return false;
        }
        HighlightInfoImpl info = (HighlightInfoImpl)obj;

        return info.getSeverity() == getSeverity()
            && info.startOffset == startOffset
            && info.endOffset == endOffset
            && Comparing.equal(info.type, type)
            && Comparing.equal(info.gutterIconRenderer, gutterIconRenderer)
            && Comparing.equal(info.forcedTextAttributes, forcedTextAttributes)
            && Comparing.equal(info.forcedTextAttributesKey, forcedTextAttributesKey)
            && Comparing.strEqual(info.getDescription(), getDescription());
    }

    public boolean equalsByActualOffset(@Nonnull HighlightInfoImpl info) {
        if (info == this) {
            return true;
        }

        return info.getSeverity() == getSeverity()
            && info.getActualStartOffset() == getActualStartOffset()
            && info.getActualEndOffset() == getActualEndOffset()
            && Comparing.equal(info.type, type)
            && Comparing.equal(info.gutterIconRenderer, gutterIconRenderer)
            && Comparing.equal(info.forcedTextAttributes, forcedTextAttributes)
            && Comparing.equal(info.forcedTextAttributesKey, forcedTextAttributesKey)
            && Comparing.strEqual(info.getDescription(), getDescription());
    }

    @Override
    public int hashCode() {
        return startOffset;
    }

    @Override
    public String toString() {
        String s = "HighlightInfo(" + startOffset + "," + endOffset + ")";
        if (getActualStartOffset() != startOffset || getActualEndOffset() != endOffset) {
            s += "; actual: (" + getActualStartOffset() + "," + getActualEndOffset() + ")";
        }
        if (highlighter != null) {
            s += " text='" + getText() + "'";
        }
        if (getDescription() != null) {
            s += ", description='" + getDescription() + "'";
        }
        s += " severity=" + getSeverity();
        s += " group=" + getGroup();
        if (quickFixActionRanges != null) {
            s += "; quickFixes: " + quickFixActionRanges;
        }
        if (gutterIconRenderer != null) {
            s += "; gutter: " + gutterIconRenderer;
        }
        return s;
    }

    @Nonnull
    public static Builder newHighlightInfo(@Nonnull HighlightInfoType type) {
        return new HighlightInfoBuilder(type);
    }

    public void setGroup(int group) {
        this.group = group;
    }

    static boolean isAcceptedByFilters(@Nonnull HighlightInfoImpl info, @Nullable PsiElement psiElement) {
        PsiFile file = psiElement == null ? null : psiElement.getContainingFile();
        for (HighlightInfoFilter filter : HighlightInfoFilter.EXTENSION_POINT_NAME.getExtensionList()) {
            if (!filter.accept(info, file)) {
                return false;
            }
        }
        info.psiElement = psiElement;
        return true;
    }

    public GutterMark getGutterIconRenderer() {
        return gutterIconRenderer;
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
            annotation.getMessage() == LocalizeValue.of() ? null : annotation.getMessage().get(),
            annotation.getTooltip() == LocalizeValue.of() ? null : annotation.getTooltip().get(),
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
            for (Annotation.QuickFixInfo quickFixInfo : fixes) {
                TextRange range = quickFixInfo.textRange;
                HighlightDisplayKey k =
                    quickFixInfo.key != null ? quickFixInfo.key : HighlightDisplayKey.find(ANNOTATOR_INSPECTION_SHORT_NAME);
                info.registerFix(quickFixInfo.quickFix, null, HighlightDisplayKey.getDisplayNameByKey(k), range, k);
            }
        }

        return info;
    }

    private static final String ANNOTATOR_INSPECTION_SHORT_NAME = "Annotator";

    @Nonnull
    private static HighlightInfoType convertType(@Nonnull Annotation annotation) {
        ProblemHighlightType type = annotation.getHighlightType();
        if (type == ProblemHighlightType.LIKE_UNUSED_SYMBOL) {
            return HighlightInfoType.UNUSED_SYMBOL;
        }
        if (type == ProblemHighlightType.LIKE_UNKNOWN_SYMBOL) {
            return HighlightInfoType.WRONG_REF;
        }
        if (type == ProblemHighlightType.LIKE_DEPRECATED) {
            return HighlightInfoType.DEPRECATED;
        }
        if (type == ProblemHighlightType.LIKE_MARKED_FOR_REMOVAL) {
            return HighlightInfoType.MARKED_FOR_REMOVAL;
        }
        return HighlightTypeUtil.convertSeverity(annotation.getSeverity());
    }

    public boolean hasHint() {
        return isFlagSet(HAS_HINT_MASK);
    }

    public void setHint(boolean hasHint) {
        setFlag(HAS_HINT_MASK, hasHint);
    }

    @Override
    public int getActualStartOffset() {
        RangeHighlighter h = highlighter;
        return h == null || !h.isValid() ? startOffset : h.getStartOffset();
    }

    @Override
    public int getActualEndOffset() {
        RangeHighlighter h = highlighter;
        return h == null || !h.isValid() ? endOffset : h.getEndOffset();
    }

    public static class IntentionActionDescriptor {
        private final IntentionAction myAction;
        private volatile List<IntentionAction> myOptions;
        private volatile HighlightDisplayKey myKey;
        private final ProblemGroup myProblemGroup;
        private final HighlightSeverity mySeverity;
        private final String myDisplayName;
        private final Image myIcon;
        private Boolean myCanCleanup;

        public IntentionActionDescriptor(@Nonnull IntentionAction action, List<IntentionAction> options, String displayName) {
            this(action, options, displayName, null);
        }

        public IntentionActionDescriptor(@Nonnull IntentionAction action, Image icon) {
            this(action, null, null, icon);
        }

        public IntentionActionDescriptor(
            @Nonnull IntentionAction action,
            @Nullable List<IntentionAction> options,
            @Nullable String displayName,
            @Nullable Image icon
        ) {
            this(action, options, displayName, icon, null, null, null);
        }

        public IntentionActionDescriptor(
            @Nonnull IntentionAction action,
            @Nullable List<IntentionAction> options,
            @Nullable String displayName,
            @Nullable Image icon,
            @Nullable HighlightDisplayKey key,
            @Nullable ProblemGroup problemGroup,
            @Nullable HighlightSeverity severity
        ) {
            myAction = action;
            myOptions = options;
            myDisplayName = displayName;
            myIcon = icon;
            myKey = key;
            myProblemGroup = problemGroup;
            mySeverity = severity;
        }

        @Nonnull
        public IntentionAction getAction() {
            return myAction;
        }

        public boolean isError() {
            return mySeverity == null || mySeverity.compareTo(HighlightSeverity.ERROR) >= 0;
        }

        public boolean isInformation() {
            return HighlightSeverity.INFORMATION.equals(mySeverity);
        }

        public boolean canCleanup(@Nonnull PsiElement element) {
            if (myCanCleanup == null) {
                InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getCurrentProfile();
                HighlightDisplayKey key = myKey;
                if (key == null) {
                    myCanCleanup = false;
                }
                else {
                    InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
                    myCanCleanup = toolWrapper != null && toolWrapper.isCleanupTool();
                }
            }
            return myCanCleanup;
        }

        @Nullable
        public List<IntentionAction> getOptions(@Nonnull PsiElement element, @Nullable Editor editor) {
            if (editor != null && Boolean.FALSE.equals(editor.getUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY))) {
                return null;
            }
            List<IntentionAction> options = myOptions;
            HighlightDisplayKey key = myKey;
            if (myProblemGroup != null) {
                String problemName = myProblemGroup.getProblemName();
                HighlightDisplayKey problemGroupKey = problemName != null ? HighlightDisplayKey.findById(problemName) : null;
                if (problemGroupKey != null) {
                    key = problemGroupKey;
                }
            }
            if (options != null || key == null) {
                return options;
            }
            IntentionManager intentionManager = IntentionManager.getInstance();
            List<IntentionAction> newOptions = intentionManager.getStandardIntentionOptions(key, element);
            InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getCurrentProfile();
            InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
            if (!(toolWrapper instanceof LocalInspectionToolWrapper)) {
                HighlightDisplayKey idKey = HighlightDisplayKey.findById(key.toString());
                if (idKey != null) {
                    toolWrapper = profile.getInspectionTool(idKey.toString(), element);
                }
            }
            if (toolWrapper != null) {

                myCanCleanup = toolWrapper.isCleanupTool();

                IntentionAction fixAllIntention = intentionManager.createFixAllIntention(toolWrapper, myAction);
                InspectionTool wrappedTool =
                    toolWrapper instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper)toolWrapper).getTool() : ((GlobalInspectionToolWrapper)toolWrapper)
                        .getTool();
                if (wrappedTool instanceof AnnotatorBasedInspection) {
                    List<IntentionAction> actions = Collections.emptyList();
                    if (myProblemGroup instanceof SuppressableProblemGroup) {
                        actions = Arrays.asList(((SuppressableProblemGroup)myProblemGroup).getSuppressActions(element));
                    }
                    if (fixAllIntention != null) {
                        if (actions.isEmpty()) {
                            return Collections.singletonList(fixAllIntention);
                        }
                        else {
                            actions = new ArrayList<>(actions);
                            actions.add(fixAllIntention);
                        }
                    }
                    return actions;
                }
                ContainerUtil.addIfNotNull(newOptions, fixAllIntention);
                if (wrappedTool instanceof CustomSuppressableInspectionTool) {
                    IntentionAction[] suppressActions = ((CustomSuppressableInspectionTool)wrappedTool).getSuppressActions(element);
                    if (suppressActions != null) {
                        ContainerUtil.addAll(newOptions, suppressActions);
                    }
                }
                else {
                    SuppressQuickFix[] suppressFixes = wrappedTool.getBatchSuppressActions(element);
                    if (suppressFixes.length > 0) {
                        newOptions.addAll(ContainerUtil.map(
                            suppressFixes,
                            SuppressIntentionActionFromFix::convertBatchToSuppressIntentionAction
                        ));
                    }
                }

            }
            if (myProblemGroup instanceof SuppressableProblemGroup) {
                IntentionAction[] suppressActions = ((SuppressableProblemGroup)myProblemGroup).getSuppressActions(element);
                ContainerUtil.addAll(newOptions, suppressActions);
            }

            //noinspection SynchronizeOnThis
            synchronized (this) {
                options = myOptions;
                if (options == null) {
                    myOptions = options = newOptions;
                }
                myKey = null;
            }

            return options;
        }

        @Nullable
        public String getDisplayName() {
            return myDisplayName;
        }

        @Override
        public String toString() {
            String text = getAction().getText();
            return "descriptor: " + (text.isEmpty() ? getAction().getClass() : text);
        }

        @Nullable
        public Image getIcon() {
            return myIcon;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IntentionActionDescriptor && myAction.equals(((IntentionActionDescriptor)obj).myAction);
        }
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
        return group;
    }

    public boolean isFromInjection() {
        return isFlagSet(FROM_INJECTION_MASK);
    }

    @Nonnull
    public String getText() {
        if (isFileLevelAnnotation()) {
            return "";
        }
        RangeHighlighter highlighter = this.highlighter;
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
        @Nullable String displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
        if (action == null) {
            return;
        }
        if (fixRange == null) {
            fixRange = new TextRange(startOffset, endOffset);
        }
        if (quickFixActionRanges == null) {
            quickFixActionRanges = Lists.newLockFreeCopyOnWriteList();
        }
        IntentionActionDescriptor desc =
            new IntentionActionDescriptor(action, options, displayName, null, key, getProblemGroup(), getSeverity());
        quickFixActionRanges.add(Pair.create(desc, fixRange));
        fixStartOffset = Math.min(fixStartOffset, fixRange.getStartOffset());
        fixEndOffset = Math.max(fixEndOffset, fixRange.getEndOffset());
        if (action instanceof HintAction) {
            setHint(true);
        }
    }

    @Override
    public void unregisterQuickFix(@Nonnull Predicate<? super IntentionAction> condition) {
        if (quickFixActionRanges != null) {
            quickFixActionRanges.removeIf(pair -> condition.test(pair.first.getAction()));
        }
    }

    @Override
    public void forEachQuickFix(@Nonnull BiConsumer<IntentionAction, TextRange> consumer) {
        if (quickFixActionRanges != null) {
            for (Pair<IntentionActionDescriptor, TextRange> range : quickFixActionRanges) {
                IntentionAction action = range.getFirst().getAction();

                consumer.accept(action, range.getSecond());
            }
        }
    }
}