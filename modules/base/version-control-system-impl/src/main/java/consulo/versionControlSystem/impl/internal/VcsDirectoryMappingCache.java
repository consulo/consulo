// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@State(name = "VcsDirectoryMappingCacheV2", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsDirectoryMappingCache implements PersistentStateComponent<VcsDirectoryMappingCache.MyState> {
  @Tag("state")
  public static class MyState {
    @Attribute
    public @Nullable String vcsName;

    @AbstractCollection(elementTag = "root", elementValueAttribute = "path")
    public Set<String> roots = new LinkedHashSet<>();
  }

  private MyState myState = new MyState();

  public static VcsDirectoryMappingCache getInstance(Project project) {
    return project.getInstance(VcsDirectoryMappingCache.class);
  }

  @Override
  public MyState getState() {
    return myState;
  }

  @Override
  public void loadState(MyState state) {
    myState = state;
  }

  public List<String> getMappings(String vcs) {
    MyState state = myState;
    if (vcs.equals(state.vcsName)) {
      return new ArrayList<>(state.roots);
    }
    return List.of();
  }

  public void setMappings(@Nullable String vcs, List<String> vcsRoots) {
    MyState newState = new MyState();
    newState.vcsName = vcs;
    newState.roots.addAll(vcsRoots);
    myState = newState;
  }
}
