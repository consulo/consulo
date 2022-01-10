package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author yole
 */
public class LocalChangeListImpl extends LocalChangeList {
  private static final Logger LOG = Logger.getInstance(LocalChangeListImpl.class);

  private final Project myProject;
  private Collection<Change> myChanges = new HashSet<>();
  private Collection<Change> myReadChangesCache = null;
  private String myId;
  @Nonnull
  private String myName;
  private String myComment = "";
  @Nullable private Object myData;

  private boolean myIsDefault = false;
  private boolean myIsReadOnly = false;
  private Map<Change, Change> myChangesBeforeUpdate;

  public static LocalChangeListImpl createEmptyChangeListImpl(Project project, String name) {
    return new LocalChangeListImpl(project, name);
  }

  private LocalChangeListImpl(Project project, final String name) {
    myProject = project;
    myId = UUID.randomUUID().toString();
    setNameImpl(name);
  }

  private LocalChangeListImpl(LocalChangeListImpl origin) {
    myId = origin.getId();
    myProject = origin.myProject;
    setNameImpl(origin.myName);
  }

  public Collection<Change> getChanges() {
    createReadChangesCache();
    return myReadChangesCache;
  }

  private void createReadChangesCache() {
    if (myReadChangesCache == null) {
      myReadChangesCache = Collections.unmodifiableCollection(new HashSet<>(myChanges));
    }
  }

  @Nonnull
  @Override
  public String getId() {
    return myId;
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  public void setName(@Nonnull final String name) {
    if (! myName.equals(name)) {
      setNameImpl(name);
    }
  }

  public String getComment() {
    return myComment;
  }

  // same as for setName()
  public void setComment(final String comment) {
    if (! Comparing.equal(comment, myComment)) {
      myComment = comment != null ? comment : "";
    }
  }

  void setNameImpl(@Nonnull final String name) {
    if (StringUtil.isEmptyOrSpaces(name) && Registry.is("vcs.log.empty.change.list.creation")) {
      LOG.info("Creating a changelist with empty name");
    }
    myName = name;
  }

  void setCommentImpl(final String comment) {
    myComment = comment;
  }

  public boolean isDefault() {
    return myIsDefault;
  }

  void setDefault(final boolean isDefault) {
    myIsDefault = isDefault;
  }

  public boolean isReadOnly() {
    return myIsReadOnly;
  }

  public void setReadOnly(final boolean isReadOnly) {
    myIsReadOnly = isReadOnly;
  }

  void setData(@Nullable Object data) {
    myData = data;
  }

  @Nullable
  @Override
  public Object getData() {
    return myData;
  }

  void addChange(Change change) {
    myReadChangesCache = null;
    myChanges.add(change);
  }

  Change removeChange(Change change) {
    for (Change localChange : myChanges) {
      if (localChange.equals(change)) {
        myChanges.remove(localChange);
        myReadChangesCache = null;
        return localChange;
      }
    }
    return null;
  }

  Collection<Change> startProcessingChanges(final Project project, @Nullable final VcsDirtyScope scope) {
    createReadChangesCache();
    final Collection<Change> result = new ArrayList<>();
    myChangesBeforeUpdate = new HashMap<>(myChanges.size());
    myChanges.forEach(it -> myChangesBeforeUpdate.put(it, it));

    for (Change oldBoy : myChangesBeforeUpdate.values()) {
      final ContentRevision before = oldBoy.getBeforeRevision();
      final ContentRevision after = oldBoy.getAfterRevision();
      if (scope == null || before != null && scope.belongsTo(before.getFile()) || after != null && scope.belongsTo(after.getFile())
          || isIgnoredChange(oldBoy, project)) {
        result.add(oldBoy);
        myChanges.remove(oldBoy);
        myReadChangesCache = null;
      }
    }
    return result;
  }

  private static boolean isIgnoredChange(@Nonnull Change change, @Nonnull Project project) {
    boolean beforeRevIgnored = change.getBeforeRevision() == null || isIgnoredRevision(change.getBeforeRevision(), project);
    boolean afterRevIgnored = change.getAfterRevision() == null || isIgnoredRevision(change.getAfterRevision(), project);
    return beforeRevIgnored && afterRevIgnored;
  }

  private static boolean isIgnoredRevision(final @Nonnull ContentRevision revision, final @Nonnull Project project) {
    return ReadAction.compute(() -> {
      if (project.isDisposed()) {
        return false;
      }
      VirtualFile vFile = revision.getFile().getVirtualFile();
      return vFile != null && ProjectLevelVcsManager.getInstance(project).isIgnored(vFile);
    });
  }

  boolean processChange(Change change) {
    LOG.debug("[process change] for '" + myName + "' isDefault: " + myIsDefault + " change: " +
              ChangesUtil.getFilePath(change).getPath());
    if (myIsDefault) {
      LOG.debug("[process change] adding because default");
      addChange(change);
      return true;
    }

    for (Change oldChange : myChangesBeforeUpdate.values()) {
      if (Comparing.equal(oldChange, change)) {
        LOG.debug("[process change] adding bacuae equal to old: " + ChangesUtil.getFilePath(oldChange).getPath());
        addChange(change);
        return true;
      }
    }
    LOG.debug("[process change] not found");
    return false;
  }

  boolean doneProcessingChanges(final List<Change> removedChanges, final List<Change> addedChanges) {
    boolean changesDetected = (myChanges.size() != myChangesBeforeUpdate.size());

    for (Change newChange : myChanges) {
      Change oldChange = findOldChange(newChange);
      if (oldChange == null) {
        addedChanges.add(newChange);
      }
    }
    changesDetected |= (! addedChanges.isEmpty());
    final List<Change> removed = new ArrayList<>(myChangesBeforeUpdate.values());
    // since there are SAME objects...
    removed.removeAll(myChanges);
    removedChanges.addAll(removed);
    changesDetected = changesDetected || (! removedChanges.isEmpty());

    myReadChangesCache = null;
    return changesDetected;
  }

  @Nullable
  private Change findOldChange(final Change newChange) {
    Change oldChange = myChangesBeforeUpdate.get(newChange);
    if (oldChange != null && sameBeforeRevision(oldChange, newChange) &&
        newChange.getFileStatus().equals(oldChange.getFileStatus())) {
      return oldChange;
    }
    return null;
  }

  private static boolean sameBeforeRevision(final Change change1, final Change change2) {
    final ContentRevision b1 = change1.getBeforeRevision();
    final ContentRevision b2 = change2.getBeforeRevision();
    if (b1 != null && b2 != null) {
      final VcsRevisionNumber rn1 = b1.getRevisionNumber();
      final VcsRevisionNumber rn2 = b2.getRevisionNumber();
      final boolean isBinary1 = (b1 instanceof BinaryContentRevision);
      final boolean isBinary2 = (b2 instanceof BinaryContentRevision);
      return rn1 != VcsRevisionNumber.NULL && rn2 != VcsRevisionNumber.NULL && rn1.compareTo(rn2) == 0 && isBinary1 == isBinary2;
    }
    return b1 == null && b2 == null;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final LocalChangeListImpl list = (LocalChangeListImpl)o;
    return myName.equals(list.myName);
  }

  public int hashCode() {
    return myName.hashCode();
  }

  @Override
  public String toString() {
    return myName.trim();
  }

  public LocalChangeList copy() {
    final LocalChangeListImpl copy = new LocalChangeListImpl(this);
    copy.myComment = myComment;
    copy.myIsDefault = myIsDefault;
    copy.myIsReadOnly = myIsReadOnly;
    copy.myData = myData;

    if (myChanges != null) {
      copy.myChanges = new HashSet<>(myChanges);
    }

    if (myChangesBeforeUpdate != null) {
      copy.myChangesBeforeUpdate = new HashMap<>(myChangesBeforeUpdate);
    }

    if (myReadChangesCache != null) {
      copy.myReadChangesCache = new HashSet<>(myReadChangesCache);
    }

    return copy;
  }

  @Nullable
  public ChangeListEditHandler getEditHandler() {
    return null;
  }

  public void setId(String id) {
    myId = id;
  }
}
