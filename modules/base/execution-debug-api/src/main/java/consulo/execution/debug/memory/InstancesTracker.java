// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.memory;

import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.execution.debug.memory.event.InstancesTrackerListener;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@State(name = "InstancesTracker", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class InstancesTracker implements PersistentStateComponent<InstancesTracker.MyState> {
    private final EventDispatcher<InstancesTrackerListener> myDispatcher = EventDispatcher.create(InstancesTrackerListener.class);
    private MyState myState = new MyState();

    public static InstancesTracker getInstance(@Nonnull Project project) {
        return project.getInstance(InstancesTracker.class);
    }

    public boolean isTracked(@Nonnull String className) {
        return myState.classes.containsKey(className);
    }

    public boolean isBackgroundTrackingEnabled() {
        return myState.isBackgroundTrackingEnabled;
    }

    @Nullable
    public TrackingType getTrackingType(@Nonnull String className) {
        return myState.classes.getOrDefault(className, null);
    }

    @Nonnull
    public Map<String, TrackingType> getTrackedClasses() {
        return new HashMap<>(myState.classes);
    }

    public void add(@Nonnull String name, @Nonnull TrackingType type) {
        if (type.equals(myState.classes.getOrDefault(name, null))) {
            return;
        }

        myState.classes.put(name, type);
        myDispatcher.getMulticaster().classChanged(name, type);
    }

    public void remove(@Nonnull String name) {
        TrackingType removed = myState.classes.remove(name);
        if (removed != null) {
            myDispatcher.getMulticaster().classRemoved(name);
        }
    }

    public void addTrackerListener(@Nonnull InstancesTrackerListener listener) {
        myDispatcher.addListener(listener);
    }

    public void addTrackerListener(@Nonnull InstancesTrackerListener listener, @Nonnull Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    public void removeTrackerListener(@Nonnull InstancesTrackerListener listener) {
        myDispatcher.removeListener(listener);
    }

    public void setBackgroundTackingEnabled(boolean state) {
        boolean oldState = myState.isBackgroundTrackingEnabled;
        if (state != oldState) {
            myState.isBackgroundTrackingEnabled = state;
            myDispatcher.getMulticaster().backgroundTrackingValueChanged(state);
        }
    }

    @Nullable
    @Override
    public MyState getState() {
        return new MyState(myState);
    }

    @Override
    public void loadState(@Nonnull MyState state) {
        myState = new MyState(state);
    }

    static class MyState {
        boolean isBackgroundTrackingEnabled = false;

        @AbstractCollection(elementTypes = Map.Entry.class)
        final Map<String, TrackingType> classes = new ConcurrentHashMap<>();

        MyState() {
        }

        MyState(@Nonnull MyState state) {
            isBackgroundTrackingEnabled = state.isBackgroundTrackingEnabled;
            classes.putAll(state.classes);
        }
    }
}
