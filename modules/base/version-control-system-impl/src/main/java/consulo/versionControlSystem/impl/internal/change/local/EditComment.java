// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class EditComment implements ChangeListCommand {
  private final String myName;
  private final String myNewComment;

  private String myOldComment;
  private LocalChangeList myListCopy;

  public EditComment(@Nonnull String name, @Nonnull String newComment) {
    myNewComment = newComment;
    myName = name;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myOldComment = worker.editComment(myName, myNewComment);

    if (myOldComment != null && !Objects.equals(myOldComment, myNewComment)) {
      myListCopy = worker.getChangeListByName(myName);
    }
    else {
      myListCopy = null; // nothing changed, no notify
    }
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myListCopy != null && myOldComment != null) {
      listener.changeListCommentChanged(myListCopy, myOldComment);
    }
  }

  @Nullable
  public String getOldComment() {
    return myOldComment;
  }
}
