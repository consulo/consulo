/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.execution.RunnerAndConfigurationSettings;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.EventListener;

@TopicAPI(ComponentScope.PROJECT)
@SuppressWarnings("deprecation")
public interface RunManagerListener extends EventListener {
  default void runConfigurationAdded(@Nonnull RunManagerListenerEvent event) {
    runConfigurationAdded(event.getSettings());
  }

  default void runConfigurationChanged(@Nonnull RunManagerListenerEvent event) {
    runConfigurationChanged(event.getSettings(), event.getExistingId());
  }

  default void runConfigurationRemoved(@Nonnull RunManagerListenerEvent event) {
    runConfigurationRemoved(event.getSettings());
  }

  default void runConfigurationSelected(@Nonnull RunManagerListenerEvent event) {
    runConfigurationSelected(event.getSettings());
  }

  default void beforeRunTasksChanged(@Nonnull RunManagerListenerEvent event) {
  }

  @Deprecated
  default void runConfigurationSelected(@Nullable RunnerAndConfigurationSettings settings) {
    runConfigurationSelected();
  }

  @Deprecated
  default void runConfigurationSelected() {
  }

  @Deprecated
  default void runConfigurationAdded(@Nonnull RunnerAndConfigurationSettings settings) {
  }

  @Deprecated
  default void runConfigurationRemoved(@Nonnull RunnerAndConfigurationSettings settings) {
  }

  @Deprecated
  default void runConfigurationChanged(@Nonnull RunnerAndConfigurationSettings settings, String existingId) {
    runConfigurationChanged(settings);
  }

  @Deprecated
  default void runConfigurationChanged(@Nonnull RunnerAndConfigurationSettings settings) {
  }

  default void beginUpdate() {
  }

  default void endUpdate() {
  }
}
