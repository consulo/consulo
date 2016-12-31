/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.updateSettings;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import consulo.lombok.annotations.ApplicationService;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author VISTALL
 * @since 02-Sep-16
 */
@State(
        name = "UpdateSettings",
        storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/updates.xml", roamingType = RoamingType.DISABLED)})
@ApplicationService
public class UpdateSettings implements PersistentStateComponent<UpdateSettings.State> {
  static class State {
    public boolean enable = true;
    public long lastTimeCheck = 0;
    public UpdateChannel channel;
  }

  private State myState = new State();

  @NotNull
  private static UpdateChannel findDefaultChannel() {
    File file = PathManager.getDistributionDirectory();
    for (UpdateChannel channel : UpdateChannel.values()) {
      if (new File(file, "." + channel.name()).exists()) {
        return channel;
      }
    }

    return UpdateChannel.release;
  }

  @NotNull
  public UpdateChannel getChannel() {
    UpdateChannel channel = myState.channel;
    if (channel == null) {
      myState.channel = channel = findDefaultChannel();
    }
    return channel;
  }

  public void setChannel(@NotNull UpdateChannel channel) {
    myState.channel = channel;
  }

  public boolean isEnable() {
    return myState.enable;
  }

  public void setEnable(boolean value) {
    myState.enable = value;
  }

  public long getLastTimeCheck() {
    return myState.lastTimeCheck;
  }

  public void setLastTimeCheck(long time) {
    myState.lastTimeCheck = time;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }
}
