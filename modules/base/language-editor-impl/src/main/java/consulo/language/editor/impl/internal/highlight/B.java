// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.highlight;

import consulo.application.Application;
import consulo.application.ApplicationManager;
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
import consulo.util.lang.xml.XmlStringUtil;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

class B implements AnnotationBuilder {
  @Nonnull
  private final AnnotationHolderImpl myHolder;
  private final
  @Nls
  String message;
  @Nonnull
  private final PsiElement myCurrentElement;
  private final
  @Nonnull
  Object myCurrentAnnotator;
  @Nonnull
  private final HighlightSeverity severity;
  private TextRange range;
  private Boolean afterEndOfLine;
  private Boolean fileLevel;
  private GutterIconRenderer gutterIconRenderer;
  private ProblemGroup problemGroup;
  private TextAttributes enforcedAttributes;
  private TextAttributesKey textAttributesKey;
  private ProblemHighlightType highlightType;
  private Boolean needsUpdateOnTyping;
  private String tooltip;
  private List<FixB> fixes;
  private boolean created;
  private final Throwable myDebugCreationPlace;

  B(@Nonnull AnnotationHolderImpl holder, @Nonnull HighlightSeverity severity, @Nls String message, @Nonnull PsiElement currentElement, @Nonnull Object currentAnnotator) {
    myHolder = holder;
    this.severity = severity;
    this.message = message;
    myCurrentElement = currentElement;
    myCurrentAnnotator = currentAnnotator;
    holder.annotationBuilderCreated(this);

    Application app = ApplicationManager.getApplication();
    myDebugCreationPlace = app.isInternal() ? new Exception() : null;
  }

  private void assertNotSet(Object o, String description) {
    if (o != null) {
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

    @Override
    public
    @Nonnull
    FixBuilder range(@Nonnull TextRange range) {
      assertNotSet(this.range, "range");
      this.range = range;
      return this;
    }

    @Override
    public
    @Nonnull
    FixBuilder key(@Nonnull HighlightDisplayKey key) {
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

    @Override
    public
    @Nonnull
    FixBuilder universal() {
      assertNotSet(this.universal, "universal");
      assertNotSet(this.batch, "batch");
      assertLQF();
      this.universal = true;
      return this;
    }

    @Override
    public
    @Nonnull
    AnnotationBuilder registerFix() {
      if (fixes == null) {
        fixes = new ArrayList<>();
      }
      fixes.add(this);
      return B.this;
    }

    @Override
    public String toString() {
      return fix + (range == null ? "" : " at " + range) + (batch == null ? "" : " batch") + (universal == null ? "" : " universal");
    }
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder withFix(@Nonnull IntentionAction fix) {
    return newFix(fix).registerFix();
  }

  @Override
  public
  @Nonnull
  FixBuilder newFix(@Nonnull IntentionAction fix) {
    return new FixB(fix);
  }

  @Override
  public
  @Nonnull
  FixBuilder newLocalQuickFix(@Nonnull LocalQuickFix fix, @Nonnull ProblemDescriptor problemDescriptor) {
    return new FixB(new LocalQuickFixAsIntentionAdapter(fix, problemDescriptor));
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder range(@Nonnull TextRange range) {
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

  @Override
  public
  @Nonnull
  AnnotationBuilder range(@Nonnull ASTNode element) {
    return range(element.getTextRange());
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder range(@Nonnull PsiElement element) {
    return range(element.getTextRange());
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder afterEndOfLine() {
    assertNotSet(afterEndOfLine, "afterEndOfLine");
    afterEndOfLine = true;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder fileLevel() {
    assertNotSet(fileLevel, "fileLevel");
    fileLevel = true;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder gutterIconRenderer(@Nonnull GutterIconRenderer gutterIconRenderer) {
    assertNotSet(this.gutterIconRenderer, "gutterIconRenderer");
    this.gutterIconRenderer = gutterIconRenderer;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder problemGroup(@Nonnull ProblemGroup problemGroup) {
    assertNotSet(this.problemGroup, "problemGroup");
    this.problemGroup = problemGroup;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder enforcedTextAttributes(@Nonnull TextAttributes enforcedAttributes) {
    assertNotSet(this.enforcedAttributes, "enforcedAttributes");
    this.enforcedAttributes = enforcedAttributes;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder textAttributes(@Nonnull TextAttributesKey textAttributes) {
    assertNotSet(this.textAttributesKey, "textAttributes");
    this.textAttributesKey = textAttributes;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder highlightType(@Nonnull ProblemHighlightType highlightType) {
    assertNotSet(this.highlightType, "highlightType");
    this.highlightType = highlightType;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder needsUpdateOnTyping() {
    return needsUpdateOnTyping(true);
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder needsUpdateOnTyping(boolean value) {
    assertNotSet(this.needsUpdateOnTyping, "needsUpdateOnTyping");
    this.needsUpdateOnTyping = value;
    return this;
  }

  @Override
  public
  @Nonnull
  AnnotationBuilder tooltip(@Nonnull String tooltip) {
    assertNotSet(this.tooltip, "tooltip");
    this.tooltip = tooltip;
    return this;
  }

  @Override
  public void create() {
    if (created) {
      throw new IllegalStateException("Must not call .create() twice");
    }
    created = true;
    if (range == null) {
      range = myCurrentElement.getTextRange();
    }
    if (tooltip == null && message != null) {
      tooltip = XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
    }
    //noinspection deprecation
    Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);
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

  private static <T extends IntentionAction & LocalQuickFix> void registerBatchFix(@Nonnull Annotation annotation, @Nonnull Object fix, @Nonnull TextRange range, HighlightDisplayKey key) {
    //noinspection unchecked
    annotation.registerBatchFix((T)fix, range, key);
  }

  void assertAnnotationCreated() {
    if (!created) {
      throw new IllegalStateException(
              "Abandoned AnnotationBuilder - its 'create()' method was never called: " + this + (myDebugCreationPlace == null ? "" : "\nSee cause for the AnnotationBuilder creation stacktrace"),
              myDebugCreationPlace);
    }
  }

  private static
  @Nonnull
  String omitIfEmpty(Object o, String name) {
    return o == null ? "" : ", " + name + "=" + o;
  }

  @Override
  public String toString() {
    return "Builder{" +
           "message='" +
           message +
           '\'' +
           ", myCurrentElement=" +
           myCurrentElement +
           " (" +
           myCurrentElement.getClass() +
           ")" +
           ", myCurrentAnnotator=" +
           myCurrentAnnotator +
           ", severity=" +
           severity +
           ", range=" +
           (range == null ? "(implicit)" + myCurrentElement.getTextRange() : range) +
           omitIfEmpty(afterEndOfLine, "afterEndOfLine") +
           omitIfEmpty(fileLevel, "fileLevel") +
           omitIfEmpty(gutterIconRenderer, "gutterIconRenderer") +
           omitIfEmpty(problemGroup, "problemGroup") +
           omitIfEmpty(enforcedAttributes, "enforcedAttributes") +
           omitIfEmpty(textAttributesKey, "textAttributesKey") +
           omitIfEmpty(highlightType, "highlightType") +
           omitIfEmpty(needsUpdateOnTyping, "needsUpdateOnTyping") +
           omitIfEmpty(tooltip, "tooltip") +
           omitIfEmpty(fixes, "fixes") +
           '}';
  }

  @Override
  @SuppressWarnings("removal")
  public Annotation createAnnotation() {
    PluginExceptionUtil.reportDeprecatedUsage("AnnotationBuilder#createAnnotation", "Use `#create()` instead");
    if (range == null) {
      range = myCurrentElement.getTextRange();
    }
    if (tooltip == null && message != null) {
      tooltip = XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
    }
    //noinspection deprecation
    Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);
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
