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
package consulo.remoteServer.configuration.deployment;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.project.Project;
import consulo.remoteServer.configuration.ServerConfiguration;

/**
 * @author VISTALL
 * @since 2024-12-03
 */
public abstract class DeployToServerRunConfiguration<S extends ServerConfiguration, D extends DeploymentConfiguration> extends RunConfigurationBase {
    public DeployToServerRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }
}
