// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.highlight;

import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.CommonProcessors;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesScheme;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.HighlightRangeExtension;
import consulo.language.editor.Pass;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.highlight.HighlightingLevelManager;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.wolfAnalyzer.ProblemImpl;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.editor.rawHighlight.*;
import consulo.language.editor.wolfAnalyzer.Problem;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.*;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoItem;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.Stack;
import consulo.util.collection.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GeneralHighlightingPass extends ProgressableTextEditorHighlightingPass {
  private static final Logger LOG = Logger.getInstance(GeneralHighlightingPass.class);
  private static final Key<Boolean> HAS_ERROR_ELEMENT = Key.create("HAS_ERROR_ELEMENT");
  public static final Predicate<PsiFile> SHOULD_HIGHLIGHT_FILTER = file -> HighlightingLevelManager.getInstance(file.getProject()).shouldHighlight(file);
  private static final Random RESTART_DAEMON_RANDOM = new Random();
  private static final Key<AtomicInteger> HIGHLIGHT_VISITOR_INSTANCE_COUNT = Key.create("HIGHLIGHT_VISITOR_INSTANCE_COUNT");

  protected final boolean myUpdateAll;
  protected final ProperTextRange myPriorityRange;

  protected final List<HighlightInfo> myHighlights = new ArrayList<>();

  protected volatile boolean myHasErrorElement;
  private volatile boolean myErrorFound;
  protected final EditorColorsScheme myGlobalScheme;
  private volatile Supplier<List<HighlightVisitorFactory>> myHighlightVisitorProducer;

  public GeneralHighlightingPass(
    @Nonnull Project project,
    @Nonnull PsiFile file,
    @Nonnull Document document,
    int startOffset,
    int endOffset,
    boolean updateAll,
    @Nonnull ProperTextRange priorityRange,
    @Nullable Editor editor,
    @Nonnull HighlightInfoProcessor highlightInfoProcessor
  ) {
    super(
      project,
      document,
      DaemonLocalize.passSyntax().get(),
      file,
      editor,
      TextRange.create(startOffset, endOffset),
      true,
      highlightInfoProcessor
    );
    myUpdateAll = updateAll;
    myPriorityRange = priorityRange;
    myHighlightVisitorProducer = () -> project.getExtensionList(HighlightVisitorFactory.class);

    PsiUtilCore.ensureValid(file);
    boolean wholeFileHighlighting = isWholeFileHighlighting();
    myHasErrorElement = !wholeFileHighlighting && Boolean.TRUE.equals(getFile().getUserData(HAS_ERROR_ELEMENT));
    final DaemonCodeAnalyzerEx daemonCodeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(myProject);
    FileStatusMapImpl fileStatusMap = daemonCodeAnalyzer.getFileStatusMap();
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
    return Objects.requireNonNull(super.getDocument());
  }

  @Nonnull
  private List<HighlightVisitor> filterVisitors(@Nonnull List<HighlightVisitorFactory> highlightVisitorFactories, @Nonnull PsiFile psiFile) {
    final List<HighlightVisitor> result = new ArrayList<>(highlightVisitorFactories.size());
    DumbService dumbService = DumbService.getInstance(myProject);

    dumbService.forEachDumAwareness(highlightVisitorFactories, highlightVisitorFactory -> {
      // skip rainbow visitor if not enabled
      if (highlightVisitorFactory instanceof RainbowVisitorFactory && !RainbowHighlighter.isRainbowEnabledWithInheritance(getColorsScheme(),
                                                                                                                          psiFile.getLanguage())) {
        return;
      }

      if (highlightVisitorFactory.suitableForFile(psiFile)) {
        incVisitorUsageCount(1);
        result.add(highlightVisitorFactory.createVisitor());
      }
    });

    if (result.isEmpty()) {
      LOG.error("No visitors registered. list=" + result + "; all visitors are:" + myProject.getExtensionList(HighlightVisitorFactory.class));
    }

    return result;
  }

  public void setHighlightVisitorProducer(@Nonnull Supplier<List<HighlightVisitorFactory>> highlightVisitorProducer) {
    myHighlightVisitorProducer = highlightVisitorProducer;
  }

  @Nonnull
  public List<HighlightVisitor> getHighlightVisitors(@Nonnull PsiFile psiFile) {
    return filterVisitors(myHighlightVisitorProducer.get(), psiFile);
  }

  // returns old value
  public int incVisitorUsageCount(int delta) {
    AtomicInteger count = myProject.getUserData(HIGHLIGHT_VISITOR_INSTANCE_COUNT);
    if (count == null) {
      count = myProject.putUserDataIfAbsent(HIGHLIGHT_VISITOR_INSTANCE_COUNT, new AtomicInteger(0));
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
    final List<HighlightVisitor> filteredVisitors = getHighlightVisitors(getFile());
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
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? Lists.append(outside, lastInside) : outside;
      }));
      List<ProperTextRange> allOutsideRanges = ContainerUtil.concat((List<List<ProperTextRange>>)ContainerUtil.map(dividedElements, dividedForRoot -> {
        List<ProperTextRange> outsideRanges = dividedForRoot.outsideRanges;
        PsiElement lastInside = ContainerUtil.getLastItem(dividedForRoot.inside);
        ProperTextRange lastInsideRange = ContainerUtil.getLastItem(dividedForRoot.insideRanges);
        return lastInside instanceof PsiFile && !(lastInside instanceof PsiCodeFragment) ? Lists.append(outsideRanges, lastInsideRange) : outsideRanges;
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
                                    @Nonnull final List<HighlightVisitor> visitors,
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
    List<HighlightInfoImpl> postInfos = new ArrayList<>(holder.size());
    // there can be extra highlights generated in PostHighlightVisitor
    for (int j = 0; j < holder.size(); j++) {
      final HighlightInfo info = holder.get(j);
      assert info != null;
      postInfos.add((HighlightInfoImpl)info);
    }
    myHighlightInfoProcessor.highlightsInsideVisiblePartAreProduced(myHighlightingSession, getEditor(), postInfos, getFile().getTextRange(), getFile().getTextRange(), POST_UPDATE_ALL);
    return success;
  }

  private boolean analyzeByVisitors(@Nonnull final List<HighlightVisitor> visitors, @Nonnull final HighlightInfoHolder holder, final int i, @Nonnull final Runnable action) {
    final boolean[] success = {true};
    if (i == visitors.size()) {
      action.run();
    }
    else {
      if (!visitors.get(i).analyze(getFile(), myUpdateAll, holder, () -> success[0] = analyzeByVisitors(visitors, holder, i + 1, action))) {
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
                           @Nonnull List<HighlightVisitor> visitors,
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
        final HighlightInfoImpl info = (HighlightInfoImpl)holder.get(j);

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

  public static final int POST_UPDATE_ALL = 5;

  private static final AtomicInteger RESTART_REQUESTS = new AtomicInteger();

  @TestOnly
  public static boolean isRestartPending() {
    return RESTART_REQUESTS.get() > 0;
  }

  private static void cancelAndRestartDaemonLater(@Nonnull ProgressIndicator progress, @Nonnull final Project project) throws ProcessCanceledException {
    RESTART_REQUESTS.incrementAndGet();
    progress.cancel();
    int delay = RESTART_DAEMON_RANDOM.nextInt(100);
    project.getUIAccess().getScheduler().schedule(() -> {
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

  @Nonnull
  protected HighlightInfoHolder createInfoHolder(@Nonnull PsiFile file) {
    List<HighlightInfoFilter> filters = HighlightInfoFilter.EXTENSION_POINT_NAME.getExtensionList();
    EditorColorsScheme actualScheme = getColorsScheme() == null ? EditorColorsManager.getInstance().getGlobalScheme() : getColorsScheme();
    return new HighlightInfoHolder(file, filters) {
      int queued;

      @Override
      @Nonnull
      public TextAttributesScheme getColorsScheme() {
        return actualScheme;
      }

      @Override
      public void queueToUpdateIncrementally() {
        for (int i = queued; i < size(); i++) {
          HighlightInfo info = get(i);
          queueInfoToUpdateIncrementally(info);
        }
        queued = size();
      }

      @Override
      public void clear() {
        super.clear();
        queued = 0;
      }
    };
  }

  protected void queueInfoToUpdateIncrementally(@Nonnull HighlightInfo info) {
    int group = ((HighlightInfoImpl)info).getGroup() == 0 ? Pass.UPDATE_ALL : ((HighlightInfoImpl)info).getGroup();
    myHighlightInfoProcessor.infoIsAvailable(myHighlightingSession, info, myPriorityRange, myRestrictRange, group);
  }

  public static void highlightTodos(@Nonnull PsiFile file,
                                    @Nonnull CharSequence text,
                                    int startOffset,
                                    int endOffset,
                                    @Nonnull ProperTextRange priorityRange,
                                    @Nonnull Collection<? super HighlightInfoImpl> insideResult,
                                    @Nonnull Collection<? super HighlightInfoImpl> outsideResult) {
    PsiTodoSearchHelper helper = PsiTodoSearchHelper.getInstance(file.getProject());
    if (!shouldHighlightTodos(helper, file)) return;
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
                                  @Nonnull Collection<? super HighlightInfoImpl> insideResult,
                                  @Nonnull Collection<? super HighlightInfoImpl> outsideResult,
                                  @Nonnull TextAttributes attributes,
                                  @Nonnull String description,
                                  @Nonnull String tooltip,
                                  @Nonnull TextRange range) {
    if (range.getStartOffset() >= restrictEndOffset || range.getEndOffset() <= restrictStartOffset) return;
    HighlightInfoImpl info = (HighlightInfoImpl)HighlightInfoImpl.newHighlightInfo(HighlightInfoType.TODO).range(range).textAttributes(attributes).description(description).escapedToolTip(tooltip)
            .createUnconditionally();
    Collection<? super HighlightInfoImpl> result = priorityRange.containsRange(info.getStartOffset(), info.getEndOffset()) ? insideResult : outsideResult;
    result.add(info);
  }

  private static boolean shouldHighlightTodos(@Nonnull PsiTodoSearchHelper helper, @Nonnull PsiFile file) {
    return helper.shouldHighlightInEditor(file);
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
        Problem problem = new ProblemImpl(file, (HighlightInfoImpl)info, hasErrorElement);
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
