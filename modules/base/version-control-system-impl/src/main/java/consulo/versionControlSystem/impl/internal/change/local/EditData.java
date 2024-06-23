// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListData;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class EditData implements ChangeListCommand {
  private final String myName;
  @Nullable
  private final ChangeListData myNewData;

  private boolean myResult;
  private LocalChangeList myListCopy;

  public EditData(@Nonnull String name, @Nullable ChangeListData newData) {
    myName = name;
    myNewData = newData;
  }

  @Override
  public void apply(final ChangeListWorker worker) {
    myResult = worker.editData(myName, myNewData);
    myListCopy = worker.getChangeListByName(myName);
  }

  @Override
  public void doNotify(final ChangeListListener listener) {
    if (myListCopy != null) {
      listener.changeListDataChanged(myListCopy);
    }
  }

  public boolean isResult() {
    return myResult;
  }
}
