// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;

public class EditName implements ChangeListCommand {
  @Nonnull
  private final String myFromName;
  @Nonnull
  private final String myToName;

  private boolean myResult;
  private LocalChangeList myListCopy;

  public EditName(@Nonnull String fromName, @Nonnull String toName) {
    myFromName = fromName;
    myToName = toName;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myResult = worker.editName(myFromName, myToName);

    myListCopy = worker.getChangeListByName(myToName);
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myListCopy != null && myResult) {
      listener.changeListRenamed(myListCopy, myFromName);
    }
  }

  public boolean isResult() {
    return myResult;
  }
}
