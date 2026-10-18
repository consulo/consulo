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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabLocation;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

@Singleton
@State(name = "Vcs.Log.Tabs.Properties", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsLogTabsProperties implements PersistentStateComponent<VcsLogTabsProperties.State> {
  public static final String MAIN_LOG_ID = "MAIN";
  private State myState = new State();

  public VcsLogTabsProperties() {
  }

  @Override
  public @Nullable State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public MainVcsLogUiProperties createProperties(final String id) {
    myState.TAB_STATES.putIfAbsent(id, new VcsLogUiPropertiesImpl.State());
    return new VcsLogUiPropertiesImpl() {
      
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

  public boolean checkTabHasFilters(String id) {
    VcsLogUiPropertiesImpl.State state = myState.TAB_STATES.get(id);
    return state != null && !state.FILTERS.isEmpty();
  }

  public void addTab(String tabId, VcsLogTabLocation location) {
    myState.OPEN_GENERIC_TABS.put(tabId, location);
  }

  public void removeTab(String tabId) {
    myState.OPEN_GENERIC_TABS.remove(tabId);
    resetState(tabId);
  }

  public void resetState(String tabId) {
    myState.TAB_STATES.remove(tabId);
  }

  public Map<String, VcsLogTabLocation> getTabs() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(myState.OPEN_GENERIC_TABS));
  }

  public static class State {
    public Map<String, VcsLogUiPropertiesImpl.State> TAB_STATES = new TreeMap<>();
    public LinkedHashMap<String, VcsLogTabLocation> OPEN_GENERIC_TABS = new LinkedHashMap<>();
  }
}
