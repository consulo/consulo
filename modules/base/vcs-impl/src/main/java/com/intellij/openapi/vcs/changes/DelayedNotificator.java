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
package com.intellij.openapi.vcs.changes;

import consulo.logging.Logger;
import com.intellij.openapi.vcs.changes.local.ChangeListCommand;
import com.intellij.util.EventDispatcher;
import javax.annotation.Nonnull;

import java.util.Collection;

public class DelayedNotificator implements ChangeListListener {
  private static final Logger LOG = Logger.getInstance(DelayedNotificator.class);

  private final EventDispatcher<ChangeListListener> myDispatcher;
  private final ChangeListManagerImpl.Scheduler myScheduler;

  public DelayedNotificator(@Nonnull EventDispatcher<ChangeListListener> dispatcher,
                            @Nonnull ChangeListManagerImpl.Scheduler scheduler) {
    myDispatcher = dispatcher;
    myScheduler = scheduler;
  }

  public void callNotify(final ChangeListCommand command) {
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
  public void changeListAdded(final ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListAdded(list));
  }

  @Override
  public void changesRemoved(final Collection<Change> changes, final ChangeList fromList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesRemoved(changes, fromList));
  }

  @Override
  public void changesAdded(final Collection<Change> changes, final ChangeList toList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesAdded(changes, toList));
  }

  @Override
  public void changeListRemoved(final ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListRemoved(list));
  }

  @Override
  public void changeListChanged(final ChangeList list) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListChanged(list));
  }

  @Override
  public void changeListRenamed(final ChangeList list, final String oldName) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListRenamed(list, oldName));
  }

  @Override
  public void changeListCommentChanged(final ChangeList list, final String oldComment) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changeListCommentChanged(list, oldComment));
  }

  @Override
  public void changesMoved(final Collection<Change> changes, final ChangeList fromList, final ChangeList toList) {
    myScheduler.submit(() -> myDispatcher.getMulticaster().changesMoved(changes, fromList, toList));
  }

  @Override
  public void defaultListChanged(final ChangeList oldDefaultList, final ChangeList newDefaultList) {
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