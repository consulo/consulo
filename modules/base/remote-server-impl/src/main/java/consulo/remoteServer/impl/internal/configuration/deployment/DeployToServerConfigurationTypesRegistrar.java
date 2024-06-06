/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationType;
import consulo.component.ComponentManager;
import consulo.remoteServer.ServerType;
import consulo.component.extension.ExtensionExtender;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author nik
 */
@Singleton
@ExtensionImpl
public class DeployToServerConfigurationTypesRegistrar implements ExtensionExtender<ConfigurationType> {
  @Override
  public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<ConfigurationType> consumer) {
    ServerType.EP_NAME.forEachExtensionSafe(DeployToServerConfigurationType::new);
  }

  @Override
  public boolean hasAnyExtensions(ComponentManager componentManager) {
    return ServerType.EP_NAME.hasAnyExtensions();
  }

  @Nonnull
  @Override
  public Class<ConfigurationType> getExtensionClass() {
    return ConfigurationType.class;
  }
}
