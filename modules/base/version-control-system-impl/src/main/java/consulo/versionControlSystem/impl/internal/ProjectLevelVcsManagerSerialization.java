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
package consulo.versionControlSystem.impl.internal;

import consulo.versionControlSystem.VcsShowConfirmationOption;
import consulo.versionControlSystem.internal.VcsShowConfirmationOptionImpl;
import consulo.versionControlSystem.internal.VcsShowOptionsSettingImpl;

import java.util.HashMap;
import java.util.Map;

public class ProjectLevelVcsManagerSerialization {
  // read-only can be kept here
  private final Map<String, VcsShowConfirmationOption.Value> myReadValue;

  public ProjectLevelVcsManagerSerialization() {
    myReadValue = new HashMap<>();
  }

  private static VcsShowOptionsSettingImpl getOrCreateOption(Map<String, VcsShowOptionsSettingImpl> options, String actionName) {
    if (!options.containsKey(actionName)) {
      options.put(actionName, new VcsShowOptionsSettingImpl(actionName));
    }
    return options.get(actionName);
  }

  public void readExternalUtil(ProjectLevelVcsManagerState state, OptionsAndConfirmations optionsAndConfirmations) {
    Map<String, VcsShowOptionsSettingImpl> options = optionsAndConfirmations.getOptions();
    for (VcsOptionsSettingState settingState : state.options) {
      if (settingState.id != null) {
        getOrCreateOption(options, settingState.id).setValue(settingState.value);
      }
    }

    myReadValue.clear();
    for (VcsConfirmationsSettingState settingState : state.confirmations) {
      if (settingState.id != null && settingState.value != null) {
        myReadValue.put(settingState.id, VcsShowConfirmationOption.Value.fromString(settingState.value));
      }
    }
  }

  public void writeExternalUtil(ProjectLevelVcsManagerState state, OptionsAndConfirmations optionsAndConfirmations) {
    Map<String, VcsShowOptionsSettingImpl> options = optionsAndConfirmations.getOptions();
    Map<String, VcsShowConfirmationOptionImpl> confirmations = optionsAndConfirmations.getConfirmations();

    for (VcsShowOptionsSettingImpl setting : options.values()) {
      if (!setting.getValue()) {
        VcsOptionsSettingState settingState = new VcsOptionsSettingState();
        settingState.value = setting.getValue();
        settingState.id = setting.getDisplayName();
        state.options.add(settingState);
      }
    }

    for (VcsShowConfirmationOptionImpl setting : confirmations.values()) {
      if (setting.getValue() != VcsShowConfirmationOption.Value.SHOW_CONFIRMATION) {
        VcsConfirmationsSettingState settingState = new VcsConfirmationsSettingState();
        settingState.value = setting.getValue().toString();
        settingState.id = setting.getDisplayName();
        state.confirmations.add(settingState);
      }
    }
  }

  public VcsShowConfirmationOption.Value getInitOptionValue(String id) {
    return myReadValue.get(id);
  }
}
