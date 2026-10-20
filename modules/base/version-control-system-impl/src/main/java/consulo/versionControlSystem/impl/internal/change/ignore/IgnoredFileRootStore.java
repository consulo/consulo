// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change.ignore;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.inject.Singleton;

@Singleton
@State(name = "IgnoredFileRootStore", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class IgnoredFileRootStore implements PersistentStateComponent<IgnoredFileRootStoreState> {
    private IgnoredFileRootStoreState myState = new IgnoredFileRootStoreState();

    public static IgnoredFileRootStore getInstance(Project project) {
        return project.getInstance(IgnoredFileRootStore.class);
    }

    public boolean containsRoot(String root) {
        return myState.generatedRoots.contains(root);
    }

    public void addRoot(String root) {
        myState.generatedRoots.add(root);
    }

    @Override
    public IgnoredFileRootStoreState getState() {
        return myState;
    }

    @Override
    public void loadState(IgnoredFileRootStoreState state) {
        myState = state;
    }
}
