// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import consulo.versionControlSystem.impl.internal.change.local.ChangeListCommand;

import java.util.List;

public class RemoveList implements ChangeListCommand {
  private final String myName;

  private LocalChangeList myListCopy;
  private LocalChangeList myDefaultListCopy;
  private List<Change> myMovedChanges;

  public RemoveList(final String name) {
    myName = name;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myListCopy = worker.getChangeListByName(myName);

    myMovedChanges = worker.removeChangeList(myName);

    myDefaultListCopy = worker.getDefaultList();
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myListCopy != null && myMovedChanges != null) {
      if (!myMovedChanges.isEmpty()) {
        listener.changesMoved(myMovedChanges, myListCopy, myDefaultListCopy);
      }
      listener.changeListRemoved(myListCopy);
    }
  }
}
