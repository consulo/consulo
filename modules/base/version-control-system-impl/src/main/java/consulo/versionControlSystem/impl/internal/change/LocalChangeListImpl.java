// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListData;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

import java.util.*;

public final class LocalChangeListImpl extends LocalChangeList {
  @Nonnull
  private final Project myProject;
  @NonNls
  @Nonnull
  private final String myId;
  @Nonnull
  private final String myName;
  @Nonnull
  private final String myComment;
  private final @Nonnull Set<? extends Change> myChanges;
  @Nullable
  private final ChangeListData myData;

  private final boolean myIsDefault;
  private final boolean myIsReadOnly;

  @Nonnull
  public static LocalChangeListImpl createEmptyChangeListImpl(@Nonnull Project project,
                                                             @Nonnull String name,
                                                             @Nullable String id) {
    return new Builder(project, name).setId(id).build();
  }

  @Nonnull
  public static String generateChangelistId() {
    return UUID.randomUUID().toString();
  }

  private LocalChangeListImpl(@Nonnull Project project,
                              @NonNls @Nonnull String id,
                              @Nonnull String name,
                              @Nonnull String comment,
                              @Nonnull Set<? extends Change> changes,
                              @Nullable ChangeListData data,
                              boolean isDefault,
                              boolean isReadOnly) {
    myProject = project;
    myId = id;
    myName = name;
    myComment = comment;
    myChanges = changes;
    myData = data;
    myIsDefault = isDefault;
    myIsReadOnly = isReadOnly;
  }

  @Nonnull
  @Override
  public Set<Change> getChanges() {
    return Collections.unmodifiableSet(myChanges);
  }

  @Nonnull
  @Override
  public String getId() {
    return myId;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public String getComment() {
    return myComment;
  }

  @Override
  public boolean isDefault() {
    return myIsDefault;
  }

  @Override
  public boolean isReadOnly() {
    return myIsReadOnly;
  }

  @Nullable
  @Override
  public ChangeListData getData() {
    return myData;
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
    return myName;
  }

  @Override
  public LocalChangeListImpl copy() {
    return this;
  }

  @Override
  public void setComment(@Nullable String comment) {
    ChangeListManager.getInstance(myProject).editComment(myName, comment);
  }

  @Override
  public void setReadOnly(boolean isReadOnly) {
    ChangeListManager.getInstance(myProject).setReadOnly(myName, isReadOnly);
  }

  public static class Builder {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final String myName;

    @Nullable
    private String myId;
    @Nonnull
    private String myComment = "";
    @Nonnull
    private Set<Change> myChanges = new HashSet<>();
    @Nullable
    private ChangeListData myData = null;
    private boolean myIsDefault = false;
    private boolean myIsReadOnly = false;

    public Builder(@Nonnull Project project, @Nonnull String name) {
      myProject = project;
      myName = name;
    }

    public Builder(@Nonnull LocalChangeListImpl list) {
      myProject = list.myProject;
      myId = list.myId;
      myName = list.myName;
      myComment = list.myComment;
      myChanges = new HashSet<>(list.myChanges);
      myData = list.myData;
      myIsDefault = list.myIsDefault;
      myIsReadOnly = list.myIsReadOnly;
    }

    public Builder setId(@NonNls @Nullable String value) {
      myId = value;
      return this;
    }

    public Builder setComment(@Nonnull String value) {
      myComment = value;
      return this;
    }

    public Builder setChanges(@Nonnull Collection<? extends Change> changes) {
      myChanges.clear();
      myChanges.addAll(changes);
      return this;
    }

    public Builder setChangesCollection(@Nonnull Set<Change> changes) {
      myChanges = changes;
      return this;
    }

    public Builder setData(@Nullable ChangeListData value) {
      myData = value;
      return this;
    }

    public Builder setDefault(boolean value) {
      myIsDefault = value;
      return this;
    }

    public Builder setReadOnly(boolean value) {
      myIsReadOnly = value;
      return this;
    }

    @Nonnull
    public LocalChangeListImpl build() {
      String id = myId != null ? myId : generateChangelistId();
      return new LocalChangeListImpl(myProject, id, myName, myComment, myChanges, myData, myIsDefault, myIsReadOnly);
    }
  }
}
