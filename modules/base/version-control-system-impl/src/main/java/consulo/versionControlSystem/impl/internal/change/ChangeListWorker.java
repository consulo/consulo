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
package consulo.versionControlSystem.impl.internal.change;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;

/**
 * should work under _external_ lock
 * just logic here: do modifications to group of change lists
 */
public class ChangeListWorker implements ChangeListsWriteOperations {
  private final static Logger LOG = Logger.getInstance(ChangeListWorker.class);

  private final Project myProject;
  private final Map<String, LocalChangeList> myMap;
  // in fact, a kind of local change
  private final DeletedFilesHolder myLocallyDeleted;
  private final SwitchedFileHolder mySwitchedHolder;
  private LocalChangeList myDefault;

  private ChangeListsIndexes myIdx;
  private final ChangesDelta myDelta;
  private final Set<String> myListsToDisappear;

  public ChangeListWorker(Project project, PlusMinusModify<BaseRevision> deltaListener) {
    myProject = project;
    myMap = new LinkedHashMap<>();
    myIdx = new ChangeListsIndexes();
    myLocallyDeleted = new DeletedFilesHolder();
    mySwitchedHolder = new SwitchedFileHolder(project, FileHolder.HolderType.SWITCHED);

    myDelta = new ChangesDelta(deltaListener);
    myListsToDisappear = new LinkedHashSet<>();
  }

  private ChangeListWorker(ChangeListWorker worker) {
    myProject = worker.myProject;
    myMap = new LinkedHashMap<>();
    myIdx = new ChangeListsIndexes(worker.myIdx);
    myLocallyDeleted = worker.myLocallyDeleted.copy();
    mySwitchedHolder = worker.mySwitchedHolder.copy();
    myDelta = worker.myDelta;
    myListsToDisappear = new LinkedHashSet<>(worker.myListsToDisappear);

    LocalChangeList defaultList = null;
    for (LocalChangeList changeList : worker.myMap.values()) {
      LocalChangeList copy = changeList.copy();

      String changeListName = copy.getName();
      myMap.put(changeListName, copy);
      if (copy.isDefault()) {
        defaultList = copy;
      }
    }
    if (defaultList == null) {
      LOG.info("default list not found when copy");
      defaultList = myMap.get(worker.getDefaultListName());
    }

    if (defaultList == null) {
      LOG.info("default list not found when copy in original object too");
      if (!myMap.isEmpty()) {
        defaultList = myMap.values().iterator().next();
      }
      else {
        // can be when there's no vcs configured
        ///LOG.error("no changelists at all");
      }
    }
    myDefault = defaultList;
  }

  public void onAfterWorkerSwitch(@Nonnull ChangeListWorker previous) {
    checkForMultipleCopiesNotMove(myDelta.step(previous.myIdx, myIdx));
  }

  private void checkForMultipleCopiesNotMove(boolean somethingChanged) {
    MultiMap<FilePath, Pair<Change, String>> moves = new MultiMap<>() {
      @Override
      @Nonnull
      protected Collection<Pair<Change, String>> createCollection() {
        return new LinkedList<>();
      }
    };

    for (LocalChangeList changeList : myMap.values()) {
      Collection<Change> changes = changeList.getChanges();
      for (Change change : changes) {
        if (change.isMoved() || change.isRenamed()) {
          moves.putValue(change.getBeforeRevision().getFile(), Pair.create(change, changeList.getName()));
        }
      }
    }
    for (FilePath filePath : moves.keySet()) {
      List<Pair<Change, String>> copies = (List<Pair<Change, String>>)moves.get(filePath);
      if (copies.size() == 1) continue;
      copies.sort(MyChangesAfterRevisionComparator.getInstance());
      for (int i = 0; i < (copies.size() - 1); i++) {
        somethingChanged = true;
        Pair<Change, String> item = copies.get(i);
        Change oldChange = item.getFirst();
        Change newChange = new Change(null, oldChange.getAfterRevision());

        LocalChangeListImpl list = (LocalChangeListImpl)myMap.get(item.getSecond());
        list.removeChange(oldChange);
        list.addChange(newChange);

        VcsKey key = myIdx.getVcsFor(oldChange);
        myIdx.changeRemoved(oldChange);
        myIdx.changeAdded(newChange, key);
      }
    }
    if (somethingChanged) {
      FileStatusManager.getInstance(myProject).fileStatusesChanged();
    }
  }

  public ChangeListWorker copy() {
    return new ChangeListWorker(this);
  }

  public boolean findListByName(@Nonnull String name) {
    return myMap.containsKey(name);
  }

  @Nullable
  public LocalChangeList getCopyByName(String name) {
    return myMap.get(name);
  }

  @Nullable
  public LocalChangeList getChangeList(String id) {
    for (LocalChangeList changeList : myMap.values()) {
      if (changeList.getId().equals(id)) {
        return changeList.copy();
      }
    }
    return null;
  }

  /**
   * @return if list with name exists, return previous default list name or null of there wasn't previous
   */
  @Override
  @Nullable
  public String setDefault(String name) {
    LocalChangeList newDefault = myMap.get(name);
    if (newDefault == null) {
      return null;
    }
    String previousName = null;
    if (myDefault != null) {
      ((LocalChangeListImpl)myDefault).setDefault(false);
      previousName = myDefault.getName();
    }

    ((LocalChangeListImpl)newDefault).setDefault(true);
    myDefault = newDefault;

    return previousName;
  }

  @Override
  public boolean setReadOnly(String name, boolean value) {
    LocalChangeList list = myMap.get(name);
    if (list != null) {
      list.setReadOnly(value);
    }
    return list != null;
  }

  @Override
  public LocalChangeList addChangeList(@Nonnull String name, @Nullable String comment, @Nullable Object data) {
    return addChangeList(null, name, comment, false, data);
  }

  LocalChangeList addChangeList(String id, @Nonnull String name, @Nullable String description, boolean inUpdate,
                                @Nullable Object data) {
    boolean contains = myMap.containsKey(name);
    LOG.assertTrue(!contains, "Attempt to create duplicate changelist " + name);
    LocalChangeListImpl newList = (LocalChangeListImpl)LocalChangeList.createEmptyChangeList(myProject, name);
    newList.setData(data);

    if (description != null) {
      newList.setCommentImpl(description);
    }
    if (id != null) {
      newList.setId(id);
    }
    myMap.put(name, newList);
    if (inUpdate) {
      // scope is not important: nothing had been added jet, nothing to move to "old state" members
      newList.startProcessingChanges(myProject, null);      // this is executed only when use through GATE
    }
    return newList.copy();
  }

  public boolean addChangeToList(@Nonnull String name, Change change, VcsKey vcsKey) {
    LOG.debug("[addChangeToList] name: " + name + " change: " + ChangesUtil.getFilePath(change).getPath() + " vcs: " +
                (vcsKey == null ? null : vcsKey.getName()));
    LocalChangeList changeList = myMap.get(name);
    if (changeList != null) {
      ((LocalChangeListImpl)changeList).addChange(change);
      myIdx.changeAdded(change, vcsKey);
    }
    return changeList != null;
  }

  public void addChangeToCorrespondingList(@Nonnull Change change, VcsKey vcsKey) {
    String path = ChangesUtil.getFilePath(change).getPath();
    LOG.debug("[addChangeToCorrespondingList] for change " + path + " type: " + change.getType() + " have before revision: " + (change.getBeforeRevision() != null));
    assert myDefault != null;
    for (LocalChangeList list : myMap.values()) {
      if (list.isDefault()) {
        LOG.debug("[addChangeToCorrespondingList] skip default list: " + list.getName() + " type: " + change.getType() + " have before revision: " + (change.getBeforeRevision() != null));
        continue;
      }
      if (((LocalChangeListImpl)list).processChange(change)) {
        LOG.debug("[addChangeToCorrespondingList] matched: " + list.getName() + " type: " + change.getType() + " have before revision: " + (change.getBeforeRevision() != null));
        myIdx.changeAdded(change, vcsKey);
        return;
      }
    }
    ((LocalChangeListImpl)myDefault).processChange(change);
    myIdx.changeAdded(change, vcsKey);
  }

  @Override
  public boolean removeChangeList(@Nonnull String name) {
    LocalChangeList list = myMap.get(name);
    if (list == null) {
      return false;
    }
    if (list.isDefault()) {
      throw new RuntimeException(new UnsupportedOperationException("Cannot remove default changelist"));
    }
    String listName = list.getName();

    for (Change change : list.getChanges()) {
      ((LocalChangeListImpl)myDefault).addChange(change);
    }

    LocalChangeList removed = myMap.remove(listName);
    return true;
  }

  @Override
  @Nullable
  public MultiMap<LocalChangeList, Change> moveChangesTo(String name, Change[] changes) {
    LocalChangeListImpl changeList = (LocalChangeListImpl)myMap.get(name);
    if (changeList != null) {
      MultiMap<LocalChangeList, Change> result = new MultiMap<>();
      for (LocalChangeList list : myMap.values()) {
        if (list.equals(changeList)) continue;
        for (Change change : changes) {
          Change removedChange = ((LocalChangeListImpl)list).removeChange(change);
          if (removedChange != null) {
            changeList.addChange(removedChange);
            result.putValue(list, removedChange);
          }
        }
      }
      return result;
    }
    return null;
  }

  @Override
  public boolean editName(@Nonnull String fromName, @Nonnull String toName) {
    if (fromName.equals(toName)) return false;
    LocalChangeList list = myMap.get(fromName);
    boolean canEdit = list != null && (!list.isReadOnly());
    if (canEdit) {
      LocalChangeListImpl listImpl = (LocalChangeListImpl)list;
      listImpl.setNameImpl(toName);
      myMap.remove(fromName);
      myMap.put(toName, list);
    }
    return canEdit;
  }

  @Override
  @Nullable
  public String editComment(@Nonnull String fromName, String newComment) {
    LocalChangeList list = myMap.get(fromName);
    if (list != null) {
      String oldComment = list.getComment();
      if (!Comparing.equal(oldComment, newComment)) {
        LocalChangeListImpl listImpl = (LocalChangeListImpl)list;
        listImpl.setCommentImpl(newComment);
      }
      return oldComment;
    }
    return null;
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  @Nullable
  public LocalChangeList getDefaultListCopy() {
    return myDefault == null ? null : myDefault.copy();
  }

  public boolean isDefaultList(LocalChangeList list) {
    return myDefault != null && list.getId().equals(myDefault.getId());
  }

  public Project getProject() {
    return myProject;
  }

  // called NOT under ChangeListManagerImpl lock
  public void notifyStartProcessingChanges(VcsModifiableDirtyScope scope) {
    Collection<Change> oldChanges = new ArrayList<>();
    for (LocalChangeList list : myMap.values()) {
      Collection<Change> affectedChanges = ((LocalChangeListImpl)list).startProcessingChanges(myProject, scope);
      if (!affectedChanges.isEmpty()) {
        oldChanges.addAll(affectedChanges);
      }
    }
    for (Change change : oldChanges) {
      myIdx.changeRemoved(change);
    }
    // scope should be modified for correct moves tracking
    correctScopeForMoves(scope, oldChanges);

    myLocallyDeleted.cleanAndAdjustScope(scope);
    mySwitchedHolder.cleanAndAdjustScope(scope);
  }

  private static void correctScopeForMoves(VcsModifiableDirtyScope scope, Collection<Change> changes) {
    if (scope == null) return;
    for (Change change : changes) {
      if (change.isMoved() || change.isRenamed()) {
        scope.addDirtyFile(change.getBeforeRevision().getFile());
        scope.addDirtyFile(change.getAfterRevision().getFile());
      }
    }
  }

  public void notifyDoneProcessingChanges(ChangeListListener dispatcher) {
    List<ChangeList> changedLists = new ArrayList<>();
    Map<LocalChangeListImpl, List<Change>> removedChanges = new HashMap<>();
    Map<LocalChangeListImpl, List<Change>> addedChanges = new HashMap<>();
    for (LocalChangeList list : myMap.values()) {
      List<Change> removed = new ArrayList<>();
      List<Change> added = new ArrayList<>();
      LocalChangeListImpl listImpl = (LocalChangeListImpl)list;
      if (listImpl.doneProcessingChanges(removed, added)) {
        changedLists.add(list);
      }
      if (!removed.isEmpty()) {
        removedChanges.put(listImpl, removed);
      }
      if (!added.isEmpty()) {
        addedChanges.put(listImpl, added);
      }
    }
    for (Map.Entry<LocalChangeListImpl, List<Change>> entry : removedChanges.entrySet()) {
      dispatcher.changesRemoved(entry.getValue(), entry.getKey());
    }
    for (Map.Entry<LocalChangeListImpl, List<Change>> entry : addedChanges.entrySet()) {
      dispatcher.changesAdded(entry.getValue(), entry.getKey());
    }
    for (ChangeList changeList : changedLists) {
      dispatcher.changeListChanged(changeList);
    }

    for (String name : myListsToDisappear) {
      LocalChangeList changeList = myMap.get(name);
      if ((changeList != null) && changeList.getChanges().isEmpty() && (!changeList.isReadOnly()) && (!changeList.isDefault())) {
        removeChangeList(name);
      }
    }
    myListsToDisappear.clear();
  }

  @Nonnull
  public List<LocalChangeList> getListsCopy() {
    List<LocalChangeList> result = new ArrayList<>();
    for (LocalChangeList list : myMap.values()) {
      result.add(list.copy());
    }
    return result;
  }

  public String getDefaultListName() {
    return myDefault == null ? null : myDefault.getName();
  }

  public List<File> getAffectedPaths() {
    SortedSet<FilePath> set = myIdx.getAffectedPaths();
    List<File> result = new ArrayList<>(set.size());
    for (FilePath path : set) {
      result.add(path.getIOFile());
    }
    return result;
  }

  @Nonnull
  public List<VirtualFile> getAffectedFiles() {
    Set<VirtualFile> result = new LinkedHashSet<>();
    for (LocalChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        ContentRevision before = change.getBeforeRevision();
        ContentRevision after = change.getAfterRevision();
        if (before != null) {
          VirtualFile file = before.getFile().getVirtualFile();
          if (file != null) {
            result.add(file);
          }
        }
        if (after != null) {
          VirtualFile file = after.getFile().getVirtualFile();
          if (file != null) {
            result.add(file);
          }
        }
      }
    }
    return new ArrayList<>(result);
  }

  @Nullable
  public LocalChangeList getListCopy(@Nonnull VirtualFile file) {
    FilePath filePath = VcsUtil.getFilePath(file);
    for (LocalChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        if (change.getAfterRevision() != null && Comparing.equal(change.getAfterRevision().getFile(), filePath) ||
          change.getBeforeRevision() != null && Comparing.equal(change.getBeforeRevision().getFile(), filePath)) {
          return list.copy();
        }
      }
    }
    return null;
  }

  @Nonnull
  public List<LocalChangeList> getAffectedLists(@Nonnull Collection<? extends Change> changes) {
    List<LocalChangeList> result = new ArrayList<>();
    for (LocalChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        if (changes.contains(change)) {
          result.add(list);
        }
      }
    }

    return result;
  }

  @Nullable
  public Change getChangeForPath(FilePath file) {
    for (LocalChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision != null && afterRevision.getFile().equals(file)) {
          return change;
        }
        ContentRevision beforeRevision = change.getBeforeRevision();
        if (beforeRevision != null && beforeRevision.getFile().equals(file)) {
          return change;
        }
      }
    }
    return null;
  }

  public FileStatus getStatus(VirtualFile file) {
    return myIdx.getStatus(file);
  }

  public FileStatus getStatus(FilePath file) {
    return myIdx.getStatus(file);
  }

  public DeletedFilesHolder getLocallyDeleted() {
    return myLocallyDeleted.copy();
  }

  public SwitchedFileHolder getSwitchedHolder() {
    return mySwitchedHolder.copy();
  }

  public void addSwitched(VirtualFile file, @Nonnull String branchName, boolean recursive) {
    mySwitchedHolder.addFile(file, branchName, recursive);
  }

  public void removeSwitched(VirtualFile file) {
    mySwitchedHolder.removeFile(file);
  }

  public String getBranchForFile(VirtualFile file) {
    return mySwitchedHolder.getBranchForFile(file);
  }

  public boolean isSwitched(VirtualFile file) {
    return mySwitchedHolder.containsFile(file);
  }

  public void addLocallyDeleted(LocallyDeletedChange change) {
    myLocallyDeleted.addFile(change);
  }

  public boolean isContainedInLocallyDeleted(FilePath filePath) {
    return myLocallyDeleted.isContainedInLocallyDeleted(filePath);
  }

  public void notifyVcsStarted(AbstractVcs vcs) {
    myLocallyDeleted.notifyVcsStarted(vcs);
    mySwitchedHolder.notifyVcsStarted(vcs);
  }

  public Collection<Change> getAllChanges() {
    Collection<Change> changes = new HashSet<>();
    for (LocalChangeList list : myMap.values()) {
      changes.addAll(list.getChanges());
    }
    return changes;
  }

  public int getChangeListsNumber() {
    return myMap.size();
  }

  private abstract class ExternalVsInternalChangesIntersection {
    protected final Collection<Change> myInChanges;
    protected final Map<Couple<String>, LocalChangeList> myInternalMap;
    protected final LocalChangeList myDefaultCopy;
    protected final Map<String, LocalChangeList> myIncludedListsCopies;

    protected ExternalVsInternalChangesIntersection(Collection<Change> inChanges) {
      myInChanges = inChanges;
      myInternalMap = new HashMap<>();
      myDefaultCopy = myDefault.copy();
      myIncludedListsCopies = new HashMap<>();
    }

    private Couple<String> keyForChange(Change change) {
      FilePath beforePath = ChangesUtil.getBeforePath(change);
      String beforeKey = beforePath == null ? null : beforePath.getPath();
      FilePath afterPath = ChangesUtil.getAfterPath(change);
      String afterKey = afterPath == null ? null : afterPath.getPath();
      return Couple.of(beforeKey, afterKey);
    }

    private void preparation() {
      for (LocalChangeList list : myMap.values()) {
        Collection<Change> managerChanges = list.getChanges();
        LocalChangeList copy = list.copy();
        for (Change change : managerChanges) {
          myInternalMap.put(keyForChange(change), copy);
        }
      }
    }

    protected abstract void processInChange(Couple<String> key, Change change);

    public void run() {
      preparation();

      for (Change change : myInChanges) {
        Couple<String> key = keyForChange(change);
        processInChange(key, change);
      }
    }

    public Map<String, LocalChangeList> getIncludedListsCopies() {
      return myIncludedListsCopies;
    }
  }

  private class GatherChangesVsListsInfo extends ExternalVsInternalChangesIntersection {
    private final Map<String, List<Change>> myListToChangesMap;

    private GatherChangesVsListsInfo(Collection<Change> inChanges) {
      super(inChanges);
      myListToChangesMap = new HashMap<>();
    }

    @Override
    protected void processInChange(Couple<String> key, Change change) {
      LocalChangeList tmpList = myInternalMap.get(key);
      if (tmpList == null) {
        tmpList = myDefaultCopy;
      }
      String tmpName = tmpList.getName();
      List<Change> list = myListToChangesMap.get(tmpName);
      if (list == null) {
        list = new ArrayList<>();
        myListToChangesMap.put(tmpName, list);
        myIncludedListsCopies.put(tmpName, tmpList);
      }
      list.add(change);
    }

    public Map<String, List<Change>> getListToChangesMap() {
      return myListToChangesMap;
    }
  }

  private class GatherListsFilterValidChanges extends ExternalVsInternalChangesIntersection {
    private final List<Change> myValidChanges;

    private GatherListsFilterValidChanges(Collection<Change> inChanges) {
      super(inChanges);
      myValidChanges = new ArrayList<>();
    }

    @Override
    protected void processInChange(Couple<String> key, Change change) {
      LocalChangeList list = myInternalMap.get(key);
      if (list != null) {
        myIncludedListsCopies.put(list.getName(), list);
        myValidChanges.add(change);
      }
    }

    public List<Change> getValidChanges() {
      return myValidChanges;
    }
  }

  @Nonnull
  public Map<String, List<Change>> listsForChanges(Collection<Change> changes, Map<String, LocalChangeList> lists) {
    GatherChangesVsListsInfo info = new GatherChangesVsListsInfo(changes);
    info.run();
    lists.putAll(info.getIncludedListsCopies());
    return info.getListToChangesMap();
  }

  @Nonnull
  public Collection<LocalChangeList> getInvolvedListsFilterChanges(Collection<Change> changes, List<Change> validChanges) {
    GatherListsFilterValidChanges worker = new GatherListsFilterValidChanges(changes);
    worker.run();
    validChanges.addAll(worker.getValidChanges());
    return worker.getIncludedListsCopies().values();
  }

  @Nullable
  public LocalChangeList listForChange(Change change) {
    for (LocalChangeList list : myMap.values()) {
      if (list.getChanges().contains(change)) return list.copy();
    }
    return null;
  }

  @Nullable
  public String listNameIfOnlyOne(@Nullable Change[] changes) {
    if (changes == null || changes.length == 0) {
      return null;
    }

    Change first = changes[0];

    for (LocalChangeList list : myMap.values()) {
      Collection<Change> listChanges = list.getChanges();
      if (listChanges.contains(first)) {
        // must contain all other
        for (int i = 1; i < changes.length; i++) {
          Change change = changes[i];
          if (!listChanges.contains(change)) {
            return null;
          }
        }
        return list.getName();
      }
    }
    return null;
  }

  public ThreeState haveChangesUnder(@Nonnull VirtualFile virtualFile) {
    FilePath dir = VcsUtil.getFilePath(virtualFile);
    FilePath changeCandidate = myIdx.getAffectedPaths().ceiling(dir);
    if (changeCandidate == null) {
      return ThreeState.NO;
    }
    return FileUtil.isAncestorThreeState(dir.getPath(), changeCandidate.getPath(), false);
  }

  @Nonnull
  public Collection<Change> getChangesIn(FilePath dirPath) {
    List<Change> changes = new ArrayList<>();
    for (ChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision != null && afterRevision.getFile().isUnder(dirPath, false)) {
          changes.add(change);
          continue;
        }

        ContentRevision beforeRevision = change.getBeforeRevision();
        if (beforeRevision != null && beforeRevision.getFile().isUnder(dirPath, false)) {
          changes.add(change);
        }
      }
    }
    return changes;
  }

  @Nullable
  public VcsKey getVcsFor(@Nonnull Change change) {
    return myIdx.getVcsFor(change);
  }

  void setListsToDisappear(Collection<String> names) {
    myListsToDisappear.addAll(names);
  }

  @Nonnull
  public ChangeListManagerGate createSelfGate() {
    return new MyGate(this);
  }

  private static class MyGate implements ChangeListManagerGate {
    private final ChangeListWorker myWorker;

    private MyGate(ChangeListWorker worker) {
      myWorker = worker;
    }

    @Override
    public List<LocalChangeList> getListsCopy() {
      return myWorker.getListsCopy();
    }

    @Nullable
    @Override
    public LocalChangeList findChangeList(String name) {
      return myWorker.getCopyByName(name);
    }

    @Override
    public LocalChangeList addChangeList(String name, String comment) {
      return myWorker.addChangeList(null, name, comment, true, null);
    }

    @Override
    public LocalChangeList findOrCreateList(String name, String comment) {
      LocalChangeList list = myWorker.getCopyByName(name);
      if (list == null) {
        list = addChangeList(name, comment);
      }
      return list;
    }

    @Override
    public void editComment(String name, String comment) {
      myWorker.editComment(name, comment);
    }

    @Override
    public void editName(String oldName, String newName) {
      myWorker.editName(oldName, newName);
    }

    @Override
    public void setListsToDisappear(Collection<String> names) {
      myWorker.setListsToDisappear(names);
    }

    @Override
    public FileStatus getStatus(VirtualFile file) {
      return myWorker.getStatus(file);
    }

    @Deprecated
    @Override
    public FileStatus getStatus(File file) {
      return myWorker.getStatus(VcsUtil.getFilePath(file));
    }

    @Override
    public FileStatus getStatus(@Nonnull FilePath filePath) {
      return myWorker.getStatus(filePath);
    }

    @Override
    public void setDefaultChangeList(@Nonnull String list) {
      myWorker.setDefault(list);
    }
  }

  public void removeRegisteredChangeFor(FilePath path) {
    myIdx.remove(path);

    for (LocalChangeList list : myMap.values()) {
      for (Change change : list.getChanges()) {
        ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision != null && afterRevision.getFile().equals(path)) {
          ((LocalChangeListImpl)list).removeChange(change);
          return;
        }
        ContentRevision beforeRevision = change.getBeforeRevision();
        if (beforeRevision != null && beforeRevision.getFile().equals(path)) {
          ((LocalChangeListImpl)list).removeChange(change);
          return;
        }
      }
    }
  }

  // assumes after revisions are all not null
  private static class MyChangesAfterRevisionComparator implements Comparator<Pair<Change, String>> {
    private static final MyChangesAfterRevisionComparator ourInstance = new MyChangesAfterRevisionComparator();

    public static MyChangesAfterRevisionComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(Pair<Change, String> o1, Pair<Change, String> o2) {
      String s1 = o1.getFirst().getAfterRevision().getFile().getPresentableUrl();
      String s2 = o2.getFirst().getAfterRevision().getFile().getPresentableUrl();
      return Platform.current().fs().isCaseSensitive() ? s1.compareTo(s2) : s1.compareToIgnoreCase(s2);
    }
  }

  @Override
  public String toString() {
    return "ChangeListWorker{myMap=" + StringUtil.join(
      myMap.values(),
      list -> "list: " + list.getName() + " changes: " + StringUtil.join(list.getChanges(), Change::toString, ", "),
      "\n"
    ) + '}';
  }
}
