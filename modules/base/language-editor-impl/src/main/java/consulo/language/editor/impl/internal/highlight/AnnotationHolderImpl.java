// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.PluginException;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.SmartList;
import consulo.util.lang.Comparing;
import consulo.util.lang.DeprecatedMethodException;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Use {@link AnnotationHolder} instead. The members of this class can suddenly change or disappear.
 */
public class AnnotationHolderImpl extends SmartList<Annotation> implements AnnotationHolder {
    private static final Logger LOG = Logger.getInstance(AnnotationHolderImpl.class);

    private final AnnotationSession myAnnotationSession;

    private final boolean myBatchMode;
    private ExternalAnnotator<?, ?> myExternalAnnotator;

    public Language myCurrentLanguage;
    public Annotator myCurrentAnnotator;

    public AnnotationHolderImpl(@Nullable Language language, @Nonnull AnnotationSession session, boolean batchMode) {
        myCurrentLanguage = language;
        myAnnotationSession = session;
        myBatchMode = batchMode;
    }

    @Override
    public boolean isBatchMode() {
        return myBatchMode;
    }

    @Override
    @RequiredReadAction
    public Annotation createErrorAnnotation(@Nonnull PsiElement elt, String message) {
        assertMyFile(elt);
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.ERROR,
            elt.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createErrorAnnotation"
        );
    }

    @Override
    public Annotation createErrorAnnotation(@Nonnull ASTNode node, String message) {
        assertMyFile(node.getPsi());
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.ERROR,
            node.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createErrorAnnotation"
        );
    }

    @Override
    public Annotation createErrorAnnotation(@Nonnull TextRange range, String message) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(HighlightSeverity.ERROR, range, message, wrapXml(message), callerClass, "createErrorAnnotation");
    }

    @Override
    @RequiredReadAction
    public Annotation createWarningAnnotation(@Nonnull PsiElement elt, String message) {
        assertMyFile(elt);
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.WARNING,
            elt.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createWarningAnnotation"
        );
    }

    @Override
    public Annotation createWarningAnnotation(@Nonnull ASTNode node, String message) {
        assertMyFile(node.getPsi());
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.WARNING,
            node.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createWarningAnnotation"
        );
    }

    @Override
    public Annotation createWarningAnnotation(@Nonnull TextRange range, String message) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(HighlightSeverity.WARNING, range, message, wrapXml(message), callerClass, "createWarningAnnotation");
    }

    @Override
    @RequiredReadAction
    public Annotation createWeakWarningAnnotation(@Nonnull PsiElement elt, @Nullable String message) {
        assertMyFile(elt);
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.WEAK_WARNING,
            elt.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createWeakWarningAnnotation"
        );
    }

    @Override
    public Annotation createWeakWarningAnnotation(@Nonnull ASTNode node, @Nullable String message) {
        assertMyFile(node.getPsi());
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.WEAK_WARNING,
            node.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createWeakWarningAnnotation"
        );
    }

    @Override
    public Annotation createWeakWarningAnnotation(@Nonnull TextRange range, String message) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.WEAK_WARNING,
            range,
            message,
            wrapXml(message),
            callerClass,
            "createWeakWarningAnnotation"
        );
    }

    @Override
    @RequiredReadAction
    public Annotation createInfoAnnotation(@Nonnull PsiElement elt, String message) {
        assertMyFile(elt);
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.INFORMATION,
            elt.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createInfoAnnotation"
        );
    }

    @Override
    public Annotation createInfoAnnotation(@Nonnull ASTNode node, String message) {
        assertMyFile(node.getPsi());
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(
            HighlightSeverity.INFORMATION,
            node.getTextRange(),
            message,
            wrapXml(message),
            callerClass,
            "createInfoAnnotation"
        );
    }

    private void assertMyFile(PsiElement node) {
        if (node == null) {
            return;
        }
        PsiFile myFile = myAnnotationSession.getFile();
        PsiFile containingFile = node.getContainingFile();
        LOG.assertTrue(containingFile != null, node);
        VirtualFile containingVFile = containingFile.getVirtualFile();
        VirtualFile myVFile = myFile.getVirtualFile();
        if (!Objects.equals(containingVFile, myVFile)) {
            LOG.error(
                "Annotation must be registered for an element inside '" + myFile + "' " +
                    "which is in '" + myVFile + "'.\n" +
                    "Element passed: '" + node + "' " +
                    "is inside the '" + containingFile + "' " +
                    "which is in '" + containingVFile + "'"
            );
        }
    }

    @Override
    public Annotation createInfoAnnotation(@Nonnull TextRange range, String message) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(HighlightSeverity.INFORMATION, range, message, wrapXml(message), callerClass, "createInfoAnnotation");
    }

    @Override
    public Annotation createAnnotation(@Nonnull HighlightSeverity severity, @Nonnull TextRange range, @Nullable String message) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(severity, range, message, wrapXml(message), callerClass, "createAnnotation");
    }

    @Nullable
    @Contract(pure = true)
    private static String wrapXml(@Nullable String message) {
        return message == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
    }

    @Override
    public Annotation createAnnotation(
        @Nonnull HighlightSeverity severity,
        @Nonnull TextRange range,
        @Nullable String message,
        @Nullable String tooltip
    ) {
        Class<?> callerClass = ReflectionUtil.findCallerClass(2);
        return doCreateAnnotation(severity, range, message, tooltip, callerClass, "createAnnotation");
    }

    private static final Set<String> ourWarnList = new ConcurrentSkipListSet<>();
    private static boolean LOG_AS_ERROR = Boolean.getBoolean("consulo.annotation.log.error");

    /**
     * @deprecated this is an old way of creating annotations, via createXXXAnnotation(). please use newAnnotation() instead
     */
    @Nonnull
    @Deprecated
    private Annotation doCreateAnnotation(
        @Nonnull HighlightSeverity severity,
        @Nonnull TextRange range,
        @Nullable String message,
        @Nullable String tooltip,
        @Nullable Class<?> callerClass,
        @Nonnull String methodName
    ) {
        return doCreateAnnotation(
            severity,
            range,
            message == null ? LocalizeValue.empty() : LocalizeValue.of(message),
            tooltip == null ? LocalizeValue.empty() : LocalizeValue.of(tooltip),
            callerClass,
            methodName,
            myCurrentLanguage
        );
    }

    /**
     * @deprecated this is an old way of creating annotations, via createXXXAnnotation(). please use newAnnotation() instead
     */
    @Nonnull
    @Deprecated
    private Annotation doCreateAnnotation(
        @Nonnull HighlightSeverity severity,
        @Nonnull TextRange range,
        @Nonnull LocalizeValue message,
        @Nonnull LocalizeValue tooltip,
        @Nullable Class<?> callerClass,
        @Nonnull String methodName,
        @Nonnull Language language
    ) {
        Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip, language);
        add(annotation);
        String callerInfo = callerClass == null ? "" : " (the call to which was found in " + callerClass + ")";
        PluginException pluginException = PluginExceptionUtil.createByClass(
            new DeprecatedMethodException(
                "'AnnotationHolder." + methodName + "()' method" + callerInfo + " is slow, non-incremental " +
                    "and thus can cause unexpected behaviour (e.g. annoying blinking), " +
                    "is deprecated and will be removed soon. " +
                    "Please use `newAnnotation().create()` instead"
            ),
            callerClass == null ? getClass() : callerClass
        );
        if (callerClass == null || ourWarnList.add(callerClass.getName())) {
            if (LOG_AS_ERROR) {
                LOG.error(pluginException);
            }
            else {
                LOG.warn(pluginException);
            }
        }
        return annotation;
    }

    public boolean hasAnnotations() {
        return !isEmpty();
    }

    @Nonnull
    @Override
    public AnnotationSession getCurrentAnnotationSession() {
        return myAnnotationSession;
    }

    // internal optimization method to reduce latency between creating Annotation and showing it on screen
    // (Do not) call this method to
    // 1. state that all Annotations in this holder are final (no further Annotation.setXXX() or .registerFix() are following) and
    // 2. queue them all for converting to RangeHighlighters in EDT
    //@ApiStatus.Internal
    public void queueToUpdateIncrementally() {
    }

    @Nonnull
    @Override
    public AnnotationBuilder newOfSeverity(@Nonnull HighlightSeverity severity, @Nonnull LocalizeValue message) {
        return new AnnotationBuilderImpl(
            this,
            severity,
            message,
            myCurrentElement,
            ObjectUtil.chooseNotNull(myCurrentAnnotator, myExternalAnnotator)
        );
    }

    @Nonnull
    @Override
    public AnnotationBuilder newSilentAnnotation(@Nonnull HighlightSeverity severity) {
        return new AnnotationBuilderImpl(
            this,
            severity,
            LocalizeValue.empty(),
            myCurrentElement,
            ObjectUtil.chooseNotNull(myCurrentAnnotator, myExternalAnnotator)
        );
    }

    public PsiElement myCurrentElement;

    public void runAnnotatorWithContext(@Nonnull PsiElement element, @Nonnull Annotator annotator) {
        myCurrentAnnotator = annotator;
        myCurrentElement = element;
        annotator.annotate(element, this);
        myCurrentElement = null;
        myCurrentAnnotator = null;
    }

    public <R> void applyExternalAnnotatorWithContext(@Nonnull PsiFile file, @Nonnull ExternalAnnotator<?, R> annotator, R result) {
        myExternalAnnotator = annotator;
        myCurrentElement = file;
        annotator.apply(file, result, this);
        myCurrentElement = null;
        myExternalAnnotator = null;
    }

    // to assert each AnnotationBuilder did call .create() in the end
    private final List<AnnotationBuilderImpl> myCreatedAnnotationBuilders = new ArrayList<>();

    void annotationBuilderCreated(@Nonnull AnnotationBuilderImpl builder) {
        synchronized (myCreatedAnnotationBuilders) {
            myCreatedAnnotationBuilders.add(builder);
        }
    }

    public void assertAllAnnotationsCreated() {
        synchronized (myCreatedAnnotationBuilders) {
            try {
                for (AnnotationBuilderImpl builder : myCreatedAnnotationBuilders) {
                    builder.assertAnnotationCreated();
                }
            }
            finally {
                myCreatedAnnotationBuilders.clear();
            }
        }
    }

    void annotationCreatedFrom(@Nonnull AnnotationBuilderImpl builder) {
        synchronized (myCreatedAnnotationBuilders) {
            myCreatedAnnotationBuilders.remove(builder);
        }
    }
}
