/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.ide.actions.runAnything;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
@Singleton
@State(name = "RunAnythingContextRecentDirectoryCache", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RunAnythingContextRecentDirectoryCache implements PersistentStateComponent<RunAnythingContextRecentDirectoryCache.State> {
  static class State {
    public List<String> paths = new ArrayList<>();
  }

  @Nonnull
  public static RunAnythingContextRecentDirectoryCache getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, RunAnythingContextRecentDirectoryCache.class);
  }

  private State myState = new State();

  @Nonnull
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }
}
