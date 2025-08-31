/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListListener;
import consulo.versionControlSystem.impl.internal.change.local.ChangeListCommand;
import jakarta.annotation.Nonnull;

import java.util.Collection;

public class DelayedNotificator implements ChangeListListener {
  private static final Logger LOG = Logger.getInstance(DelayedNotificator.class);

  private final EventDispatcher<ChangeListListener> myDispatcher;
  private final ChangeListScheduler myScheduler;

  public DelayedNotificator(@Nonnull EventDispatcher<ChangeListListener> dispatcher,
                            @Nonnull ChangeListScheduler scheduler) {
    myDispatcher = dispatcher;
    myScheduler = scheduler;
  }

  public void callNotify(ChangeListCommand command) {
    myScheduler.submit(() -> {
      try {
        command.doNotify(myDispatcher);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    });
  }

  @Override
  public void changeListAdded(ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListAdded(list));
  }

  @Override
  public void changesRemoved(Collection<Change> changes, ChangeList fromList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesRemoved(changes, fromList));
  }

  @Override
  public void changesAdded(Collection<Change> changes, ChangeList toList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesAdded(changes, toList));
  }

  @Override
  public void changeListRemoved(ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListRemoved(list));
  }

  @Override
  public void changeListChanged(ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListChanged(list));
  }

  @Override
  public void changeListRenamed(ChangeList list, String oldName) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListRenamed(list, oldName));
  }

  @Override
  public void changeListCommentChanged(ChangeList list, String oldComment) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListCommentChanged(list, oldComment));
  }

  @Override
  public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesMoved(changes, fromList, toList));
  }

  @Override
  public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().defaultListChanged(oldDefaultList, newDefaultList));
  }

  @Override
  public void unchangedFileStatusChanged() {
    myScheduler.submit(() -> myDispatcher.getMulticaster().unchangedFileStatusChanged());
  }

  @Override
  public void changeListUpdateDone() {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListUpdateDone());
  }
}