// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.ui.event.ModalityStateListener;
import consulo.application.impl.internal.LaterInvocator;
import consulo.util.collection.FactoryMap;
import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.component.util.BusyObject;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ex.UiActivity;
import consulo.ui.ex.UiActivityMonitor;
import consulo.ui.ex.awt.UIUtil;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.*;

@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION)
public class UiActivityMonitorImpl extends UiActivityMonitor implements ModalityStateListener, Disposable {
  private final Map<Project, BusyContainer> myObjects = FactoryMap.create(this::create);

  @Nonnull
  private BusyContainer create(Project key) {
    if (myObjects.isEmpty()) {
      installListener();
    }
    return key == null ? new BusyContainer(null) : new BusyContainer(null) {
      @Nonnull
      @Override
      protected BusyImpl createBusyImpl(@Nonnull Set<UiActivity> key) {
        return new BusyImpl(key, this) {
          @Override
          public boolean isReady() {
            for (Map.Entry<Project, BusyContainer> entry : myObjects.entrySet()) {
              BusyContainer eachContainer = entry.getValue();
              BusyImpl busy = eachContainer.getOrCreateBusy(myToWatchArray);
              if (busy == this) continue;
              if (!busy.isOwnReady()) return false;
            }
            return isOwnReady();
          }
        };
      }
    };
  }

  private boolean myActive;

  @Nonnull
  private final BusyObject myEmptyBusy = new BusyObject.Impl() {
    @Override
    public boolean isReady() {
      return true;
    }
  };

  public void installListener() {
    LaterInvocator.addModalityStateListener(this, this);
  }

  @Override
  public void dispose() {
    myObjects.clear();
  }

  @Override
  public void beforeModalityStateChanged(boolean entering, @Nonnull Object modalEntity) {
    SwingUtilities.invokeLater(this::maybeReady);
  }

  public void maybeReady() {
    for (BusyContainer each : myObjects.values()) {
      each.onReady();
    }
  }

  @Nonnull
  @Override
  public BusyObject getBusy(@Nonnull ComponentManager project, @Nonnull UiActivity... toWatch) {
    if (!isActive()) return myEmptyBusy;

    return _getBusy((Project)project, toWatch);
  }

  @Nonnull
  @Override
  public BusyObject getBusy(@Nonnull UiActivity... toWatch) {
    if (!isActive()) return myEmptyBusy;

    return _getBusy(null, toWatch);
  }

  @Override
  public void addActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity) {
    addActivity(project, activity, getDefaultModalityState());
  }

  @Override
  public void addActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity, @Nonnull consulo.ui.ModalityState effectiveModalityState) {
    if (!isActive()) return;


    UIUtil.invokeLaterIfNeeded(() -> getBusyContainer((Project)project).addActivity(activity, effectiveModalityState));
  }

  @Override
  public void removeActivity(@Nonnull ComponentManager project, @Nonnull UiActivity activity) {
    if (!isActive()) return;

    UIUtil.invokeLaterIfNeeded(() -> _getBusy((Project)project).removeActivity(activity));
  }

  @Override
  public void addActivity(@Nonnull UiActivity activity) {
    addActivity(activity, getDefaultModalityState());
  }

  private static IdeaModalityState getDefaultModalityState() {
    return (IdeaModalityState)ApplicationManager.getApplication().getNoneModalityState();
  }

  @Override
  public void addActivity(@Nonnull UiActivity activity, @Nonnull consulo.ui.ModalityState effectiveModalityState) {
    if (!isActive()) return;

    UIUtil.invokeLaterIfNeeded(() -> getBusyContainer(null).addActivity(activity, effectiveModalityState));
  }

  @Override
  public void removeActivity(@Nonnull UiActivity activity) {
    if (!isActive()) return;

    UIUtil.invokeLaterIfNeeded(() -> _getBusy(null).removeActivity(activity));
  }

  @Nonnull
  private BusyImpl _getBusy(@Nullable Project key, @Nonnull UiActivity... toWatch) {
    return getBusyContainer(key).getOrCreateBusy(toWatch);
  }

  @Nonnull
  private BusyContainer getBusyContainer(@Nullable Project key) {
    BusyContainer container = myObjects.get(key);
    return container != null ? container : getGlobalBusy();
  }

  void initBusyObjectFor(@Nullable Project key) {
    myObjects.put(key, new BusyContainer(key));
  }

  boolean hasObjectFor(Project project) {
    return myObjects.containsKey(project);
  }

  private BusyContainer getGlobalBusy() {
    return myObjects.get(null);
  }

  @Override
  public void clear() {
    Set<Project> keys = myObjects.keySet();
    for (Project each : keys) {
      myObjects.get(each).clear();
    }
  }

  @Override
  public void setActive(boolean active) {
    if (myActive == active) return;

    if (myActive) {
      clear();
    }

    myActive = active;
  }

  public boolean isActive() {
    return myActive;
  }

  private static class ActivityInfo {
    private final IdeaModalityState myEffectiveState;

    private ActivityInfo(@Nonnull IdeaModalityState effectiveState) {
      myEffectiveState = effectiveState;
    }

    @Nonnull
    public IdeaModalityState getEffectiveState() {
      return myEffectiveState;
    }
  }

  @Nonnull
  protected IdeaModalityState getCurrentState() {
    return IdeaModalityState.current();
  }

  private class BusyImpl extends BusyObject.Impl {

    private final Map<UiActivity, ActivityInfo> myActivities = new HashMap<>();

    private final Set<UiActivity> myQueuedToRemove = new HashSet<>();

    protected final Set<UiActivity> myToWatch;
    protected final UiActivity[] myToWatchArray;
    private final UiActivityMonitorImpl.BusyContainer myContainer;

    private BusyImpl(@Nonnull Set<UiActivity> toWatch, @Nonnull BusyContainer container) {
      myToWatch = toWatch;
      myToWatchArray = toWatch.toArray(new UiActivity[0]);
      myContainer = container;
    }

    @Override
    public boolean isReady() {
      return isOwnReady() && getGlobalBusy().getOrCreateBusy(myToWatchArray).isOwnReady();
    }

    boolean isOwnReady() {
      Map<UiActivity, ActivityInfo> infoToCheck = new HashMap<>();

      for (Set<UiActivity> eachActivitySet : myContainer.myActivities2Object.keySet()) {
        BusyImpl eachBusyObject = myContainer.myActivities2Object.get(eachActivitySet);
        if (eachBusyObject == this) continue;

        for (UiActivity eachOtherActivity : eachActivitySet) {
          for (UiActivity eachToWatch : myToWatch) {
            if (eachToWatch.isSameOrGeneralFor(eachOtherActivity) && eachBusyObject.myActivities.containsKey(eachOtherActivity)) {
              infoToCheck.put(eachOtherActivity, eachBusyObject.myActivities.get(eachOtherActivity));
            }
          }
        }
      }

      infoToCheck.putAll(myActivities);

      if (infoToCheck.isEmpty()) return true;

      IdeaModalityState current = getCurrentState();
      for (Map.Entry<UiActivity, ActivityInfo> entry : infoToCheck.entrySet()) {
        ActivityInfo info = entry.getValue();
        if (!current.dominates(info.getEffectiveState())) {
          return false;
        }
      }

      return true;
    }

    public void addActivity(@Nonnull UiActivity activity, @Nonnull IdeaModalityState effectiveModalityState) {
      if (!myToWatch.isEmpty() && !myToWatch.contains(activity)) return;

      myActivities.put(activity, new ActivityInfo(effectiveModalityState));
      myQueuedToRemove.remove(activity);

      myContainer.onActivityAdded(activity);
    }

    public void removeActivity(@Nonnull UiActivity activity) {
      if (!myActivities.containsKey(activity)) return;

      myQueuedToRemove.add(activity);

      Runnable runnable = () -> {
        if (!myQueuedToRemove.contains(activity)) return;

        myQueuedToRemove.remove(activity);
        myActivities.remove(activity);
        myContainer.onActivityRemoved(this, activity);

        onReady();
      };
      SwingUtilities.invokeLater(runnable);
    }
  }

  public class BusyContainer implements Disposable {
    private final Map<Set<UiActivity>, BusyImpl> myActivities2Object = new HashMap<>();
    private final Map<BusyImpl, Set<UiActivity>> myObject2Activities = new HashMap<>();

    private final Set<UiActivity> myActivities = new HashSet<>();

    private boolean myRemovingActivityNow;
    @Nullable
    private final Project myProject;

    public BusyContainer(@Nullable Project project) {
      myProject = project;
      registerBusyObject(new HashSet<>());
      if (project != null) {
        Disposer.register(project, this);
      }
    }

    @Nonnull
    public BusyImpl getOrCreateBusy(@Nonnull UiActivity... activities) {
      Set<UiActivity> key = new HashSet<>(Arrays.asList(activities));

      if (myActivities2Object.containsKey(key)) {
        return myActivities2Object.get(key);
      }
      return registerBusyObject(key);
    }

    @Nonnull
    private BusyImpl registerBusyObject(@Nonnull Set<UiActivity> key) {
      BusyImpl busy = createBusyImpl(key);
      myActivities2Object.put(key, busy);
      myObject2Activities.put(busy, key);
      return busy;
    }

    @Nonnull
    protected BusyImpl createBusyImpl(@Nonnull Set<UiActivity> key) {
      return new BusyImpl(key, this);
    }

    public void onReady() {
      Iterator<Set<UiActivity>> keyIterator = myActivities2Object.keySet().iterator();
      while (keyIterator.hasNext()) {
        Set<UiActivity> eachKey = keyIterator.next();
        BusyImpl busy = myActivities2Object.get(eachKey);
        busy.onReady();
        if (busy.isReady()) {
          keyIterator.remove();
          myObject2Activities.remove(busy);
        }
      }
    }

    public void clear() {
      UiActivity[] activities = myActivities.toArray(new UiActivity[0]);
      for (UiActivity each : activities) {
        removeActivity(each);
      }
    }

    public void onActivityAdded(@Nonnull UiActivity activity) {
      myActivities.add(activity);
    }

    public void onActivityRemoved(@Nonnull BusyImpl busy, @Nonnull UiActivity activity) {
      if (myRemovingActivityNow) return;

      Map<BusyImpl, Set<UiActivity>> toRemove = new HashMap<>();

      try {
        myRemovingActivityNow = true;

        myActivities.remove(activity);
        for (BusyImpl each : myObject2Activities.keySet()) {
          if (each != busy) {
            each.removeActivity(activity);
          }
          if (each.isReady()) {
            Set<UiActivity> activities = myObject2Activities.get(busy);
            toRemove.put(busy, activities);
          }
        }
      }
      finally {
        for (BusyImpl each : toRemove.keySet()) {
          Set<UiActivity> activities = myObject2Activities.remove(each);
          myActivities2Object.remove(activities);
        }

        myRemovingActivityNow = false;
      }
    }

    public void addActivity(@Nonnull UiActivity activity, @Nonnull consulo.ui.ModalityState state) {
      getOrCreateBusy(activity);
      Set<BusyImpl> busies = myObject2Activities.keySet();
      for (BusyImpl each : busies) {
        each.addActivity(activity, (IdeaModalityState)state);
      }
    }

    @Override
    public void dispose() {
      myObjects.remove(myProject);
    }
  }
}
