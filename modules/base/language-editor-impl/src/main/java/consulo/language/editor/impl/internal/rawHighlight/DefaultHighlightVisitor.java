// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.rawHighlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.HighlightErrorFilter;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;
import consulo.language.editor.impl.internal.highlight.AnnotationHolderImpl;
import consulo.language.editor.intention.ErrorQuickFixProvider;
import consulo.language.editor.rawHighlight.HighlightInfo;
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
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DefaultHighlightVisitor implements HighlightVisitor {
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

    public DefaultHighlightVisitor(@Nonnull Project project, boolean highlightErrorElements, boolean runAnnotators, boolean batchMode) {
        myProject = project;
        myHighlightErrorElements = highlightErrorElements;
        myRunAnnotators = runAnnotators;
        myDumbService = DumbService.getInstance(project);
        myBatchMode = batchMode;
    }

    @Override
    public boolean analyze(@Nonnull PsiFile file, boolean updateWholeFile, @Nonnull HighlightInfoHolder holder, @Nonnull Runnable action) {
        myDumb = myDumbService.isDumb();
        myHolder = holder;

        myAnnotationHolder = new AnnotationHolderImpl(holder.getAnnotationSession(), myBatchMode) {
            @Override
            public void queueToUpdateIncrementally() {
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
    @RequiredReadAction
    public void visit(@Nonnull PsiElement element) {
        if (myRunAnnotators) {
            runAnnotators(element);
        }
        if (element instanceof PsiErrorElement && myHighlightErrorElements) {
            visitErrorElement((PsiErrorElement)element);
        }
    }

    @RequiredReadAction
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

    @RequiredReadAction
    private void visitErrorElement(@Nonnull PsiErrorElement element) {
        if (HighlightErrorFilter.EP_NAME.findFirstSafe(myProject, filter -> !filter.shouldHighlightErrorElement(element)) != null) {
            return;
        }

        myHolder.add(createErrorElementInfo(element));
    }

    @RequiredReadAction
    private static HighlightInfo createErrorElementInfo(@Nonnull PsiErrorElement element) {
        HighlightInfo.Builder builder = createInfoWithoutFixes(element);
        element.getProject()
            .getExtensionPoint(ErrorQuickFixProvider.class)
            .forEachExtensionSafe(it -> it.registerErrorQuickFix(element, builder));

        return builder.createUnconditionally();
    }

    @RequiredReadAction
    @Nonnull
    private static HighlightInfo.Builder createInfoWithoutFixes(@Nonnull PsiErrorElement element) {
        TextRange range = element.getTextRange();
        LocalizeValue errorDescription = element.getErrorDescriptionValue();
        if (!range.isEmpty()) {
            return HighlightInfoImpl.newHighlightInfo(HighlightInfoType.ERROR).range(range).descriptionAndTooltip(errorDescription);
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
            return builder;
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
        return builder;
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