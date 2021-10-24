/*
 * Copyright 2013-2021 consulo.io
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
package consulo.startup;

import consulo.ide.eap.EarlyAccessProgramDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24/10/2021
 */
public class PluginPermissionEarlyAccessProgramDescriptor extends EarlyAccessProgramDescriptor {
  @Nonnull
  @Override
  public String getName() {
    return "Plugin permissions";
  }

  @Nullable
  @Override
  public String getDescription() {
    return "Enable plugin permissions for plugins. See https://github.com/consulo/long-term-todo/issues/12";
  }

  @Override
  public boolean isRestartRequired() {
    return true;
  }
}
