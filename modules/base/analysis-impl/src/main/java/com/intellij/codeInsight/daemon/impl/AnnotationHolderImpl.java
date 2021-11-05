// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.diagnostic.PluginException;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Use {@link AnnotationHolder} instead. The members of this class can suddenly change or disappear.
 */
//@ApiStatus.Internal
public class AnnotationHolderImpl extends SmartList<Annotation> implements AnnotationHolder {
  private static final Logger LOG = Logger.getInstance(AnnotationHolderImpl.class);
  private final AnnotationSession myAnnotationSession;

  private final boolean myBatchMode;
  Annotator myCurrentAnnotator;
  private ExternalAnnotator<?, ?> myExternalAnnotator;

  /**
   * @deprecated Do not instantiate the AnnotationHolderImpl directly, please use the one provided to {@link Annotator#annotate(PsiElement, AnnotationHolder)} instead
   */
  //@ApiStatus.Internal
  @Deprecated
  public AnnotationHolderImpl(@Nonnull AnnotationSession session) {
    this(session, false);
    PluginException.reportDeprecatedUsage("AnnotationHolderImpl(AnnotationSession)", "Please use the AnnotationHolder passed to Annotator.annotate() instead");
  }

  /**
   * @deprecated Do not instantiate the AnnotationHolderImpl directly, please use the one provided to {@link Annotator#annotate(PsiElement, AnnotationHolder)} instead
   */
  //@ApiStatus.Internal
  @Deprecated
  public AnnotationHolderImpl(@Nonnull AnnotationSession session, boolean batchMode) {
    myAnnotationSession = session;
    myBatchMode = batchMode;
  }

  @Override
  public boolean isBatchMode() {
    return myBatchMode;
  }

  @Override
  public Annotation createErrorAnnotation(@Nonnull PsiElement elt, String message) {
    assertMyFile(elt);
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.ERROR, elt.getTextRange(), message, wrapXml(message), callerClass, "createErrorAnnotation");
  }

  @Override
  public Annotation createErrorAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.ERROR, node.getTextRange(), message, wrapXml(message), callerClass, "createErrorAnnotation");
  }

  @Override
  public Annotation createErrorAnnotation(@Nonnull TextRange range, String message) {
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.ERROR, range, message, wrapXml(message), callerClass, "createErrorAnnotation");
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull PsiElement elt, String message) {
    assertMyFile(elt);
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WARNING, elt.getTextRange(), message, wrapXml(message), callerClass, "createWarningAnnotation");
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WARNING, node.getTextRange(), message, wrapXml(message), callerClass, "createWarningAnnotation");
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull TextRange range, String message) {
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WARNING, range, message, wrapXml(message), callerClass, "createWarningAnnotation");
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull PsiElement elt, @Nullable String message) {
    assertMyFile(elt);
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WEAK_WARNING, elt.getTextRange(), message, wrapXml(message), callerClass, "createWeakWarningAnnotation");
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull ASTNode node, @Nullable String message) {
    assertMyFile(node.getPsi());
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WEAK_WARNING, node.getTextRange(), message, wrapXml(message), callerClass, "createWeakWarningAnnotation");
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull TextRange range, String message) {
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.WEAK_WARNING, range, message, wrapXml(message), callerClass, "createWeakWarningAnnotation");
  }

  @Override
  public Annotation createInfoAnnotation(@Nonnull PsiElement elt, String message) {
    assertMyFile(elt);
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.INFORMATION, elt.getTextRange(), message, wrapXml(message), callerClass, "createInfoAnnotation");
  }

  @Override
  public Annotation createInfoAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(HighlightSeverity.INFORMATION, node.getTextRange(), message, wrapXml(message), callerClass, "createInfoAnnotation");
  }

  private void assertMyFile(PsiElement node) {
    if (node == null) return;
    PsiFile myFile = myAnnotationSession.getFile();
    PsiFile containingFile = node.getContainingFile();
    LOG.assertTrue(containingFile != null, node);
    VirtualFile containingVFile = containingFile.getVirtualFile();
    VirtualFile myVFile = myFile.getVirtualFile();
    if (!Comparing.equal(containingVFile, myVFile)) {
      LOG.error("Annotation must be registered for an element inside '" +
                myFile +
                "' which is in '" +
                myVFile +
                "'.\n" +
                "Element passed: '" +
                node +
                "' is inside the '" +
                containingFile +
                "' which is in '" +
                containingVFile +
                "'");
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
  public Annotation createAnnotation(@Nonnull HighlightSeverity severity, @Nonnull TextRange range, @Nullable String message, @Nullable String tooltip) {
    Class<?> callerClass = ReflectionUtil.findCallerClass(2);
    return doCreateAnnotation(severity, range, message, tooltip, callerClass, "createAnnotation");
  }

  /**
   * @deprecated this is an old way of creating annotations, via createXXXAnnotation(). please use newAnnotation() instead
   */
  @Nonnull
  @Deprecated
  private Annotation doCreateAnnotation(@Nonnull HighlightSeverity severity,
                                        @Nonnull TextRange range,
                                        @Nullable String message,
                                        @Nullable String tooltip,
                                        @Nullable Class<?> callerClass,
                                        @Nonnull String methodName) {
    Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);
    add(annotation);
    String callerInfo = callerClass == null ? "" : " (the call to which was found in " + callerClass + ")";
    PluginException pluginException = PluginException.createByClass(new IncorrectOperationException("'AnnotationHolder." +
                                                                                                    methodName +
                                                                                                    "()' method" +
                                                                                                    callerInfo +
                                                                                                    " is slow, non-incremental " +
                                                                                                    "and thus can cause unexpected behaviour (e.g. annoying blinking), " +
                                                                                                    "is deprecated and will be removed soon. " +
                                                                                                    "Please use `newAnnotation().create()` instead"), callerClass == null ? getClass() : callerClass);
    LOG.warn(pluginException);
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
  void queueToUpdateIncrementally() {
  }

  @Nonnull
  @Override
  public AnnotationBuilder newAnnotation(@Nonnull HighlightSeverity severity, @Nonnull @Nls String message) {
    return new B(this, severity, message, myCurrentElement, ObjectUtils.chooseNotNull(myCurrentAnnotator, myExternalAnnotator));
  }

  @Nonnull
  @Override
  public AnnotationBuilder newSilentAnnotation(@Nonnull HighlightSeverity severity) {
    return new B(this, severity, null, myCurrentElement, ObjectUtils.chooseNotNull(myCurrentAnnotator, myExternalAnnotator));
  }

  PsiElement myCurrentElement;

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
  private final List<B> myCreatedAnnotationBuilders = new ArrayList<>();

  void annotationBuilderCreated(@Nonnull B builder) {
    synchronized (myCreatedAnnotationBuilders) {
      myCreatedAnnotationBuilders.add(builder);
    }
  }

  public void assertAllAnnotationsCreated() {
    synchronized (myCreatedAnnotationBuilders) {
      try {
        for (B builder : myCreatedAnnotationBuilders) {
          builder.assertAnnotationCreated();
        }
      }
      finally {
        myCreatedAnnotationBuilders.clear();
      }
    }
  }

  void annotationCreatedFrom(@Nonnull B builder) {
    synchronized (myCreatedAnnotationBuilders) {
      myCreatedAnnotationBuilders.remove(builder);
    }
  }
}
