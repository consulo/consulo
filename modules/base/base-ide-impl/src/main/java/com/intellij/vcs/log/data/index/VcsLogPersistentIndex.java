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
package com.intellij.vcs.log.data.index;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import consulo.disposer.Disposer;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.EmptyConsumer;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.io.*;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.*;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.ui.filter.VcsLogTextFilterImpl;
import com.intellij.vcs.log.util.PersistentSet;
import com.intellij.vcs.log.util.PersistentSetImpl;
import com.intellij.vcs.log.util.StopWatch;
import com.intellij.vcs.log.util.TroveUtil;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import gnu.trove.TIntHashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.intellij.vcs.log.data.index.VcsLogFullDetailsIndex.INDEX;
import static com.intellij.vcs.log.util.PersistentUtil.*;

public class VcsLogPersistentIndex implements VcsLogIndex, Disposable {
  private static final Logger LOG = Logger.getInstance(VcsLogPersistentIndex.class);
  private static final int VERSION = 0;

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final FatalErrorHandler myFatalErrorsConsumer;
  @Nonnull
  private final VcsLogProgress myProgress;
  @Nonnull
  private final Map<VirtualFile, VcsLogProvider> myProviders;
  @Nonnull
  private final VcsLogStorage myHashMap;
  @Nonnull
  private final VcsUserRegistryImpl myUserRegistry;
  @Nonnull
  private final Set<VirtualFile> myRoots;

  @Nullable private final MyIndexStorage myIndexStorage;

  @Nonnull
  private final SingleTaskController<IndexingRequest, Void> mySingleTaskController = new MySingleTaskController();
  @Nonnull
  private final Map<VirtualFile, AtomicInteger> myNumberOfTasks = ContainerUtil.newHashMap();

  @Nonnull
  private Map<VirtualFile, TIntHashSet> myCommitsToIndex = new HashMap<>();

  public VcsLogPersistentIndex(@Nonnull Project project,
                               @Nonnull VcsLogStorage hashMap,
                               @Nonnull VcsLogProgress progress,
                               @Nonnull Map<VirtualFile, VcsLogProvider> providers,
                               @Nonnull FatalErrorHandler fatalErrorsConsumer,
                               @Nonnull Disposable disposableParent) {
    myHashMap = hashMap;
    myProject = project;
    myProgress = progress;
    myProviders = providers;
    myFatalErrorsConsumer = fatalErrorsConsumer;
    myRoots = ContainerUtil.newLinkedHashSet();

    for (Map.Entry<VirtualFile, VcsLogProvider> entry : providers.entrySet()) {
      if (VcsLogProperties.get(entry.getValue(), VcsLogProperties.SUPPORTS_INDEXING)) {
        myRoots.add(entry.getKey());
      }
    }

    myUserRegistry = (VcsUserRegistryImpl)ServiceManager.getService(myProject, VcsUserRegistry.class);

    myIndexStorage = createIndexStorage(fatalErrorsConsumer, calcLogId(myProject, providers));

    for (VirtualFile root : myRoots) {
      myNumberOfTasks.put(root, new AtomicInteger());
    }

    Disposer.register(disposableParent, this);
  }

  protected MyIndexStorage createIndexStorage(@Nonnull FatalErrorHandler fatalErrorHandler, @Nonnull String logId) {
    try {
      return IOUtil.openCleanOrResetBroken(() -> new MyIndexStorage(logId, myUserRegistry, myRoots, fatalErrorHandler, this),
                                           () -> MyIndexStorage.cleanup(logId));
    }
    catch (IOException e) {
      myFatalErrorsConsumer.consume(this, e);
    }
    return null;
  }

  public static int getVersion() {
    return VcsLogStorageImpl.VERSION + VERSION;
  }

  @Override
  public synchronized void scheduleIndex(boolean full) {
    if (myCommitsToIndex.isEmpty()) return;
    Map<VirtualFile, TIntHashSet> commitsToIndex = myCommitsToIndex;

    for (VirtualFile root : commitsToIndex.keySet()) {
      myNumberOfTasks.get(root).incrementAndGet();
    }
    myCommitsToIndex = ContainerUtil.newHashMap();

    mySingleTaskController.request(new IndexingRequest(commitsToIndex, full));
  }

  private void storeDetail(@Nonnull VcsFullCommitDetails detail) {
    if (myIndexStorage == null) return;
    try {
      int index = myHashMap.getCommitIndex(detail.getId(), detail.getRoot());

      myIndexStorage.messages.put(index, detail.getFullMessage());
      myIndexStorage.trigrams.update(index, detail);
      myIndexStorage.users.update(index, detail);
      myIndexStorage.paths.update(index, detail);

      myIndexStorage.commits.put(index);
    }
    catch (IOException e) {
      myFatalErrorsConsumer.consume(this, e);
    }
  }

  private void flush() {
    try {
      if (myIndexStorage != null) {
        myIndexStorage.messages.force();
        myIndexStorage.trigrams.flush();
        myIndexStorage.users.flush();
        myIndexStorage.paths.flush();
        myIndexStorage.commits.flush();
      }
    }
    catch (StorageException e) {
      myFatalErrorsConsumer.consume(this, e);
    }
  }

  public void markCorrupted() {
    if (myIndexStorage != null) myIndexStorage.commits.markCorrupted();
  }

  @Override
  public boolean isIndexed(int commit) {
    try {
      return myIndexStorage == null || myIndexStorage.commits.contains(commit);
    }
    catch (IOException e) {
      myFatalErrorsConsumer.consume(this, e);
    }
    return false;
  }

  @Override
  public synchronized boolean isIndexed(@Nonnull VirtualFile root) {
    return myRoots.contains(root) && (!myCommitsToIndex.containsKey(root) && myNumberOfTasks.get(root).get() == 0);
  }

  @Override
  public synchronized void markForIndexing(int index, @Nonnull VirtualFile root) {
    if (isIndexed(index) || !myRoots.contains(root)) return;
    TIntHashSet set = myCommitsToIndex.get(root);
    if (set == null) {
      set = new TIntHashSet();
      myCommitsToIndex.put(root, set);
    }
    set.add(index);
  }

  @Nonnull
  private <T> IntSet filter(@Nonnull PersistentMap<Integer, T> map, @Nonnull Condition<T> condition) {
    IntSet result = IntSets.newHashSet();
    if (myIndexStorage == null) return result;
    try {
      Processor<Integer> processor = integer -> {
        try {
          T value = map.get(integer);
          if (value != null) {
            if (condition.value(value)) {
              result.add(integer);
            }
          }
        }
        catch (IOException e) {
          myFatalErrorsConsumer.consume(this, e);
          return false;
        }
        return true;
      };
      if (myIndexStorage.messages instanceof PersistentHashMap) {
        ((PersistentHashMap<Integer, T>)myIndexStorage.messages).processKeysWithExistingMapping(processor);
      }
      else {
        myIndexStorage.messages.processKeys(processor);
      }
    }
    catch (IOException e) {
      myFatalErrorsConsumer.consume(this, e);
    }

    return result;
  }

  @Nonnull
  private IntSet filterUsers(@Nonnull Set<VcsUser> users) {
    if (myIndexStorage != null) {
      try {
        return myIndexStorage.users.getCommitsForUsers(users);
      }
      catch (IOException | StorageException e) {
        myFatalErrorsConsumer.consume(this, e);
      }
      catch (RuntimeException e) {
        processRuntimeException(e);
      }
    }
    return IntSets.newHashSet();
  }

  @Nonnull
  private IntSet filterPaths(@Nonnull Collection<FilePath> paths) {
    if (myIndexStorage != null) {
      try {
        return myIndexStorage.paths.getCommitsForPaths(paths);
      }
      catch (IOException | StorageException e) {
        myFatalErrorsConsumer.consume(this, e);
      }
      catch (RuntimeException e) {
        processRuntimeException(e);
      }
    }
    return IntSets.newHashSet();
  }

  @Nonnull
  public IntSet filterMessages(@Nonnull VcsLogTextFilter filter) {
    if (myIndexStorage != null) {
      try {
        if (!filter.isRegex()) {
          IntSet commitsForSearch = myIndexStorage.trigrams.getCommitsForSubstring(filter.getText());
          if (commitsForSearch != null) {
            IntSet result = IntSets.newHashSet();

            PrimitiveIterator.OfInt iterator = commitsForSearch.iterator();
            while (iterator.hasNext()) {
              int commit = iterator.nextInt();

              try {
                String value = myIndexStorage.messages.get(commit);
                if (value != null) {
                  if (VcsLogTextFilterImpl.matches(filter, value)) {
                    result.add(commit);
                  }
                }
              }
              catch (IOException e) {
                myFatalErrorsConsumer.consume(this, e);
                break;
              }
            }
            return result;
          }
        }
      }
      catch (StorageException e) {
        myFatalErrorsConsumer.consume(this, e);
      }
      catch (RuntimeException e) {
        processRuntimeException(e);
      }

      return filter(myIndexStorage.messages, message -> VcsLogTextFilterImpl.matches(filter, message));
    }

    return IntSet.of();
  }

  private void processRuntimeException(@Nonnull RuntimeException e) {
    if (myIndexStorage != null) myIndexStorage.markCorrupted();
    if (e.getCause() instanceof IOException || e.getCause() instanceof StorageException) {
      myFatalErrorsConsumer.consume(this, e);
    }
    else {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean canFilter(@Nonnull List<VcsLogDetailsFilter> filters) {
    if (filters.isEmpty() || myIndexStorage == null) return false;
    for (VcsLogDetailsFilter filter : filters) {
      if (filter instanceof VcsLogTextFilter ||
          filter instanceof VcsLogUserFilter ||
          filter instanceof VcsLogStructureFilter) {
        continue;
      }
      return false;
    }
    return true;
  }

  @Override
  @Nonnull
  public Set<Integer> filter(@Nonnull List<VcsLogDetailsFilter> detailsFilters) {
    VcsLogTextFilter textFilter = ContainerUtil.findInstance(detailsFilters, VcsLogTextFilter.class);
    VcsLogUserFilter userFilter = ContainerUtil.findInstance(detailsFilters, VcsLogUserFilter.class);
    VcsLogStructureFilter pathFilter = ContainerUtil.findInstance(detailsFilters, VcsLogStructureFilter.class);

    IntSet filteredByMessage = null;
    if (textFilter != null) {
      filteredByMessage = filterMessages(textFilter);
    }

    IntSet filteredByUser = null;
    if (userFilter != null) {
      Set<VcsUser> users = ContainerUtil.newHashSet();
      for (VirtualFile root : myRoots) {
        users.addAll(userFilter.getUsers(root));
      }

      filteredByUser = filterUsers(users);
    }

    IntSet filteredByPath = null;
    if (pathFilter != null) {
      filteredByPath = filterPaths(pathFilter.getFiles());
    }

    return TroveUtil.intersect(filteredByMessage, filteredByPath, filteredByUser);
  }

  @Nullable
  @Override
  public String getFullMessage(int index) {
    if (myIndexStorage != null) {
      try {
        return myIndexStorage.messages.get(index);
      }
      catch (IOException e) {
        myFatalErrorsConsumer.consume(this, e);
      }
    }
    return null;
  }

  @Override
  public void dispose() {
  }

  private static class MyIndexStorage {
    private static final String COMMITS = "commits";
    private static final String MESSAGES = "messages";
    private static final int MESSAGES_VERSION = 0;
    @Nonnull
    private final PersistentSet<Integer> commits;
    @Nonnull
    private final PersistentMap<Integer, String> messages;
    @Nonnull
    private final VcsLogMessagesTrigramIndex trigrams;
    @Nonnull
    private final VcsLogUserIndex users;
    @Nonnull
    private final VcsLogPathsIndex paths;
    private static final String INPUTS = "inputs";

    public MyIndexStorage(@Nonnull String logId,
                          @Nonnull VcsUserRegistryImpl userRegistry,
                          @Nonnull Set<VirtualFile> roots,
                          @Nonnull FatalErrorHandler fatalErrorHandler,
                          @Nonnull Disposable parentDisposable)
            throws IOException {
      Disposable disposable = Disposable.newDisposable();
      Disposer.register(parentDisposable, disposable);

      try {
        int version = getVersion();

        File commitsStorage = getStorageFile(INDEX, COMMITS, logId, version, true);
        commits = new PersistentSetImpl<>(commitsStorage, EnumeratorIntegerDescriptor.INSTANCE, Page.PAGE_SIZE, null, version);
        Disposer.register(disposable, () -> catchAndWarn(commits::close));

        File messagesStorage = getStorageFile(INDEX, MESSAGES, logId, VcsLogStorageImpl.VERSION + MESSAGES_VERSION, true);
        messages = new PersistentHashMap<>(messagesStorage, new IntInlineKeyDescriptor(), EnumeratorStringDescriptor.INSTANCE,
                                           Page.PAGE_SIZE);
        Disposer.register(disposable, () -> catchAndWarn(messages::close));

        trigrams = new VcsLogMessagesTrigramIndex(logId, fatalErrorHandler, disposable);
        users = new VcsLogUserIndex(logId, userRegistry, fatalErrorHandler, disposable);
        paths = new VcsLogPathsIndex(logId, roots, fatalErrorHandler, disposable);
      }
      catch (Throwable t) {
        Disposer.dispose(disposable);
        throw t;
      }

      // cleanup of old index storage files
      // to remove after 2017.1 release
      cleanupOldStorageFile(MESSAGES, logId);
      cleanupOldStorageFile(INDEX + "-" + VcsLogMessagesTrigramIndex.TRIGRAMS, logId);
      cleanupOldStorageFile(INDEX + "-no-" + VcsLogMessagesTrigramIndex.TRIGRAMS, logId);
      cleanupOldStorageFile(INDEX + "-" + INPUTS + "-" + VcsLogMessagesTrigramIndex.TRIGRAMS, logId);
      cleanupOldStorageFile(INDEX + "-" + VcsLogPathsIndex.PATHS, logId);
      cleanupOldStorageFile(INDEX + "-no-" + VcsLogPathsIndex.PATHS, logId);
      cleanupOldStorageFile(INDEX + "-" + VcsLogPathsIndex.PATHS + "-ids", logId);
      cleanupOldStorageFile(INDEX + "-" + INPUTS + "-" + VcsLogPathsIndex.PATHS, logId);
      cleanupOldStorageFile(INDEX + "-" + VcsLogUserIndex.USERS, logId);
      cleanupOldStorageFile(INDEX + "-" + INPUTS + "-" + VcsLogUserIndex.USERS, logId);
    }

    void markCorrupted() {
      catchAndWarn(commits::markCorrupted);
    }

    private static void catchAndWarn(@Nonnull ThrowableRunnable<IOException> runnable) {
      try {
        runnable.run();
      }
      catch (IOException e) {
        LOG.warn(e);
      }
    }

    private static void cleanup(@Nonnull String logId) {
      if (!cleanupStorageFiles(INDEX, logId)) {
        LOG.error("Could not clean up storage files in " + new File(LOG_CACHE, INDEX) + " starting with " + logId);
      }
    }
  }

  private class MySingleTaskController extends SingleTaskController<IndexingRequest, Void> {
    public MySingleTaskController() {
      super(EmptyConsumer.getInstance());
    }

    @Override
    protected void startNewBackgroundTask() {
      ApplicationManager.getApplication().invokeLater(() -> {
        Task.Backgroundable task = new Task.Backgroundable(VcsLogPersistentIndex.this.myProject, "Indexing Commit Data", true,
                                                           PerformInBackgroundOption.ALWAYS_BACKGROUND) {
          @Override
          public void run(@Nonnull ProgressIndicator indicator) {
            List<IndexingRequest> requests;
            while (!(requests = popRequests()).isEmpty()) {
              for (IndexingRequest request : requests) {
                try {
                  request.run(indicator);
                }
                catch (ProcessCanceledException reThrown) {
                  throw reThrown;
                }
                catch (Throwable t) {
                  LOG.error("Error while indexing", t);
                }
              }
            }

            taskCompleted(null);
          }
        };
        ProgressIndicator indicator = myProgress.createProgressIndicator(false);
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator);
      });
    }
  }

  private class IndexingRequest {
    private static final int MAGIC_NUMBER = 150000;
    private static final int BATCH_SIZE = 1000;
    private final Map<VirtualFile, TIntHashSet> myCommits;
    private final boolean myFull;

    public IndexingRequest(@Nonnull Map<VirtualFile, TIntHashSet> commits, boolean full) {
      myCommits = commits;
      myFull = full;
    }

    public void run(@Nonnull ProgressIndicator indicator) {
      indicator.setIndeterminate(false);
      indicator.setFraction(0);

      long time = System.currentTimeMillis();

      CommitsCounter counter = new CommitsCounter(indicator, myCommits.values().stream().mapToInt(TIntHashSet::size).sum());
      LOG.debug("Indexing " + counter.allCommits + " commits");

      for (VirtualFile root : myCommits.keySet()) {
        try {
          if (myFull) {
            indexAll(root, myCommits.get(root), counter);
          }
          else {
            indexOneByOne(root, myCommits.get(root), counter);
          }
        }
        finally {
          myNumberOfTasks.get(root).decrementAndGet();
        }
      }

      LOG.debug(StopWatch.formatTime(System.currentTimeMillis() - time) +
                " for indexing " +
                counter.newIndexedCommits +
                " new commits out of " +
                counter.allCommits);
      int leftCommits = counter.allCommits - counter.newIndexedCommits - counter.oldCommits;
      if (leftCommits > 0) {
        LOG.warn("Did not index " + leftCommits + " commits");
      }
    }

    private void indexOneByOne(@Nonnull VirtualFile root,
                               @Nonnull TIntHashSet commitsSet,
                               @Nonnull CommitsCounter counter) {
      IntStream commits = TroveUtil.stream(commitsSet).filter(c -> {
        if (isIndexed(c)) {
          counter.oldCommits++;
          return false;
        }
        return true;
      });

      indexOneByOne(root, counter, commits);
    }

    private void indexOneByOne(@Nonnull VirtualFile root,
                               @Nonnull CommitsCounter counter,
                               @Nonnull IntStream commits) {
      // We pass hashes to VcsLogProvider#readFullDetails in batches
      // in order to avoid allocating too much memory for these hashes
      // (we have up to 150K commits here that will occupy up to 18Mb as Strings).
      TroveUtil.processBatches(commits, BATCH_SIZE, batch -> {
        counter.indicator.checkCanceled();

        if (indexOneByOne(root, batch)) {
          counter.newIndexedCommits += batch.size();
        }

        counter.displayProgress();
      });

      flush();
    }

    private boolean indexOneByOne(@Nonnull VirtualFile root, @Nonnull TIntHashSet commits) {
      VcsLogProvider provider = myProviders.get(root);
      try {
        List<String> hashes = TroveUtil.map(commits, value -> myHashMap.getCommitId(value).getHash().asString());
        provider.readFullDetails(root, hashes, VcsLogPersistentIndex.this::storeDetail);
      }
      catch (VcsException e) {
        LOG.error(e);
        commits.forEach(value -> {
          markForIndexing(value, root);
          return true;
        });
        return false;
      }
      return true;
    }

    public void indexAll(@Nonnull VirtualFile root,
                         @Nonnull TIntHashSet commitsSet,
                         @Nonnull CommitsCounter counter) {
      TIntHashSet notIndexed = new TIntHashSet();
      TroveUtil.stream(commitsSet).forEach(c -> {
        if (isIndexed(c)) {
          counter.oldCommits++;
        }
        else {
          notIndexed.add(c);
        }
      });
      counter.displayProgress();

      if (notIndexed.size() <= MAGIC_NUMBER) {
        indexOneByOne(root, counter, TroveUtil.stream(notIndexed));
      }
      else {
        try {
          myProviders.get(root).readAllFullDetails(root, details -> {
            int index = myHashMap.getCommitIndex(details.getId(), details.getRoot());
            if (notIndexed.contains(index)) {
              storeDetail(details);
              counter.newIndexedCommits++;
            }

            counter.indicator.checkCanceled();
            counter.displayProgress();
          });
        }
        catch (VcsException e) {
          LOG.error(e);
          notIndexed.forEach(value -> {
            markForIndexing(value, root);
            return true;
          });
        }
      }

      flush();
    }
  }

  private static class CommitsCounter {
    @Nonnull
    public final ProgressIndicator indicator;
    public final int allCommits;
    public volatile int newIndexedCommits;
    public volatile int oldCommits;

    private CommitsCounter(@Nonnull ProgressIndicator indicator, int commits) {
      this.indicator = indicator;
      this.allCommits = commits;
    }

    public void displayProgress() {
      indicator.setFraction(((double)newIndexedCommits + oldCommits) / allCommits);
    }
  }
}
