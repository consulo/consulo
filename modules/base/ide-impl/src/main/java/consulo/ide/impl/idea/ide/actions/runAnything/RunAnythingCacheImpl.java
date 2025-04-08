// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.internal.RunAnythingCache;
import consulo.project.Project;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@State(name = "RunAnythingCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class RunAnythingCacheImpl implements RunAnythingCache, PersistentStateComponent<RunAnythingCacheImpl.State> {
    private final State mySettings = new State();

    public static RunAnythingCacheImpl getInstance(Project project) {
        return project.getInstance(RunAnythingCacheImpl.class);
    }

    /**
     * @return true is group is visible; false if it's hidden
     */
    @Override
    public boolean isGroupVisible(@Nonnull String key) {
        return mySettings.myKeys.getOrDefault(key, true);
    }

    /**
     * Saves group visibility flag
     *
     * @param key     to store visibility flag
     * @param visible true if group should be shown
     */
    @Override
    public void saveGroupVisibilityKey(@Nonnull String key, boolean visible) {
        mySettings.myKeys.put(key, visible);
    }

    @Nonnull
    @Override
    public State getState() {
        return mySettings;
    }

    @Override
    public void loadState(@Nonnull State state) {
        XmlSerializerUtil.copyBean(state, mySettings);
    }

    public static class State {
        @Nonnull
        @MapAnnotation(entryTagName = "visibility", keyAttributeName = "group", valueAttributeName = "flag")
        public final Map<String, Boolean> myKeys = new HashMap<>();

        @Nonnull
        @Tag("commands")
        @AbstractCollection(surroundWithTag = false, elementTag = "command")
        public final List<String> myCommands = new ArrayList<>();

        @Nonnull
        public List<String> getCommands() {
            return myCommands;
        }
    }
}