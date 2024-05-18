/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.impl.internal.dashboard.action;

import consulo.annotation.DeprecationInfo;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.runner.ExecutionEnvironment;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 14.05.2024
 */
@Deprecated
@DeprecationInfo("Speicific for Rider. Drop")
public class RunToolbarProcessData {
  public static Consumer<ExecutionEnvironment> prepareBaseSettingCustomization(RunnerAndConfigurationSettings settings,
                                                                               Consumer<ExecutionEnvironment> additional) {
    return additional;
  }

  public static Consumer<ExecutionEnvironment> prepareSuppressMainSlotCustomization(RunnerAndConfigurationSettings settings,
                                                                               Consumer<ExecutionEnvironment> additional) {
    return additional;
  }
}
