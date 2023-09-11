// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution;


import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.execution.*;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.TargetAwareRunProfile;
import consulo.execution.event.ExecutionTargetListener;
import consulo.execution.event.RunManagerListener;
import consulo.execution.event.RunManagerListenerEvent;
import consulo.ide.impl.idea.execution.compound.CompoundRunConfiguration;
import consulo.ide.impl.idea.execution.impl.RunManagerImpl;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.BiPredicate;

@Singleton
@State(name = "ExecutionTargetManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public final class ExecutionTargetManagerImpl extends ExecutionTargetManager implements PersistentStateComponent<Element> {
  public static final ExecutionTarget MULTIPLE_TARGETS = new ExecutionTarget() {
    @Nonnull
    @Override
    public String getId() {
      return "multiple_targets";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return ExecutionBundle.message("multiple.specified");
    }

    @Override
    public Image getIcon() {
      return null;
    }

    @Override
    public boolean canRun(@Nonnull RunConfiguration configuration) {
      return true;
    }
  };

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final Object myActiveTargetLock = new Object();
  @Nullable
  private ExecutionTarget myActiveTarget;

  @Nullable
  private String mySavedActiveTargetId;

  @Inject
  public ExecutionTargetManagerImpl(@Nonnull Project project) {
    myProject = project;

    project.getMessageBus().connect().subscribe(RunManagerListener.class, new RunManagerListener() {
      @Override
      public void runConfigurationChanged(@Nonnull RunManagerListenerEvent event) {
        RunnerAndConfigurationSettings settings = event.getSettings();

        if (settings == event.getSource().getSelectedConfiguration()) {
          updateActiveTarget(event.getSource(), settings);
        }
      }

      @Override
      public void runConfigurationSelected(@Nonnull RunManagerListenerEvent event) {
        updateActiveTarget(event.getSource(), event.getSettings());
      }
    });
  }

  @Nullable
  private RunManagerImpl myRunManager;

  @Nonnull
  private RunManagerImpl getRunManager() {
    RunManagerImpl runManager = myRunManager;
    if (runManager == null) {
      runManager = RunManagerImpl.getInstanceImpl(myProject);
      myRunManager = runManager;
    }
    return runManager;
  }

  @TestOnly
  public void setRunManager(@Nonnull RunManagerImpl runManager) {
    myRunManager = runManager;
  }

  @Override
  public Element getState() {
    Element state = new Element("state");
    synchronized (myActiveTargetLock) {
      if (mySavedActiveTargetId != null && !mySavedActiveTargetId.equals(DefaultExecutionTarget.INSTANCE.getId())) {
        state.setAttribute("SELECTED_TARGET", mySavedActiveTargetId);
      }
    }
    return state;
  }

  @Override
  public void loadState(@Nonnull Element state) {
    synchronized (myActiveTargetLock) {
      if (myActiveTarget == null && mySavedActiveTargetId == null) {
        mySavedActiveTargetId = state.getAttributeValue("SELECTED_TARGET");
      }
    }
  }

  @Nonnull
  @Override
  public ExecutionTarget getActiveTarget() {
    RunManagerImpl runManager = getRunManager();
    synchronized (myActiveTargetLock) {
      if (myActiveTarget == null) {
        updateActiveTarget(runManager);
      }
      return myActiveTarget;
    }
  }

  @Override
  public void setActiveTarget(@Nonnull ExecutionTarget target) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    RunManagerImpl runManager = getRunManager();
    synchronized (myActiveTargetLock) {
      updateActiveTarget(runManager, runManager.getSelectedConfiguration(), target);
    }
  }

  private void updateActiveTarget(@Nonnull RunManager runManager) {
    updateActiveTarget(runManager, runManager.getSelectedConfiguration());
  }

  private void updateActiveTarget(@Nonnull RunManager runManager, @Nullable RunnerAndConfigurationSettings settings) {
    updateActiveTarget(runManager, settings, null);
  }

  private void updateActiveTarget(@Nonnull RunManager runManager,
                                  @Nullable RunnerAndConfigurationSettings settings,
                                  @Nullable ExecutionTarget toSelect) {
    List<ExecutionTarget> suitable =
      settings == null ? List.of(DefaultExecutionTarget.INSTANCE) : getTargetsForImpl(runManager, settings.getConfiguration());
    ExecutionTarget toNotify;
    synchronized (myActiveTargetLock) {
      if (toSelect == null && !DefaultExecutionTarget.INSTANCE.equals(myActiveTarget)) {
        toSelect = myActiveTarget;
      }

      int index = -1;
      if (toSelect != null) {
        index = suitable.indexOf(toSelect);
      }
      else if (mySavedActiveTargetId != null) {
        for (int i = 0, size = suitable.size(); i < size; i++) {
          if (suitable.get(i).getId().equals(mySavedActiveTargetId)) {
            index = i;
            break;
          }
        }
      }
      toNotify = doSetActiveTarget(index >= 0 ? suitable.get(index) : getDefaultTarget(suitable));
    }

    if (toNotify != null) {
      myProject.getMessageBus().syncPublisher(ExecutionTargetListener.class).activeTargetChanged(toNotify);
    }
  }

  private ExecutionTarget getDefaultTarget(List<? extends ExecutionTarget> suitable) {
    // The following cases are possible when we enter this method:
    // a) mySavedActiveTargetId == null. It means that we open / import project for the first time and there is no target selected
    // In this case we are trying to find the first ExecutionTarget that is ready, because we do not have any other conditions.
    // b) mySavedActiveTargetId != null. It means that some target was saved, but we weren't able to find it. Right now it can happen
    // when and only when there was a device connected, it was saved as a target, next the device was disconnected and other device was
    // connected / no devices left connected. In this case we should not select the target that is ready, cause most probably user still
    // needs some device to be selected (or at least the device placeholder). As all the devices and device placeholders are always shown
    // at the beginning of the list, selecting the first item works in this case.
    ExecutionTarget result =
      mySavedActiveTargetId == null ? ContainerUtil.find(suitable, ExecutionTarget::isReady) : ContainerUtil.getFirstItem(suitable);
    return result != null ? result : DefaultExecutionTarget.INSTANCE;
  }

  @Nullable
  private ExecutionTarget doSetActiveTarget(@Nonnull ExecutionTarget newTarget) {
    if (!DefaultExecutionTarget.INSTANCE.equals(newTarget)) {
      mySavedActiveTargetId = newTarget.getId();
    }

    ExecutionTarget prev = myActiveTarget;
    myActiveTarget = newTarget;
    if (prev != null && !prev.equals(myActiveTarget)) {
      return myActiveTarget;
    }
    return null;
  }

  @Override
  public boolean doCanRun(@Nullable RunConfiguration configuration, @Nonnull ExecutionTarget target) {
    return doCanRunImpl(getRunManager(), configuration, target);
  }

  private boolean doCanRunImpl(@Nonnull RunManager runManager, @Nullable RunConfiguration configuration, @Nonnull ExecutionTarget target) {
    if (configuration == null) {
      return false;
    }

    boolean isCompound = configuration instanceof CompoundRunConfiguration;
    if (isCompound && target == MULTIPLE_TARGETS) {
      return true;
    }

    ExecutionTarget defaultTarget = DefaultExecutionTarget.INSTANCE;
    boolean checkFallbackToDefault = isCompound && !target.equals(defaultTarget);

    return doWithEachNonCompoundWithSpecifiedTarget(runManager, configuration, (subConfiguration, executionTarget) -> {
      if (!(subConfiguration instanceof TargetAwareRunProfile)) {
        return true;
      }

      TargetAwareRunProfile targetAwareProfile = (TargetAwareRunProfile)subConfiguration;
      return target.canRun(subConfiguration) && targetAwareProfile.canRunOn(target) || (checkFallbackToDefault && defaultTarget.canRun(
        subConfiguration) && targetAwareProfile.canRunOn(defaultTarget));
    });
  }

  @Nonnull
  @Override
  public List<ExecutionTarget> getTargetsFor(@Nullable RunConfiguration configuration) {
    return getTargetsForImpl(getRunManager(), configuration);
  }

  @Nonnull
  private List<ExecutionTarget> getTargetsForImpl(@Nonnull RunManager runManager, @Nullable RunConfiguration configuration) {
    if (configuration == null) {
      return List.of();
    }

    List<ExecutionTargetProvider> providers = ExecutionTargetProvider.EXTENSION_NAME.getExtensionList();
    LinkedHashSet<ExecutionTarget> result = new LinkedHashSet<>();

    Set<ExecutionTarget> specifiedTargets = new HashSet<>();
    doWithEachNonCompoundWithSpecifiedTarget(runManager, configuration, (subConfiguration, executionTarget) -> {
      for (ExecutionTargetProvider eachTargetProvider : providers) {
        List<ExecutionTarget> supportedTargets = eachTargetProvider.getTargets(myProject, subConfiguration);
        if (executionTarget == null) {
          result.addAll(supportedTargets);
        }
        else if (supportedTargets.contains(executionTarget)) {
          result.add(executionTarget);
          specifiedTargets.add(executionTarget);
          break;
        }
      }
      return true;
    });

    if (!result.isEmpty()) {
      specifiedTargets.forEach(it -> result.retainAll(Collections.singleton(it)));
      if (result.isEmpty()) {
        result.add(MULTIPLE_TARGETS);
      }
    }
    return Collections.unmodifiableList(ContainerUtil.filter(result, it -> doCanRunImpl(runManager, configuration, it)));
  }

  private boolean doWithEachNonCompoundWithSpecifiedTarget(@Nonnull RunManager runManager,
                                                           @Nonnull RunConfiguration configuration,
                                                           @Nonnull BiPredicate<? super RunConfiguration, ? super ExecutionTarget> action) {
    Set<RunConfiguration> recursionGuard = new HashSet<>();
    LinkedList<Pair<RunConfiguration, ExecutionTarget>> toProcess = new LinkedList<>();
    toProcess.add(Pair.create(configuration, null));

    while (!toProcess.isEmpty()) {
      Pair<RunConfiguration, ExecutionTarget> eachWithTarget = toProcess.pollFirst();
      assert eachWithTarget != null;
      if (!recursionGuard.add(eachWithTarget.first)) {
        continue;
      }

      RunConfiguration eachConfiguration = eachWithTarget.first;
      if (eachConfiguration instanceof CompoundRunConfiguration) {
        for (Map.Entry<RunConfiguration, ExecutionTarget> subConfigWithTarget : ((CompoundRunConfiguration)eachConfiguration).getConfigurationsWithTargets(
          runManager).entrySet()) {
          toProcess.add(Pair.create(subConfigWithTarget.getKey(), subConfigWithTarget.getValue()));
        }
      }
      else if (!action.test(eachWithTarget.first, eachWithTarget.second)) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  public ExecutionTarget findTargetByIdFor(@Nullable RunConfiguration configuration, @Nullable String id) {
    if (id == null) {
      return null;
    }
    else {
      return ContainerUtil.find(getTargetsFor(configuration), it -> it.getId().equals(id));
    }
  }

  @Override
  public void update() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    updateActiveTarget(getRunManager());
  }

  @TestOnly
  public void reset(@Nullable RunManagerImpl runManager) {
    mySavedActiveTargetId = null;
    myActiveTarget = null;
    myRunManager = runManager;
  }
}
