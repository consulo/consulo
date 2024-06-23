// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.internal.BackgroundTaskUtil;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.local.ChangeListCommand;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class DelayedNotificator implements ChangeListListener {
  private static final Logger LOG = Logger.getInstance(DelayedNotificator.class);

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final ChangeListManager myManager;
  @Nonnull
  private final ChangeListScheduler myScheduler;

  public DelayedNotificator(@Nonnull Project project,
                            @Nonnull ChangeListManager manager,
                            @Nonnull ChangeListScheduler scheduler) {
    myProject = project;
    myManager = manager;
    myScheduler = scheduler;
  }

  public void callNotify(final ChangeListCommand command) {
    myScheduler.submit(() -> {
      try {
        command.doNotify(getMulticaster());
      }
      catch (ProcessCanceledException ignore) {
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    });
  }

  @Override
  public void changeListAdded(final ChangeList list) {
    myScheduler.submit(() -> getMulticaster().changeListAdded(list));
  }

  @Override
  public void changesRemoved(final Collection<? extends Change> changes, final ChangeList fromList) {
    myScheduler.submit(() -> getMulticaster().changesRemoved(changes, fromList));
  }

  @Override
  public void changesAdded(final Collection<? extends Change> changes, final ChangeList toList) {
    myScheduler.submit(() -> getMulticaster().changesAdded(changes, toList));
  }

  @Override
  public void changeListRemoved(final ChangeList list) {
    myScheduler.submit(() -> getMulticaster().changeListRemoved(list));
  }

  @Override
  public void changeListChanged(final ChangeList list) {
    myScheduler.submit(() -> getMulticaster().changeListChanged(list));
  }

  @Override
  public void changeListRenamed(final ChangeList list, final String oldName) {
    myScheduler.submit(() -> getMulticaster().changeListRenamed(list, oldName));
  }

  @Override
  public void changeListDataChanged(@Nonnull ChangeList list) {
    myScheduler.submit(() -> getMulticaster().changeListDataChanged(list));
  }

  @Override
  public void changeListCommentChanged(final ChangeList list, final String oldComment) {
    myScheduler.submit(() -> getMulticaster().changeListCommentChanged(list, oldComment));
  }

  @Override
  public void changesMoved(final Collection<? extends Change> changes, final ChangeList fromList, final ChangeList toList) {
    myScheduler.submit(() -> getMulticaster().changesMoved(changes, fromList, toList));
  }

  @Override
  public void defaultListChanged(ChangeList oldDefaultList, ChangeList newDefaultList) {
    defaultListChanged(oldDefaultList, newDefaultList, false);
  }

  @Override
  public void defaultListChanged(final ChangeList oldDefaultList, final ChangeList newDefaultList, boolean automatic) {
    myScheduler.submit(() -> getMulticaster().defaultListChanged(oldDefaultList, newDefaultList, automatic));
  }


  @Override
  public void changedFileStatusChanged(boolean upToDate) {
    myScheduler.submit(() -> getMulticaster().changedFileStatusChanged(upToDate));
  }

  @Override
  public void unchangedFileStatusChanged(boolean upToDate) {
    myScheduler.submit(() -> getMulticaster().unchangedFileStatusChanged(upToDate));
  }

  @Override
  public void changeListUpdateDone() {
    myScheduler.submit(() -> getMulticaster().changeListUpdateDone());
  }

  @Override
  public void allChangeListsMappingsChanged() {
    myScheduler.submit(() -> getMulticaster().allChangeListsMappingsChanged());
  }

  @Override
  public void changeListAvailabilityChanged() {
    myScheduler.submit(() -> getMulticaster().changeListAvailabilityChanged());
  }

  public void changeListsForFileChanged(@Nonnull FilePath path,
                                        @Nonnull Set<String> removedChangeListsIds,
                                        @Nonnull Set<String> addedChangeListsIds) {
    myScheduler.submit(() -> {
      if (!myManager.areChangeListsEnabled()) return;

      Change change = myManager.getChange(path);
      if (change == null) return;
      List<Change> changes = Collections.singletonList(change);

      for (String listId : removedChangeListsIds) {
        LocalChangeList changeList = myManager.getChangeList(listId);
        if (changeList != null) {
          getMulticaster().changesRemoved(changes, changeList);
        }
      }

      for (String listId : addedChangeListsIds) {
        LocalChangeList changeList = myManager.getChangeList(listId);
        if (changeList != null) {
          getMulticaster().changesAdded(changes, changeList);
        }
      }
    });
  }

  @Nonnull
  private ChangeListListener getMulticaster() {
    return BackgroundTaskUtil.syncPublisher(myProject, ChangeListListener.TOPIC);
  }
}