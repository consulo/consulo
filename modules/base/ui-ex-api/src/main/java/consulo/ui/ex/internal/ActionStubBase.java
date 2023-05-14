/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.ex.internal;

import consulo.application.Application;
import consulo.container.plugin.PluginId;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * from kotlin
 */
public interface ActionStubBase {
  String getId();

  PluginId getPluginId();

  String getIconPath();

  @Nullable
  AnAction initialize(@Nonnull Application application, @Nonnull ActionManager manager);
}
