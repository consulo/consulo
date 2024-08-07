/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.function.Computable;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.commited.*;
import consulo.versionControlSystem.impl.internal.MessageBusUtil;
import consulo.versionControlSystem.impl.internal.ProgressManagerQueue;
import consulo.versionControlSystem.internal.ProjectLevelVcsManagerEx;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author yole
 */
@State(name = "CommittedChangesCache", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class CommittedChangesCache implements PersistentStateComponent<CommittedChangesCache.State> {
  private static final Logger LOG = Logger.getInstance(CommittedChangesCache.class);

  private final Project myProject;
  private final MessageBus myBus;
  private ProgressManagerQueue myTaskQueue;
  private MessageBusConnection myConnection;
  private boolean myRefreshingIncomingChanges = false;
  private int myPendingUpdateCount = 0;
  private State myState = new State();
  private ScheduledFuture myFuture;
  private List<CommittedChangeList> myCachedIncomingChangeLists;
  private Set<CommittedChangeList> myNewIncomingChanges = new LinkedHashSet<>();
  private ProjectLevelVcsManager myVcsManager;

  public static final Change[] ALL_CHANGES = new Change[0];
  private MyRefreshRunnable myRefresnRunnable;

  private Map<String, Pair<Long, List<CommittedChangeList>>> myExternallyLoadedChangeLists;
  private CachesHolder myCachesHolder;
  private RepositoryLocationCache myLocationCache;

  public static class State {
    private int myInitialCount = 500;
    private int myInitialDays = 90;
    private int myRefreshInterval = 30;
    private boolean myRefreshEnabled = false;

    public int getInitialCount() {
      return myInitialCount;
    }

    public void setInitialCount(final int initialCount) {
      myInitialCount = initialCount;
    }

    public int getInitialDays() {
      return myInitialDays;
    }

    public void setInitialDays(final int initialDays) {
      myInitialDays = initialDays;
    }

    public int getRefreshInterval() {
      return myRefreshInterval;
    }

    public void setRefreshInterval(final int refreshInterval) {
      myRefreshInterval = refreshInterval;
    }

    public boolean isRefreshEnabled() {
      return myRefreshEnabled;
    }

    public void setRefreshEnabled(final boolean refreshEnabled) {
      myRefreshEnabled = refreshEnabled;
    }
  }

  public static CommittedChangesCache getInstance(Project project) {
    return project.getComponent(CommittedChangesCache.class);
  }

  @Inject
  public CommittedChangesCache(final Project project, final ProjectLevelVcsManager vcsManager) {
    myProject = project;
    myVcsManager = vcsManager;
    myBus = project.getMessageBus();

    if (!project.isDefault()) {
      myConnection = myBus.connect();
      final VcsListener vcsListener = new VcsListener() {
        @Override
        public void directoryMappingChanged() {
          myLocationCache.reset();
          refreshAllCachesAsync(false, true);
          refreshIncomingChangesAsync();
          myTaskQueue.run(new Runnable() {
            @Override
            public void run() {
              final List<ChangesCacheFile> files = myCachesHolder.getAllCaches();
              for (ChangesCacheFile file : files) {
                final RepositoryLocation location = file.getLocation();
                fireChangesLoaded(location, Collections.<CommittedChangeList>emptyList());
              }
              fireIncomingReloaded();
            }
          });
        }
      };
      myLocationCache = new RepositoryLocationCache(project);
      myCachesHolder = new CachesHolder(project, myLocationCache);
      myTaskQueue = new ProgressManagerQueue(project, VcsLocalize.committedChangesRefreshProgress().get());
      ((ProjectLevelVcsManagerEx)vcsManager).addInitializationRequest(VcsInitObject.COMMITTED_CHANGES_CACHE, new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
              if (myProject.isDisposed()) return;
              myTaskQueue.start();
              myConnection.subscribe(VcsMappingListener.class, vcsListener);
              myConnection.subscribe(PluginVcsMappingListener.class, vcsListener);
            }
          });
        }
      });
      Disposer.register(project, () -> {
        cancelRefreshTimer();
        myConnection.disconnect();
      });
      myExternallyLoadedChangeLists = new ConcurrentHashMap<>();
    }
  }

  public MessageBus getMessageBus() {
    return myBus;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
    updateRefreshTimer();
  }

  @Nullable
  public CommittedChangesProvider getProviderForProject() {
    final AbstractVcs[] vcss = myVcsManager.getAllActiveVcss();
    List<AbstractVcs> vcsWithProviders = new ArrayList<>();
    for (AbstractVcs vcs : vcss) {
      if (vcs.getCommittedChangesProvider() != null) {
        vcsWithProviders.add(vcs);
      }
    }
    if (vcsWithProviders.isEmpty()) {
      return null;
    }
    if (vcsWithProviders.size() == 1) {
      return vcsWithProviders.get(0).getCommittedChangesProvider();
    }
    return new CompositeCommittedChangesProvider(myProject, vcsWithProviders.toArray(new AbstractVcs[vcsWithProviders.size()]));
  }

  public boolean isMaxCountSupportedForProject() {
    for (AbstractVcs vcs : myVcsManager.getAllActiveVcss()) {
      final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
      if (provider instanceof CachingCommittedChangesProvider) {
        final CachingCommittedChangesProvider cachingProvider = (CachingCommittedChangesProvider)provider;
        if (!cachingProvider.isMaxCountSupported()) {
          return false;
        }
      }
    }
    return true;
  }

  private class MyProjectChangesLoader implements Runnable {
    private final ChangeBrowserSettings mySettings;
    private final int myMaxCount;
    private final boolean myCacheOnly;
    private final Consumer<List<CommittedChangeList>> myConsumer;
    private final Consumer<List<VcsException>> myErrorConsumer;

    private final LinkedHashSet<CommittedChangeList> myResult = new LinkedHashSet<>();
    private final List<VcsException> myExceptions = new ArrayList<>();
    private boolean myDisposed = false;

    private MyProjectChangesLoader(ChangeBrowserSettings settings,
                                   int maxCount,
                                   boolean cacheOnly,
                                   Consumer<List<CommittedChangeList>> consumer,
                                   Consumer<List<VcsException>> errorConsumer) {
      mySettings = settings;
      myMaxCount = maxCount;
      myCacheOnly = cacheOnly;
      myConsumer = consumer;
      myErrorConsumer = errorConsumer;
    }

    @Override
    public void run() {
      for (AbstractVcs vcs : myVcsManager.getAllActiveVcss()) {
        final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
        if (provider == null) continue;

        final VcsCommittedListsZipper vcsZipper = provider.getZipper();
        CommittedListsSequencesZipper zipper = null;
        if (vcsZipper != null) {
          zipper = new CommittedListsSequencesZipper(vcsZipper);
        }
        boolean zipSupported = zipper != null;

        final Map<VirtualFile, RepositoryLocation> map = myCachesHolder.getAllRootsUnderVcs(vcs);

        for (VirtualFile root : map.keySet()) {
          if (myProject.isDisposed()) return;

          final RepositoryLocation location = map.get(root);

          try {
            final List<CommittedChangeList> lists = getChanges(mySettings, root, vcs, myMaxCount, myCacheOnly, provider, location);
            if (lists != null) {
              if (zipSupported) {
                zipper.add(location, lists);
              }
              else {
                myResult.addAll(lists);
              }
            }
          }
          catch (VcsException e) {
            myExceptions.add(e);
          }
          catch (ProcessCanceledException e) {
            myDisposed = true;
          }
        }

        if (zipSupported) {
          myResult.addAll(zipper.execute());
        }
      }

      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          LOG.info("FINISHED CommittedChangesCache.getProjectChangesAsync - execution in queue");
          if (myProject.isDisposed()) {
            return;
          }
          if (myExceptions.size() > 0) {
            myErrorConsumer.accept(myExceptions);
          }
          else if (!myDisposed) {
            myConsumer.accept(new ArrayList<>(myResult));
          }
        }
      }, ModalityState.nonModal());
    }
  }

  public void getProjectChangesAsync(final ChangeBrowserSettings settings,
                                     final int maxCount,
                                     final boolean cacheOnly,
                                     final Consumer<List<CommittedChangeList>> consumer,
                                     final Consumer<List<VcsException>> errorConsumer) {
    final MyProjectChangesLoader loader = new MyProjectChangesLoader(settings, maxCount, cacheOnly, consumer, errorConsumer);
    myTaskQueue.run(loader);
  }

  @Nullable
  public List<CommittedChangeList> getChanges(ChangeBrowserSettings settings,
                                              final VirtualFile file,
                                              @Nonnull final AbstractVcs vcs,
                                              final int maxCount,
                                              final boolean cacheOnly,
                                              final CommittedChangesProvider provider,
                                              final RepositoryLocation location) throws VcsException {
    if (settings instanceof CompositeCommittedChangesProvider.CompositeChangeBrowserSettings) {
      settings = ((CompositeCommittedChangesProvider.CompositeChangeBrowserSettings)settings).get(vcs);
    }
    if (provider instanceof CachingCommittedChangesProvider) {
      try {
        if (cacheOnly) {
          ChangesCacheFile cacheFile = myCachesHolder.getCacheFile(vcs, file, location);
          if (!cacheFile.isEmpty()) {

            final RepositoryLocation fileLocation = cacheFile.getLocation();
            fileLocation.onBeforeBatch();
            final List<CommittedChangeList> committedChangeLists = cacheFile.readChanges(settings, maxCount);
            fileLocation.onAfterBatch();
            return committedChangeLists;
          }
          return null;
        }
        else {
          if (canGetFromCache(vcs, settings, file, location, maxCount)) {
            return getChangesWithCaching(vcs, settings, file, location, maxCount);
          }
        }
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }
    //noinspection unchecked
    return provider.getCommittedChanges(settings, location, maxCount);
  }

  private boolean canGetFromCache(final AbstractVcs vcs,
                                  final ChangeBrowserSettings settings,
                                  final VirtualFile root,
                                  final RepositoryLocation location,
                                  final int maxCount) throws IOException {
    ChangesCacheFile cacheFile = myCachesHolder.getCacheFile(vcs, root, location);
    if (cacheFile.isEmpty()) {
      return true;   // we'll initialize the cache and check again after that
    }
    if (settings.USE_DATE_BEFORE_FILTER && !settings.USE_DATE_AFTER_FILTER) {
      return cacheFile.hasCompleteHistory();
    }
    if (settings.USE_CHANGE_BEFORE_FILTER && !settings.USE_CHANGE_AFTER_FILTER) {
      return cacheFile.hasCompleteHistory();
    }

    boolean hasDateFilter =
      settings.USE_DATE_AFTER_FILTER || settings.USE_DATE_BEFORE_FILTER || settings.USE_CHANGE_AFTER_FILTER || settings.USE_CHANGE_BEFORE_FILTER;
    boolean hasNonDateFilter = settings.isNonDateFilterSpecified();
    if (!hasDateFilter && hasNonDateFilter) {
      return cacheFile.hasCompleteHistory();
    }
    if (settings.USE_DATE_AFTER_FILTER && settings.getDateAfter().getTime() < cacheFile.getFirstCachedDate().getTime()) {
      return cacheFile.hasCompleteHistory();
    }
    if (settings.USE_CHANGE_AFTER_FILTER && settings.getChangeAfterFilter().longValue() < cacheFile.getFirstCachedChangelist()) {
      return cacheFile.hasCompleteHistory();
    }
    return true;
  }

  public void hasCachesForAnyRoot(@Nullable final Consumer<Boolean> continuation) {
    myTaskQueue.run(new Runnable() {
      @Override
      public void run() {
        final Ref<Boolean> success = new Ref<>();
        try {
          success.set(hasCachesWithEmptiness(false));
        }
        catch (ProcessCanceledException e) {
          success.set(true);
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            continuation.accept(success.get());
          }
        }, myProject.getDisposed());
      }
    });
  }

  public boolean hasEmptyCaches() {
    try {
      return hasCachesWithEmptiness(true);
    }
    catch (ProcessCanceledException e) {
      return false;
    }
  }

  private boolean hasCachesWithEmptiness(final boolean emptiness) {
    final Ref<Boolean> resultRef = new Ref<>(Boolean.FALSE);
    myCachesHolder.iterateAllCaches(new Function<>() {
      @Override
      @Nonnull
      public Boolean apply(final ChangesCacheFile changesCacheFile) {
        try {
          if (changesCacheFile.isEmpty() == emptiness) {
            resultRef.set(true);
            return true;
          }
        }
        catch (IOException e) {
          LOG.info(e);
        }
        return false;
      }
    });
    return resultRef.get();
  }

  @Nullable
  public Iterator<ChangesBunch> getBackBunchedIterator(final AbstractVcs vcs,
                                                       final VirtualFile root,
                                                       final RepositoryLocation location,
                                                       final int bunchSize) {
    final ChangesCacheFile cacheFile = myCachesHolder.getCacheFile(vcs, root, location);
    try {
      if (!cacheFile.isEmpty()) {
        return cacheFile.getBackBunchedIterator(bunchSize);
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return null;
  }

  private List<CommittedChangeList> getChangesWithCaching(final AbstractVcs vcs,
                                                          final ChangeBrowserSettings settings,
                                                          final VirtualFile root,
                                                          final RepositoryLocation location,
                                                          final int maxCount)
    throws VcsException, IOException {
    ChangesCacheFile cacheFile = myCachesHolder.getCacheFile(vcs, root, location);
    if (cacheFile.isEmpty()) {
      List<CommittedChangeList> changes = initCache(cacheFile);
      if (canGetFromCache(vcs, settings, root, location, maxCount)) {
        settings.filterChanges(changes);
        return trimToSize(changes, maxCount);
      }
      //noinspection unchecked
      return cacheFile.getProvider().getCommittedChanges(settings, location, maxCount);
    }
    else {
      // we take location instance that would be used for deserialization
      final RepositoryLocation fileLocation = cacheFile.getLocation();
      fileLocation.onBeforeBatch();
      final List<CommittedChangeList> changes = cacheFile.readChanges(settings, maxCount);
      fileLocation.onAfterBatch();
      List<CommittedChangeList> newChanges = refreshCache(cacheFile);
      settings.filterChanges(newChanges);
      changes.addAll(newChanges);
      return trimToSize(changes, maxCount);
    }
  }

  @TestOnly
  public void refreshAllCaches() throws IOException, VcsException {
    final Collection<ChangesCacheFile> files = myCachesHolder.getAllCaches();
    for (ChangesCacheFile file : files) {
      if (file.isEmpty()) {
        initCache(file);
      }
      else {
        refreshCache(file);
      }
    }
  }

  private List<CommittedChangeList> initCache(final ChangesCacheFile cacheFile) throws VcsException, IOException {
    debug("Initializing cache for " + cacheFile.getLocation());
    final CachingCommittedChangesProvider provider = cacheFile.getProvider();
    final RepositoryLocation location = cacheFile.getLocation();
    final ChangeBrowserSettings settings = provider.createDefaultSettings();
    int maxCount = 0;
    if (isMaxCountSupportedForProject()) {
      maxCount = myState.getInitialCount();
    }
    else {
      settings.USE_DATE_AFTER_FILTER = true;
      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_YEAR, -myState.getInitialDays());
      settings.setDateAfter(calendar.getTime());
    }
    //noinspection unchecked
    final List<CommittedChangeList> changes = provider.getCommittedChanges(settings, location, maxCount);
    // when initially initializing cache, assume all changelists are locally available
    writeChangesInReadAction(cacheFile, changes); // this sorts changes in chronological order
    if (maxCount > 0 && changes.size() < myState.getInitialCount()) {
      cacheFile.setHaveCompleteHistory(true);
    }
    if (changes.size() > 0) {
      fireChangesLoaded(location, changes);
    }
    return changes;
  }

  private void fireChangesLoaded(final RepositoryLocation location, final List<CommittedChangeList> changes) {
    MessageBusUtil.invokeLaterIfNeededOnSyncPublisher(myProject, CommittedChangesListener.class, new Consumer<>() {
      @Override
      public void accept(CommittedChangesListener listener) {
        listener.changesLoaded(location, changes);
      }
    });
  }

  private void fireIncomingReloaded() {
    MessageBusUtil.invokeLaterIfNeededOnSyncPublisher(myProject, CommittedChangesListener.class, new Consumer<>() {
      @Override
      public void accept(CommittedChangesListener listener) {
        listener.incomingChangesUpdated(Collections.<CommittedChangeList>emptyList());
      }
    });
  }

  // todo: fix - would externally loaded nesseccerily for file? i.e. just not efficient now
  private List<CommittedChangeList> refreshCache(final ChangesCacheFile cacheFile) throws VcsException, IOException {
    final List<CommittedChangeList> newLists = new ArrayList<>();

    final CachingCommittedChangesProvider provider = cacheFile.getProvider();
    final RepositoryLocation location = cacheFile.getLocation();

    final Pair<Long, List<CommittedChangeList>> externalLists = myExternallyLoadedChangeLists.get(location.getKey());
    final long latestChangeList = getLatestListForFile(cacheFile);
    if ((externalLists != null) && (latestChangeList == externalLists.first.longValue())) {
      newLists.addAll(appendLoadedChanges(cacheFile, location, externalLists.second));
      myExternallyLoadedChangeLists.clear();
    }

    final ChangeBrowserSettings defaultSettings = provider.createDefaultSettings();
    int maxCount = 0;
    if (provider.refreshCacheByNumber()) {
      final long number = cacheFile.getLastCachedChangelist();
      debug("Refreshing cache for " + location + " since #" + number);
      if (number >= 0) {
        defaultSettings.CHANGE_AFTER = Long.toString(number);
        defaultSettings.USE_CHANGE_AFTER_FILTER = true;
      }
      else {
        maxCount = myState.getInitialCount();
      }
    }
    else {
      final Date date = cacheFile.getLastCachedDate();
      debug("Refreshing cache for " + location + " since " + date);
      defaultSettings.setDateAfter(date);
      defaultSettings.USE_DATE_AFTER_FILTER = true;
    }
    final List<CommittedChangeList> newChanges = provider.getCommittedChanges(defaultSettings, location, maxCount);
    debug("Loaded " + newChanges.size() + " new changelists");
    newLists.addAll(appendLoadedChanges(cacheFile, location, newChanges));

    return newLists;
  }

  private static void debug(@NonNls String message) {
    LOG.debug(message);
  }

  private List<CommittedChangeList> appendLoadedChanges(final ChangesCacheFile cacheFile,
                                                        final RepositoryLocation location,
                                                        final List<CommittedChangeList> newChanges) throws IOException {
    final List<CommittedChangeList> savedChanges = writeChangesInReadAction(cacheFile, newChanges);
    if (savedChanges.size() > 0) {
      fireChangesLoaded(location, savedChanges);
    }
    return savedChanges;
  }

  private static List<CommittedChangeList> writeChangesInReadAction(final ChangesCacheFile cacheFile,
                                                                    final List<CommittedChangeList> newChanges) throws IOException {
    // ensure that changes are loaded before taking read action, to avoid stalling UI
    for (CommittedChangeList changeList : newChanges) {
      changeList.getChanges();
    }
    final Ref<IOException> ref = new Ref<>();
    final List<CommittedChangeList> savedChanges =
      ApplicationManager.getApplication().runReadAction(new Computable<List<CommittedChangeList>>() {
        @Override
        public List<CommittedChangeList> compute() {
          try {
            return cacheFile.writeChanges(newChanges);    // skip duplicates;
          }
          catch (IOException e) {
            ref.set(e);
            return null;
          }
        }
      });
    if (!ref.isNull()) {
      throw ref.get();
    }
    return savedChanges;
  }

  private static List<CommittedChangeList> trimToSize(final List<CommittedChangeList> changes, final int maxCount) {
    if (maxCount > 0) {
      while (changes.size() > maxCount) {
        changes.remove(0);
      }
    }
    return changes;
  }

  public List<CommittedChangeList> loadIncomingChanges(boolean inBackground) {
    final List<CommittedChangeList> result = new ArrayList<>();
    final Collection<ChangesCacheFile> caches = myCachesHolder.getAllCaches();

    final MultiMap<AbstractVcs, Pair<RepositoryLocation, List<CommittedChangeList>>> byVcs =
      new MultiMap<>();

    for (ChangesCacheFile cache : caches) {
      try {
        if (inBackground && (!cache.getVcs().isVcsBackgroundOperationsAllowed(cache.getRootPath().getVirtualFile()))) continue;
        if (!cache.isEmpty()) {
          debug("Loading incoming changes for " + cache.getLocation());
          final List<CommittedChangeList> incomingChanges = cache.loadIncomingChanges();
          byVcs.putValue(cache.getVcs(), Pair.create(cache.getLocation(), incomingChanges));
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }

    for (AbstractVcs vcs : byVcs.keySet()) {
      final CommittedChangesProvider committedChangesProvider = vcs.getCommittedChangesProvider();
      VcsCommittedListsZipper vcsZipper = committedChangesProvider.getZipper();
      if (vcsZipper != null) {
        final VcsCommittedListsZipper incomingZipper = new IncomingListsZipper(vcsZipper);
        final CommittedListsSequencesZipper zipper = new CommittedListsSequencesZipper(incomingZipper);
        for (Pair<RepositoryLocation, List<CommittedChangeList>> pair : byVcs.get(vcs)) {
          zipper.add(pair.getFirst(), pair.getSecond());
        }
        result.addAll(zipper.execute());
      }
      else {
        for (Pair<RepositoryLocation, List<CommittedChangeList>> pair : byVcs.get(vcs)) {
          result.addAll(pair.getSecond());
        }
      }
    }

    myCachedIncomingChangeLists = result;
    debug("Incoming changes loaded");
    notifyIncomingChangesUpdated(result);
    return result;
  }

  private static class IncomingListsZipper extends VcsCommittedListsZipperAdapter {
    private final VcsCommittedListsZipper myVcsZipper;

    private IncomingListsZipper(final VcsCommittedListsZipper vcsZipper) {
      super(null);
      myVcsZipper = vcsZipper;
    }

    @Override
    public Pair<List<RepositoryLocationGroup>, List<RepositoryLocation>> groupLocations(final List<RepositoryLocation> in) {
      return myVcsZipper.groupLocations(in);
    }

    @Override
    public CommittedChangeList zip(final RepositoryLocationGroup group, final List<CommittedChangeList> lists) {
      if (lists.size() == 1) {
        return lists.get(0);
      }
      final CommittedChangeList victim = ReceivedChangeList.unwrap(lists.get(0));
      final ReceivedChangeList result = new ReceivedChangeList(victim);
      result.setForcePartial(false);
      final Set<Change> baseChanges = new HashSet<>();

      for (CommittedChangeList list : lists) {
        baseChanges.addAll(ReceivedChangeList.unwrap(list).getChanges());

        final Collection<Change> changes = list.getChanges();
        for (Change change : changes) {
          if (!result.getChanges().contains(change)) {
            result.addChange(change);
          }
        }
      }
      result.setForcePartial(baseChanges.size() != result.getChanges().size());
      return result;
    }

    @Override
    public long getNumber(final CommittedChangeList list) {
      return myVcsZipper.getNumber(list);
    }
  }

  public void commitMessageChanged(final AbstractVcs vcs, final RepositoryLocation location, final long number, final String newMessage) {
    myTaskQueue.run(new Runnable() {
      @Override
      public void run() {
        final ChangesCacheFile file = myCachesHolder.haveCache(location);
        if (file != null) {
          try {
            if (file.isEmpty()) return;
            file.editChangelist(number, newMessage);
            loadIncomingChanges(true);
            fireChangesLoaded(location, Collections.<CommittedChangeList>emptyList());
          }
          catch (IOException e) {
            VcsBalloonProblemNotifier.showOverChangesView(myProject,
                                                          "Didn't update Repository changes with new message due to error: " + e.getMessage(),
                                                          NotificationType.ERROR);
          }
        }
      }
    });
  }

  public void loadIncomingChangesAsync(@Nullable final Consumer<List<CommittedChangeList>> consumer, final boolean inBackground) {
    debug("Loading incoming changes");
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        final List<CommittedChangeList> list = loadIncomingChanges(inBackground);
        if (consumer != null) {
          consumer.accept(new ArrayList<>(list));
        }
      }
    };
    myTaskQueue.run(task);
  }

  public void clearCaches(final Runnable continuation) {
    myTaskQueue.run(new Runnable() {
      @Override
      public void run() {
        myCachesHolder.clearAllCaches();
        myCachedIncomingChangeLists = null;
        continuation.run();
        MessageBusUtil.invokeLaterIfNeededOnSyncPublisher(myProject,
                                                          CommittedChangesListener.class,
                                                          new Consumer<>() {
                                                            @Override
                                                            public void accept(CommittedChangesListener listener) {
                                                              listener.changesCleared();
                                                            }
                                                          });
      }
    });
  }

  @Nullable
  public List<CommittedChangeList> getCachedIncomingChanges() {
    return myCachedIncomingChangeLists;
  }

  public void processUpdatedFiles(final UpdatedFiles updatedFiles) {
    processUpdatedFiles(updatedFiles, null);
  }

  public void processUpdatedFiles(final UpdatedFiles updatedFiles,
                                  @Nullable final Consumer<List<CommittedChangeList>> incomingChangesConsumer) {
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        debug("Processing updated files");
        final Collection<ChangesCacheFile> caches = myCachesHolder.getAllCaches();
        myPendingUpdateCount += caches.size();
        for (final ChangesCacheFile cache : caches) {
          try {
            if (cache.isEmpty()) {
              pendingUpdateProcessed(incomingChangesConsumer);
              continue;
            }
            debug("Processing updated files in " + cache.getLocation());
            boolean needRefresh = cache.processUpdatedFiles(updatedFiles, myNewIncomingChanges);
            if (needRefresh) {
              debug("Found unaccounted files, requesting refresh");
              // todo do we need double-queueing here???
              processUpdatedFilesAfterRefresh(cache, updatedFiles, incomingChangesConsumer);
            }
            else {
              debug("Clearing cached incoming changelists");
              myCachedIncomingChangeLists = null;
              pendingUpdateProcessed(incomingChangesConsumer);
            }
          }
          catch (IOException e) {
            LOG.error(e);
          }
        }
      }
    };
    myTaskQueue.run(task);
  }

  private void pendingUpdateProcessed(@Nullable Consumer<List<CommittedChangeList>> incomingChangesConsumer) {
    myPendingUpdateCount--;
    if (myPendingUpdateCount == 0) {
      notifyIncomingChangesUpdated(myNewIncomingChanges);
      if (incomingChangesConsumer != null) {
        incomingChangesConsumer.accept(ContainerUtil.newArrayList(myNewIncomingChanges));
      }
      myNewIncomingChanges.clear();
    }
  }

  private void processUpdatedFilesAfterRefresh(final ChangesCacheFile cache,
                                               final UpdatedFiles updatedFiles,
                                               @Nullable final Consumer<List<CommittedChangeList>> incomingChangesConsumer) {
    refreshCacheAsync(cache, false, new RefreshResultConsumer() {
      @Override
      public void receivedChanges(final List<CommittedChangeList> committedChangeLists) {
        try {
          debug("Processing updated files after refresh in " + cache.getLocation());
          boolean result = true;
          if (committedChangeLists.size() > 0) {
            // received some new changelists, try to process updated files again
            result = cache.processUpdatedFiles(updatedFiles, myNewIncomingChanges);
          }
          debug(result ? "Still have unaccounted files" : "No more unaccounted files");
          // for svn, we won't get exact revision numbers in updatedFiles, so we have to double-check by
          // checking revisions we have locally
          if (result) {
            cache.refreshIncomingChanges();
            debug("Clearing cached incoming changelists");
            myCachedIncomingChangeLists = null;
          }
          pendingUpdateProcessed(incomingChangesConsumer);
        }
        catch (IOException e) {
          LOG.error(e);
        }
        catch (VcsException e) {
          notifyRefreshError(e);
        }
      }

      @Override
      public void receivedError(VcsException ex) {
        notifyRefreshError(ex);
      }
    });
  }

  private void fireIncomingChangesUpdated(final List<CommittedChangeList> lists) {
    MessageBusUtil.invokeLaterIfNeededOnSyncPublisher(myProject,
                                                      CommittedChangesListener.class,
                                                      listener -> listener.incomingChangesUpdated(new ArrayList<CommittedChangeList>(lists)));
  }

  private void notifyIncomingChangesUpdated(@Nullable final Collection<CommittedChangeList> receivedChanges) {
    final Collection<CommittedChangeList> changes = receivedChanges == null ? myCachedIncomingChangeLists : receivedChanges;
    if (changes == null) {
      final Application application = ApplicationManager.getApplication();
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          final List<CommittedChangeList> lists = loadIncomingChanges(true);
          fireIncomingChangesUpdated(lists);
        }
      };
      if (application.isDispatchThread()) {
        myTaskQueue.run(runnable);
      }
      else {
        runnable.run();
      }
      return;
    }
    final ArrayList<CommittedChangeList> listCopy = new ArrayList<>(changes);
    fireIncomingChangesUpdated(listCopy);
  }

  private void notifyRefreshError(final VcsException e) {
    MessageBusUtil.invokeLaterIfNeededOnSyncPublisher(myProject,
                                                      CommittedChangesListener.class,
                                                      listener -> listener.refreshErrorStatusChanged(e));
  }

  private CommittedChangesListener getPublisher(final Consumer<CommittedChangesListener> listener) {
    return ApplicationManager.getApplication().runReadAction((Supplier<CommittedChangesListener>)() -> {
      if (myProject.isDisposed()) throw new ProcessCanceledException();
      return myBus.syncPublisher(CommittedChangesListener.class);
    });
  }

  public boolean isRefreshingIncomingChanges() {
    return myRefreshingIncomingChanges;
  }

  public boolean refreshIncomingChanges() {
    boolean hasChanges = false;
    final Collection<ChangesCacheFile> caches = myCachesHolder.getAllCaches();
    for (ChangesCacheFile file : caches) {
      try {
        if (file.isEmpty()) {
          continue;
        }
        debug("Refreshing incoming changes for " + file.getLocation());
        boolean changesForCache = file.refreshIncomingChanges();
        hasChanges |= changesForCache;
      }
      catch (IOException e) {
        LOG.error(e);
      }
      catch (VcsException e) {
        notifyRefreshError(e);
      }
    }
    return hasChanges;
  }

  public void refreshIncomingChangesAsync() {
    debug("Refreshing incoming changes in background");
    myRefreshingIncomingChanges = true;
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        refreshIncomingChanges();

        refreshIncomingUi();
      }
    };
    myTaskQueue.run(task);
  }

  private void refreshIncomingUi() {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        myRefreshingIncomingChanges = false;
        debug("Incoming changes refresh complete, clearing cached incoming changes");
        notifyReloadIncomingChanges();
      }
    }, ModalityState.nonModal(), myProject.getDisposed());
  }

  public void refreshAllCachesAsync(final boolean initIfEmpty, final boolean inBackground) {
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        final List<ChangesCacheFile> files = myCachesHolder.getAllCaches();
        final RefreshResultConsumer notifyConsumer = new RefreshResultConsumer() {
          private VcsException myError = null;
          private int myCount = 0;
          private int totalChangesCount = 0;

          @Override
          public void receivedChanges(List<CommittedChangeList> changes) {
            totalChangesCount += changes.size();
            checkDone();
          }

          @Override
          public void receivedError(VcsException ex) {
            myError = ex;
            checkDone();
          }

          private void checkDone() {
            myCount++;
            if (myCount == files.size()) {
              myTaskQueue.run(new Runnable() {
                @Override
                public void run() {
                  if (totalChangesCount > 0) {
                    notifyReloadIncomingChanges();
                  }
                  else {
                    myProject.getMessageBus().syncPublisher(CommittedChangesReloadListener.class).emptyRefresh();
                  }
                }
              });
              notifyRefreshError(myError);
            }
          }
        };
        for (ChangesCacheFile file : files) {
          if ((!inBackground) || file.getVcs().isVcsBackgroundOperationsAllowed(file.getRootPath().getVirtualFile())) {
            refreshCacheAsync(file, initIfEmpty, notifyConsumer, false);
          }
        }
      }
    };
    myTaskQueue.run(task);
  }

  private void notifyReloadIncomingChanges() {
    myCachedIncomingChangeLists = null;
    notifyIncomingChangesUpdated(null);
  }

  private void refreshCacheAsync(final ChangesCacheFile cache, final boolean initIfEmpty, @Nullable final RefreshResultConsumer consumer) {
    refreshCacheAsync(cache, initIfEmpty, consumer, true);
  }

  private void refreshCacheAsync(final ChangesCacheFile cache,
                                 final boolean initIfEmpty,
                                 @Nullable final RefreshResultConsumer consumer,
                                 final boolean asynch) {
    try {
      if (!initIfEmpty && cache.isEmpty()) {
        return;
      }
    }
    catch (IOException e) {
      LOG.error(e);
      return;
    }
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        try {
          final List<CommittedChangeList> list;
          if (initIfEmpty && cache.isEmpty()) {
            list = initCache(cache);
          }
          else {
            list = refreshCache(cache);
          }
          if (consumer != null) {
            consumer.receivedChanges(list);
          }
        }
        catch (ProcessCanceledException ex) {
          // ignore
        }
        catch (IOException e) {
          LOG.error(e);
        }
        catch (VcsException e) {
          if (consumer != null) {
            consumer.receivedError(e);
          }
        }
      }
    };
    if (asynch) {
      myTaskQueue.run(task);
    }
    else {
      task.run();
    }
  }

  private void updateRefreshTimer() {
    cancelRefreshTimer();
    if (myState.isRefreshEnabled()) {
      myRefresnRunnable = new MyRefreshRunnable(this);
      // if "schedule with fixed rate" is used, then after waking up from stand-by mode, events are generated for inactive period
      // it does not make sense

      ApplicationConcurrency concurrency = Application.get().getInstance(ApplicationConcurrency.class);

      myFuture = concurrency.getScheduledExecutorService()
                            .scheduleWithFixedDelay(myRefresnRunnable,
                                                    myState.getRefreshInterval() * 60,
                                                    myState.getRefreshInterval() * 60,
                                                    TimeUnit.SECONDS);
    }
  }

  private void cancelRefreshTimer() {
    if (myRefresnRunnable != null) {
      myRefresnRunnable.cancel();
      myRefresnRunnable = null;
    }
    if (myFuture != null) {
      myFuture.cancel(false);
      myFuture = null;
    }
  }

  @Nullable
  public Pair<CommittedChangeList, Change> getIncomingChangeList(final VirtualFile file) {
    if (myCachedIncomingChangeLists != null) {
      File ioFile = new File(file.getPath());
      for (CommittedChangeList changeList : myCachedIncomingChangeLists) {
        for (Change change : changeList.getChanges()) {
          if (change.affectsFile(ioFile)) {
            return Pair.create(changeList, change);
          }
        }
      }
    }
    return null;
  }

  private long getLatestListForFile(final ChangesCacheFile file) {
    try {
      if ((file == null) || (file.isEmpty())) {
        return -1;
      }
      return file.getLastCachedChangelist();
    }
    catch (IOException e) {
      return -1;
    }
  }

  public CachesHolder getCachesHolder() {
    return myCachesHolder;
  }

  public void submitExternallyLoaded(final RepositoryLocation location, final long myLastCl, final List<CommittedChangeList> lists) {
    myExternallyLoadedChangeLists.put(location.getKey(), new Pair<>(myLastCl, lists));
  }

  private interface RefreshResultConsumer {
    void receivedChanges(List<CommittedChangeList> changes);

    void receivedError(VcsException ex);
  }

  private static class MyRefreshRunnable implements Runnable {
    private CommittedChangesCache myCache;

    private MyRefreshRunnable(final CommittedChangesCache cache) {
      myCache = cache;
    }

    private void cancel() {
      myCache = null;
    }

    @Override
    public void run() {
      final CommittedChangesCache cache = myCache;
      if (cache == null) return;
      cache.refreshAllCachesAsync(false, true);
      final List<ChangesCacheFile> list = cache.getCachesHolder().getAllCaches();
      for (ChangesCacheFile file : list) {
        if (file.getVcs().isVcsBackgroundOperationsAllowed(file.getRootPath().getVirtualFile())) {
          if (file.getProvider().refreshIncomingWithCommitted()) {
            cache.refreshIncomingChangesAsync();
            break;
          }
        }
      }
    }
  }

  public RepositoryLocationCache getLocationCache() {
    return myLocationCache;
  }
}
