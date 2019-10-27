/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import consulo.logging.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author max
 */
public class AnnotationHolderImpl extends SmartList<Annotation> implements AnnotationHolder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl");
  private final AnnotationSession myAnnotationSession;

  private final boolean myBatchMode;

  public AnnotationHolderImpl(@Nonnull AnnotationSession session) {
    this(session, false);
  }

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
    return createAnnotation(HighlightSeverity.ERROR, elt.getTextRange(), message);
  }

  @Override
  public Annotation createErrorAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    return createAnnotation(HighlightSeverity.ERROR, node.getTextRange(), message);
  }

  @Override
  public Annotation createErrorAnnotation(@Nonnull TextRange range, String message) {
    return createAnnotation(HighlightSeverity.ERROR, range, message);
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull PsiElement elt, String message) {
    assertMyFile(elt);
    return createAnnotation(HighlightSeverity.WARNING, elt.getTextRange(), message);
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    return createAnnotation(HighlightSeverity.WARNING, node.getTextRange(), message);
  }

  @Override
  public Annotation createWarningAnnotation(@Nonnull TextRange range, String message) {
    return createAnnotation(HighlightSeverity.WARNING, range, message);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull PsiElement elt, @javax.annotation.Nullable String message) {
    assertMyFile(elt);
    return createAnnotation(HighlightSeverity.WEAK_WARNING, elt.getTextRange(), message);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull ASTNode node, @javax.annotation.Nullable String message) {
    assertMyFile(node.getPsi());
    return createAnnotation(HighlightSeverity.WEAK_WARNING, node.getTextRange(), message);
  }

  @Override
  public Annotation createWeakWarningAnnotation(@Nonnull TextRange range, String message) {
    return createAnnotation(HighlightSeverity.WEAK_WARNING, range, message);
  }

  @Override
  public Annotation createInfoAnnotation(@Nonnull PsiElement elt, String message) {
    assertMyFile(elt);
    return createAnnotation(HighlightSeverity.INFORMATION, elt.getTextRange(), message);
  }

  @Override
  public Annotation createInfoAnnotation(@Nonnull ASTNode node, String message) {
    assertMyFile(node.getPsi());
    return createAnnotation(HighlightSeverity.INFORMATION, node.getTextRange(), message);
  }

  private void assertMyFile(PsiElement node) {
    if (node == null) return;
    PsiFile myFile = myAnnotationSession.getFile();
    PsiFile containingFile = node.getContainingFile();
    LOG.assertTrue(containingFile != null, node);
    VirtualFile containingVFile = containingFile.getVirtualFile();
    VirtualFile myVFile = myFile.getVirtualFile();
    if (!Comparing.equal(containingVFile, myVFile)) {
      LOG.error(
              "Annotation must be registered for an element inside '" + myFile + "' which is in '" + myVFile + "'.\n" +
              "Element passed: '" + node + "' is inside the '" + containingFile + "' which is in '" + containingVFile + "'");
    }
  }

  @Override
  public Annotation createInfoAnnotation(@Nonnull TextRange range, String message) {
    return createAnnotation(HighlightSeverity.INFORMATION, range, message);
  }

  @Override
  public Annotation createAnnotation(@Nonnull HighlightSeverity severity, @Nonnull TextRange range, @javax.annotation.Nullable String message) {
    //noinspection HardCodedStringLiteral
    //TODO: FIXME
    @NonNls
    String tooltip = message == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(message));
    Annotation annotation = new Annotation(range.getStartOffset(), range.getEndOffset(), severity, message, tooltip);
    add(annotation);
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
}
