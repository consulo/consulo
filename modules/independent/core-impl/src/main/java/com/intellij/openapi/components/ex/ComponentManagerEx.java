/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.components.ex;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.PluginDescriptor;

import javax.annotation.Nonnull;

/**
 * @author max
 */
@Deprecated
public interface ComponentManagerEx extends ComponentManager {

  default void registerComponent(@Nonnull ComponentConfig config) {
    throw new UnsupportedOperationException();
  }

  default void registerComponent(@Nonnull ComponentConfig config, PluginDescriptor pluginDescriptor) {
    throw new UnsupportedOperationException();
  }

  default void initializeFromStateStore(@Nonnull Object component, boolean service) {
    throw new UnsupportedOperationException();
  }
}
