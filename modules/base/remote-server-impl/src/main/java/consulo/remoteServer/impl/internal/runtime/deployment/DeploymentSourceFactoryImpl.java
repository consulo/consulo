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
package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.module.Module;
import consulo.module.ModulePointerManager;
import consulo.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceFactory;
import consulo.remoteServer.configuration.deployment.ModuleDeploymentSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
@Singleton
@ServiceImpl
public class DeploymentSourceFactoryImpl implements DeploymentSourceFactory {
  private final ModulePointerManager myModulePointerManager;
  private final ArtifactPointerManager myArtifactPointerManager;

  @Inject
  public DeploymentSourceFactoryImpl(ModulePointerManager modulePointerManager, ArtifactPointerManager artifactPointerManager) {
    myModulePointerManager = modulePointerManager;
    myArtifactPointerManager = artifactPointerManager;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public ModuleDeploymentSource createModuleDeploymentSource(@Nonnull Module module) {
    return new ModuleDeploymentSourceImpl(myModulePointerManager.create(module));
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public ModuleDeploymentSource createModuleDeploymentSource(@Nonnull String moduleName) {
    return new ModuleDeploymentSourceImpl(myModulePointerManager.create(moduleName));
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public ArtifactDeploymentSource createArtifactDeploymentSource(@Nonnull Artifact artifact) {
    return new ArtifactDeploymentSourceImpl(myArtifactPointerManager.create(artifact));
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public ArtifactDeploymentSource createArtifactDeploymentSource(@Nonnull String artifactName) {
    return new ArtifactDeploymentSourceImpl(myArtifactPointerManager.create(artifactName));
  }
}
