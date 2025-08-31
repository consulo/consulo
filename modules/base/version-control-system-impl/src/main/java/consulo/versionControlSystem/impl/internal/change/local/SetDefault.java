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

public class SetDefault implements ChangeListCommand {
  private final String myNewDefaultName;
  private String myPrevious;
  private LocalChangeList myOldDefaultListCopy;
  private LocalChangeList myNewDefaultListCopy;

  public SetDefault(String newDefaultName) {
    myNewDefaultName = newDefaultName;
  }

  public void apply(ChangeListWorker worker) {
    myOldDefaultListCopy = worker.getDefaultListCopy();
    myPrevious = worker.setDefault(myNewDefaultName);
    myNewDefaultListCopy = worker.getDefaultListCopy();
  }

  public void doNotify(EventDispatcher<ChangeListListener> dispatcher) {
    dispatcher.getMulticaster().defaultListChanged(myOldDefaultListCopy, myNewDefaultListCopy);
  }

  public String getPrevious() {
    return myPrevious;
  }
}
