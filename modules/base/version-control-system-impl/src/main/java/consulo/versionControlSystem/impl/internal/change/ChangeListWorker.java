// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.ReadAction;
import consulo.application.util.SystemInfo;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FactoryMap;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.MultiMap;
import consulo.util.interner.Interner;
import consulo.util.lang.*;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.impl.internal.change.ui.ChangeListDeltaListener;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Should work under lock of {@link ChangeListManagerImpl#myDataLock}.
 */
public final class ChangeListWorker {
  private final static Logger LOG = Logger.getInstance(ChangeListWorker.class);
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final DelayedNotificator myDelayedNotificator;
  private final boolean myMainWorker;

  private boolean myChangeListsEnabled = true;

  private final Set<ListData> myLists = new HashSet<>();
  private ListData myDefault;

  private final Map<Change, ListData> myChangeMappings = new HashMap<>();
  private final Map<FilePath, PartialChangeTracker> myPartialChangeTrackers = new HashMap<>();

  private final ChangeListsIndexes myIdx = new ChangeListsIndexes();
  private @Nullable AffectedPathSet myAffectedPaths; // used only by main worker, rebuilt on myIdx changes.

  @Nullable
  private Map<ListData, Set<Change>> myReadOnlyChangesCache = null;
  private final AtomicBoolean myReadOnlyChangesCacheInvalidated = new AtomicBoolean(false);

  public ChangeListWorker(@Nonnull Project project, @Nonnull DelayedNotificator delayedNotificator) {
    myProject = project;
    myDelayedNotificator = delayedNotificator;
    myMainWorker = true;

    myAffectedPaths = new AffectedPathSet(Collections.emptyList());
    ensureDefaultListExists();
  }

  private ChangeListWorker(@Nonnull ChangeListWorker worker) {
    myProject = worker.myProject;
    myDelayedNotificator = worker.myDelayedNotificator;
    myChangeListsEnabled = worker.myChangeListsEnabled;
    myMainWorker = false;

    myIdx.copyFrom(worker.myIdx);
    myAffectedPaths = null;

    Map<ListData, ListData> listMapping = copyListsDataFrom(worker.myLists);

    worker.myChangeMappings.forEach((change, oldList) -> {
      ListData newList = notNullList(listMapping.get(oldList));
      myChangeMappings.put(change, newList);
    });

    for (Map.Entry<FilePath, PartialChangeTracker> entry : worker.myPartialChangeTrackers.entrySet()) {
      myPartialChangeTrackers.put(entry.getKey(), new PartialChangeTrackerDump(entry.getValue(), myDefault));
    }
  }

  @Nonnull
  private Map<ListData, ListData> copyListsDataFrom(@Nonnull Collection<ListData> lists) {
    ListData oldDefault = myDefault;
    List<String> oldIds = ContainerUtil.map(myLists, list -> list.id);

    myLists.clear();
    myDefault = null;

    Map<ListData, ListData> listMapping = new HashMap<>();
    for (ListData oldList : lists) {
      ListData newList = new ListData(oldList);
      if (newList.isDefault && myDefault != null) {
        LOG.error("multiple default lists found when copy");
        newList.isDefault = false;
      }

      newList = putNewListData(newList);
      if (newList.isDefault) myDefault = newList;
      listMapping.put(oldList, newList);
    }

    ensureDefaultListExists();


    if (myMainWorker) {
      if (!oldDefault.id.equals(myDefault.id)) {
        fireDefaultListChanged(oldDefault.id, myDefault.id);
      }

      HashSet<String> removedListIds = new HashSet<>(oldIds);
      for (ListData list : myLists) {
        removedListIds.remove(list.id);
      }

      for (String listId : removedListIds) {
        fireChangeListRemoved(listId);
      }
    }


    myReadOnlyChangesCache = null;

    return listMapping;
  }

  private void ensureDefaultListExists() {
    if (myDefault != null) return;

    if (myLists.isEmpty()) {
      putNewListData(new ListData(null, LocalChangeList.getDefaultName()));
    }

    myDefault = myLists.iterator().next();
    myDefault.isDefault = true;
  }

  public ChangeListWorker copy() {
    return new ChangeListWorker(this);
  }


  public void registerChangeTracker(@Nonnull FilePath filePath, @Nonnull PartialChangeTracker tracker) {
    if (myPartialChangeTrackers.containsKey(filePath)) {
      LOG.error(String.format("Attempt to register duplicate trackers: %s; old: %s; new: %s",
                              filePath, myPartialChangeTrackers.get(filePath), tracker));
      return;
    }

    myPartialChangeTrackers.put(filePath, tracker);

    ListData oldList = null;
    Change change = getChangeForAfterPath(filePath);
    if (change != null) {
      oldList = removeChangeMapping(change);
    }

    tracker.initChangeTracking(myDefault.id, ContainerUtil.map(myLists, list -> list.id), oldList != null ? oldList.id : null);

    List<String> oldIds = oldList != null ? Collections.singletonList(oldList.id) : Collections.emptyList();
    notifyChangelistsChanged(filePath, oldIds, tracker.getAffectedChangeListsIds());

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[registerChangeTracker] path: %s, old list: %s", filePath, oldList != null ? oldList.id : "null"));
    }
  }

  public void unregisterChangeTracker(@Nonnull FilePath filePath, @Nonnull PartialChangeTracker tracker) {
    boolean trackerRemoved = myPartialChangeTrackers.remove(filePath, tracker);
    if (trackerRemoved) {
      ListData newList = null;
      Change change = getChangeForAfterPath(filePath);
      if (change != null) {
        newList = getMainList(tracker);
        putChangeMapping(change, newList);
      }

      List<String> newIds = newList != null ? Collections.singletonList(newList.id) : Collections.emptyList();
      notifyChangelistsChanged(filePath, tracker.getAffectedChangeListsIds(), newIds);

      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("[unregisterChangeTracker] path: %s, new list: %s, tracker lists: %s",
                                filePath, newList != null ? newList.id : "null", tracker.getAffectedChangeListsIds()));
      }
    }
    else {
      Map.Entry<FilePath, PartialChangeTracker> entry = ContainerUtil.find(myPartialChangeTrackers.entrySet(),
                                                                           it -> Comparing.equal(it.getValue(), tracker));

      if (entry != null) {
        LOG.error(String.format("Unregistered tracker with wrong path: tracker: %s", tracker));

        FilePath actualFilePath = entry.getKey();
        unregisterChangeTracker(actualFilePath, tracker);
      }
      else {
        LOG.error(String.format("Tracker is not registered: tracker: %s", tracker));
      }
    }
  }

  @Nonnull
  private ListData getMainList(@Nonnull PartialChangeTracker tracker) {
    List<String> changelistIds = tracker.getAffectedChangeListsIds();
    if (changelistIds.size() == 1) {
      ListData list = getDataByIdVerify(changelistIds.get(0));
      if (list != null) return list;
    }
    return myDefault;
  }


  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public LocalChangeList getDefaultList() {
    return toChangeList(myDefault);
  }

  @Nullable
  public LocalChangeList getChangeListByName(@Nullable String name) {
    if (name == null) return null;
    return toChangeList(getDataByName(name));
  }

  @Nullable
  public LocalChangeList getChangeListById(@Nullable String id) {
    if (id == null) return null;
    return toChangeList(getDataById(id));
  }

  @Nonnull
  public List<LocalChangeList> getChangeLists() {
    List<LocalChangeListImpl> lists = ContainerUtil.map(myLists, this::toChangeList);
    return ContainerUtil.sorted(lists, ChangesUtil.CHANGELIST_COMPARATOR);
  }

  @Nonnull
  public List<LocalChangeListImpl> getChangeListsImpl() {
    List<LocalChangeListImpl> lists = ContainerUtil.map(myLists, this::toChangeList);
    return ContainerUtil.sorted(lists, ChangesUtil.CHANGELIST_COMPARATOR);
  }

  public int getChangeListsNumber() {
    return myLists.size();
  }


  @Nullable
  public Change getChangeForPath(@Nullable FilePath filePath) {
    if (filePath == null) return null;
    return myIdx.getChange(filePath);
  }

  @Nullable
  private Change getChangeForAfterPath(@Nullable FilePath filePath) {
    Change change = getChangeForPath(filePath);
    if (change == null) return null;

    ContentRevision after = change.getAfterRevision();
    if (after != null && after.getFile().equals(filePath)) {
      return change;
    }
    return null;
  }

  @Nonnull
  public Collection<Change> getAllChanges() {
    return new ArrayList<>(myIdx.getChanges());
  }

  @Nonnull
  public List<LocalChangeList> getAffectedLists(@Nonnull Collection<? extends Change> changes) {
    return ContainerUtil.map(getAffectedListsData(changes), this::toChangeList);
  }

  @Nonnull
  private List<ListData> getAffectedListsData(@Nonnull Collection<? extends Change> changes) {
    Set<ListData> result = new HashSet<>();

    for (Change change : changes) {
      ListData list = myChangeMappings.get(change);
      if (list != null) {
        result.add(list);
      }
      else {
        PartialChangeTracker tracker = getChangeTrackerFor(change);
        if (tracker != null) {
          Set<ListData> affectedLists = getAffectedLists(tracker);
          if (change instanceof ChangeListChange) {
            String changeListId = ((ChangeListChange)change).getChangeListId();
            ContainerUtil.addIfNotNull(result, ContainerUtil.find(affectedLists, partialList -> partialList.id.equals(changeListId)));
          }
          else {
            result.addAll(affectedLists);
          }
        }
      }
    }

    return new ArrayList<>(result);
  }

  @Nonnull
  private List<Change> getChangesIn(@Nonnull ListData data) {
    List<Change> changes = new ArrayList<>();

    for (Change change : myIdx.getChanges()) {
      ListData list = myChangeMappings.get(change);
      if (list != null) {
        if (list == data) {
          changes.add(change);
        }
      }
      else {
        PartialChangeTracker tracker = getChangeTrackerFor(change);
        if (tracker != null && tracker.getAffectedChangeListsIds().contains(data.id)) {
          changes.add(change);
        }
      }
    }

    return changes;
  }

  @Nonnull
  private Map<ListData, Set<Change>> getChangesMapping() {
    Map<ListData, Set<Change>> map = new HashMap<>();

    for (Change change : myIdx.getChanges()) {
      ListData list = myChangeMappings.get(change);
      if (list != null) {
        Set<Change> listChanges = map.computeIfAbsent(list, key -> new HashSet<>());

        AbstractVcs vcs = myIdx.getVcsFor(change);
        if (vcs != null && vcs.arePartialChangelistsSupported()) {
          listChanges.add(toChangeListChange(change, list));
        }
        else {
          listChanges.add(change);
        }
      }
      else {
        PartialChangeTracker tracker = getChangeTrackerFor(change);
        if (tracker != null) {
          Set<ListData> lists = getAffectedLists(tracker);
          for (ListData partialList : lists) {
            Set<Change> listChanges = map.computeIfAbsent(partialList, key -> new HashSet<>());
            listChanges.add(toChangeListChange(change, partialList));
          }
        }
      }
    }

    return map;
  }


  @Nonnull
  public List<FilePath> getAffectedPaths() {
    return new ArrayList<>(myIdx.getAffectedPaths());
  }

  /**
   * {@link ThreeState#NO} - there are no changed files under this directory
   * {@link ThreeState#YES} - there are modified direct children of this directory
   * {@link ThreeState#UNSURE} - there are modified non-direct children of this directory
   */
  public @Nonnull ThreeState haveChangesUnder(@Nonnull VirtualFile virtualFile) {
    assert myMainWorker;
    if (myAffectedPaths == null) {
      LOG.error("Accessing non-initialized affected files set", new Throwable());
      return ThreeState.NO;
    }

    FilePath filePath = VcsUtil.getFilePath(virtualFile);
    return myAffectedPaths.haveChangesUnder(filePath);
  }

  @Nullable
  public AbstractVcs getVcsFor(@Nonnull Change change) {
    return myIdx.getVcsFor(change);
  }

  @Nullable
  public FileStatus getStatus(@Nonnull VirtualFile file) {
    return myIdx.getStatus(VcsUtil.getFilePath(file));
  }

  @Nullable
  public FileStatus getStatus(@Nonnull FilePath file) {
    return myIdx.getStatus(file);
  }


  @Nullable
  public String setDefaultList(@Nonnull String name) {
    if (!assertChangeListsEnabled()) return null;

    ListData newDefault = getDataByName(name);
    if (newDefault == null || newDefault.isDefault) return null;

    ListData oldDefault = myDefault;

    myDefault.isDefault = false;
    newDefault.isDefault = true;
    myDefault = newDefault;

    fireDefaultListChanged(oldDefault.id, newDefault.id);

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[setDefaultList %s] name: %s id: %s", myMainWorker ? "" : "- updater", name, newDefault.id));
    }

    return oldDefault.name;
  }

  public boolean setReadOnly(@Nonnull String name, boolean value) {
    if (!assertChangeListsEnabled()) return false;

    ListData list = getDataByName(name);
    if (list == null) return false;

    list.isReadOnly = value;

    return true;
  }

  public boolean editName(@Nonnull String fromName, @Nonnull String toName) {
    if (!assertChangeListsEnabled()) return false;

    if (fromName.equals(toName)) return false;
    if (getDataByName(toName) != null) return false;

    final ListData list = getDataByName(fromName);
    if (list == null || list.isReadOnly) return false;

    list.name = toName;

    return true;
  }

  @Nullable
  public String editComment(@Nonnull String name, @Nonnull String newComment) {
    if (!assertChangeListsEnabled()) return null;

    final ListData list = getDataByName(name);
    if (list == null) return null;

    final String oldComment = list.comment;
    if (!Objects.equals(oldComment, newComment)) {
      list.comment = newComment;
    }

    return oldComment;
  }

  public boolean editData(@Nonnull String name, @Nullable ChangeListData newData) {
    if (!assertChangeListsEnabled()) return false;

    final ListData list = getDataByName(name);
    if (list == null) return false;

    list.data = newData;

    return true;
  }


  @Nonnull
  public LocalChangeList addChangeList(@Nonnull String name, @Nullable String description, @Nullable String id,
                                       @Nullable ChangeListData data) {
    ListData listData = addChangeListEntry(name, description, id, data);
    return toChangeList(listData);
  }

  @Nonnull
  private ListData addChangeListEntry(@Nonnull String name, @Nullable String description, @Nullable String id,
                                      @Nullable ChangeListData data) {
    if (!assertChangeListsEnabled()) return myDefault;

    ListData existingList = getDataByName(name);
    if (existingList != null) {
      LOG.error("Attempt to create duplicate changelist " + name);
      return existingList;
    }

    ListData list = new ListData(id, name);
    list.comment = StringUtil.notNullize(description);
    list.data = data;

    list = putNewListData(list);

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[addChangeList %s] name: %s id: %s", myMainWorker ? "" : "- updater", name, list.id));
    }

    return list;
  }

  @Nullable
  public List<Change> removeChangeList(@Nonnull String name) {
    if (!assertChangeListsEnabled()) return null;

    ListData removedList = getDataByName(name);
    if (removedList == null) return null;

    if (removedList.isDefault) {
      LOG.error("Cannot remove default changelist");
      return null;
    }

    List<Change> movedChanges = new ArrayList<>();
    myChangeMappings.replaceAll((change, list) -> {
      if (list == removedList) {
        movedChanges.add(change);
        return myDefault;
      }
      return list;
    });

    fireChangeListRemoved(removedList.id);
    myReadOnlyChangesCache = null;

    myLists.remove(removedList);

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[removeChangeList %s] name: %s id: %s", myMainWorker ? "" : "- updater", name, removedList.id));
    }

    return movedChanges;
  }

  @Nullable
  public MultiMap<LocalChangeList, Change> moveChangesTo(@Nonnull String name, @Nonnull List<? extends Change> changes) {
    if (!assertChangeListsEnabled()) return MultiMap.empty();

    final ListData targetList = getDataByName(name);
    if (targetList == null) return null;

    final MultiMap<ListData, Change> result = new MultiMap<>();

    for (Change change : changes) {
      ListData list = myChangeMappings.get(change);
      if (list != null) {
        if (list != targetList) {
          myChangeMappings.replace(change, targetList);

          result.putValue(list, change);
        }
      }
      else {
        PartialChangeTracker tracker = getChangeTrackerFor(change);
        if (tracker != null) {
          if (change instanceof ChangeListChange) {
            String fromListId = ((ChangeListChange)change).getChangeListId();
            ListData fromList = getDataById(fromListId);

            if (fromList != null && fromList != targetList) {
              tracker.moveChanges(fromList.id, targetList.id);

              result.putValue(fromList, change);
            }
          }
          else {
            HashSet<ListData> fromLists = getAffectedLists(tracker);
            fromLists.remove(targetList);

            if (!fromLists.isEmpty()) {
              tracker.moveChangesTo(targetList.id);

              for (ListData fromList : fromLists) {
                result.putValue(fromList, change);
              }
            }
          }
        }
      }
    }

    myReadOnlyChangesCache = null;

    MultiMap<LocalChangeList, Change> notifications = new MultiMap<>();
    for (Map.Entry<ListData, Collection<Change>> entry : result.entrySet()) {
      notifications.put(toChangeList(entry.getKey()), entry.getValue());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[moveChangesTo %s] name: %s id: %s, changes: %s",
                              myMainWorker ? "" : "- updater", targetList.name, targetList.id, changes));
    }

    return notifications;
  }

  /**
   * Called without external lock
   */
  public void notifyChangelistsChanged(@Nonnull FilePath path,
                                       @Nonnull List<String> beforeChangeListsIds,
                                       @Nonnull List<String> afterChangeListsIds) {
    myReadOnlyChangesCacheInvalidated.set(true);

    Set<String> removed = new HashSet<>(beforeChangeListsIds);
    afterChangeListsIds.forEach(removed::remove);
    Set<String> added = new HashSet<>(afterChangeListsIds);
    beforeChangeListsIds.forEach(added::remove);

    if (!removed.isEmpty() || !added.isEmpty()) {
      // We can't take CLM.LOCK here, so LocalChangeList will be created in delayed notificator itself
      myDelayedNotificator.changeListsForFileChanged(path, removed, added);
    }
  }


  public void applyChangesFromUpdate(@Nonnull ChangeListWorker updatedWorker,
                                     @Nonnull ChangeListDeltaListener deltaListener) {
    assert myMainWorker;
    assert myChangeListsEnabled == updatedWorker.myChangeListsEnabled;
    HashMap<Change, ListData> oldChangeMappings = new HashMap<>(myChangeMappings);

    notifyPathsChanged(myIdx, updatedWorker.myIdx, deltaListener);

    myIdx.copyFrom(updatedWorker.myIdx);
    myAffectedPaths = new AffectedPathSet(myIdx.getAffectedPaths());
    myChangeMappings.clear();

    Map<ListData, ListData> listMapping = copyListsDataFrom(updatedWorker.myLists);

    for (Change change : myIdx.getChanges()) {
      PartialChangeTracker tracker = getChangeTrackerFor(change);
      if (tracker == null) {
        ListData oldList = updatedWorker.myChangeMappings.get(change);

        ListData newList = null;
        if (oldList == null) {
          if (updatedWorker.getChangeTrackerFor(change) == null) {
            LOG.error("Change mapping not found");
          }
        }
        else {
          newList = listMapping.get(oldList);

          if (newList == null) {
            LOG.error("List mapping not found");
          }
        }

        if (newList == null) {
          ListData oldMappedList = oldChangeMappings.get(change);
          if (oldMappedList != null) newList = getDataById(oldMappedList.id);
        }
        if (newList == null) newList = myDefault;

        myChangeMappings.put(change, newList);
      }
    }

    myDelayedNotificator.allChangeListsMappingsChanged();

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format("[applyChangesFromUpdate] %s", this));
    }
  }

  public void setChangeListsEnabled(boolean enabled) {
    if (myChangeListsEnabled == enabled) return;

    LOG.debug("[setChangeListsEnabled] - " + enabled);
    if (enabled) {
      myChangeListsEnabled = true;
      enableChangeLists();
    }
    else {
      disableChangeLists();
      myChangeListsEnabled = false;
    }

    myDelayedNotificator.allChangeListsMappingsChanged();
    myDelayedNotificator.changeListAvailabilityChanged();
    LOG.debug("after [setChangeListsEnabled] - " + enabled);
  }

  private void disableChangeLists() {
    ListData fallbackList = getDataByName(LocalChangeList.getDefaultName());
    if (fallbackList == null) {
      fallbackList = putNewListData(new ListData(null, LocalChangeList.getDefaultName()));
    }

    LocalChangeList oldDefaultList = getDefaultList();

    String oldComment = editComment(fallbackList.name, "");
    boolean readOnlyChanged = setReadOnly(fallbackList.name, true);
    boolean dataChanged = editData(fallbackList.name, null);
    boolean defaultChanged = setDefaultList(fallbackList.name) != null;

    List<LocalChangeList> removedLists = new ArrayList<>();
    for (ListData list : new ArrayList<>(myLists)) {
      if (list.isDefault) continue;
      removedLists.add(getChangeListByName(list.name));
      removeChangeList(list.name);
    }

    LocalChangeListImpl newList = toChangeList(fallbackList);
    if (!StringUtil.isEmpty(oldComment)) myDelayedNotificator.changeListCommentChanged(newList, oldComment);
    if (readOnlyChanged) myDelayedNotificator.changeListChanged(newList);
    if (dataChanged) myDelayedNotificator.changeListDataChanged(newList);
    if (defaultChanged) myDelayedNotificator.defaultListChanged(oldDefaultList, newList, false);

    for (LocalChangeList oldList : removedLists) {
      Collection<Change> movedChanges = oldList.getChanges();
      if (!movedChanges.isEmpty()) myDelayedNotificator.changesMoved(movedChanges, oldList, newList);
      myDelayedNotificator.changeListRemoved(oldList);
    }
  }

  private void enableChangeLists() {
    LOG.assertTrue(myLists.size() == 1);
    boolean readOnlyChanged = setReadOnly(myDefault.name, false);

    LocalChangeListImpl newList = toChangeList(myDefault);
    if (readOnlyChanged) myDelayedNotificator.changeListChanged(newList);
  }

  public boolean areChangeListsEnabled() {
    return myChangeListsEnabled;
  }

  private boolean assertChangeListsEnabled() {
    if (myChangeListsEnabled) return true;
    LOG.error("Changelists are disabled, modification ignored", new Throwable());
    return false;
  }

  private static void notifyPathsChanged(@Nonnull ChangeListsIndexes was, @Nonnull ChangeListsIndexes became,
                                         @Nonnull ChangeListDeltaListener deltaListener) {
    final Set<BaseRevision> toRemove = new HashSet<>();
    final Set<BaseRevision> toAdd = new HashSet<>();
    final Set<BeforeAfter<BaseRevision>> toModify = new HashSet<>();
    was.getDelta(became, toRemove, toAdd, toModify);

    for (BaseRevision pair : toRemove) {
      deltaListener.removed(pair);
    }
    for (BaseRevision pair : toAdd) {
      deltaListener.added(pair);
    }
    for (BeforeAfter<BaseRevision> beforeAfter : toModify) {
      deltaListener.modified(beforeAfter.getBefore(), beforeAfter.getAfter());
    }
  }

  public void setChangeLists(@Nonnull Collection<LocalChangeListImpl> lists) {
    assert myMainWorker;
    if (!myChangeListsEnabled) return;

    myIdx.clear();
    myChangeMappings.clear();

    copyListsDataFrom(ContainerUtil.map(lists, ListData::new));

    for (LocalChangeListImpl list : lists) {
      ListData listData = notNullList(getDataByIdVerify(list.getId()));

      for (Change change : list.getChanges()) {
        if (myIdx.getChanges().contains(change)) continue;

        myIdx.changeAdded(change, null);

        PartialChangeTracker tracker = getChangeTrackerFor(change);
        if (tracker == null) {
          myChangeMappings.put(change, listData);
        }
      }
    }

    myAffectedPaths = new AffectedPathSet(myIdx.getAffectedPaths());

    myDelayedNotificator.allChangeListsMappingsChanged();
  }


  private void fireDefaultListChanged(@Nonnull String oldDefaultId, @Nonnull String newDefaultId) {
    for (PartialChangeTracker tracker : myPartialChangeTrackers.values()) {
      tracker.defaultListChanged(oldDefaultId, newDefaultId);
    }
  }

  private void fireChangeListRemoved(@Nonnull String listId) {
    for (PartialChangeTracker tracker : myPartialChangeTrackers.values()) {
      tracker.changeListRemoved(listId);
    }
  }


  @Nullable
  private ListData removeChangeMapping(@Nonnull Change change) {
    ListData oldList = myChangeMappings.remove(change);
    myReadOnlyChangesCache = null;
    return oldList;
  }

  private void putChangeMapping(@Nonnull Change change, @Nonnull ListData list) {
    myChangeMappings.put(change, list);
    myReadOnlyChangesCache = null;
  }

  @Nonnull
  private ListData putNewListData(@Nonnull ListData list) {
    ListData listWithSameName = getDataByName(list.name);
    if (listWithSameName != null) {
      LOG.error(String.format("Attempt to create changelist with same name: %s - %s", list.name, list.id));
      return listWithSameName;
    }

    ListData listWithSameId = getDataById(list.id);
    if (listWithSameId != null) {
      LOG.error(String.format("Attempt to create changelist with same id: %s - %s", list.name, list.id));
      return listWithSameId;
    }

    myLists.add(list);
    return list;
  }

  @Nonnull
  private ListData notNullList(@Nullable ListData listData) {
    if (listData == null) LOG.error("ListData not found");
    return ObjectUtil.notNull(listData, myDefault);
  }

  @Nullable
  private ListData getDataById(@Nonnull String id) {
    return ContainerUtil.find(myLists, list -> list.id.equals(id));
  }

  @Nullable
  private ListData getDataByName(@Nonnull String name) {
    return ContainerUtil.find(myLists, list -> list.name.equals(name));
  }

  @Nullable
  private ListData getDataByIdVerify(@Nonnull String id) {
    ListData list = getDataById(id);
    if (myMainWorker && list == null) LOG.error(String.format("Unknown changelist %s", id));
    return list;
  }

  @Nullable
  private PartialChangeTracker getChangeTrackerFor(@Nonnull Change change) {
    if (!myChangeListsEnabled) return null;
    if (myPartialChangeTrackers.isEmpty()) return null;

    if (!myIdx.getChanges().contains(change)) return null;
    ContentRevision revision = change.getAfterRevision();
    if (revision == null) return null;
    return myPartialChangeTrackers.get(revision.getFile());
  }

  @Nonnull
  private HashSet<ListData> getAffectedLists(@Nonnull PartialChangeTracker tracker) {
    HashSet<ListData> data = new HashSet<>();
    for (String listId : tracker.getAffectedChangeListsIds()) {
      ListData partialList = getDataById(listId);
      if (myMainWorker && partialList == null) {
        LOG.warn(String.format("Unknown changelist %s for file %s", listId, tracker));
        tracker.initChangeTracking(myDefault.id, ContainerUtil.map(myLists, list -> list.id), null);
      }
      data.add(partialList != null ? partialList : myDefault);
    }
    return data;
  }

  @Contract("!null -> !null; null -> null")
  private LocalChangeListImpl toChangeList(@Nullable ListData data) {
    if (data == null) return null;

    if (myReadOnlyChangesCache == null || myReadOnlyChangesCacheInvalidated.get()) {
      myReadOnlyChangesCacheInvalidated.set(false);
      myReadOnlyChangesCache = getChangesMapping();
    }
    Set<Change> cachedChanges = myReadOnlyChangesCache.get(data);
    Set<Change> changes = cachedChanges != null ? Collections.unmodifiableSet(cachedChanges) : Collections.emptySet();

    return buildChangeListFrom(data)
      .setChangesCollection(changes)
      .build();
  }

  /**
   * Unlike {@link #toChangeList(ListData)}, will not populate {@link LocalChangeList#getChanges()}.
   */
  @Contract("!null -> !null; null -> null")
  private LocalChangeListImpl toLightChangeList(@Nullable ListData data) {
    if (data == null) return null;

    return buildChangeListFrom(data)
      .build();
  }

  @Nonnull
  private LocalChangeListImpl.Builder buildChangeListFrom(@Nonnull ListData data) {
    return new LocalChangeListImpl.Builder(myProject, data.name)
      .setId(data.id)
      .setComment(data.comment)
      .setData(data.data)
      .setDefault(data.isDefault)
      .setReadOnly(data.isReadOnly);
  }

  @Nonnull
  private static Change toChangeListChange(@Nonnull Change change, @Nonnull ListData list) {
    if (change.getClass() == Change.class) {
      return new ChangeListChange(change, list.name, list.id);
    }
    return change;
  }

  private static final class ListData {
    @Nonnull
    public final String id;

    @Nonnull
    public String name;
    @Nonnull
    public String comment = "";
    @Nullable
    public ChangeListData data;

    public boolean isDefault = false;
    public boolean isReadOnly = false; // read-only lists cannot be removed or renamed

    ListData(@Nullable String id, @Nonnull String name) {
      this.id = id != null ? id : LocalChangeListImpl.generateChangelistId();
      this.name = name;
    }

    ListData(@Nonnull LocalChangeListImpl list) {
      this.id = list.getId();
      this.name = list.getName();
      this.comment = list.getComment();
      this.data = list.getData();
      this.isDefault = list.isDefault();
      this.isReadOnly = list.isReadOnly();
    }

    ListData(@Nonnull ListData list) {
      this.id = list.id;
      this.name = list.name;
      this.comment = list.comment;
      this.data = list.data;
      this.isDefault = list.isDefault;
      this.isReadOnly = list.isReadOnly;
    }
  }

  @Override
  @NonNls
  public String toString() {
    String lists = StringUtil.join(myLists, list -> String.format("list: %s (%s) changes: %s", list.name, list.id,
                                                                  StringUtil.join(getChangesIn(list), ", ")), "\n"); //NON-NLS
    String trackers = StringUtil.join(myPartialChangeTrackers.entrySet(),
                                      (entry) -> entry.getKey() + " " + entry.getValue().getAffectedChangeListsIds(), ",");
    return String.format("ChangeListWorker{ default = %s, lists = {\n%s }\ntrackers = %s\n}", myDefault.id, lists, trackers);
  }

  public static final class ChangeListUpdater implements ChangeListManagerGate {
    private final ChangeListWorker myWorker;

    @SuppressWarnings("SSBasedInspection")
    private final Map<String, Interner<Change>> myChangesBeforeUpdateMap =
      FactoryMap.create(it -> Interner.createHashInterner(HashingStrategy.canonical()));

    private static final String CONFLICT_CHANGELIST_ID = "";
    private final Map<FilePath, String> myListsForPathsBeforeUpdate = new HashMap<>();

    private final Set<String> myListsToDisappear = new HashSet<>();

    public ChangeListUpdater(@Nonnull ChangeListWorker worker) {
      myWorker = worker.copy();
    }

    @Nonnull
    public Project getProject() {
      return myWorker.getProject();
    }


    public void notifyStartProcessingChanges(@Nullable VcsModifiableDirtyScope scope) {
      myWorker.myChangeMappings.forEach((change, list) -> {
        putPathBeforeUpdate(ChangesUtil.getBeforePath(change), list.id);
        putPathBeforeUpdate(ChangesUtil.getAfterPath(change), list.id);

        myChangesBeforeUpdateMap.get(list.id).intern(change);
      });

      List<Change> removedChanges = removeChangesUnderScope(scope);

      // scope should be modified for correct moves tracking
      if (scope != null) {
        for (Change change : removedChanges) {
          if (change.isMoved() || change.isRenamed()) {
            scope.addDirtyFile(change.getBeforeRevision().getFile());
            scope.addDirtyFile(change.getAfterRevision().getFile());
          }
        }
      }

      for (ChangeListChangeAssigner extension : myWorker.getProject().getExtensionPoint(ChangeListChangeAssigner.class)) {
        try {
          extension.beforeChangesProcessing(scope);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }

    private void putPathBeforeUpdate(@Nullable FilePath path, @Nonnull String listId) {
      if (path == null) return;

      String oldListId = myListsForPathsBeforeUpdate.get(path);
      if (CONFLICT_CHANGELIST_ID.equals(oldListId) || listId.equals(oldListId)) return;

      if (oldListId == null) {
        myListsForPathsBeforeUpdate.put(path, listId);
      }
      else {
        myListsForPathsBeforeUpdate.put(path, CONFLICT_CHANGELIST_ID);
      }
    }

    @Nullable
    private String guessChangeListByPaths(@Nonnull Change change) {
      FilePath bPath = ChangesUtil.getBeforePath(change);
      FilePath aPath = ChangesUtil.getAfterPath(change);

      String bListId = myListsForPathsBeforeUpdate.get(bPath);
      String aListId = myListsForPathsBeforeUpdate.get(aPath);
      if (CONFLICT_CHANGELIST_ID.equals(bListId) || CONFLICT_CHANGELIST_ID.equals(aListId)) return null;
      if (bListId == null && aListId == null) return null;
      if (bListId == null) return aListId;
      if (aListId == null) return bListId;
      return bListId.equals(aListId) ? bListId : null;
    }

    @Nonnull
    private List<Change> removeChangesUnderScope(@Nullable VcsDirtyScope scope) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Process scope: %s", scope));
      }

      List<Change> removed = new ArrayList<>();
      for (Change change : myWorker.myIdx.getChanges()) {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        boolean isUnderScope = scope == null ||
          before != null && scope.belongsTo(before.getFile()) ||
          after != null && scope.belongsTo(after.getFile()) ||
          isIgnoredChange(before, after, getProject());
        if (isUnderScope) {
          removed.add(change);
        }
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("under scope - %s, change - %s", isUnderScope, change));
        }
      }

      for (Change change : removed) {
        myWorker.myIdx.changeRemoved(change);
        myWorker.removeChangeMapping(change);
      }

      return removed;
    }

    private static boolean isIgnoredChange(@Nullable ContentRevision before, @Nullable ContentRevision after, @Nonnull Project project) {
      return isIgnoredRevision(before, project) && isIgnoredRevision(after, project);
    }

    private static boolean isIgnoredRevision(@Nullable ContentRevision revision, final @Nonnull Project project) {
      if (revision == null) return true;
      return ReadAction.compute(() -> {
        if (project.isDisposed()) return false;
        return ProjectLevelVcsManager.getInstance(project).isIgnored(revision.getFile());
      });
    }


    public void notifyDoneProcessingChanges(@Nonnull DelayedNotificator dispatcher, @Nullable VcsDirtyScope scope) {
      List<ChangeList> changedLists = new ArrayList<>();
      final Map<LocalChangeListImpl, List<Change>> removedChanges = new HashMap<>();
      final Map<LocalChangeListImpl, List<Change>> addedChanges = new HashMap<>();
      for (ChangeListWorker.ListData list : myWorker.myLists) {
        final List<Change> removed = new ArrayList<>();
        final List<Change> added = new ArrayList<>();
        doneProcessingChanges(list, removed, added);

        LocalChangeListImpl changeList = myWorker.toChangeList(list);
        if (!removed.isEmpty() || !added.isEmpty()) {
          changedLists.add(changeList);
        }
        if (!removed.isEmpty()) {
          removedChanges.put(changeList, removed);
        }
        if (!added.isEmpty()) {
          addedChanges.put(changeList, added);
        }
      }
      removedChanges.forEach((changeList, changes) -> dispatcher.changesRemoved(changes, changeList));
      addedChanges.forEach((changeList, changes) -> dispatcher.changesAdded(changes, changeList));
      for (ChangeList changeList : changedLists) {
        dispatcher.changeListChanged(changeList);
      }

      for (String name : myListsToDisappear) {
        final ChangeListWorker.ListData list = myWorker.getDataByName(name);
        if (list != null && myWorker.getChangesIn(list).isEmpty() && !list.isReadOnly && !list.isDefault) {
          myWorker.removeChangeList(name);
        }
      }
      myListsToDisappear.clear();

      myChangesBeforeUpdateMap.clear();
      myListsForPathsBeforeUpdate.clear();

      for (ChangeListChangeAssigner extension : myWorker.getProject().getExtensionPoint(ChangeListChangeAssigner.class)) {
        try {
          extension.markChangesProcessed(scope);
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }

    private void doneProcessingChanges(@Nonnull ChangeListWorker.ListData list,
                                       @Nonnull List<? super Change> removedChanges,
                                       @Nonnull List<? super Change> addedChanges) {
      Interner<Change> changesBeforeUpdate = myChangesBeforeUpdateMap.get(list.id);

      Set<Change> listChanges = new HashSet<>(myWorker.getChangesIn(list));

      for (Change newChange : listChanges) {
        Change oldChange = findOldChange(changesBeforeUpdate, newChange);
        if (oldChange == null) {
          addedChanges.add(newChange);
        }
      }

      removedChanges.addAll(changesBeforeUpdate.getValues());
      removedChanges.removeAll(listChanges);
    }

    @Nullable
    private static Change findOldChange(@Nonnull Interner<Change> changesBeforeUpdate, @Nonnull Change newChange) {
      Change oldChange = changesBeforeUpdate.get(newChange);
      if (oldChange != null && sameBeforeRevision(oldChange, newChange) &&
        newChange.getFileStatus().equals(oldChange.getFileStatus())) {
        return oldChange;
      }
      return null;
    }

    private static boolean sameBeforeRevision(@Nonnull Change change1, @Nonnull Change change2) {
      final ContentRevision b1 = change1.getBeforeRevision();
      final ContentRevision b2 = change2.getBeforeRevision();
      if (b1 != null && b2 != null) {
        final VcsRevisionNumber rn1 = b1.getRevisionNumber();
        final VcsRevisionNumber rn2 = b2.getRevisionNumber();
        return rn1 != VcsRevisionNumber.NULL && rn2 != VcsRevisionNumber.NULL &&
          b1.getClass() == b2.getClass() && rn1.compareTo(rn2) == 0;
      }
      return b1 == null && b2 == null;
    }


    public void finish() {
      checkForMultipleCopiesNotMove();
    }

    @Nonnull
    public ChangeListWorker getUpdatedWorker() {
      return myWorker;
    }

    private void checkForMultipleCopiesNotMove() {
      final MultiMap<FilePath, Change> moves = new MultiMap<>();

      for (Change change : myWorker.myIdx.getChanges()) {
        if (change.isMoved() || change.isRenamed()) {
          moves.putValue(change.getBeforeRevision().getFile(), change);
        }
      }
      for (FilePath filePath : moves.keySet()) {
        final List<Change> copies = (List<Change>)moves.get(filePath);
        if (copies.size() == 1) continue;
        copies.sort(CHANGES_AFTER_REVISION_COMPARATOR);
        for (int i = 0; i < (copies.size() - 1); i++) {
          final Change oldChange = copies.get(i);
          final Change newChange = new Change(null, oldChange.getAfterRevision());

          final AbstractVcs vcs = myWorker.myIdx.getVcsFor(oldChange);

          myWorker.myIdx.changeRemoved(oldChange);
          myWorker.myIdx.changeAdded(newChange, vcs);

          ListData list = myWorker.removeChangeMapping(oldChange);
          if (list != null) myWorker.putChangeMapping(newChange, list);
        }
      }
    }

    private final Comparator<Change> CHANGES_AFTER_REVISION_COMPARATOR = (o1, o2) -> {
      String s1 = o1.getAfterRevision().getFile().getPresentableUrl();
      String s2 = o2.getAfterRevision().getFile().getPresentableUrl();
      return SystemInfo.isFileSystemCaseSensitive ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2);
    };


    public void addChangeToList(@Nonnull String name, @Nonnull Change change, AbstractVcs vcs) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("[addChangeToList] name: %s change: %s vcs: %s", name, ChangesUtil.getFilePath(change).getPath(),
                                vcs == null ? null : vcs.getName()));
      }

      ListData list = myWorker.getDataByName(name);
      if (list != null) {
        addChangeToList(list, change, vcs);
      }
      else {
        LOG.error(String.format("Changelist not found: vcs - %s", vcs == null ? null : vcs.getName()));
        addChangeToCorrespondingList(change, vcs);
      }
    }

    public void addChangeToCorrespondingList(@Nonnull Change change, AbstractVcs vcs) {
      ListData listData = guessListForChange(change);
      addChangeToList(listData, change, vcs);
    }

    @Nullable
    private ListData guessListForChange(@Nonnull Change change) {
      if (LOG.isDebugEnabled()) {
        final String path = ChangesUtil.getFilePath(change).getPath();
        LOG.debug("[addChangeToCorrespondingList] for change " + path + " type: " + change.getType() +
                    " have before revision: " + (change.getBeforeRevision() != null));
      }

      for (ChangeListWorker.ListData list : myWorker.myLists) {
        Interner<Change> changesBeforeUpdate = myChangesBeforeUpdateMap.get(list.id);
        if (changesBeforeUpdate.get(change) != null) {
          LOG.debug("[addChangeToCorrespondingList] matched by change: ", list.name);
          return list;
        }
      }

      String listId = guessChangeListByPaths(change);
      if (listId != null) {
        ListData list = myWorker.getDataById(listId);
        if (list != null) {
          LOG.debug("[addChangeToCorrespondingList] matched by paths: ", list.name);
          return list;
        }
      }

      String assignedChangeListId = myWorker.getProject().getExtensionPoint(ChangeListChangeAssigner.class).computeSafeIfAny(assigner -> {
        return assigner.getChangeListIdFor(change, this);
      });
      if (assignedChangeListId != null) {
        ListData list = myWorker.getDataById(assignedChangeListId);
        if (list != null) {
          LOG.debug("[addChangeToCorrespondingList] added to list from assigner: ", list.name);
          return list;
        }
        else {
          LOG.debug("[addChangeToCorrespondingList] failed to add to non-existent list from assigner: ", assignedChangeListId);
        }
      }

      ContentRevision revision = change.getAfterRevision();
      if (revision != null && myWorker.myChangeListsEnabled && myWorker.myPartialChangeTrackers.get(revision.getFile()) != null) {
        LOG.debug("[addChangeToCorrespondingList] partial tracker found");
        return null;
      }

      LOG.debug("[addChangeToCorrespondingList] added to default list");
      return myWorker.myDefault;
    }

    private void addChangeToList(@Nullable ListData list, @Nonnull Change change, AbstractVcs vcs) {
      if (myWorker.myIdx.getChanges().contains(change)) {
        LOG.warn(String.format("Multiple equal changes added: %s", change));
        return;
      }

      myWorker.myIdx.changeAdded(change, vcs);
      if (list != null) {
        myWorker.putChangeMapping(change, list);
      }
      myWorker.myReadOnlyChangesCache = null;
    }

    public void removeRegisteredChangeFor(@Nullable FilePath filePath) {
      Change change = myWorker.getChangeForPath(filePath);
      if (change == null) return;

      myWorker.myIdx.changeRemoved(change);
      myWorker.removeChangeMapping(change);
    }


    @Nonnull
    @Override
    public List<LocalChangeList> getListsCopy() {
      return myWorker.getChangeLists();
    }

    @Nullable
    @Override
    public LocalChangeList findChangeList(@Nullable String name) {
      if (name == null) return null;
      ListData data = myWorker.getDataByName(name);
      if (data == null) return null;
      // Skip changes for performance reasons.
      // We're in the middle of filling lists with changes from VCS,
      // while each 'addChangeToList' call clears 'myReadOnlyChangesCache' cache.
      // Thus, a populating list with changes will re-build 'myReadOnlyChangesCache' for each invocation.
      return myWorker.toLightChangeList(data);
    }

    @Nonnull
    @Override
    public LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment) {
      ListData data = myWorker.addChangeListEntry(name, comment, null, null);
      return myWorker.toLightChangeList(data);
    }

    @Nonnull
    @Override
    public LocalChangeList findOrCreateList(@Nonnull String name, @Nullable String comment) {
      LocalChangeList list = findChangeList(name);
      if (list != null) return list;
      return addChangeList(name, comment);
    }

    @Override
    public void editComment(@Nonnull String name, @Nullable String comment) {
      myWorker.editComment(name, StringUtil.notNullize(comment));
    }

    @Override
    public void editName(@Nonnull String oldName, @Nonnull String newName) {
      myWorker.editName(oldName, newName);
    }

    @Override
    public void setListsToDisappear(@Nonnull Collection<String> names) {
      myListsToDisappear.addAll(names);
    }

    @Override
    public FileStatus getStatus(@Nonnull VirtualFile file) {
      return myWorker.getStatus(file);
    }

    @Override
    public FileStatus getStatus(@Nonnull FilePath filePath) {
      return myWorker.getStatus(filePath);
    }

    @Override
    public void setDefaultChangeList(@Nonnull String list) {
      myWorker.setDefaultList(list);
    }
  }


  private static class PartialChangeTrackerDump implements PartialChangeTracker {
    @Nonnull
    private final Set<String> myChangeListsIds;
    @Nonnull
    private String myDefaultId;

    PartialChangeTrackerDump(@Nonnull PartialChangeTracker tracker,
                             @Nonnull ListData defaultList) {
      myChangeListsIds = new HashSet<>(tracker.getAffectedChangeListsIds());
      myDefaultId = defaultList.id;
    }

    @Nonnull
    @Override
    public List<String> getAffectedChangeListsIds() {
      return new ArrayList<>(myChangeListsIds);
    }

    @Override
    public void initChangeTracking(@Nonnull String defaultId, @Nonnull List<String> changelistsId, @Nullable String fileChangelistIds) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void defaultListChanged(@Nonnull String oldListId, @Nonnull String newListId) {
      myDefaultId = newListId;
    }

    @Override
    public void changeListRemoved(@Nonnull String listId) {
      myChangeListsIds.remove(listId);
      if (myChangeListsIds.isEmpty()) myChangeListsIds.add(myDefaultId);
    }

    @Override
    public void moveChanges(@Nonnull String fromListId, @Nonnull String toListId) {
      if (myChangeListsIds.remove(fromListId)) {
        myChangeListsIds.add(toListId);
      }
    }

    @Override
    public void moveChangesTo(@Nonnull String toListId) {
      myChangeListsIds.clear();
      myChangeListsIds.add(toListId);
    }
  }

  public interface PartialChangeTracker {
    @Nonnull
    List<String> getAffectedChangeListsIds();

    void initChangeTracking(@Nonnull String defaultId, @Nonnull List<String> changelistsIds, @Nullable String fileChangelistId);

    void defaultListChanged(@Nonnull String oldListId, @Nonnull String newListId);

    void changeListRemoved(@Nonnull String listId);

    void moveChanges(@Nonnull String fromListId, @Nonnull String toListId);

    void moveChangesTo(@Nonnull String toListId);
  }
}
