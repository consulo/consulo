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
package consulo.backgroundTaskByVfsChange;

import com.intellij.execution.CommonProgramRunConfigurationParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 3:44/07.10.13
 */
public interface BackgroundTaskByVfsParameters extends CommonProgramRunConfigurationParameters {
  void setExePath(@NotNull String path);

  @NotNull
  String getExePath();

  void setOutPath(@Nullable String path);

  @Nullable
  String getOutPath();

  void set(@NotNull BackgroundTaskByVfsParameters parameters);

  boolean isShowConsole();

  void setShowConsole(boolean console);
}
