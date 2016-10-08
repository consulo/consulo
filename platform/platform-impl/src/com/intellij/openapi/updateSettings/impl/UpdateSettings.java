/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.*;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

@State(
        name = "UpdatesConfigurable",
        storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/updates.xml", roamingType = RoamingType.DISABLED)}
)
@Deprecated
public class UpdateSettings implements PersistentStateComponent<UpdateSettings.State> {
  private static final UpdateSettings ourInstance = new UpdateSettings();
  private State myState = new State();

  public UpdateSettings() {
  }

  public static UpdateSettings getInstance() {
    return ourInstance;
  }

  static class State {

    public boolean CHECK_NEEDED = true;
    public long LAST_TIME_CHECKED = 0;

    public String LAST_BUILD_CHECKED;
    public String UPDATE_CHANNEL_TYPE = ChannelStatus.RELEASE_CODE;
  }

  public boolean isCheckNeeded() {
    return myState.CHECK_NEEDED;
  }

  public void setCheckNeeded(boolean value) {
    myState.CHECK_NEEDED = value;
  }

  @NotNull
  public String getUpdateChannelType() {
    return myState.UPDATE_CHANNEL_TYPE;
  }

  public long getLastTimeChecked() {
    return myState.LAST_TIME_CHECKED;
  }

  public void setUpdateChannelType(@NotNull String value) {
    myState.UPDATE_CHANNEL_TYPE = value;
  }


  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
    myState.LAST_BUILD_CHECKED = StringUtil.nullize(myState.LAST_BUILD_CHECKED);
  }

  public void forceCheckForUpdateAfterRestart() {
    myState.LAST_TIME_CHECKED = 0;
  }

  public void saveLastCheckedInfo() {
    myState.LAST_TIME_CHECKED = System.currentTimeMillis();
    myState.LAST_BUILD_CHECKED = ApplicationInfo.getInstance().getBuild().asString();
  }
}