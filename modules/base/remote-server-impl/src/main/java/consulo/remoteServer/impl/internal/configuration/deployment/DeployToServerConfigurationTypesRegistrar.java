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
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.component.extension.ExtensionExtender;
import consulo.execution.configuration.ConfigurationType;
import consulo.remoteServer.ServerType;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author nik
 */
@ExtensionImpl
@SuppressWarnings("unchecked")
public class DeployToServerConfigurationTypesRegistrar implements ExtensionExtender<ConfigurationType> {
    @Nonnull
    public static DeployToServerConfigurationType<?> getConfigurationType(ServerType<?> serverType) {
        return (DeployToServerConfigurationType<?>) Application.get().getExtensionPoint(ConfigurationType.class).findFirstSafe(configurationType -> {
            if (configurationType instanceof DeployToServerConfigurationType deploy) {
                return deploy.getServerType() == serverType;
            }
            return false;
        });
    }

    @Override
    public void extend(@Nonnull ComponentManager componentManager, @Nonnull Consumer<ConfigurationType> consumer) {
        componentManager.getExtensionPoint(ServerType.class).forEachExtensionSafe(serverType -> {
            consumer.accept(new DeployToServerConfigurationType(serverType));
        });
    }

    @Override
    public boolean hasAnyExtensions(ComponentManager componentManager) {
        return componentManager.getExtensionPoint(ServerType.class).hasAnyExtensions();
    }

    @Nonnull
    @Override
    public Class<ConfigurationType> getExtensionClass() {
        return ConfigurationType.class;
    }
}
