// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;

public class SetReadOnly implements ChangeListCommand {
  private final String myName;
  private final boolean myValue;

  private boolean myResult;
  private LocalChangeList myListCopy;

  public SetReadOnly(@Nonnull String name, boolean value) {
    myName = name;
    myValue = value;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myResult = worker.setReadOnly(myName, myValue);

    myListCopy = worker.getChangeListByName(myName);
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myListCopy != null && myResult) {
      listener.changeListChanged(myListCopy);
    }
  }

  public boolean isResult() {
    return myResult;
  }
}
