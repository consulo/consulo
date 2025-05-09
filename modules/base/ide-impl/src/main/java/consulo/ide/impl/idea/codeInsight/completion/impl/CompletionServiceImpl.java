// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.ide.impl.idea.codeInsight.completion.*;
import consulo.ide.impl.idea.codeInsight.lookup.*;
import consulo.application.ApplicationManager;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.Classifier;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.impl.internal.completion.CompletionData;
import consulo.language.editor.impl.internal.completion.LookupStatisticsWeigher;
import consulo.logging.Logger;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.Weigher;
import consulo.language.WeighingService;
import consulo.language.impl.DebugUtil;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * @author peter
 */
@Singleton
@ServiceImpl
public final class CompletionServiceImpl extends CompletionService {
  private static final Logger LOG = Logger.getInstance(CompletionServiceImpl.class);
  private static volatile CompletionPhase ourPhase = CompletionPhase.NoCompletion;
  private static Throwable ourPhaseTrace;

  @Nullable
  private CompletionProcess myApiCompletionProcess;

  @Inject
  public CompletionServiceImpl(Application application) {
    application.getMessageBus().connect().subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectClosing(@Nonnull Project project) {
        CompletionProgressIndicator indicator = getCurrentCompletionProgressIndicator();
        if (indicator != null && indicator.getProject() == project) {
          indicator.closeAndFinish(true);
          setCompletionPhase(CompletionPhase.NoCompletion);
        }
        else if (indicator == null) {
          setCompletionPhase(CompletionPhase.NoCompletion);
        }
      }
    });
  }

  @Override
  public void performCompletion(final CompletionParameters parameters, final Consumer<? super CompletionResult> consumer) {
    myApiCompletionProcess = parameters.getProcess();
    try {
      super.performCompletion(parameters, consumer);
    }
    finally {
      myApiCompletionProcess = null;
    }
  }

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass"})
  public static CompletionServiceImpl getCompletionService() {
    return (CompletionServiceImpl)CompletionService.getCompletionService();
  }

  @Override
  public void setAdvertisementText(@Nullable final String text) {
    if (text == null) return;
    final CompletionProgressIndicator completion = getCurrentCompletionProgressIndicator();
    if (completion != null) {
      completion.addAdvertisement(text, null);
    }
  }

  @Override
  protected String suggestPrefix(CompletionParameters parameters) {
    final PsiElement position = parameters.getPosition();
    final int offset = parameters.getOffset();
    TextRange range = position.getTextRange();
    assert range.containsOffset(offset) : position + "; " + offset + " not in " + range;
    //noinspection deprecation
    return CompletionData.findPrefixStatic(position, offset);
  }

  @Override
  @Nonnull
  protected PrefixMatcher createMatcher(String prefix, boolean typoTolerant) {
    return createMatcher(prefix, true, typoTolerant);
  }

  @Nonnull
  private static CamelHumpMatcher createMatcher(String prefix, boolean caseSensitive, boolean typoTolerant) {
    return new CamelHumpMatcher(prefix, caseSensitive, typoTolerant);
  }

  @Override
  protected CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer, @Nonnull CompletionContributor contributor, PrefixMatcher matcher) {
    return new CompletionResultSetImpl(consumer, matcher, contributor, parameters, defaultSorter(parameters, matcher), null);
  }

  @Override
  public CompletionProcess getCurrentCompletion() {
    CompletionProgressIndicator indicator = getCurrentCompletionProgressIndicator();
    return indicator != null ? indicator : myApiCompletionProcess;
  }

  public static CompletionProgressIndicator getCurrentCompletionProgressIndicator() {
    if (isPhase(CompletionPhase.BgCalculation.class, CompletionPhase.ItemsCalculated.class, CompletionPhase.CommittingDocuments.class, CompletionPhase.Synchronous.class)) {
      return ourPhase.indicator;
    }
    return null;
  }

  private static class CompletionResultSetImpl extends CompletionResultSet {
    private final CompletionParameters myParameters;
    private final CompletionSorterImpl mySorter;
    @Nullable
    private final CompletionResultSetImpl myOriginal;

    CompletionResultSetImpl(Consumer<? super CompletionResult> consumer, PrefixMatcher prefixMatcher,
                            CompletionContributor contributor, CompletionParameters parameters,
                            @Nonnull CompletionSorterImpl sorter, @Nullable CompletionResultSetImpl original) {
      super(prefixMatcher, consumer, contributor);
      myParameters = parameters;
      mySorter = sorter;
      myOriginal = original;
    }

    @Override
    public void addAllElements(@Nonnull Iterable<? extends LookupElement> elements) {
      CompletionThreadingBase.withBatchUpdate(() -> super.addAllElements(elements), myParameters.getProcess());
    }

    @Override
    public void addElement(@Nonnull final LookupElement element) {
      ProgressManager.checkCanceled();
      if (!element.isValid()) {
        LOG.error("Invalid lookup element: " + element + " of " + element.getClass() + " in " + myParameters.getOriginalFile() + " of " + myParameters.getOriginalFile().getClass());
        return;
      }

      CompletionResult matched = CompletionResult.wrap(element, getPrefixMatcher(), mySorter);
      if (matched != null) {
        passResult(matched);
      }
    }

    @Override
    @Nonnull
    public CompletionResultSet withPrefixMatcher(@Nonnull final PrefixMatcher matcher) {
      if (matcher.equals(getPrefixMatcher())) {
        return this;
      }

      return new CompletionResultSetImpl(getConsumer(), matcher, myContributor, myParameters, mySorter, this);
    }

    @Override
    public void stopHere() {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Completion stopped\n" + DebugUtil.currentStackTrace());
      }
      super.stopHere();
      if (myOriginal != null) {
        myOriginal.stopHere();
      }
    }

    @Override
    @Nonnull
    public CompletionResultSet withPrefixMatcher(@Nonnull final String prefix) {
      return withPrefixMatcher(getPrefixMatcher().cloneWithPrefix(prefix));
    }

    @Nonnull
    @Override
    public CompletionResultSet withRelevanceSorter(@Nonnull CompletionSorter sorter) {
      return new CompletionResultSetImpl(getConsumer(), getPrefixMatcher(), myContributor, myParameters, (CompletionSorterImpl)sorter, this);
    }

    @Override
    public void addLookupAdvertisement(@Nonnull String text) {
      getCompletionService().setAdvertisementText(text);
    }

    @Nonnull
    @Override
    public CompletionResultSet caseInsensitive() {
      PrefixMatcher matcher = getPrefixMatcher();
      boolean typoTolerant = matcher instanceof CamelHumpMatcher && ((CamelHumpMatcher)matcher).isTypoTolerant();
      return withPrefixMatcher(createMatcher(matcher.getPrefix(), false, typoTolerant));
    }

    @Override
    public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
      CompletionProcess process = myParameters.getProcess();
      if (process instanceof CompletionProgressIndicator) {
        ((CompletionProgressIndicator)process).addWatchedPrefix(myParameters.getOffset() - getPrefixMatcher().getPrefix().length(), prefixCondition);
      }
    }

    @Override
    public void restartCompletionWhenNothingMatches() {
      CompletionProcess process = myParameters.getProcess();
      if (process instanceof CompletionProgressIndicator) {
        ((CompletionProgressIndicator)process).getLookup().setStartCompletionWhenNothingMatches(true);
      }
    }
  }

  @SafeVarargs
  public static void assertPhase(@Nonnull Class<? extends CompletionPhase>... possibilities) {
    if (!isPhase(possibilities)) {
      LOG.error(ourPhase + "; set at " + ExceptionUtil.getThrowableText(ourPhaseTrace));
    }
  }

  @SafeVarargs
  public static boolean isPhase(@Nonnull Class<? extends CompletionPhase>... possibilities) {
    CompletionPhase phase = getCompletionPhase();
    for (Class<? extends CompletionPhase> possibility : possibilities) {
      if (possibility.isInstance(phase)) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  public static void setCompletionPhase(@Nonnull CompletionPhase phase) {
    UIAccess.assertIsUIThread();
    CompletionPhase oldPhase = getCompletionPhase();
    CompletionProgressIndicator oldIndicator = oldPhase.indicator;
    if (oldIndicator != null && !(phase instanceof CompletionPhase.BgCalculation)) {
      LOG.assertTrue(!oldIndicator.isRunning() || oldIndicator.isCanceled(), "don't change phase during running completion: oldPhase=" + oldPhase);
    }
    boolean wasCompletionRunning = isRunningPhase(oldPhase);
    boolean isCompletionRunning = isRunningPhase(phase);
    if (isCompletionRunning != wasCompletionRunning) {
      ApplicationManager.getApplication().getMessageBus().syncPublisher(CompletionPhaseListener.class).completionPhaseChanged(isCompletionRunning);
    }

    Disposer.dispose(oldPhase);
    ourPhase = phase;
    ourPhaseTrace = new Throwable();
  }

  private static boolean isRunningPhase(@Nonnull CompletionPhase phase) {
    return phase != CompletionPhase.NoCompletion && !(phase instanceof CompletionPhase.ZombiePhase) && !(phase instanceof CompletionPhase.ItemsCalculated);
  }


  public static CompletionPhase getCompletionPhase() {
    return ourPhase;
  }

  @Override
  public CompletionSorterImpl defaultSorter(CompletionParameters parameters, final PrefixMatcher matcher) {
    final CompletionLocation location = new CompletionLocation(parameters);

    CompletionSorterImpl sorter = emptySorter();
    sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new LiveTemplateWeigher()));
    sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new PreferStartMatching()));

    for (final Weigher weigher : WeighingService.getWeighers(CompletionService.RELEVANCE_KEY)) {
      final String id = weigher.toString();
      if ("prefix".equals(id)) {
        sorter = sorter.withClassifier(CompletionSorterImpl.weighingFactory(new RealPrefixMatchingWeigher()));
      }
      else if ("stats".equals(id)) {
        sorter = sorter.withClassifier(new ClassifierFactory<LookupElement>("stats") {
          @Override
          public Classifier<LookupElement> createClassifier(Classifier<LookupElement> next) {
            return new LookupStatisticsWeigher(location, next);
          }
        });
      }
      else {
        sorter = sorter.weigh(new LookupElementWeigher(id, true, false) {
          @Override
          public Comparable weigh(@Nonnull LookupElement element) {
            //noinspection unchecked
            return weigher.weigh(element, location);
          }
        });
      }
    }

    return sorter.withClassifier("priority", true, new ClassifierFactory<LookupElement>("liftShorter") {
      @Override
      public Classifier<LookupElement> createClassifier(final Classifier<LookupElement> next) {
        return new LiftShorterItemsClassifier("liftShorter", next, new LiftShorterItemsClassifier.LiftingCondition(), false);
      }
    });
  }

  @Override
  public CompletionSorterImpl emptySorter() {
    return new CompletionSorterImpl(new ArrayList<>());
  }
}
