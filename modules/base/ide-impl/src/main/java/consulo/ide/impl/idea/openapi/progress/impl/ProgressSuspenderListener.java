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
package consulo.ide.impl.idea.openapi.progress.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import jakarta.annotation.Nonnull;

@TopicAPI(ComponentScope.APPLICATION)
public interface ProgressSuspenderListener {
  /**
   * Called (on any thread) when a new progress is created with suspension capability
   */
  default void suspendableProgressAppeared(@Nonnull ProgressSuspender suspender) {
  }

  /**
   * Called (on any thread) when a progress is suspended or resumed
   */
  default void suspendedStatusChanged(@Nonnull ProgressSuspender suspender) {
  }
}
