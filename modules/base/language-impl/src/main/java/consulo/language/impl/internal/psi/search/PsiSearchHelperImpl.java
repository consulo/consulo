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

package consulo.language.impl.internal.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AccessRule;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.internal.TooManyUsagesStatus;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ApplicationUtil;
import consulo.application.util.ReadActionProcessor;
import consulo.application.util.StringSearcher;
import consulo.application.util.concurrent.JobLauncher;
import consulo.application.util.function.Processors;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.language.cacheBuilder.CacheManager;
import consulo.language.content.FileIndexFacade;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.language.internal.PsiSearchHelperEx;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.search.*;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IdIndexEntry;
import consulo.language.util.CommentUtilCore;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.AsyncFuture;
import consulo.util.concurrent.AsyncUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PsiSearchHelperImpl implements PsiSearchHelperEx {
  private static final Logger LOG = Logger.getInstance(PsiSearchHelperImpl.class);
  private final PsiManagerEx myManager;
  private final DumbService myDumbService;
  private final CacheManager myCacheManager;

  public enum Options {
    PROCESS_INJECTED_PSI,
    CASE_SENSITIVE_SEARCH,
    PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE
  }

  @Inject
  public PsiSearchHelperImpl(@Nonnull PsiManager manager, DumbService dumbService, CacheManager cacheManager) {
    myCacheManager = cacheManager;
    myManager = (PsiManagerEx)manager;
    myDumbService = dumbService;
  }

  @Override
  @Nonnull
  public PsiElement[] findCommentsContainingIdentifier(@Nonnull String identifier, @Nonnull SearchScope searchScope) {
    List<PsiElement> result = Collections.synchronizedList(new ArrayList<>());
    Predicate<PsiElement> processor = Processors.cancelableCollectProcessor(result);
    processCommentsContainingIdentifier(identifier, searchScope, processor);
    return PsiUtilCore.toPsiElementArray(result);
  }

  @Override
  public boolean processCommentsContainingIdentifier(@Nonnull String identifier,
                                                     @Nonnull SearchScope searchScope,
                                                     @Nonnull Predicate<PsiElement> processor) {
    TextOccurenceProcessor occurrenceProcessor = (element, offsetInElement) -> {
      if (CommentUtilCore.isCommentTextElement(element) && element.findReferenceAt(offsetInElement) == null) {
        return processor.test(element);
      }
      return true;
    };
    return processElementsWithWord(occurrenceProcessor, searchScope, identifier, UsageSearchContext.IN_COMMENTS, true);
  }

  @Override
  public boolean processCandidateFilesForText(@Nonnull GlobalSearchScope scope,
                                              short searchContext,
                                              boolean caseSensitively,
                                              @Nonnull String text,
                                              @Nonnull Predicate<? super VirtualFile> processor) {
    List<IdIndexEntry> entries = getWordEntries(text, caseSensitively);
    if (entries.isEmpty()) return true;

    Predicate<Integer> contextMatches = matchContextCondition(searchContext);
    return processFilesContainingAllKeys(myManager.getProject(), scope, contextMatches, entries, processor);
  }

  @Nonnull
  private static Predicate<Integer> matchContextCondition(short searchContext) {
    return context -> (context & searchContext) != 0;
  }

  @Override
  public boolean processElementsWithWord(@Nonnull TextOccurenceProcessor processor,
                                         @Nonnull SearchScope searchScope,
                                         @Nonnull String text,
                                         short searchContext,
                                         boolean caseSensitive) {
    return processElementsWithWord(processor, searchScope, text, searchContext, caseSensitive, shouldProcessInjectedPsi(searchScope));
  }

  @Override
  public boolean processElementsWithWord(@Nonnull TextOccurenceProcessor processor,
                                         @Nonnull SearchScope searchScope,
                                         @Nonnull String text,
                                         short searchContext,
                                         boolean caseSensitive,
                                         boolean processInjectedPsi) {
    EnumSet<Options> options = EnumSet.of(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE);
    if (caseSensitive) options.add(Options.CASE_SENSITIVE_SEARCH);
    if (processInjectedPsi) options.add(Options.PROCESS_INJECTED_PSI);

    return processElementsWithWord(processor, searchScope, text, searchContext, options, null);
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> processElementsWithWordAsync(@Nonnull TextOccurenceProcessor processor,
                                                           @Nonnull SearchScope searchScope,
                                                           @Nonnull String text,
                                                           short searchContext,
                                                           boolean caseSensitively) {
    boolean result = processElementsWithWord(processor, searchScope, text, searchContext, caseSensitively,
                                             shouldProcessInjectedPsi(searchScope));
    return AsyncUtil.wrapBoolean(result);
  }

  @Override
  public boolean hasIdentifierInFile(@Nonnull PsiFile file, @Nonnull String name) {
    return IdIndex.hasIdentifierInFile(file, name);
  }

  public boolean processElementsWithWord(@Nonnull TextOccurenceProcessor processor,
                                         @Nonnull SearchScope searchScope,
                                         @Nonnull String text,
                                         short searchContext,
                                         @Nonnull EnumSet<Options> options,
                                         @Nullable String containerName) {
    return bulkProcessElementsWithWord(searchScope, text, searchContext, options, containerName, (scope, offsetsInScope, searcher) ->
      LowLevelSearchUtil.processElementsAtOffsets(scope, searcher, options.contains(Options.PROCESS_INJECTED_PSI), getOrCreateIndicator(),
                                                  offsetsInScope, processor));
  }

  private boolean bulkProcessElementsWithWord(@Nonnull SearchScope searchScope,
                                              @Nonnull String text,
                                              short searchContext,
                                              @Nonnull EnumSet<Options> options,
                                              @Nullable String containerName, @Nonnull final BulkOccurrenceProcessor processor) {
    if (text.isEmpty()) {
      throw new IllegalArgumentException("Cannot search for elements with empty text");
    }
    final ProgressIndicator progress = getOrCreateIndicator();
    if (searchScope instanceof GlobalSearchScope) {
      StringSearcher searcher = new StringSearcher(text, options.contains(Options.CASE_SENSITIVE_SEARCH), true,
                                                   searchContext == UsageSearchContext.IN_STRINGS,
                                                   options.contains(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE));

      return processElementsWithTextInGlobalScope(processor,
                                                  (GlobalSearchScope)searchScope,
                                                  searcher,
                                                  searchContext, options.contains(Options.CASE_SENSITIVE_SEARCH), containerName, progress
      );
    }
    LocalSearchScope scope = (LocalSearchScope)searchScope;
    PsiElement[] scopeElements = scope.getScope();
    final StringSearcher searcher = new StringSearcher(text, options.contains(Options.CASE_SENSITIVE_SEARCH), true,
                                                       searchContext == UsageSearchContext.IN_STRINGS,
                                                       options.contains(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE));
    ReadActionProcessor<PsiElement> localProcessor = new ReadActionProcessor<>() {
      @RequiredReadAction
      @Override
      public boolean processInReadAction(PsiElement scopeElement) {
        if (!scopeElement.isValid()) return true;
        if (!scopeElement.isPhysical() || scopeElement instanceof PsiCompiledElement) {
          scopeElement = scopeElement.getNavigationElement();
        }
        if (scopeElement instanceof PsiCompiledElement) {
          // can't scan text of the element
          return true;
        }
        if (scopeElement.getTextRange() == TextRange.EMPTY_RANGE) {
          // clients can put whatever they want to the LocalSearchScope. Skip what we can't process.
          LOG.debug("Element " + scopeElement + " of class " + scopeElement.getClass() + " has null range");
          return true;
        }
        return processor.execute(scopeElement, LowLevelSearchUtil.getTextOccurrencesInScope(scopeElement, searcher, progress), searcher);
      }

      @Override
      public String toString() {
        return processor.toString();
      }
    };
    return JobLauncher.getInstance().invokeConcurrentlyUnderProgress(Arrays.asList(scopeElements), progress, true, true, localProcessor);
  }

  @Nonnull
  private static ProgressIndicator getOrCreateIndicator() {
    ProgressIndicator progress = ProgressIndicatorProvider.getGlobalProgressIndicator();
    if (progress == null) progress = new EmptyProgressIndicator();
    progress.setIndeterminate(false);
    return progress;
  }

  static boolean shouldProcessInjectedPsi(@Nonnull SearchScope scope) {
    return !(scope instanceof LocalSearchScope) || !((LocalSearchScope)scope).isIgnoreInjectedPsi();
  }

  @Nonnull
  private static Predicate<PsiElement> localProcessor(@Nonnull final BulkOccurrenceProcessor processor,
                                                      @Nonnull final ProgressIndicator progress,
                                                      @Nonnull final StringSearcher searcher) {
    return new ReadActionProcessor<>() {
      @Override
      public boolean processInReadAction(PsiElement scopeElement) {
        if (scopeElement instanceof PsiCompiledElement) {
          // can't scan text of the element
          return true;
        }

        return scopeElement.isValid() &&
          processor.execute(scopeElement, LowLevelSearchUtil.getTextOccurrencesInScope(scopeElement, searcher, progress), searcher);
      }

      @Override
      public String toString() {
        return processor.toString();
      }
    };
  }

  private boolean processElementsWithTextInGlobalScope(@Nonnull BulkOccurrenceProcessor processor,
                                                       @Nonnull GlobalSearchScope scope,
                                                       @Nonnull StringSearcher searcher,
                                                       short searchContext,
                                                       boolean caseSensitively,
                                                       @Nullable String containerName,
                                                       @Nonnull ProgressIndicator progress) {
    progress.pushState();
    boolean result;
    try {
      progress.setText(PsiBundle.message("psi.scanning.files.progress"));

      String text = searcher.getPattern();
      Set<VirtualFile> fileSet = new HashSet<>();
      getFilesWithText(scope, searchContext, caseSensitively, text, fileSet);

      progress.setText(PsiBundle.message("psi.search.for.word.progress", text));

      Predicate<PsiElement> localProcessor = localProcessor(processor, progress, searcher);
      if (containerName != null) {
        List<VirtualFile> intersectionWithContainerFiles = new ArrayList<>();
        // intersectionWithContainerFiles holds files containing words from both `text` and `containerName`
        getFilesWithText(scope, searchContext, caseSensitively, text + " " + containerName, intersectionWithContainerFiles);
        if (!intersectionWithContainerFiles.isEmpty()) {
          int totalSize = fileSet.size();
          result = processPsiFileRoots(intersectionWithContainerFiles, totalSize, 0, progress, localProcessor);

          if (result) {
            fileSet.removeAll(intersectionWithContainerFiles);
            if (!fileSet.isEmpty()) {
              result =
                processPsiFileRoots(new ArrayList<>(fileSet), totalSize, intersectionWithContainerFiles.size(), progress, localProcessor);
            }
          }
          return result;
        }
      }
      result = fileSet.isEmpty() || processPsiFileRoots(new ArrayList<>(fileSet), fileSet.size(), 0, progress, localProcessor);
    }
    finally {
      progress.popState();
    }
    return result;
  }

  /**
   * @param files                 to scan for references in this pass.
   * @param totalSize             the number of files to scan in both passes. Can be different from {@code files.size()} in case of
   *                              two-pass scan, where we first scan files containing container name and then all the rest files.
   * @param alreadyProcessedFiles the number of files scanned in previous pass.
   * @return true if completed
   */
  private boolean processPsiFileRoots(@Nonnull List<VirtualFile> files,
                                      int totalSize,
                                      int alreadyProcessedFiles,
                                      @Nonnull ProgressIndicator progress,
                                      @Nonnull Predicate<? super PsiFile> localProcessor) {
    myManager.startBatchFilesProcessingMode();
    try {
      AtomicInteger counter = new AtomicInteger(alreadyProcessedFiles);
      AtomicBoolean stopped = new AtomicBoolean(false);

      return processFilesConcurrentlyDespiteWriteActions(myManager.getProject(), files, progress, stopped, vfile -> {
        TooManyUsagesStatus.getFrom(progress).pauseProcessingIfTooManyUsages();
        try {
          processVirtualFile(vfile, stopped, localProcessor);
        }
        catch (ProcessCanceledException | IndexNotReadyException e) {
          throw e;
        }
        catch (Throwable e) {
          LOG.error("Error during processing of: " + vfile.getName(), e);
          throw e;
        }
        if (progress.isRunning()) {
          double fraction = (double)counter.incrementAndGet() / totalSize;
          progress.setFraction(fraction);
        }
        return !stopped.get();
      });
    }
    finally {
      myManager.finishBatchFilesProcessingMode();
    }
  }

  @Override
  public boolean processFilesConcurrentlyDespiteWriteActions(@Nonnull Project project,
                                                             @Nonnull List<? extends VirtualFile> files,
                                                             @Nonnull ProgressIndicator progress,
                                                             @Nonnull AtomicBoolean stopped,
                                                             @Nonnull Predicate<? super VirtualFile> localProcessor) {
    return ContainerUtil.process(files, localProcessor);
  }

  private void processVirtualFile(@Nonnull VirtualFile vfile,
                                  @Nonnull AtomicBoolean stopped,
                                  @Nonnull Predicate<? super PsiFile> localProcessor) throws ApplicationUtil.CannotRunReadActionException {
    PsiFile file = ApplicationUtil.tryRunReadAction(() -> vfile.isValid() ? myManager.findFile(vfile) : null);
    if (file != null && !(file instanceof PsiBinaryFile)) {
      ApplicationUtil.tryRunReadAction(() -> {
        Project project = myManager.getProject();
        if (project.isDisposed()) throw new ProcessCanceledException();
        if (DumbService.isDumb(project)) throw ApplicationUtil.CannotRunReadActionException.create();

        List<PsiFile> psiRoots = file.getViewProvider().getAllFiles();
        Set<PsiFile> processed = new HashSet<>(psiRoots.size() * 2, (float)0.5);
        for (PsiFile psiRoot : psiRoots) {
          ProgressManager.checkCanceled();
          assert psiRoot != null : "One of the roots of file " + file + " is null. All roots: " + psiRoots + "; ViewProvider: " +
            file.getViewProvider() + "; Virtual file: " + file.getViewProvider().getVirtualFile();
          if (!processed.add(psiRoot)) continue;
          if (!psiRoot.isValid()) {
            continue;
          }

          if (!localProcessor.test(psiRoot)) {
            stopped.set(true);
            break;
          }
        }
      });
    }
  }

  private void getFilesWithText(@Nonnull GlobalSearchScope scope,
                                short searchContext,
                                boolean caseSensitively,
                                @Nonnull String text,
                                @Nonnull Collection<VirtualFile> result) {
    myManager.startBatchFilesProcessingMode();
    try {
      Predicate<VirtualFile> processor = Processors.cancelableCollectProcessor(result);
      boolean success = processFilesWithText(scope, searchContext, caseSensitively, text, processor);
      // success == false means exception in index
    }
    finally {
      myManager.finishBatchFilesProcessingMode();
    }
  }

  @Override
  public boolean processFilesWithText(@Nonnull GlobalSearchScope scope,
                                      short searchContext,
                                      boolean caseSensitively,
                                      @Nonnull String text,
                                      @Nonnull Predicate<VirtualFile> processor) {
    List<IdIndexEntry> entries = getWordEntries(text, caseSensitively);
    if (entries.isEmpty()) return true;

    Predicate<Integer> contextMatches = integer -> (integer & searchContext) != 0;
    return processFilesContainingAllKeys(myManager.getProject(), scope, contextMatches, entries, processor);
  }

  @Override
  @Nonnull
  public PsiFile[] findFilesWithPlainTextWords(@Nonnull String word) {
    return myCacheManager.getFilesWithWord(word, UsageSearchContext.IN_PLAIN_TEXT,
                                           GlobalSearchScope.projectScope(myManager.getProject()),
                                           true);
  }


  @Override
  public boolean processUsagesInNonJavaFiles(@Nonnull String qName,
                                             @Nonnull PsiNonJavaFileReferenceProcessor processor,
                                             @Nonnull GlobalSearchScope searchScope) {
    return processUsagesInNonJavaFiles(null, qName, processor, searchScope);
  }

  @Override
  public boolean processUsagesInNonJavaFiles(@Nullable PsiElement originalElement,
                                             @Nonnull String qName,
                                             @Nonnull PsiNonJavaFileReferenceProcessor processor,
                                             @Nonnull GlobalSearchScope initialScope) {
    if (qName.isEmpty()) {
      throw new IllegalArgumentException("Cannot search for elements with empty text. Element: " + originalElement + "; " + (originalElement == null ? null : originalElement
        .getClass()));
    }
    ProgressIndicator progress = getOrCreateIndicator();

    int dotIndex = qName.lastIndexOf('.');
    int dollarIndex = qName.lastIndexOf('$');
    int maxIndex = Math.max(dotIndex, dollarIndex);
    String wordToSearch = maxIndex >= 0 ? qName.substring(maxIndex + 1) : qName;
    ThrowableComputable<GlobalSearchScope, RuntimeException> action1 = () -> {
      if (originalElement != null && myManager.isInProject(originalElement) && initialScope.isSearchInLibraries()) {
        return initialScope.intersectWith(GlobalSearchScope.projectScope(myManager.getProject()));
      }
      return initialScope;
    };
    GlobalSearchScope theSearchScope = AccessRule.read(action1);
    PsiFile[] files = myDumbService.runReadActionInSmartMode(() -> myCacheManager.getFilesWithWord(wordToSearch,
                                                                                                   UsageSearchContext.IN_PLAIN_TEXT,
                                                                                                   theSearchScope,
                                                                                                   true));

    StringSearcher searcher = new StringSearcher(qName, true, true, false);

    progress.pushState();
    Ref<Boolean> cancelled = Ref.create(Boolean.FALSE);
    try {
      progress.setText(PsiBundle.message("psi.search.in.non.java.files.progress"));

      SearchScope useScope =
        originalElement == null ? null : myDumbService.runReadActionInSmartMode(() -> PsiSearchScopeUtil.getUseScope(originalElement));

      int patternLength = qName.length();
      for (int i = 0; i < files.length; i++) {
        progress.checkCanceled();
        PsiFile psiFile = files[i];
        if (psiFile instanceof PsiBinaryFile) continue;

        ThrowableComputable<CharSequence, RuntimeException> action = () -> psiFile.getViewProvider().getContents();
        CharSequence text = AccessRule.read(action);

        LowLevelSearchUtil.processTextOccurrences(text, 0, text.length(), searcher, progress, index -> {
          boolean isReferenceOK = myDumbService.runReadActionInSmartMode(() -> {
            PsiReference referenceAt = psiFile.findReferenceAt(index);
            return referenceAt == null || useScope == null || !PsiSearchScopeUtil.isInScope(useScope.intersectWith(initialScope), psiFile);
          });
          if (isReferenceOK && !processor.process(psiFile, index, index + patternLength)) {
            cancelled.set(Boolean.TRUE);
            return false;
          }

          return true;
        });
        if (cancelled.get()) break;
        progress.setFraction((double)(i + 1) / files.length);
      }
    }
    finally {
      progress.popState();
    }

    return !cancelled.get();
  }

  @Override
  public boolean processAllFilesWithWord(@Nonnull String word,
                                         @Nonnull GlobalSearchScope scope,
                                         @Nonnull Predicate<PsiFile> processor,
                                         boolean caseSensitively) {
    return myCacheManager.processFilesWithWord(processor, word, UsageSearchContext.IN_CODE, scope, caseSensitively);
  }

  @Override
  public boolean processAllFilesWithWordInText(@Nonnull String word,
                                               @Nonnull GlobalSearchScope scope,
                                               @Nonnull Predicate<PsiFile> processor,
                                               boolean caseSensitively) {
    return myCacheManager.processFilesWithWord(processor, word, UsageSearchContext.IN_PLAIN_TEXT, scope, caseSensitively);
  }

  @Override
  public boolean processAllFilesWithWordInComments(@Nonnull String word,
                                                   @Nonnull GlobalSearchScope scope,
                                                   @Nonnull Predicate<PsiFile> processor) {
    return myCacheManager.processFilesWithWord(processor, word, UsageSearchContext.IN_COMMENTS, scope, true);
  }

  @Override
  public boolean processAllFilesWithWordInLiterals(@Nonnull String word,
                                                   @Nonnull GlobalSearchScope scope,
                                                   @Nonnull Predicate<PsiFile> processor) {
    return myCacheManager.processFilesWithWord(processor, word, UsageSearchContext.IN_STRINGS, scope, true);
  }

  private static class RequestWithProcessor {
    @Nonnull
    private final PsiSearchRequest request;
    @Nonnull
    private Predicate<? super PsiReference> refProcessor;

    private RequestWithProcessor(@Nonnull PsiSearchRequest request, @Nonnull Predicate<? super PsiReference> processor) {
      this.request = request;
      refProcessor = processor;
    }

    private boolean uniteWith(@Nonnull RequestWithProcessor another) {
      if (request.equals(another.request)) {
        Predicate<? super PsiReference> myProcessor = refProcessor;
        if (myProcessor != another.refProcessor) {
          refProcessor = psiReference -> myProcessor.test(psiReference) && another.refProcessor.test(psiReference);
        }
        return true;
      }
      return false;
    }

    @Override
    public String toString() {
      return request.toString();
    }
  }

  @Override
  public boolean processRequests(@Nonnull SearchRequestCollector collector, @Nonnull Predicate<? super PsiReference> processor) {
    Map<SearchRequestCollector, Predicate<? super PsiReference>> collectors = new HashMap<>();
    collectors.put(collector, processor);

    ProgressIndicator progress = getOrCreateIndicator();
    appendCollectorsFromQueryRequests(collectors);
    boolean result;
    do {
      MultiMap<Set<IdIndexEntry>, RequestWithProcessor> globals = new MultiMap<>();
      List<Supplier<Boolean>> customs = new ArrayList<>();
      Set<RequestWithProcessor> locals = new LinkedHashSet<>();
      Map<RequestWithProcessor, Predicate<PsiElement>> localProcessors = new HashMap<>();
      distributePrimitives(collectors, locals, globals, customs, localProcessors, progress);
      result = processGlobalRequestsOptimized(globals, progress, localProcessors);
      if (result) {
        for (RequestWithProcessor local : locals) {
          result = processSingleRequest(local.request, local.refProcessor);
          if (!result) break;
        }
        if (result) {
          for (Supplier<Boolean> custom : customs) {
            result = custom.get();
            if (!result) break;
          }
        }
        if (!result) break;
      }
    }
    while (appendCollectorsFromQueryRequests(collectors));
    return result;
  }

  @Nonnull
  @Override
  public AsyncFuture<Boolean> processRequestsAsync(@Nonnull SearchRequestCollector collector, @Nonnull Predicate<PsiReference> processor) {
    return AsyncUtil.wrapBoolean(processRequests(collector, processor));
  }

  private static boolean appendCollectorsFromQueryRequests(@Nonnull Map<SearchRequestCollector, Predicate<? super PsiReference>> collectors) {
    boolean changed = false;
    Deque<SearchRequestCollector> queue = new LinkedList<>(collectors.keySet());
    while (!queue.isEmpty()) {
      SearchRequestCollector each = queue.removeFirst();
      for (QuerySearchRequest request : each.takeQueryRequests()) {
        request.runQuery();
        assert !collectors.containsKey(request.collector) || collectors.get(request.collector) == request.processor;
        collectors.put(request.collector, request.processor);
        queue.addLast(request.collector);
        changed = true;
      }
    }
    return changed;
  }

  private boolean processGlobalRequestsOptimized(@Nonnull MultiMap<Set<IdIndexEntry>, RequestWithProcessor> singles,
                                                 @Nonnull ProgressIndicator progress,
                                                 @Nonnull Map<RequestWithProcessor, Predicate<PsiElement>> localProcessors) {
    if (singles.isEmpty()) {
      return true;
    }

    if (singles.size() == 1) {
      Collection<? extends RequestWithProcessor> requests = singles.values();
      if (requests.size() == 1) {
        RequestWithProcessor theOnly = requests.iterator().next();
        return processSingleRequest(theOnly.request, theOnly.refProcessor);
      }
    }

    progress.pushState();
    progress.setText(PsiBundle.message("psi.scanning.files.progress"));
    boolean result;

    try {
      // intersectionCandidateFiles holds files containing words from all requests in `singles` and words in corresponding container names
      MultiMap<VirtualFile, RequestWithProcessor> intersectionCandidateFiles = createMultiMap();
      // restCandidateFiles holds files containing words from all requests in `singles` but EXCLUDING words in corresponding container names
      MultiMap<VirtualFile, RequestWithProcessor> restCandidateFiles = createMultiMap();
      collectFiles(singles, progress, intersectionCandidateFiles, restCandidateFiles);

      if (intersectionCandidateFiles.isEmpty() && restCandidateFiles.isEmpty()) {
        return true;
      }

      Set<String> allWords = new TreeSet<>();
      for (RequestWithProcessor singleRequest : localProcessors.keySet()) {
        allWords.add(singleRequest.request.word);
      }
      progress.setText(PsiBundle.message("psi.search.for.word.progress", getPresentableWordsDescription(allWords)));

      if (intersectionCandidateFiles.isEmpty()) {
        result = processCandidates(localProcessors, restCandidateFiles, progress, restCandidateFiles.size(), 0);
      }
      else {
        int totalSize = restCandidateFiles.size() + intersectionCandidateFiles.size();
        result = processCandidates(localProcessors, intersectionCandidateFiles, progress, totalSize, 0);
        if (result) {
          result = processCandidates(localProcessors, restCandidateFiles, progress, totalSize, intersectionCandidateFiles.size());
        }
      }
    }
    finally {
      progress.popState();
    }

    return result;
  }

  private boolean processCandidates(@Nonnull Map<RequestWithProcessor, Predicate<PsiElement>> localProcessors,
                                    @Nonnull MultiMap<VirtualFile, RequestWithProcessor> candidateFiles,
                                    @Nonnull ProgressIndicator progress,
                                    int totalSize,
                                    int alreadyProcessedFiles) {
    List<VirtualFile> files = new ArrayList<>(candidateFiles.keySet());

    return processPsiFileRoots(files, totalSize, alreadyProcessedFiles, progress, psiRoot -> {
      VirtualFile vfile = psiRoot.getVirtualFile();
      for (RequestWithProcessor singleRequest : candidateFiles.get(vfile)) {
        Predicate<PsiElement> localProcessor = localProcessors.get(singleRequest);
        if (!localProcessor.test(psiRoot)) {
          return false;
        }
      }
      return true;
    });
  }

  @Nonnull
  private static String getPresentableWordsDescription(@Nonnull Set<String> allWords) {
    StringBuilder result = new StringBuilder();
    for (String string : allWords) {
      if (string != null && !string.isEmpty()) {
        if (result.length() > 50) {
          result.append("...");
          break;
        }
        if (result.length() != 0) result.append(", ");
        result.append(string);
      }
    }
    return result.toString();
  }

  @Nonnull
  private static BulkOccurrenceProcessor adaptProcessor(@Nonnull PsiSearchRequest singleRequest,
                                                        @Nonnull Predicate<? super PsiReference> consumer) {
    SearchScope searchScope = singleRequest.searchScope;
    final boolean ignoreInjectedPsi = searchScope instanceof LocalSearchScope && ((LocalSearchScope)searchScope).isIgnoreInjectedPsi();
    final RequestResultProcessor wrapped = singleRequest.processor;
    return new BulkOccurrenceProcessor() {
      @Override
      public boolean execute(@Nonnull PsiElement scope, @Nonnull int[] offsetsInScope, @Nonnull StringSearcher searcher) {
        try {
          if (wrapped instanceof RequestResultProcessor.BulkResultProcessor) {
            return ((RequestResultProcessor.BulkResultProcessor)wrapped).processTextOccurrences(scope, offsetsInScope, consumer);
          }

          return LowLevelSearchUtil.processElementsAtOffsets(scope, searcher, !ignoreInjectedPsi,
                                                             getOrCreateIndicator(), offsetsInScope,
                                                             (element, offsetInElement) -> {
                                                               if (ignoreInjectedPsi && element instanceof PsiLanguageInjectionHost)
                                                                 return true;
                                                               return wrapped.processTextOccurrence(element, offsetInElement, consumer);
                                                             });
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception | Error e) {
          LOG.error(e);
          return true;
        }
      }

      @Override
      public String toString() {
        return consumer.toString();
      }
    };
  }

  private void collectFiles(@Nonnull MultiMap<Set<IdIndexEntry>, RequestWithProcessor> singles,
                            @Nonnull ProgressIndicator progress,
                            @Nonnull MultiMap<VirtualFile, RequestWithProcessor> intersectionResult,
                            @Nonnull MultiMap<VirtualFile, RequestWithProcessor> restResult) {
    for (Map.Entry<Set<IdIndexEntry>, Collection<RequestWithProcessor>> entry : singles.entrySet()) {
      Set<IdIndexEntry> keys = entry.getKey();
      if (keys.isEmpty()) {
        continue;
      }

      Collection<RequestWithProcessor> processors = entry.getValue();
      GlobalSearchScope commonScope = uniteScopes(processors);
      Set<VirtualFile> intersectionWithContainerNameFiles = intersectionWithContainerNameFiles(commonScope, processors, keys);

      List<VirtualFile> result = new ArrayList<>();
      Predicate<VirtualFile> processor = Processors.cancelableCollectProcessor(result);
      processFilesContainingAllKeys(myManager.getProject(), commonScope, null, keys, processor);
      for (VirtualFile file : result) {
        progress.checkCanceled();
        for (IdIndexEntry indexEntry : keys) {
          myDumbService.runReadActionInSmartMode(
            () -> FileBasedIndex.getInstance().processValues(IdIndex.NAME, indexEntry, file, (file1, value) -> {
              int mask = value.intValue();
              for (RequestWithProcessor single : processors) {
                PsiSearchRequest request = single.request;
                if ((mask & request.searchContext) != 0 && request.searchScope.contains(file1)) {
                  MultiMap<VirtualFile, RequestWithProcessor> result1 =
                    intersectionWithContainerNameFiles == null || !intersectionWithContainerNameFiles.contains(file1) ? restResult : intersectionResult;
                  result1.putValue(file1, single);
                }
              }
              return true;
            }, commonScope));
        }
      }
    }
  }

  /**
   * @param commonScope
   * @param data
   * @param keys
   * @return null means we did not find common container files
   */
  @Nullable
  private Set<VirtualFile> intersectionWithContainerNameFiles(@Nonnull GlobalSearchScope commonScope,
                                                              @Nonnull Collection<RequestWithProcessor> data,
                                                              @Nonnull Set<IdIndexEntry> keys) {
    String commonName = null;
    short searchContext = 0;
    boolean caseSensitive = true;
    for (RequestWithProcessor r : data) {
      String containerName = r.request.containerName;
      if (containerName != null) {
        if (commonName == null) {
          commonName = containerName;
          searchContext = r.request.searchContext;
          caseSensitive = r.request.caseSensitive;
        }
        else if (commonName.equals(containerName)) {
          searchContext |= r.request.searchContext;
          caseSensitive &= r.request.caseSensitive;
        }
        else {
          return null;
        }
      }
    }
    if (commonName == null) return null;

    List<IdIndexEntry> entries = getWordEntries(commonName, caseSensitive);
    if (entries.isEmpty()) return null;
    entries.addAll(keys); // should find words from both text and container names

    short finalSearchContext = searchContext;
    Predicate<Integer> contextMatches = context -> (context.intValue() & finalSearchContext) != 0;
    Set<VirtualFile> containerFiles = new HashSet<>();
    Predicate<VirtualFile> processor = Processors.cancelableCollectProcessor(containerFiles);
    processFilesContainingAllKeys(myManager.getProject(), commonScope, contextMatches, entries, processor);

    return containerFiles;
  }

  @Nonnull
  private static MultiMap<VirtualFile, RequestWithProcessor> createMultiMap() {
    // usually there is just one request
    return MultiMap.createSmart();
  }

  @Nonnull
  private static GlobalSearchScope uniteScopes(@Nonnull Collection<RequestWithProcessor> requests) {
    Set<GlobalSearchScope> scopes = ContainerUtil.map2LinkedSet(requests, r -> (GlobalSearchScope)r.request.searchScope);
    return GlobalSearchScope.union(scopes.toArray(new GlobalSearchScope[scopes.size()]));
  }

  private static void distributePrimitives(@Nonnull Map<SearchRequestCollector, Predicate<? super PsiReference>> collectors,
                                           @Nonnull Set<RequestWithProcessor> locals,
                                           @Nonnull MultiMap<Set<IdIndexEntry>, RequestWithProcessor> globals,
                                           @Nonnull List<Supplier<Boolean>> customs,
                                           @Nonnull Map<RequestWithProcessor, Predicate<PsiElement>> localProcessors,
                                           @Nonnull ProgressIndicator progress) {
    for (Map.Entry<SearchRequestCollector, Predicate<? super PsiReference>> entry : collectors.entrySet()) {
      Predicate<? super PsiReference> processor = entry.getValue();
      SearchRequestCollector collector = entry.getKey();
      for (PsiSearchRequest primitive : collector.takeSearchRequests()) {
        SearchScope scope = primitive.searchScope;
        if (scope instanceof LocalSearchScope) {
          registerRequest(locals, primitive, processor);
        }
        else {
          Set<IdIndexEntry> key = new HashSet<>(getWordEntries(primitive.word, primitive.caseSensitive));
          registerRequest(globals.getModifiable(key), primitive, processor);
        }
      }
      for (Predicate<Predicate<? super PsiReference>> customAction : collector.takeCustomSearchActions()) {
        customs.add(() -> customAction.test(processor));
      }
    }

    for (Map.Entry<Set<IdIndexEntry>, Collection<RequestWithProcessor>> entry : globals.entrySet()) {
      for (RequestWithProcessor singleRequest : entry.getValue()) {
        PsiSearchRequest primitive = singleRequest.request;
        StringSearcher searcher = new StringSearcher(primitive.word, primitive.caseSensitive, true, false);
        BulkOccurrenceProcessor adapted = adaptProcessor(primitive, singleRequest.refProcessor);

        Predicate<PsiElement> localProcessor = localProcessor(adapted, progress, searcher);

        assert !localProcessors.containsKey(singleRequest) || localProcessors.get(singleRequest) == localProcessor;
        localProcessors.put(singleRequest, localProcessor);
      }
    }
  }

  private static void registerRequest(@Nonnull Collection<RequestWithProcessor> collection,
                                      @Nonnull PsiSearchRequest primitive,
                                      @Nonnull Predicate<? super PsiReference> processor) {
    RequestWithProcessor singleRequest = new RequestWithProcessor(primitive, processor);

    for (RequestWithProcessor existing : collection) {
      if (existing.uniteWith(singleRequest)) {
        return;
      }
    }
    collection.add(singleRequest);
  }

  private boolean processSingleRequest(@Nonnull PsiSearchRequest single, @Nonnull Predicate<? super PsiReference> consumer) {
    EnumSet<Options> options = EnumSet.of(Options.PROCESS_ONLY_JAVA_IDENTIFIERS_IF_POSSIBLE);
    if (single.caseSensitive) options.add(Options.CASE_SENSITIVE_SEARCH);
    if (shouldProcessInjectedPsi(single.searchScope)) options.add(Options.PROCESS_INJECTED_PSI);

    return bulkProcessElementsWithWord(single.searchScope, single.word, single.searchContext, options, single.containerName,
                                       adaptProcessor(single, consumer)
    );
  }

  @Nonnull
  @Override
  public SearchCostResult isCheapEnoughToSearch(@Nonnull String name,
                                                @Nonnull GlobalSearchScope scope,
                                                @Nullable final PsiFile fileToIgnoreOccurrencesIn,
                                                @Nullable ProgressIndicator progress) {
    final AtomicInteger count = new AtomicInteger();
    final ProgressIndicator indicator = progress == null ? new EmptyProgressIndicator() : progress;
    Predicate<VirtualFile> processor = new Predicate<>() {
      private final VirtualFile virtualFileToIgnoreOccurrencesIn =
        fileToIgnoreOccurrencesIn == null ? null : fileToIgnoreOccurrencesIn.getVirtualFile();

      @Override
      public boolean test(VirtualFile file) {
        indicator.checkCanceled();
        if (Comparing.equal(file, virtualFileToIgnoreOccurrencesIn)) return true;
        int value = count.incrementAndGet();
        return value < 10;
      }
    };
    List<IdIndexEntry> keys = getWordEntries(name, true);
    boolean cheap = keys.isEmpty() || processFilesContainingAllKeys(myManager.getProject(), scope, null, keys, processor);

    if (!cheap) {
      return SearchCostResult.TOO_MANY_OCCURRENCES;
    }

    return count.get() == 0 ? SearchCostResult.ZERO_OCCURRENCES : SearchCostResult.FEW_OCCURRENCES;
  }

  private static boolean processFilesContainingAllKeys(@Nonnull Project project,
                                                       @Nonnull GlobalSearchScope scope,
                                                       @Nullable Predicate<Integer> checker,
                                                       @Nonnull Collection<IdIndexEntry> keys,
                                                       @Nonnull Predicate<? super VirtualFile> processor) {
    FileIndexFacade index = FileIndexFacade.getInstance(project);
    return DumbService.getInstance(project).runReadActionInSmartMode(
      () -> FileBasedIndex.getInstance().processFilesContainingAllKeys(IdIndex.NAME, keys, scope, checker,
                                                                       file -> !index.shouldBeFound(scope, file) || processor.test(file)));
  }

  @Nonnull
  private static List<IdIndexEntry> getWordEntries(@Nonnull String name, boolean caseSensitively) {
    List<String> words = StringUtil.getWordsInStringLongestFirst(name);
    if (words.isEmpty()) {
      String trimmed = name.trim();
      if (StringUtil.isNotEmpty(trimmed)) {
        words = Collections.singletonList(trimmed);
      }
    }
    if (words.isEmpty()) return Collections.emptyList();
    return ContainerUtil.map2List(words, word -> new IdIndexEntry(word, caseSensitively));
  }
}
