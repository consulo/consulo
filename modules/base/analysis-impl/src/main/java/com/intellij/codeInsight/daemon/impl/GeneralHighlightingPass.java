// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInsight.problems.ProblemImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.psi.*;
import com.intellij.psi.impl.search.PsiTodoSearchHelperImpl;
import com.intellij.psi.search.PsiTodoSearchHelper;
import com.intellij.psi.search.TodoAttributesUtil;
import com.intellij.psi.search.TodoItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.NotNullProducer;
import com.intellij.util.SmartList;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.containers.Stack;
import com.intellij.xml.util.XmlStringUtil;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GeneralHighlightingPass extends ProgressableTextEditorHighlightingPass {
  private static final Logger LOG = Logger.getInstance(GeneralHighlightingPass.class);
  private static final String PRESENTABLE_NAME = DaemonBundle.message("pass.syntax");
  private static final Key<Boolean> HAS_ERROR_ELEMENT = Key.create("HAS_ERROR_ELEMENT");
  static final Condition<PsiFile> SHOULD_HIGHLIGHT_FILTER = file -> HighlightingLevelManager.getInstance(file.getProject()).shouldHighlight(file);
  private static final Random RESTART_DAEMON_RANDOM = new Random();

  final boolean myUpdateAll;
  final ProperTextRange myPriorityRange;

  final List<HighlightInfo> myHighlights = new ArrayList<>();

  protected volatile boolean myHasErrorElement;
  private volatile boolean myErrorFound;
  final EditorColorsScheme myGlobalScheme;
  private volatile NotNullProducer<HighlightVisitor[]> myHighlightVisitorProducer = this::cloneHighlightVisitors;

  public GeneralHighlightingPass(@Nonnull Project project,
                                 @Nonnull PsiFile file,
                                 @Nonnull Document document,
                                 int startOffset,
                                 int endOffset,
                                 boolean updateAll,
                                 @Nonnull ProperTextRange priorityRange,
                                 @Nullable Editor editor,
                                 @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    super(project, document, PRESENTABLE_NAME, file, editor, TextRange.create(startOffset, endOffset), true, highlightInfoProcessor);
    myUpdateAll = updateAll;
    myPriorityRange = priorityRange;

    PsiUtilCore.ensureValid(file);
    boolean wholeFileHighlighting = isWholeFileHighlighting();
    myHasErrorElement = !wholeFileHighlighting && Boolean.TRUE.equals(getFile().getUserData(HAS_ERROR_ELEMENT));
    final DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
    FileStatusMap fileStatusMap = daemonCodeAnalyzer.getFileStatusMap();
    myErrorFound = !wholeFileHighlighting && fileStatusMap.wasErrorFound(getDocument());

    // initial guess to show correct progress in the traffic light icon
    setProgressLimit(document.getTextLength() / 2); // approx number of PSI elements = file length/2
    myGlobalScheme = editor != null ? editor.getColorsScheme() : EditorColorsManager.getInstance().getGlobalScheme();
  }

  @Nonnull
  private PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public Document getDocument() {
    // this pass always get not-null document
    //noinspection ConstantConditions
    return super.getDocument();
  }

  private static final Key<AtomicInteger> HIGHLIGHT_VISITOR_INSTANCE_COUNT = new Key<>("HIGHLIGHT_VISITOR_INSTANCE_COUNT");

  @Nonnull
  private HighlightVisitor[] cloneHighlightVisitors() {
    int oldCount = incVisitorUsageCount(1);
    HighlightVisitor[] highlightVisitors = HighlightVisitor.EP_HIGHLIGHT_VISITOR.getExtensions(myProject);
    if (oldCount != 0) {
      HighlightVisitor[] clones = new HighlightVisitor[highlightVisitors.length];
      for (int i = 0; i < highlightVisitors.length; i++) {
        HighlightVisitor highlightVisitor = highlightVisitors[i];
        HighlightVisitor cloned = highlightVisitor.clone();
        assert cloned.getClass() == highlightVisitor.getClass() : highlightVisitor.getClass() +
                                                                  ".clone() must return a copy of " +
                                                                  highlightVisitor.getClass() +
                                                                  "; but got: " +
                                                                  cloned +
                                                                  " of " +
                                                                  cloned.getClass();
        clones[i] = cloned;
      }
      highlightVisitors = clones;
    }
    return highlightVisitors;
  }

  @Nonnull
  private HighlightVisitor[] filterVisitors(@Nonnull HighlightVisitor[] highlightVisitors, @Nonnull PsiFile psiFile) {
    final List<HighlightVisitor> visitors = new ArrayList<>(highlightVisitors.length);
    List<HighlightVisitor> list = Arrays.asList(highlightVisitors);
    for (HighlightVisitor visitor : DumbService.getInstance(myProject).filterByDumbAwareness(list)) {
      if (visitor instanceof RainbowVisitor && !RainbowHighlighter.isRainbowEnabledWithInheritance(getColorsScheme(), psiFile.getLanguage())) {
        continue;
      }
      if (visitor.suitableForFile(psiFile)) {
        visitors.add(visitor);
      }
    }
    if (visitors.isEmpty()) {
      LOG.error("No visitors registered. list=" + list + "; all visitors are:" + Arrays.asList(HighlightVisitor.EP_HIGHLIGHT_VISITOR.getExtensions(myProject)));
    }

    return visitors.toArray(new HighlightVisitor[0]);
  }

  void setHighlightVisitorProducer(@Nonnull NotNullProducer<HighlightVisitor[]> highlightVisitorProducer) {
    myHighlightVisitorProducer = highlightVisitorProducer;
  }

  @Nonnull
  HighlightVisitor[] getHighlightVisitors(@Nonnull PsiFile psiFile) {
    return filterVisitors(myHighlightVisitorProducer.produce(), psiFile);
  }

  // returns old value
  int incVisitorUsageCount(int delta) {
    AtomicInteger count = myProject.getUserData(HIGHLIGHT_VISITOR_INSTANCE_COUNT);
    if (count == null) {
      count = ((UserDataHolderEx)myProject).putUserDataIfAbsent(HIGHLIGHT_VISITOR_INSTANCE_COUNT, new AtomicInteger(0));
    }
    int old = count.getAndAdd(delta);
    assert old + delta >= 0 : old + ";" + delta;
    return old;
  }

  @Override
  protected void collectInformationWithProgress(@Nonnull final ProgressIndicator progress) {
    final List<HighlightInfo> outsideResult = new ArrayList<>(100);
    final List<HighlightInfo> insideResult = new ArrayList<>(100);

    final DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
    final HighlightVisitor[] filteredVisitors = getHighlightVisitors(getFile());
    try {
      List<Divider.DividedElements> dividedElements = new ArrayList<>();
      Divider.divideInsideAndOutsideAllRoots(getFile(), myRestrictRange, myPriorityRange, SHOULD_HIGHLIGHT_FILTER, new CommonProcessors.CollectProcessor<>(dividedElements));
      List<PsiElement> allInsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(dividedElements, dividedForRoot -> {
        List<PsiElement> inside = dividedForRoot.inside;
        PsiElement lastInside = ContainerUtil.getLastItem(inside);
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? inside.subList(0, inside.size() - 1) : inside;
      }));


      List<ProperTextRange> allInsideRanges = ContainerUtil.concat((List<List<ProperTextRange>>)ContainerUtil.map(dividedElements, dividedForRoot -> {
        List<ProperTextRange> insideRanges = dividedForRoot.insideRanges;
        PsiElement lastInside = ContainerUtil.getLastItem(dividedForRoot.inside);
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? insideRanges.subList(0, insideRanges.size() - 1) : insideRanges;
      }));

      List<PsiElement> allOutsideElements = ContainerUtil.concat((List<List<PsiElement>>)ContainerUtil.map(dividedElements, dividedForRoot -> {
        List<PsiElement> outside = dividedForRoot.outside;
        PsiElement lastInside = ContainerUtil.getLastItem(dividedForRoot.inside);
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? ContainerUtil.append(outside, lastInside) : outside;
      }));
      List<ProperTextRange> allOutsideRanges = ContainerUtil.concat((List<List<ProperTextRange>>)ContainerUtil.map(dividedElements, dividedForRoot -> {
        List<ProperTextRange> outsideRanges = dividedForRoot.outsideRanges;
        PsiElement lastInside = ContainerUtil.getLastItem(dividedForRoot.inside);
        ProperTextRange lastInsideRange = ContainerUtil.getLastItem(dividedForRoot.insideRanges);
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? ContainerUtil.append(outsideRanges, lastInsideRange) : outsideRanges;
      }));


      setProgressLimit(allInsideElements.size() + allOutsideElements.size());

      final boolean forceHighlightParents = forceHighlightParents();

      if (!isDumbMode()) {
        highlightTodos(getFile(), getDocument().getCharsSequence(), myRestrictRange.getStartOffset(), myRestrictRange.getEndOffset(), myPriorityRange, insideResult, outsideResult);
      }

      boolean success = collectHighlights(allInsideElements, allInsideRanges, allOutsideElements, allOutsideRanges, filteredVisitors, insideResult, outsideResult, forceHighlightParents);

      if (success) {
        myHighlightInfoProcessor.highlightsOutsideVisiblePartAreProduced(myHighlightingSession, getEditor(), outsideResult, myPriorityRange, myRestrictRange, getId());

        if (myUpdateAll) {
          daemonCodeAnalyzer.getFileStatusMap().setErrorFoundFlag(myProject, getDocument(), myErrorFound);
        }
      }
      else {
        cancelAndRestartDaemonLater(progress, myProject);
      }
    }
    finally {
      incVisitorUsageCount(-1);
      myHighlights.addAll(insideResult);
      myHighlights.addAll(outsideResult);
    }
  }

  private boolean isWholeFileHighlighting() {
    return myUpdateAll && myRestrictRange.equalsToRange(0, getDocument().getTextLength());
  }

  @Override
  protected void applyInformationWithProgress() {
    getFile().putUserData(HAS_ERROR_ELEMENT, myHasErrorElement);

    if (myUpdateAll) {
      ((HighlightingSessionImpl)myHighlightingSession).applyInEDT(this::reportErrorsToWolf);
    }
  }

  @Override
  @Nonnull
  public List<HighlightInfo> getInfos() {
    return new ArrayList<>(myHighlights);
  }

  private boolean collectHighlights(@Nonnull final List<? extends PsiElement> elements1,
                                    @Nonnull final List<? extends ProperTextRange> ranges1,
                                    @Nonnull final List<? extends PsiElement> elements2,
                                    @Nonnull final List<? extends ProperTextRange> ranges2,
                                    @Nonnull final HighlightVisitor[] visitors,
                                    @Nonnull final List<HighlightInfo> insideResult,
                                    @Nonnull final List<? super HighlightInfo> outsideResult,
                                    final boolean forceHighlightParents) {
    final Set<PsiElement> skipParentsSet = new HashSet<>();

    // TODO - add color scheme to holder
    final HighlightInfoHolder holder = createInfoHolder(getFile());

    final int chunkSize = Math.max(1, (elements1.size() + elements2.size()) / 100); // one percent precision is enough

    boolean success = analyzeByVisitors(visitors, holder, 0, () -> {
      Stack<TextRange> nestedRange = new Stack<>();
      Stack<List<HighlightInfo>> nestedInfos = new Stack<>();
      runVisitors(elements1, ranges1, chunkSize, skipParentsSet, holder, insideResult, outsideResult, forceHighlightParents, visitors, nestedRange, nestedInfos);
      final TextRange priorityIntersection = myPriorityRange.intersection(myRestrictRange);
      if ((!elements1.isEmpty() || !insideResult.isEmpty()) && priorityIntersection != null) { // do not apply when there were no elements to highlight
        myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), insideResult, myPriorityRange, myRestrictRange, getId());
      }
      runVisitors(elements2, ranges2, chunkSize, skipParentsSet, holder, insideResult, outsideResult, forceHighlightParents, visitors, nestedRange, nestedInfos);
    });
    List<HighlightInfo> postInfos = new ArrayList<>(holder.size());
    // there can be extra highlights generated in PostHighlightVisitor
    for (int j = 0; j < holder.size(); j++) {
      final HighlightInfo info = holder.get(j);
      assert info != null;
      postInfos.add(info);
    }
    myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), postInfos, getFile().getTextRange(), getFile().getTextRange(), POST_UPDATE_ALL);
    return success;
  }

  private boolean analyzeByVisitors(@Nonnull final HighlightVisitor[] visitors, @Nonnull final HighlightInfoHolder holder, final int i, @Nonnull final Runnable action) {
    final boolean[] success = {true};
    if (i == visitors.length) {
      action.run();
    }
    else {
      if (!visitors[i].analyze(getFile(), myUpdateAll, holder, () -> success[0] = analyzeByVisitors(visitors, holder, i + 1, action))) {
        success[0] = false;
      }
    }
    return success[0];
  }

  private void runVisitors(@Nonnull List<? extends PsiElement> elements,
                           @Nonnull List<? extends ProperTextRange> ranges,
                           int chunkSize,
                           @Nonnull Set<? super PsiElement> skipParentsSet,
                           @Nonnull HighlightInfoHolder holder,
                           @Nonnull List<? super HighlightInfo> insideResult,
                           @Nonnull List<? super HighlightInfo> outsideResult,
                           boolean forceHighlightParents,
                           @Nonnull HighlightVisitor[] visitors,
                           @Nonnull Stack<TextRange> nestedRange,
                           @Nonnull Stack<List<HighlightInfo>> nestedInfos) {
    boolean failed = false;
    int nextLimit = chunkSize;
    for (int i = 0; i < elements.size(); i++) {
      PsiElement element = elements.get(i);
      ProgressManager.checkCanceled();

      PsiElement parent = element.getParent();
      if (element != getFile() && !skipParentsSet.isEmpty() && element.getFirstChild() != null && skipParentsSet.contains(element)) {
        skipParentsSet.add(parent);
        continue;
      }

      boolean isErrorElement = element instanceof PsiErrorElement;
      if (isErrorElement) {
        myHasErrorElement = true;
      }

      for (HighlightVisitor visitor : visitors) {
        try {
          visitor.visit(element);
        }
        catch (ProcessCanceledException | IndexNotReadyException e) {
          throw e;
        }
        catch (Exception e) {
          if (!failed) {
            LOG.error("In file: " + myFile.getViewProvider().getVirtualFile(), e);
          }
          failed = true;
        }
      }

      if (i == nextLimit) {
        advanceProgress(chunkSize);
        nextLimit = i + chunkSize;
      }

      TextRange elementRange = ranges.get(i);
      List<HighlightInfo> infosForThisRange = holder.size() == 0 ? null : new ArrayList<>(holder.size());
      for (int j = 0; j < holder.size(); j++) {
        final HighlightInfo info = holder.get(j);

        if (!myRestrictRange.contains(info)) continue;
        List<? super HighlightInfo> result = myPriorityRange.containsRange(info.getStartOffset(), info.getEndOffset()) && !(element instanceof PsiFile) ? insideResult : outsideResult;
        result.add(info);
        boolean isError = info.getSeverity() == HighlightSeverity.ERROR;
        if (isError) {
          if (!forceHighlightParents) {
            skipParentsSet.add(parent);
          }
          myErrorFound = true;
        }
        // if this highlight info range is exactly the same as the element range we are visiting
        // that means we can clear this highlight as soon as visitors won't produce any highlights during visiting the same range next time.
        // We also know that we can remove syntax error element.
        info.setBijective(elementRange.equalsToRange(info.startOffset, info.endOffset) || isErrorElement);

        myHighlightInfoProcessor.infoIsAvailable(myHighlightingSession, info, myPriorityRange, myRestrictRange, Pass.UPDATE_ALL);
        infosForThisRange.add(info);
      }
      holder.clear();

      // include infos which we got while visiting nested elements with the same range
      while (true) {
        if (!nestedRange.isEmpty() && elementRange.contains(nestedRange.peek())) {
          TextRange oldRange = nestedRange.pop();
          List<HighlightInfo> oldInfos = nestedInfos.pop();
          if (elementRange.equals(oldRange)) {
            if (infosForThisRange == null) {
              infosForThisRange = oldInfos;
            }
            else if (oldInfos != null) {
              infosForThisRange.addAll(oldInfos);
            }
          }
        }
        else {
          break;
        }
      }
      nestedRange.push(elementRange);
      nestedInfos.push(infosForThisRange);
      if (parent == null || !hasSameRangeAsParent(parent, element)) {
        myHighlightInfoProcessor.allHighlightsForRangeAreProduced(myHighlightingSession, elementRange, infosForThisRange);
      }
    }
    advanceProgress(elements.size() - (nextLimit - chunkSize));
  }

  private static boolean hasSameRangeAsParent(PsiElement parent, PsiElement element) {
    return element.getStartOffsetInParent() == 0 && element.getTextLength() == parent.getTextLength();
  }

  private static final int POST_UPDATE_ALL = 5;

  private static final AtomicInteger RESTART_REQUESTS = new AtomicInteger();

  @TestOnly
  static boolean isRestartPending() {
    return RESTART_REQUESTS.get() > 0;
  }

  private static void cancelAndRestartDaemonLater(@Nonnull ProgressIndicator progress, @Nonnull final Project project) throws ProcessCanceledException {
    RESTART_REQUESTS.incrementAndGet();
    progress.cancel();
    Application application = ApplicationManager.getApplication();
    int delay = application.isUnitTestMode() ? 0 : RESTART_DAEMON_RANDOM.nextInt(100);
    EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
      RESTART_REQUESTS.decrementAndGet();
      if (!project.isDisposed()) {
        DaemonCodeAnalyzer.getInstance(project).restart();
      }
    }, delay, TimeUnit.MILLISECONDS);
    throw new ProcessCanceledException();
  }

  private boolean forceHighlightParents() {
    boolean forceHighlightParents = false;
    for (HighlightRangeExtension extension : HighlightRangeExtension.EP_NAME.getExtensionList()) {
      if (extension.isForceHighlightParents(getFile())) {
        forceHighlightParents = true;
        break;
      }
    }
    return forceHighlightParents;
  }

  protected HighlightInfoHolder createInfoHolder(@Nonnull PsiFile file) {
    final HighlightInfoFilter[] filters = HighlightInfoFilter.EXTENSION_POINT_NAME.getExtensions();
    return new CustomHighlightInfoHolder(file, getColorsScheme(), filters);
  }

  static void highlightTodos(@Nonnull PsiFile file,
                             @Nonnull CharSequence text,
                             int startOffset,
                             int endOffset,
                             @Nonnull ProperTextRange priorityRange,
                             @Nonnull Collection<? super HighlightInfo> insideResult,
                             @Nonnull Collection<? super HighlightInfo> outsideResult) {
    PsiTodoSearchHelper helper = PsiTodoSearchHelper.getInstance(file.getProject());
    if (helper == null || !shouldHighlightTodos(helper, file)) return;
    TodoItem[] todoItems = helper.findTodoItems(file, startOffset, endOffset);
    if (todoItems.length == 0) return;

    for (TodoItem todoItem : todoItems) {
      ProgressManager.checkCanceled();

      TextRange textRange = todoItem.getTextRange();
      List<TextRange> additionalRanges = todoItem.getAdditionalTextRanges();

      StringJoiner joiner = new StringJoiner("\n");
      JBIterable.of(textRange).append(additionalRanges).forEach(range -> joiner.add(text.subSequence(range.getStartOffset(), range.getEndOffset())));
      String description = joiner.toString();
      String tooltip = XmlStringUtil.escapeString(StringUtil.shortenPathWithEllipsis(description, 1024)).replace("\n", "<br>");

      TextAttributes attributes = TodoAttributesUtil.getTextAttributes(todoItem.getPattern().getAttributes());
      addTodoItem(startOffset, endOffset, priorityRange, insideResult, outsideResult, attributes, description, tooltip, textRange);
      if (!additionalRanges.isEmpty()) {
        TextAttributes attributesForAdditionalLines = attributes.clone();
        attributesForAdditionalLines.setErrorStripeColor(null);
        for (TextRange range : additionalRanges) {
          addTodoItem(startOffset, endOffset, priorityRange, insideResult, outsideResult, attributesForAdditionalLines, description, tooltip, range);
        }
      }
    }
  }

  private static void addTodoItem(int restrictStartOffset,
                                  int restrictEndOffset,
                                  @Nonnull ProperTextRange priorityRange,
                                  @Nonnull Collection<? super HighlightInfo> insideResult,
                                  @Nonnull Collection<? super HighlightInfo> outsideResult,
                                  @Nonnull TextAttributes attributes,
                                  @Nonnull String description, @Nonnull String tooltip, @Nonnull TextRange range) {
    if (range.getStartOffset() >= restrictEndOffset || range.getEndOffset() <= restrictStartOffset) return;
    HighlightInfo info = HighlightInfo.newHighlightInfo(HighlightInfoType.TODO).range(range).textAttributes(attributes).description(description).escapedToolTip(tooltip).createUnconditionally();
    Collection<? super HighlightInfo> result = priorityRange.containsRange(info.getStartOffset(), info.getEndOffset()) ? insideResult : outsideResult;
    result.add(info);
  }

  private static boolean shouldHighlightTodos(@Nonnull PsiTodoSearchHelper helper, @Nonnull PsiFile file) {
    return helper instanceof PsiTodoSearchHelperImpl && ((PsiTodoSearchHelperImpl)helper).shouldHighlightInEditor(file);
  }

  private void reportErrorsToWolf() {
    if (!getFile().getViewProvider().isPhysical()) return; // e.g. errors in evaluate expression
    Project project = getFile().getProject();
    if (!PsiManager.getInstance(project).isInProject(getFile())) return; // do not report problems in libraries
    VirtualFile file = getFile().getVirtualFile();
    if (file == null) return;

    List<Problem> problems = convertToProblems(getInfos(), file, myHasErrorElement);
    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance(project);

    boolean hasErrors = DaemonCodeAnalyzerEx.hasErrors(project, getDocument());
    if (!hasErrors || isWholeFileHighlighting()) {
      wolf.reportProblems(file, problems);
    }
    else {
      wolf.weHaveGotProblems(file, problems);
    }
  }

  @Nonnull
  private static List<Problem> convertToProblems(@Nonnull Collection<? extends HighlightInfo> infos, @Nonnull VirtualFile file, final boolean hasErrorElement) {
    List<Problem> problems = new SmartList<>();
    for (HighlightInfo info : infos) {
      if (info.getSeverity() == HighlightSeverity.ERROR) {
        Problem problem = new ProblemImpl(file, info, hasErrorElement);
        problems.add(problem);
      }
    }
    return problems;
  }

  @Override
  public String toString() {
    return super.toString() + " updateAll=" + myUpdateAll + " range= " + myRestrictRange;
  }
}
