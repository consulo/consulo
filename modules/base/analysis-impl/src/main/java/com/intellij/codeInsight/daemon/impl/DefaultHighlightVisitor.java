/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.impl.analysis.ErrorQuickFixProvider;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.localize.LocalizeValue;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author yole
 */
class DefaultHighlightVisitor implements HighlightVisitor, DumbAware {
  private AnnotationHolderImpl myAnnotationHolder;

  private final HighlightErrorFilter[] myErrorFilters;
  private final Project myProject;
  private final boolean myHighlightErrorElements;
  private final boolean myRunAnnotators;
  private final DumbService myDumbService;
  private HighlightInfoHolder myHolder;
  private final boolean myBatchMode;
  private final CachedAnnotators myCachedAnnotators;

  @Inject
  DefaultHighlightVisitor(@Nonnull Project project, @Nonnull CachedAnnotators cachedAnnotators) {
    this(project, true, true, false, cachedAnnotators);
  }

  DefaultHighlightVisitor(@Nonnull Project project,
                          boolean highlightErrorElements,
                          boolean runAnnotators,
                          boolean batchMode,
                          @Nonnull CachedAnnotators cachedAnnotators) {
    myProject = project;
    myHighlightErrorElements = highlightErrorElements;
    myRunAnnotators = runAnnotators;
    myCachedAnnotators = cachedAnnotators;
    myErrorFilters = HighlightErrorFilter.EP_NAME.getExtensions(project);
    myDumbService = DumbService.getInstance(project);
    myBatchMode = batchMode;
  }

  @Override
  public boolean suitableForFile(@Nonnull final PsiFile file) {
    return true;
  }

  @Override
  public boolean analyze(@Nonnull final PsiFile file,
                         final boolean updateWholeFile,
                         @Nonnull final HighlightInfoHolder holder,
                         @Nonnull final Runnable action) {
    myHolder = holder;
    myAnnotationHolder = new AnnotationHolderImpl(holder.getAnnotationSession(), myBatchMode);
    try {
      action.run();
    }
    finally {
      myAnnotationHolder.clear();
      myAnnotationHolder = null;
      myHolder = null;
    }
    return true;
  }

  @Override
  public void visit(@Nonnull PsiElement element) {
    if (element instanceof PsiErrorElement) {
      if (myHighlightErrorElements) visitErrorElement((PsiErrorElement)element);
    }
    else {
      if (myRunAnnotators) runAnnotators(element);
    }

    if (myAnnotationHolder.hasAnnotations()) {
      for (Annotation annotation : myAnnotationHolder) {
        myHolder.add(HighlightInfo.fromAnnotation(annotation, myBatchMode));
      }
      myAnnotationHolder.clear();
    }
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  @Nonnull
  public HighlightVisitor clone() {
    return new DefaultHighlightVisitor(myProject, myHighlightErrorElements, myRunAnnotators, myBatchMode, myCachedAnnotators);
  }

  @Override
  public int order() {
    return 2;
  }

  private void runAnnotators(PsiElement element) {
    List<Annotator> annotators = myCachedAnnotators.get(element.getLanguage().getID());
    if (annotators.isEmpty()) return;
    final boolean dumb = myDumbService.isDumb();

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < annotators.size(); i++) {
      Annotator annotator = annotators.get(i);
      if (dumb && !DumbService.isDumbAware(annotator)) {
        continue;
      }

      ProgressManager.checkCanceled();

      annotator.annotate(element, myAnnotationHolder);
    }
  }

  @RequiredReadAction
  private void visitErrorElement(final PsiErrorElement element) {
    for(HighlightErrorFilter errorFilter: myErrorFilters) {
      if (!errorFilter.shouldHighlightErrorElement(element)) {
        return;
      }
    }
    HighlightInfo info = createErrorElementInfo(element);
    myHolder.add(info);
  }

  @RequiredReadAction
  private static HighlightInfo createErrorElementInfo(@Nonnull PsiErrorElement element) {
    TextRange range = element.getTextRange();
    LocalizeValue errorDescription = element.getErrorDescriptionValue();
    if (!range.isEmpty()) {
      HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(range);
      if (errorDescription != LocalizeValue.empty()) {
        builder.descriptionAndTooltip(errorDescription);
      }
      final HighlightInfo info = builder.create();
      if (info != null) {
        for(ErrorQuickFixProvider provider: ErrorQuickFixProvider.EP_NAME.getExtensionList()) {
          provider.registerErrorQuickFix(element, info);
        }
      }
      return info;
    }
    int offset = range.getStartOffset();
    PsiFile containingFile = element.getContainingFile();
    int fileLength = containingFile.getTextLength();
    FileViewProvider viewProvider = containingFile.getViewProvider();
    PsiElement elementAtOffset = viewProvider.findElementAt(offset, LanguageUtil.getRootLanguage(element));
    String text = elementAtOffset == null ? null : elementAtOffset.getText();
    HighlightInfo info;
    if (offset < fileLength && text != null && !StringUtil.startsWithChar(text, '\n') && !StringUtil.startsWithChar(text, '\r')) {
      HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(offset, offset + 1);
      if (errorDescription != LocalizeValue.empty()) {
        builder.descriptionAndTooltip(errorDescription);
      }
      info = builder.create();
    }
    else {
      int start;
      int end;
      if (offset > 0) {
        start = offset/* - 1*/;
        end = offset;
      }
      else {
        start = offset;
        end = offset < fileLength ? offset + 1 : offset;
      }
      HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR).range(element, start, end);
      if (errorDescription != LocalizeValue.empty()) {
        builder.descriptionAndTooltip(errorDescription);
      }
      builder.endOfLine();
      info = builder.create();
    }
    return info;
  }
}