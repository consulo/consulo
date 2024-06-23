// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;

public class SetDefault implements ChangeListCommand {
  private final String myNewDefaultName;
  private final boolean myAutomatic;

  private boolean myResult;
  private LocalChangeList myOldDefaultListCopy;
  private LocalChangeList myNewDefaultListCopy;

  public SetDefault(@Nonnull String newDefaultName, boolean automatic) {
    myNewDefaultName = newDefaultName;
    myAutomatic = automatic;
  }

  @Override
  public void apply(ChangeListWorker worker) {
    LocalChangeList list = worker.getChangeListByName(myNewDefaultName);
    if (list == null || list.isDefault()) {
      myOldDefaultListCopy = null;
      myNewDefaultListCopy = null;
      myResult = false;
      return;
    }

    String oldDefaultName = worker.setDefaultList(myNewDefaultName);
    myOldDefaultListCopy = worker.getChangeListByName(oldDefaultName);
    myNewDefaultListCopy = worker.getChangeListByName(myNewDefaultName);
    myResult = oldDefaultName != null;
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myOldDefaultListCopy != null && myNewDefaultListCopy != null && myResult) {
      listener.defaultListChanged(myOldDefaultListCopy, myNewDefaultListCopy, myAutomatic);
    }
  }
}
