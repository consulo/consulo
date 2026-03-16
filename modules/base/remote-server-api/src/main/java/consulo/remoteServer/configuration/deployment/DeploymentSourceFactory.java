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
package consulo.remoteServer.configuration.deployment;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.compiler.artifact.Artifact;
import consulo.module.Module;


/**
 * @author VISTALL
 * @since 22/01/2023
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface DeploymentSourceFactory {
  
  @RequiredReadAction
  ModuleDeploymentSource createModuleDeploymentSource(Module module);

  
  @RequiredReadAction
  ModuleDeploymentSource createModuleDeploymentSource(String moduleName);

  
  @RequiredReadAction
  ArtifactDeploymentSource createArtifactDeploymentSource(Artifact artifact);

  
  @RequiredReadAction
  ArtifactDeploymentSource createArtifactDeploymentSource(String artifactName);
}
