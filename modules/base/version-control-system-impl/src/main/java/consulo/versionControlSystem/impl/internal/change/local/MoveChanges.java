// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

public class MoveChanges implements ChangeListCommand {
  private final String myName;
  private final @Nonnull List<? extends Change> myChanges;

  private MultiMap<LocalChangeList, Change> myMovedFrom;
  private LocalChangeList myListCopy;

  public MoveChanges(@Nonnull String name, @Nonnull List<? extends Change> changes) {
    myName = name;
    myChanges = changes;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myMovedFrom = worker.moveChangesTo(myName, myChanges);

    myListCopy = worker.getChangeListByName(myName);
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myMovedFrom != null && myListCopy != null) {
      for (LocalChangeList fromList : myMovedFrom.keySet()) {
        Collection<Change> changesInList = myMovedFrom.get(fromList);
        listener.changesMoved(changesInList, fromList, myListCopy);
      }
    }
  }
}
