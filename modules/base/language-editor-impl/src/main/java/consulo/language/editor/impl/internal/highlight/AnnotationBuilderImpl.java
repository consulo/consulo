// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ApplicationProperties;
import consulo.application.ReadAction;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.util.PluginExceptionUtil;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixAsIntentionAdapter;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

class AnnotationBuilderImpl implements AnnotationBuilder {
    private static final BiFunction<LocalizeManager, String, String> TOOLTIP_ESCAPE =
        (localizeManager, message) -> XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));

    @Nonnull
    private final AnnotationHolderImpl myHolder;
    @Nonnull
    private final LocalizeValue myMessage;
    @Nonnull
    private final PsiElement myCurrentElement;
    @Nonnull
    private final Object myCurrentAnnotator;
    @Nonnull
    private final HighlightSeverity mySeverity;
    private TextRange range;
    private Boolean afterEndOfLine;
    private Boolean fileLevel;
    private GutterIconRenderer gutterIconRenderer;
    private ProblemGroup problemGroup;
    private TextAttributes enforcedAttributes;
    private TextAttributesKey textAttributesKey;
    private ProblemHighlightType highlightType;
    private Boolean needsUpdateOnTyping;
    private LocalizeValue myTooltip = LocalizeValue.empty();
    private List<FixB> fixes;
    private boolean created;
    private final Throwable myDebugCreationPlace;

    AnnotationBuilderImpl(
        @Nonnull AnnotationHolderImpl holder,
        @Nonnull HighlightSeverity severity,
        @Nonnull LocalizeValue message,
        @Nonnull PsiElement currentElement,
        @Nonnull Object currentAnnotator
    ) {
        myHolder = holder;
        mySeverity = severity;
        myMessage = message;
        myCurrentElement = currentElement;
        myCurrentAnnotator = currentAnnotator;
        holder.annotationBuilderCreated(this);

        myDebugCreationPlace = ApplicationProperties.isInSandbox() ? new Exception() : null;
    }

    private void assertNotSet(Object o, String description) {
        assertNotSet(o, null, description);
    }

    private void assertNotSet(Object o, Object nullValue, String description) {
        if (o != nullValue) {
            markNotAbandoned(); // it crashed, not abandoned
            throw new IllegalStateException(description + " was set already");
        }
    }

    private void markNotAbandoned() {
        created = true;
    }

    private class FixB implements FixBuilder {
        @Nonnull
        IntentionAction fix;
        TextRange range;
        HighlightDisplayKey key;
        Boolean batch;
        Boolean universal;

        FixB(@Nonnull IntentionAction fix) {
            this.fix = fix;
        }

        @Nonnull
        @Override
        public FixBuilder range(@Nonnull TextRange range) {
            assertNotSet(this.range, "range");
            this.range = range;
            return this;
        }

        @Nonnull
        @Override
        public FixBuilder key(@Nonnull HighlightDisplayKey key) {
            assertNotSet(this.key, "key");
            this.key = key;
            return this;
        }

        @Override
        public
        @Nonnull
        FixBuilder batch() {
            assertNotSet(this.universal, "universal");
            assertNotSet(this.batch, "batch");
            assertLQF();
            this.batch = true;
            return this;
        }

        private void assertLQF() {
            if (!(fix instanceof LocalQuickFix || fix instanceof LocalQuickFixAsIntentionAdapter)) {
                markNotAbandoned();
                throw new IllegalArgumentException("Fix " + fix + " must be instance of LocalQuickFix to be registered as batch");
            }
        }

        @Nonnull
        @Override
        public FixBuilder universal() {
            assertNotSet(this.universal, "universal");
            assertNotSet(this.batch, "batch");
            assertLQF();
            this.universal = true;
            return this;
        }

        @Nonnull
        @Override
        public AnnotationBuilder registerFix() {
            if (fixes == null) {
                fixes = new ArrayList<>();
            }
            fixes.add(this);
            return AnnotationBuilderImpl.this;
        }

        @Override
        public String toString() {
            return fix + (range == null ? "" : " at " + range) + (batch == null ? "" : " batch") + (universal == null ? "" : " universal");
        }
    }

    @Nonnull
    @Override
    public AnnotationBuilder withFix(@Nonnull IntentionAction fix) {
        return newFix(fix).registerFix();
    }

    @Nonnull
    @Override
    public FixBuilder newFix(@Nonnull IntentionAction fix) {
        return new FixB(fix);
    }

    @Nonnull
    @Override
    public FixBuilder newLocalQuickFix(@Nonnull LocalQuickFix fix, @Nonnull ProblemDescriptor problemDescriptor) {
        return new FixB(new LocalQuickFixAsIntentionAdapter(fix, problemDescriptor));
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public AnnotationBuilder range(@Nonnull TextRange range) {
        assertNotSet(this.range, "range");
        TextRange currentElementRange = myCurrentElement.getTextRange();
        if (!currentElementRange.contains(range)) {
            markNotAbandoned();
            String message = "Range must be inside element being annotated: " + currentElementRange + "; but got: " + range;
            throw PluginExceptionUtil.createByClass(message, null, myCurrentElement.getClass());
        }

        this.range = range;
        return this;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public AnnotationBuilder range(@Nonnull ASTNode element) {
        return range(element.getTextRange());
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public AnnotationBuilder range(@Nonnull PsiElement element) {
        return range(element.getTextRange());
    }

    @Nonnull
    @Override
    public AnnotationBuilder afterEndOfLine() {
        assertNotSet(afterEndOfLine, "afterEndOfLine");
        afterEndOfLine = true;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder fileLevel() {
        assertNotSet(fileLevel, "fileLevel");
        fileLevel = true;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder gutterIconRenderer(@Nonnull GutterIconRenderer gutterIconRenderer) {
        assertNotSet(this.gutterIconRenderer, "gutterIconRenderer");
        this.gutterIconRenderer = gutterIconRenderer;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder problemGroup(@Nonnull ProblemGroup problemGroup) {
        assertNotSet(this.problemGroup, "problemGroup");
        this.problemGroup = problemGroup;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder enforcedTextAttributes(@Nonnull TextAttributes enforcedAttributes) {
        assertNotSet(this.enforcedAttributes, "enforcedAttributes");
        this.enforcedAttributes = enforcedAttributes;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder textAttributes(@Nonnull TextAttributesKey textAttributes) {
        assertNotSet(this.textAttributesKey, "textAttributes");
        this.textAttributesKey = textAttributes;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder highlightType(@Nonnull ProblemHighlightType highlightType) {
        assertNotSet(this.highlightType, "highlightType");
        this.highlightType = highlightType;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder needsUpdateOnTyping() {
        return needsUpdateOnTyping(true);
    }

    @Nonnull
    @Override
    public AnnotationBuilder needsUpdateOnTyping(boolean value) {
        assertNotSet(this.needsUpdateOnTyping, "needsUpdateOnTyping");
        this.needsUpdateOnTyping = value;
        return this;
    }

    @Nonnull
    @Override
    public AnnotationBuilder tooltip(@Nonnull LocalizeValue tooltip) {
        assertNotSet(myTooltip, LocalizeValue.of(), "tooltip");
        myTooltip = tooltip;
        return this;
    }

    @Override
    @RequiredReadAction
    public void create() {
        if (created) {
            throw new IllegalStateException("Must not call .create() twice");
        }
        created = true;
        if (range == null) {
            range = myCurrentElement.getTextRange();
        }

        if (myTooltip == LocalizeValue.of() && myMessage != LocalizeValue.of()) {
            myTooltip = myMessage.map(TOOLTIP_ESCAPE);
        }

        //noinspection deprecation
        Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), mySeverity, myMessage, myTooltip);
        if (needsUpdateOnTyping != null) {
            annotation.setNeedsUpdateOnTyping(needsUpdateOnTyping);
        }
        if (highlightType != null) {
            annotation.setHighlightType(highlightType);
        }
        if (textAttributesKey != null) {
            annotation.setTextAttributes(textAttributesKey);
        }
        if (enforcedAttributes != null) {
            annotation.setEnforcedTextAttributes(enforcedAttributes);
        }
        if (problemGroup != null) {
            annotation.setProblemGroup(problemGroup);
        }
        if (gutterIconRenderer != null) {
            annotation.setGutterIconRenderer(gutterIconRenderer);
        }
        if (fileLevel != null) {
            annotation.setFileLevelAnnotation(fileLevel);
        }
        if (afterEndOfLine != null) {
            annotation.setAfterEndOfLine(afterEndOfLine);
        }
        if (fixes != null) {
            for (FixB fb : fixes) {
                IntentionAction fix = fb.fix;
                TextRange finalRange = fb.range == null ? this.range : fb.range;
                if (fb.batch != null && fb.batch) {
                    registerBatchFix(annotation, fix, finalRange, fb.key);
                }
                else if (fb.universal != null && fb.universal) {
                    registerBatchFix(annotation, fix, finalRange, fb.key);
                    annotation.registerFix(fix, finalRange, fb.key);
                }
                else {
                    annotation.registerFix(fix, finalRange, fb.key);
                }
            }
        }
        myHolder.add(annotation);
        myHolder.queueToUpdateIncrementally();
        myHolder.annotationCreatedFrom(this);
    }

    private static <T extends IntentionAction & LocalQuickFix> void registerBatchFix(
        @Nonnull Annotation annotation,
        @Nonnull Object fix,
        @Nonnull TextRange range,
        HighlightDisplayKey key
    ) {
        //noinspection unchecked
        annotation.registerBatchFix((T) fix, range, key);
    }

    void assertAnnotationCreated() {
        if (!created) {
            throw new IllegalStateException(
                "Abandoned AnnotationBuilder - its 'create()' method was never called: " + this +
                    (myDebugCreationPlace == null ? "" : "\nSee cause for the AnnotationBuilder creation stacktrace"),
                myDebugCreationPlace
            );
        }
    }

    @Nonnull
    private static String omitIfEmpty(Object o, String name) {
        return o == null ? "" : ", " + name + "=" + o;
    }

    @Override
    public String toString() {
        return "Builder{" +
            "message='" + myMessage + '\'' +
            ", myCurrentElement=" + myCurrentElement + " (" + myCurrentElement.getClass() + ")" +
            ", myCurrentAnnotator=" + myCurrentAnnotator +
            ", severity=" + mySeverity +
            ", range=" + (range == null ? "(implicit)" + ReadAction.compute(myCurrentElement::getTextRange) : range) +
            omitIfEmpty(afterEndOfLine, "afterEndOfLine") +
            omitIfEmpty(fileLevel, "fileLevel") +
            omitIfEmpty(gutterIconRenderer, "gutterIconRenderer") +
            omitIfEmpty(problemGroup, "problemGroup") +
            omitIfEmpty(enforcedAttributes, "enforcedAttributes") +
            omitIfEmpty(textAttributesKey, "textAttributesKey") +
            omitIfEmpty(highlightType, "highlightType") +
            omitIfEmpty(needsUpdateOnTyping, "needsUpdateOnTyping") +
            omitIfEmpty(myTooltip, "tooltip") +
            omitIfEmpty(fixes, "fixes") +
            '}';
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings("removal")
    public Annotation createAnnotation() {
        PluginExceptionUtil.reportDeprecatedUsage("AnnotationBuilder#createAnnotation", "Use `#create()` instead");
        if (range == null) {
            range = myCurrentElement.getTextRange();
        }

        if (myTooltip == LocalizeValue.of() && myMessage != LocalizeValue.of()) {
            myTooltip = myMessage.map(TOOLTIP_ESCAPE);
        }

        //noinspection deprecation
        Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), mySeverity, myMessage, myTooltip);
        if (needsUpdateOnTyping != null) {
            annotation.setNeedsUpdateOnTyping(needsUpdateOnTyping);
        }
        if (highlightType != null) {
            annotation.setHighlightType(highlightType);
        }
        if (textAttributesKey != null) {
            annotation.setTextAttributes(textAttributesKey);
        }
        if (enforcedAttributes != null) {
            annotation.setEnforcedTextAttributes(enforcedAttributes);
        }
        if (problemGroup != null) {
            annotation.setProblemGroup(problemGroup);
        }
        if (gutterIconRenderer != null) {
            annotation.setGutterIconRenderer(gutterIconRenderer);
        }
        if (fileLevel != null) {
            annotation.setFileLevelAnnotation(fileLevel);
        }
        if (afterEndOfLine != null) {
            annotation.setAfterEndOfLine(afterEndOfLine);
        }
        if (fixes != null) {
            for (FixB fb : fixes) {
                IntentionAction fix = fb.fix;
                TextRange finalRange = fb.range == null ? this.range : fb.range;
                if (fb.batch != null && fb.batch) {
                    registerBatchFix(annotation, fix, finalRange, fb.key);
                }
                else if (fb.universal != null && fb.universal) {
                    registerBatchFix(annotation, fix, finalRange, fb.key);
                    annotation.registerFix(fix, finalRange, fb.key);
                }
                else {
                    annotation.registerFix(fix, finalRange, fb.key);
                }
            }
        }
        myHolder.add(annotation);
        myHolder.annotationCreatedFrom(this);
        return annotation;
    }
}
