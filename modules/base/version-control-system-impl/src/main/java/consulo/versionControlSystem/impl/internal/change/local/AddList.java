// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListData;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class AddList implements ChangeListCommand {
  @Nonnull
  private final String myName;
  @Nullable
  private final String myComment;
  @Nullable
  private final ChangeListData myData;

  private boolean myWasListCreated;
  private LocalChangeList myNewListCopy;
  private String myOldComment;

  public AddList(@Nonnull String name, @Nullable String comment, @Nullable ChangeListData data) {
    myName = name;
    myComment = comment;
    myData = data;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    LocalChangeList list = worker.getChangeListByName(myName);
    if (list == null) {
      // Create list with the same id, if we were invoked before (on "temp" worker during CLM update).
      String id = myNewListCopy != null ? myNewListCopy.getId() : null;

      myWasListCreated = true;
      myOldComment = null;
      myNewListCopy = worker.addChangeList(myName, myComment, id, myData);
    }
    else if (StringUtil.isNotEmpty(myComment)) {
      myWasListCreated = false;
      myOldComment = worker.editComment(myName, myComment);
      myNewListCopy = worker.getChangeListByName(myName);
    }
    else {
      myWasListCreated = false;
      myOldComment = null;
      myNewListCopy = list;
    }
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myWasListCreated) {
      listener.changeListAdded(myNewListCopy);
    }
    else if (myNewListCopy != null && myOldComment != null) {
      listener.changeListCommentChanged(myNewListCopy, myOldComment);
    }
  }

  @Nullable
  public LocalChangeList getNewListCopy() {
    return myNewListCopy;
  }
}
