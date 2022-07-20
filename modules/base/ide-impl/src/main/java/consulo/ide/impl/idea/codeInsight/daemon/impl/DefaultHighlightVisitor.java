// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.daemon.AnnotatorStatisticsCollector;
import consulo.language.editor.intention.ErrorQuickFixProvider;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.language.Language;
import consulo.language.editor.HighlightErrorFilter;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.rawHighlight.HighlightInfoHolder;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.LanguageUtil;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@ExtensionImpl
final class DefaultHighlightVisitor implements HighlightVisitor, DumbAware {
  private AnnotationHolderImpl myAnnotationHolder;
  private final Map<Language, List<Annotator>> myAnnotators = ConcurrentFactoryMap.createMap(this::createAnnotators);
  private final Project myProject;
  private final boolean myHighlightErrorElements;
  private final boolean myRunAnnotators;
  private final DumbService myDumbService;
  private HighlightInfoHolder myHolder;
  private final boolean myBatchMode;
  private boolean myDumb;
  private final AnnotatorStatisticsCollector myAnnotatorStatisticsCollector = new AnnotatorStatisticsCollector();

  @Inject
  DefaultHighlightVisitor(@Nonnull Project project) {
    this(project, true, true, false);
  }

  DefaultHighlightVisitor(@Nonnull Project project, boolean highlightErrorElements, boolean runAnnotators, boolean batchMode) {
    myProject = project;
    myHighlightErrorElements = highlightErrorElements;
    myRunAnnotators = runAnnotators;
    myDumbService = DumbService.getInstance(project);
    myBatchMode = batchMode;
  }

  @Override
  public boolean suitableForFile(@Nonnull PsiFile file) {
    return true;
  }

  @Override
  public boolean analyze(@Nonnull PsiFile file, boolean updateWholeFile, @Nonnull HighlightInfoHolder holder, @Nonnull Runnable action) {
    myDumb = myDumbService.isDumb();
    myHolder = holder;

    myAnnotationHolder = new AnnotationHolderImpl(holder.getAnnotationSession(), myBatchMode) {
      @Override
      void queueToUpdateIncrementally() {
        if (!isEmpty()) {
          myAnnotatorStatisticsCollector.reportAnnotationProduced(myCurrentAnnotator, get(0));
          //noinspection ForLoopReplaceableByForEach
          for (int i = 0; i < size(); i++) {
            Annotation annotation = get(i);
            holder.add(HighlightInfoImpl.fromAnnotation(annotation, myBatchMode));
          }
          holder.queueToUpdateIncrementally();
          clear();
        }
      }
    };
    try {
      action.run();
      myAnnotationHolder.assertAllAnnotationsCreated();
    }
    finally {
      myAnnotators.clear();
      myHolder = null;
      myAnnotationHolder = null;
      myAnnotatorStatisticsCollector.reportAnalysisFinished(myProject, holder.getAnnotationSession(), file);
    }
    return true;
  }

  @Override
  public void visit(@Nonnull PsiElement element) {
    if (myRunAnnotators) {
      runAnnotators(element);
    }
    if (element instanceof PsiErrorElement && myHighlightErrorElements) {
      visitErrorElement((PsiErrorElement)element);
    }
  }

  @SuppressWarnings("CloneDoesntCallSuperClone")
  @Override
  @Nonnull
  public HighlightVisitor clone() {
    return new DefaultHighlightVisitor(myProject, myHighlightErrorElements, myRunAnnotators, myBatchMode);
  }

  private void runAnnotators(@Nonnull PsiElement element) {
    List<Annotator> annotators = myAnnotators.get(element.getLanguage());
    if (!annotators.isEmpty()) {
      AnnotationHolderImpl holder = myAnnotationHolder;
      holder.myCurrentElement = element;
      for (Annotator annotator : annotators) {
        if (!myDumb || DumbService.isDumbAware(annotator)) {
          ProgressManager.checkCanceled();
          holder.myCurrentAnnotator = annotator;
          annotator.annotate(element, holder);
          // assume that annotator is done messing with just created annotations after its annotate() method completed
          // and we can start applying them incrementally at last
          // (but not sooner, thanks to awfully racey Annotation.setXXX() API)
          holder.queueToUpdateIncrementally();
        }
      }
    }
  }

  private void visitErrorElement(@Nonnull PsiErrorElement element) {
    if (HighlightErrorFilter.EP_NAME.findFirstSafe(myProject, filter -> !filter.shouldHighlightErrorElement(element)) != null) {
      return;
    }

    myHolder.add(createErrorElementInfo(element));
  }

  private static HighlightInfoImpl createErrorElementInfo(@Nonnull PsiErrorElement element) {
    HighlightInfoImpl info = createInfoWithoutFixes(element);
    if (info != null) {
      ErrorQuickFixProvider.EP_NAME.forEachExtensionSafe(provider -> provider.registerErrorQuickFix(element, info));
    }
    return info;
  }

  private static HighlightInfoImpl createInfoWithoutFixes(@Nonnull PsiErrorElement element) {
    TextRange range = element.getTextRange();
    LocalizeValue errorDescription = element.getErrorDescriptionValue();
    if (!range.isEmpty()) {
      return (HighlightInfoImpl)HighlightInfoImpl.newHighlightInfo(HighlightInfoType.ERROR).range(range).descriptionAndTooltip(errorDescription).create();
    }
    int offset = range.getStartOffset();
    PsiFile containingFile = element.getContainingFile();
    int fileLength = containingFile.getTextLength();
    FileViewProvider viewProvider = containingFile.getViewProvider();
    PsiElement elementAtOffset = viewProvider.findElementAt(offset, LanguageUtil.getRootLanguage(element));
    String text = elementAtOffset == null ? null : elementAtOffset.getText();
    if (offset < fileLength && text != null && !StringUtil.startsWithChar(text, '\n') && !StringUtil.startsWithChar(text, '\r')) {
      HighlightInfoImpl.Builder builder = HighlightInfoImpl.newHighlightInfo(HighlightInfoType.ERROR).range(offset, offset + 1);
      builder.descriptionAndTooltip(errorDescription);
      return (HighlightInfoImpl)builder.create();
    }
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
    HighlightInfoImpl.Builder builder = HighlightInfoImpl.newHighlightInfo(HighlightInfoType.ERROR).range(element, start, end);
    builder.descriptionAndTooltip(errorDescription);
    builder.endOfLine();
    return (HighlightInfoImpl)builder.create();
  }

  @Nonnull
  private List<Annotator> createAnnotators(@Nonnull Language language) {
    List<AnnotatorFactory> annotatorFactories = AnnotatorFactory.forLanguage(myProject, language);
    if (annotatorFactories.isEmpty()) {
      return List.of();
    }
    
    List<Annotator> result = new ArrayList<>();
    for (AnnotatorFactory factory : annotatorFactories) {
      Annotator annotator = factory.createAnnotator();
      if (annotator != null) {
        result.add(annotator);

        myAnnotatorStatisticsCollector.reportNewAnnotatorCreated(annotator);
      }
    }

    return result;
  }
}