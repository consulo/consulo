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
package consulo.versionControlSystem.impl.internal.change.local;

import consulo.proxy.EventDispatcher;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;

import java.util.Collection;

public class MoveChanges implements ChangeListCommand {
  private final String myName;
  private final Change[] myChanges;
  private MultiMap<LocalChangeList,Change> myMovedFrom;
  private LocalChangeList myListCopy;

  public MoveChanges(String name, Change[] changes) {
    myName = name;
    myChanges = changes;
  }

  public void apply(ChangeListWorker worker) {
    myMovedFrom = worker.moveChangesTo(myName, myChanges);
    myListCopy = worker.getCopyByName(myName);
  }

  public void doNotify(EventDispatcher<ChangeListListener> dispatcher) {
    if ((myMovedFrom != null) && (myListCopy != null)) {
      for(LocalChangeList fromList: myMovedFrom.keySet()) {
        Collection<Change> changesInList = myMovedFrom.get(fromList);
        dispatcher.getMulticaster().changesMoved(changesInList, fromList, myListCopy);
      }
    }
  }

  public MultiMap<LocalChangeList, Change> getMovedFrom() {
    return myMovedFrom;
  }
}
