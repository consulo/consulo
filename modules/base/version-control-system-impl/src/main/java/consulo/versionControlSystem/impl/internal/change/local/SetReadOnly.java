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
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ChangeListWorker;

public class SetReadOnly implements ChangeListCommand {
  private final String myName;
  private final boolean myValue;
  private boolean myResult;
  private LocalChangeList myListCopy;

  public SetReadOnly(String name, boolean value) {
    myName = name;
    myValue = value;
  }

  public void apply(ChangeListWorker worker) {
    myResult = worker.setReadOnly(myName, myValue);
    myListCopy = worker.getCopyByName(myName);
  }

  public void doNotify(EventDispatcher<ChangeListListener> dispatcher) {
    dispatcher.getMulticaster().changeListChanged(myListCopy);
  }

  public void consume(ChangeListWorker worker) {
    myResult = worker.setReadOnly(myName, myValue);
  }

  public boolean isResult() {
    return myResult;
  }
}
