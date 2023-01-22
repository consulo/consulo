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
package consulo.ide.impl.idea.remoteServer.impl.configuration.deploySource;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointerUtil;
import consulo.ide.impl.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import consulo.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ide.impl.idea.remoteServer.impl.configuration.deploySource.impl.ArtifactDeploymentSourceImpl;
import org.jdom.Element;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author nik
 */
@ExtensionImpl
public class ArtifactDeploymentSourceType extends DeploymentSourceType<ArtifactDeploymentSource> {
  private static final String NAME_ATTRIBUTE = "name";

  public ArtifactDeploymentSourceType() {
    super("artifact");
  }

  @Nonnull
  @Override
  public ArtifactDeploymentSource load(@Nonnull Element tag, @Nonnull Project project) {
    return new ArtifactDeploymentSourceImpl(ArtifactPointerUtil.getPointerManager(project).create(tag.getAttributeValue(NAME_ATTRIBUTE)));
  }

  @Override
  public void save(@Nonnull ArtifactDeploymentSource source, @Nonnull Element tag) {
    tag.setAttribute(NAME_ATTRIBUTE, source.getArtifactPointer().getName());
  }

  @Override
  public void setBuildBeforeRunTask(@Nonnull RunConfiguration configuration,
                                    @Nonnull ArtifactDeploymentSource source) {
    Artifact artifact = source.getArtifact();
    if (artifact != null) {
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(configuration.getProject(), configuration, artifact);
    }
  }

  @Override
  public void updateBuildBeforeRunOption(@Nonnull JComponent runConfigurationEditorComponent, @Nonnull Project project,
                                         @Nonnull ArtifactDeploymentSource source, boolean select) {
    Artifact artifact = source.getArtifact();
    if (artifact != null) {
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRunOption(runConfigurationEditorComponent, project, artifact, select);
    }
  }
}
