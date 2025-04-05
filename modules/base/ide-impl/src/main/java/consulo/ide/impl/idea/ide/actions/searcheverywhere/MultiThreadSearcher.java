// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.application.Application;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.impl.internal.progress.SensitiveProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.util.ConcurrencyUtil;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static consulo.ide.impl.idea.ide.actions.searcheverywhere.SEResultsEqualityProvider.SEEqualElementsActionType.REPLACE;
import static consulo.ide.impl.idea.ide.actions.searcheverywhere.SEResultsEqualityProvider.SEEqualElementsActionType.SKIP;

/**
 * @author msokolov
 */
class MultiThreadSearcher implements SESearcher {
    private static final Logger LOG = Logger.getInstance(MultiThreadSearcher.class);

    @Nonnull
    private final Listener myListener;
    @Nonnull
    private final Executor myNotificationExecutor;
    @Nonnull
    private final SEResultsEqualityProvider myEqualityProvider;

    /**
     * Creates MultiThreadSearcher with search results {@link Listener} and specifies executor which going to be used to call listener methods.
     * Use this constructor when you for example need to receive listener events only in AWT thread
     *
     * @param listener             {@link Listener} to get notifications about searching process
     * @param notificationExecutor searcher guarantees that all listener methods will be called only through this executor
     * @param equalityProviders    collection of equality providers that checks if found elements are already in the search results
     */
    MultiThreadSearcher(
        @Nonnull Listener listener,
        @Nonnull Executor notificationExecutor,
        @Nonnull Collection<? extends SEResultsEqualityProvider> equalityProviders
    ) {
        myListener = listener;
        myNotificationExecutor = notificationExecutor;
        myEqualityProvider = SEResultsEqualityProvider.composite(equalityProviders);
    }

    @Override
    public ProgressIndicator search(
        @Nonnull Map<? extends SearchEverywhereContributor<?>, Integer> contributorsAndLimits,
        @Nonnull String pattern
    ) {
        LOG.debug("Search started for pattern [", pattern, "]");

        Collection<? extends SearchEverywhereContributor<?>> contributors = contributorsAndLimits.keySet();
        if (pattern.isEmpty()) {
            if (Application.get().isUnitTestMode()) {
                contributors = Collections.emptySet(); //empty search string is not allowed for tests
            }
            else {
                contributors = ContainerUtil.filter(contributors, SearchEverywhereContributor::isEmptyPatternSupported);
            }
        }

        ProgressIndicator indicator;
        FullSearchResultsAccumulator accumulator;
        if (!contributors.isEmpty()) {
            CountDownLatch latch = new CountDownLatch(contributors.size());
            ProgressIndicatorWithCancelListener indicatorWithCancelListener = new ProgressIndicatorWithCancelListener();
            accumulator = new FullSearchResultsAccumulator(
                contributorsAndLimits,
                myEqualityProvider,
                myListener,
                myNotificationExecutor,
                indicatorWithCancelListener
            );

            for (SearchEverywhereContributor<?> contributor : contributors) {
                Runnable task = createSearchTask(pattern, accumulator, indicatorWithCancelListener, contributor, latch::countDown);
                Application.get().executeOnPooledThread(task);
            }

            Runnable finisherTask = createFinisherTask(latch, accumulator, indicatorWithCancelListener);
            Future<?> finisherFeature = Application.get().executeOnPooledThread(finisherTask);
            indicatorWithCancelListener.setCancelCallback(() -> {
                accumulator.stop();
                finisherFeature.cancel(true);
            });
            indicator = indicatorWithCancelListener;
        }
        else {
            indicator = new ProgressIndicatorBase();
            accumulator =
                new FullSearchResultsAccumulator(contributorsAndLimits, myEqualityProvider, myListener, myNotificationExecutor, indicator);
        }

        indicator.start();
        if (contributors.isEmpty()) {
            indicator.stop();
            accumulator.searchFinished();
        }

        return indicator;
    }

    @Override
    public ProgressIndicator findMoreItems(
        @Nonnull Map<? extends SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
        @Nonnull String pattern,
        @Nonnull SearchEverywhereContributor<?> contributor,
        int newLimit
    ) {
        ProgressIndicator indicator = new ProgressIndicatorBase();
        ResultsAccumulator accumulator = new ShowMoreResultsAccumulator(
            alreadyFound,
            myEqualityProvider,
            contributor,
            newLimit,
            myListener,
            myNotificationExecutor,
            indicator
        );
        indicator.start();
        Runnable task = createSearchTask(pattern, accumulator, indicator, contributor, indicator::stop);
        Application.get().executeOnPooledThread(task);

        return indicator;
    }

    @Nonnull
    private static Runnable createSearchTask(
        String pattern,
        ResultsAccumulator accumulator,
        ProgressIndicator indicator,
        SearchEverywhereContributor<?> contributor,
        Runnable finalCallback
    ) {
        //noinspection unchecked
        ContributorSearchTask<?> task =
            new ContributorSearchTask<>((SearchEverywhereContributor<Object>)contributor, pattern, accumulator, indicator, finalCallback);
        return ConcurrencyUtil.underThreadNameRunnable("SE-SearchTask", task);
    }

    private static Runnable createFinisherTask(
        CountDownLatch latch,
        FullSearchResultsAccumulator accumulator,
        ProgressIndicator indicator
    ) {
        return ConcurrencyUtil.underThreadNameRunnable("SE-FinisherTask", () -> {
            try {
                latch.await();
                if (!indicator.isCanceled()) {
                    accumulator.searchFinished();
                }
                indicator.stop();
            }
            catch (InterruptedException e) {
                LOG.debug("Finisher interrupted before search process is finished");
            }
        });
    }

    private static class ContributorSearchTask<Item> implements Runnable {
        private final ResultsAccumulator myAccumulator;
        private final Runnable finishCallback;

        private final SearchEverywhereContributor<Item> myContributor;
        private final String myPattern;
        private final ProgressIndicator myIndicator;

        private ContributorSearchTask(
            SearchEverywhereContributor<Item> contributor,
            String pattern,
            ResultsAccumulator accumulator,
            ProgressIndicator indicator,
            Runnable callback
        ) {
            myContributor = contributor;
            myPattern = pattern;
            myAccumulator = accumulator;
            myIndicator = indicator;
            finishCallback = callback;
        }


        @Override
        public void run() {
            LOG.debug("Search task started for contributor ", myContributor);
            try {
                boolean repeat;
                do {
                    ProgressIndicator wrapperIndicator = new SensitiveProgressWrapper(myIndicator);
                    try {
                        Runnable runnable = (myContributor instanceof WeightedSearchEverywhereContributor)
                            ? () -> ((WeightedSearchEverywhereContributor<Item>)myContributor).fetchWeightedElements(
                                myPattern,
                                wrapperIndicator,
                                descriptor -> processFoundItem(descriptor.getItem(), descriptor.getWeight(), wrapperIndicator)
                            )
                            : () -> myContributor.fetchElements(
                                myPattern,
                                wrapperIndicator,
                                element -> {
                                    int priority = myContributor.getElementPriority(element, myPattern);
                                    return processFoundItem(element, priority, wrapperIndicator);
                                }
                            );

                        ProgressManager.getInstance().runProcess(runnable, wrapperIndicator);
                    }
                    catch (ProcessCanceledException ignore) {
                    }
                    repeat = !myIndicator.isCanceled() && wrapperIndicator.isCanceled();
                }
                while (repeat);

                if (myIndicator.isCanceled()) {
                    return;
                }
                myAccumulator.contributorFinished(myContributor);
            }
            finally {
                finishCallback.run();
            }
            LOG.debug("Search task finished for contributor ", myContributor);
        }

        private boolean processFoundItem(Item element, int priority, ProgressIndicator wrapperIndicator) {
            try {
                if (element == null) {
                    LOG.debug("Skip null element");
                    return true;
                }

                boolean added = myAccumulator.addElement(element, myContributor, priority, wrapperIndicator);
                if (!added) {
                    myAccumulator.setContributorHasMore(myContributor, true);
                }
                return added;
            }
            catch (InterruptedException e) {
                LOG.warn("Search task was interrupted");
                return false;
            }
        }
    }

    private static abstract class ResultsAccumulator {
        protected final Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> sections;
        protected final MultiThreadSearcher.Listener myListener;
        protected final Executor myNotificationExecutor;
        protected final SEResultsEqualityProvider myEqualityProvider;
        protected final ProgressIndicator myProgressIndicator;

        ResultsAccumulator(
            Map<SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> sections,
            SEResultsEqualityProvider equalityProvider,
            Listener listener,
            Executor notificationExecutor,
            ProgressIndicator progressIndicator
        ) {
            this.sections = sections;
            myEqualityProvider = equalityProvider;
            myListener = listener;
            myNotificationExecutor = notificationExecutor;
            myProgressIndicator = progressIndicator;
        }

        protected Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> getActionsWithOtherElements(
            SearchEverywhereFoundElementInfo newElement
        ) {
            Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> res =
                new EnumMap<>(SEResultsEqualityProvider.SEEqualElementsActionType.class);
            res.put(REPLACE, new ArrayList<>());
            res.put(SKIP, new ArrayList<>());
            sections.values().stream().flatMap(Collection::stream).forEach(info -> {
                SEResultsEqualityProvider.SEEqualElementsActionType action = myEqualityProvider.compareItems(newElement, info);
                if (action != SEResultsEqualityProvider.SEEqualElementsActionType.DO_NOTHING) {
                    res.get(action).add(info);
                }
            });

            return res;
        }

        protected void runInNotificationExecutor(Runnable runnable) {
            myNotificationExecutor.execute(() -> {
                if (!myProgressIndicator.isCanceled()) {
                    runnable.run();
                }
            });
        }

        public abstract boolean addElement(
            Object element,
            SearchEverywhereContributor<?> contributor,
            int priority,
            ProgressIndicator indicator
        ) throws InterruptedException;

        public abstract void contributorFinished(SearchEverywhereContributor<?> contributor);

        public abstract void setContributorHasMore(SearchEverywhereContributor<?> contributor, boolean hasMore);
    }

    private static class ShowMoreResultsAccumulator extends ResultsAccumulator {
        private final SearchEverywhereContributor<?> myExpandedContributor;
        private final int myNewLimit;
        private volatile boolean hasMore;

        ShowMoreResultsAccumulator(
            Map<? extends SearchEverywhereContributor<?>, Collection<SearchEverywhereFoundElementInfo>> alreadyFound,
            SEResultsEqualityProvider equalityProvider,
            SearchEverywhereContributor<?> contributor,
            int newLimit,
            Listener listener,
            Executor notificationExecutor,
            ProgressIndicator progressIndicator
        ) {
            super(new ConcurrentHashMap<>(alreadyFound), equalityProvider, listener, notificationExecutor, progressIndicator);
            myExpandedContributor = contributor;
            myNewLimit = newLimit;
        }

        @Override
        public boolean addElement(Object element, SearchEverywhereContributor<?> contributor, int priority, ProgressIndicator indicator) {
            assert contributor == myExpandedContributor; // Only expanded contributor items allowed

            Collection<SearchEverywhereFoundElementInfo> section = sections.get(contributor);
            SearchEverywhereFoundElementInfo newElementInfo = new SearchEverywhereFoundElementInfo(element, priority, contributor);

            if (section.size() >= myNewLimit) {
                return false;
            }

            Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> otherElementsMap =
                getActionsWithOtherElements(newElementInfo);
            if (otherElementsMap.get(REPLACE).isEmpty() && !otherElementsMap.get(SKIP).isEmpty()) {
                LOG.debug(String.format(
                    "Element %s for contributor %s was skipped",
                    element.toString(),
                    contributor.getSearchProviderId()
                ));
                return true;
            }

            section.add(newElementInfo);
            runInNotificationExecutor(() -> myListener.elementsAdded(Collections.singletonList(newElementInfo)));

            List<SearchEverywhereFoundElementInfo> toRemove = new ArrayList<>(otherElementsMap.get(REPLACE));
            toRemove.forEach(info -> {
                Collection<SearchEverywhereFoundElementInfo> list = sections.get(info.getContributor());
                list.remove(info);
                LOG.debug(String.format(
                    "Element %s for contributor %s is removed",
                    info.getElement().toString(),
                    info.getContributor().getSearchProviderId()
                ));
            });
            runInNotificationExecutor(() -> myListener.elementsRemoved(toRemove));
            return true;
        }

        @Override
        public void setContributorHasMore(SearchEverywhereContributor<?> contributor, boolean hasMore) {
            assert contributor == myExpandedContributor; // Only expanded contributor items allowed
            this.hasMore = hasMore;

        }

        @Override
        public void contributorFinished(SearchEverywhereContributor<?> contributor) {
            runInNotificationExecutor(() -> myListener.searchFinished(Collections.singletonMap(contributor, hasMore)));
        }
    }

    private static class FullSearchResultsAccumulator extends ResultsAccumulator {
        private final Map<? extends SearchEverywhereContributor<?>, Integer> sectionsLimits;
        private final Map<? extends SearchEverywhereContributor<?>, Condition> conditionsMap;
        private final Map<SearchEverywhereContributor<?>, Boolean> hasMoreMap = new ConcurrentHashMap<>();
        private final Set<SearchEverywhereContributor<?>> finishedContributorsSet = ContainerUtil.newConcurrentSet();
        private final Lock lock = new ReentrantLock();
        private volatile boolean mySearchFinished = false;

        FullSearchResultsAccumulator(
            Map<? extends SearchEverywhereContributor<?>, Integer> contributorsAndLimits,
            SEResultsEqualityProvider equalityProvider,
            Listener listener,
            Executor notificationExecutor,
            ProgressIndicator progressIndicator
        ) {
            super(
                contributorsAndLimits.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue()))),
                equalityProvider,
                listener,
                notificationExecutor,
                progressIndicator
            );
            sectionsLimits = contributorsAndLimits;
            conditionsMap =
                contributorsAndLimits.keySet().stream().collect(Collectors.toMap(Function.identity(), c -> lock.newCondition()));
        }

        @Override
        public void setContributorHasMore(SearchEverywhereContributor<?> contributor, boolean hasMore) {
            hasMoreMap.put(contributor, hasMore);
        }

        @Override
        public boolean addElement(
            Object element,
            SearchEverywhereContributor<?> contributor,
            int priority,
            ProgressIndicator indicator
        ) throws InterruptedException {
            SearchEverywhereFoundElementInfo newElementInfo = new SearchEverywhereFoundElementInfo(element, priority, contributor);
            Condition condition = conditionsMap.get(contributor);
            Collection<SearchEverywhereFoundElementInfo> section = sections.get(contributor);
            int limit = sectionsLimits.get(contributor);

            lock.lock();
            try {
                while (section.size() >= limit && !mySearchFinished) {
                    indicator.checkCanceled();
                    condition.await(100, TimeUnit.MILLISECONDS);
                }

                if (mySearchFinished) {
                    return false;
                }

                Map<SEResultsEqualityProvider.SEEqualElementsActionType, Collection<SearchEverywhereFoundElementInfo>> otherElementsMap =
                    getActionsWithOtherElements(newElementInfo);
                if (otherElementsMap.get(REPLACE).isEmpty() && !otherElementsMap.get(SKIP).isEmpty()) {
                    LOG.debug(String.format(
                        "Element %s for contributor %s was skipped",
                        element.toString(),
                        contributor.getSearchProviderId()
                    ));
                    return true;
                }

                section.add(newElementInfo);
                runInNotificationExecutor(() -> myListener.elementsAdded(Collections.singletonList(newElementInfo)));

                List<SearchEverywhereFoundElementInfo> toRemove = new ArrayList<>(otherElementsMap.get(REPLACE));
                toRemove.forEach(info -> {
                    Collection<SearchEverywhereFoundElementInfo> list = sections.get(info.getContributor());
                    Condition listCondition = conditionsMap.get(info.getContributor());
                    list.remove(info);
                    LOG.debug(String.format(
                        "Element %s for contributor %s is removed",
                        info.getElement().toString(),
                        info.getContributor().getSearchProviderId()
                    ));
                    listCondition.signal();
                });
                runInNotificationExecutor(() -> myListener.elementsRemoved(toRemove));

                if (section.size() >= limit) {
                    stopSearchIfNeeded();
                }
                return true;
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public void contributorFinished(SearchEverywhereContributor<?> contributor) {
            lock.lock();
            try {
                finishedContributorsSet.add(contributor);
                stopSearchIfNeeded();
            }
            finally {
                lock.unlock();
            }
        }

        public void searchFinished() {
            runInNotificationExecutor(() -> myListener.searchFinished(hasMoreMap));
        }

        public void stop() {
            lock.lock();
            try {
                mySearchFinished = true;
                conditionsMap.values().forEach(Condition::signalAll);
            }
            finally {
                lock.unlock();
            }
        }

        /**
         * could be used only when current thread owns {@link #lock}
         */
        private void stopSearchIfNeeded() {
            if (sections.keySet().stream().allMatch(this::isContributorFinished)) {
                mySearchFinished = true;
                conditionsMap.values().forEach(Condition::signalAll);
            }
        }

        private boolean isContributorFinished(SearchEverywhereContributor<?> contributor) {
            return finishedContributorsSet.contains(contributor) || sections.get(contributor).size() >= sectionsLimits.get(contributor);
        }
    }

    private static class ProgressIndicatorWithCancelListener extends ProgressIndicatorBase {

        private volatile Runnable cancelCallback = () -> {
        };

        private void setCancelCallback(Runnable cancelCallback) {
            this.cancelCallback = cancelCallback;
        }

        @Override
        protected void onRunningChange() {
            if (isCanceled()) {
                cancelCallback.run();
            }
        }
    }
}
