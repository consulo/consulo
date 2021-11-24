/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.updateSettings.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import consulo.container.plugin.PluginId;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 21/11/2021
 */
@Singleton
@State(name = "UpdateHistory", storages = @Storage(value = "updateHistory.xml", roamingType = RoamingType.DISABLED))
public class UpdateHistory implements PersistentStateComponent<UpdateHistory.State> {
  public static class State {
    public Map<String, String> pluginVersions = new TreeMap<>();

    public boolean showChangeLog;

    public boolean showExperimentalWarning = true;
  }

  private State myState = new State();

  public void replaceHistory(Map<String, String> history) {
    myState.pluginVersions.putAll(history);

    myState.showChangeLog = true;
  }

  public void setShowChangeLog(boolean value) {
    myState.showChangeLog = value;
  }

  public boolean isShowChangeLog() {
    return myState.showChangeLog;
  }

  public void setShowExperimentalWarning(boolean value) {
    myState.showExperimentalWarning = value;
  }

  public boolean isShowExperimentalWarning() {
    return myState.showExperimentalWarning;
  }

  @Nonnull
  public String getHistoryVersion(PluginId pluginId, String defaultVersion) {
    return myState.pluginVersions.getOrDefault(pluginId.getIdString(), defaultVersion);
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
}
