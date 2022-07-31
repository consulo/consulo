/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.openapi.vcs.changes.local;

import consulo.versionControlSystem.change.ChangeListListener;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListWorker;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.util.EventDispatcher;
import javax.annotation.Nonnull;

public class EditName implements ChangeListCommand {
  @Nonnull
  private final String myFromName;
  @Nonnull
  private final String myToName;
  private boolean myResult;
  private LocalChangeList myListCopy;

  public EditName(@Nonnull final String fromName, @Nonnull final String toName) {
    myFromName = fromName;
    myToName = toName;
  }

  public void apply(final ChangeListWorker worker) {
    final LocalChangeList fromList = worker.getCopyByName(myFromName);
    if (fromList != null && (! fromList.isReadOnly())) {
      myResult = worker.editName(myFromName, myToName);
      myListCopy = worker.getCopyByName(myToName);
    }
  }

  public void doNotify(final EventDispatcher<ChangeListListener> dispatcher) {
    if (myListCopy != null && (! myListCopy.isReadOnly())) {
      dispatcher.getMulticaster().changeListRenamed(myListCopy, myFromName);
    }
  }

  public boolean isResult() {
    return myResult;
  }
}
