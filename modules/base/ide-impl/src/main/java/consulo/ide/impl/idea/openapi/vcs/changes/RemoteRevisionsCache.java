// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.internal.BackgroundTaskUtil;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RemoteStatusChangeNodeDecorator;
import consulo.project.Project;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.impl.internal.change.ControlledCycle;
import consulo.versionControlSystem.impl.internal.change.RemoteRevisionsStateCache;
import consulo.versionControlSystem.update.UpdateFilesHelper;
import consulo.versionControlSystem.update.UpdatedFiles;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RemoteRevisionsCache implements VcsListener {

  public static final Class<RemoteRevisionChangeListener> REMOTE_VERSION_CHANGED = RemoteRevisionChangeListener.class;

  public static final int DEFAULT_REFRESH_INTERVAL = 3 * 60 * 1000;

  private final RemoteRevisionsNumbersCache myRemoteRevisionsNumbersCache;
  private final RemoteRevisionsStateCache myRemoteRevisionsStateCache;

  private final ProjectLevelVcsManager myVcsManager;

  @Nonnull
  private final RemoteStatusChangeNodeDecorator myChangeDecorator;
  private final Project myProject;
  private final ControlledCycle myControlledCycle;

  public static RemoteRevisionsCache getInstance(final Project project) {
    return project.getInstance(RemoteRevisionsCache.class);
  }

  @Inject
  public RemoteRevisionsCache(final Project project) {
    myProject = project;

    myRemoteRevisionsNumbersCache = new RemoteRevisionsNumbersCache(myProject);
    myRemoteRevisionsStateCache = new RemoteRevisionsStateCache(myProject);

    myChangeDecorator = new RemoteStatusChangeNodeDecorator(this);

    myVcsManager = ProjectLevelVcsManager.getInstance(project);

    final VcsConfiguration vcsConfiguration = VcsConfiguration.getInstance(myProject);
    myControlledCycle = new ControlledCycle(project, () -> {
      final boolean shouldBeDone = vcsConfiguration.isChangedOnServerEnabled() && myVcsManager.hasActiveVcss();

      if (shouldBeDone) {
        boolean somethingChanged = myRemoteRevisionsNumbersCache.updateStep();
        somethingChanged |= myRemoteRevisionsStateCache.updateStep();
        if (somethingChanged) {
          BackgroundTaskUtil.syncPublisher(myProject, REMOTE_VERSION_CHANGED).versionChanged();
        }
      }
      return shouldBeDone;
    }, VcsBundle.message("changes.finishing.changed.on.server.update"), DEFAULT_REFRESH_INTERVAL);

    MessageBusConnection connection = myProject.getMessageBus().connect();
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, this);
    connection.subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED_IN_PLUGIN, this);

    if ((!myProject.isDefault()) && vcsConfiguration.isChangedOnServerEnabled()) {
      myVcsManager.runAfterInitialization(() -> {
        // do not start if there're no vcses
        if (!myVcsManager.hasActiveVcss() || !vcsConfiguration.isChangedOnServerEnabled()) return;
        myControlledCycle.startIfNotStarted();
      });
    }
  }

  public void updateAutomaticRefreshAlarmState(boolean remoteCacheStateChanged) {
    manageAlarm();
  }

  private void manageAlarm() {
    final VcsConfiguration vcsConfiguration = VcsConfiguration.getInstance(myProject);
    if ((!myProject.isDefault()) && myVcsManager.hasActiveVcss() && vcsConfiguration.isChangedOnServerEnabled()) {
      // will check whether is already started inside
      // interval is checked further, this is small and constant
      myControlledCycle.startIfNotStarted();
    }
    else {
      myControlledCycle.stop();
    }
  }

  @Override
  public void directoryMappingChanged() {
    if (!VcsConfiguration.getInstance(myProject).isChangedOnServerEnabled()) {
      manageAlarm();
    }
    else {
      BackgroundTaskUtil.executeOnPooledThread(myProject, () -> {
        try {
          myRemoteRevisionsNumbersCache.directoryMappingChanged();
          myRemoteRevisionsStateCache.directoryMappingChanged();
          manageAlarm();
        }
        catch (ProcessCanceledException ignore) {
        }
      });
    }
  }

  public void changeUpdated(@Nonnull String path, @Nonnull AbstractVcs vcs) {
    if (RemoteDifferenceStrategy.ASK_TREE_PROVIDER.equals(vcs.getRemoteDifferenceStrategy())) {
      myRemoteRevisionsStateCache.changeUpdated(path, vcs);
    }
    else {
      myRemoteRevisionsNumbersCache.changeUpdated(path, vcs);
    }
  }

  public void invalidate(final UpdatedFiles updatedFiles) {
    final Collection<String> newForTree = new ArrayList<>();
    final Collection<String> newForUsual = new ArrayList<>();
    UpdateFilesHelper.iterateAffectedFiles(updatedFiles, pair -> {
      AbstractVcs vcs = myVcsManager.findVcsByName(pair.getSecond());
      if (vcs == null) return;

      if (RemoteDifferenceStrategy.ASK_TREE_PROVIDER.equals(vcs.getRemoteDifferenceStrategy())) {
        newForTree.add(pair.getFirst());
      }
      else {
        newForUsual.add(pair.getFirst());
      }
    });

    myRemoteRevisionsStateCache.invalidate(newForTree);
    myRemoteRevisionsNumbersCache.invalidate(newForUsual);
  }

  public void changeRemoved(@Nonnull String path, @Nonnull AbstractVcs vcs) {
    if (RemoteDifferenceStrategy.ASK_TREE_PROVIDER.equals(vcs.getRemoteDifferenceStrategy())) {
      myRemoteRevisionsStateCache.changeRemoved(path, vcs);
    }
    else {
      myRemoteRevisionsNumbersCache.changeRemoved(path, vcs);
    }
  }

  /**
   * @return false if not up to date
   */
  public boolean isUpToDate(@Nonnull Change change) {
    if (myProject.isDisposed()) return true;
    final AbstractVcs vcs = ChangesUtil.getVcsForChange(change, myProject);
    if (vcs == null) return true;
    final RemoteDifferenceStrategy strategy = vcs.getRemoteDifferenceStrategy();
    if (RemoteDifferenceStrategy.ASK_TREE_PROVIDER.equals(strategy)) {
      return myRemoteRevisionsStateCache.isUpToDate(change, vcs);
    }
    else {
      return myRemoteRevisionsNumbersCache.isUpToDate(change, vcs);
    }
  }

  @Nonnull
  public RemoteStatusChangeNodeDecorator getChangesNodeDecorator() {
    return myChangeDecorator;
  }
}
