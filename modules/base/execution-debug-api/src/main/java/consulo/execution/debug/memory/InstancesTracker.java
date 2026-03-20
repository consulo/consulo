// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.memory;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.execution.debug.memory.event.InstancesTrackerListener;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@State(name = "InstancesTracker", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class InstancesTracker implements PersistentStateComponent<InstancesTracker.MyState> {
    private final EventDispatcher<InstancesTrackerListener> myDispatcher = EventDispatcher.create(InstancesTrackerListener.class);
    private MyState myState = new MyState();

    public static InstancesTracker getInstance(Project project) {
        return project.getInstance(InstancesTracker.class);
    }

    public boolean isTracked(String className) {
        return myState.classes.containsKey(className);
    }

    public boolean isBackgroundTrackingEnabled() {
        return myState.isBackgroundTrackingEnabled;
    }

    public @Nullable TrackingType getTrackingType(String className) {
        return myState.classes.getOrDefault(className, null);
    }

    
    public Map<String, TrackingType> getTrackedClasses() {
        return new HashMap<>(myState.classes);
    }

    public void add(String name, TrackingType type) {
        if (type.equals(myState.classes.getOrDefault(name, null))) {
            return;
        }

        myState.classes.put(name, type);
        myDispatcher.getMulticaster().classChanged(name, type);
    }

    public void remove(String name) {
        TrackingType removed = myState.classes.remove(name);
        if (removed != null) {
            myDispatcher.getMulticaster().classRemoved(name);
        }
    }

    public void addTrackerListener(InstancesTrackerListener listener) {
        myDispatcher.addListener(listener);
    }

    public void addTrackerListener(InstancesTrackerListener listener, Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    public void removeTrackerListener(InstancesTrackerListener listener) {
        myDispatcher.removeListener(listener);
    }

    public void setBackgroundTackingEnabled(boolean state) {
        boolean oldState = myState.isBackgroundTrackingEnabled;
        if (state != oldState) {
            myState.isBackgroundTrackingEnabled = state;
            myDispatcher.getMulticaster().backgroundTrackingValueChanged(state);
        }
    }

    @Override
    public @Nullable MyState getState() {
        return new MyState(myState);
    }

    @Override
    public void loadState(MyState state) {
        myState = new MyState(state);
    }

    static class MyState {
        boolean isBackgroundTrackingEnabled = false;

        @AbstractCollection(elementTypes = Map.Entry.class)
        final Map<String, TrackingType> classes = new ConcurrentHashMap<>();

        MyState() {
        }

        MyState(MyState state) {
            isBackgroundTrackingEnabled = state.isBackgroundTrackingEnabled;
            classes.putAll(state.classes);
        }
    }
}
