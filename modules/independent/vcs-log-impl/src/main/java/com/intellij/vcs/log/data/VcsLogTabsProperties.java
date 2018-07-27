/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.vcs.log.data;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Map;

@State(name = "Vcs.Log.Tabs.Properties", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@Singleton
public class VcsLogTabsProperties implements PersistentStateComponent<VcsLogTabsProperties.State> {
  public static final String MAIN_LOG_ID = "MAIN";
  private State myState = new State();

  @Inject
  public VcsLogTabsProperties() {
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public MainVcsLogUiProperties createProperties(@Nonnull final String id) {
    myState.TAB_STATES.putIfAbsent(id, new VcsLogUiPropertiesImpl.State());
    return new VcsLogUiPropertiesImpl() {
      @Nonnull
      @Override
      public State getState() {
        State state = myState.TAB_STATES.get(id);
        if (state == null) {
          state = new VcsLogUiPropertiesImpl.State();
          myState.TAB_STATES.put(id, state);
        }
        return state;
      }

      @Override
      public void loadState(State state) {
        myState.TAB_STATES.put(id, state);
      }
    };
  }

  public static class State {
    public Map<String, VcsLogUiPropertiesImpl.State> TAB_STATES = ContainerUtil.newTreeMap();
  }
}
