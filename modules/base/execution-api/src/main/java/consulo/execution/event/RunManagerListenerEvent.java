/*
 * Copyright 2013-2023 consulo.io
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
package consulo.execution.event;

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import jakarta.annotation.Nullable;

import java.util.EventObject;

/**
 * @author VISTALL
 * @since 11/09/2023
 */
public class RunManagerListenerEvent extends EventObject {
  private final RunnerAndConfigurationSettings mySettings;
  private final String myExistingId;

  public RunManagerListenerEvent(RunManager source, RunnerAndConfigurationSettings settings, String existingId) {
    super(source);
    mySettings = settings;
    myExistingId = existingId;
  }

  @Nullable
  public String getExistingId() {
    return myExistingId;
  }

  public RunnerAndConfigurationSettings getSettings() {
    return mySettings;
  }

  @Override
  public RunManager getSource() {
    return (RunManager)super.getSource();
  }
}
