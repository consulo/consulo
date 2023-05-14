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
package consulo.ide.impl.packaging.impl.run;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.execution.BuildArtifactsBeforeRunTaskHelper;
import consulo.dataContext.DataContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
@Singleton
@ServiceImpl
public class BuildArtifactsBeforeRunTaskHelperImpl implements BuildArtifactsBeforeRunTaskHelper {
  private final Project myProject;

  @Inject
  public BuildArtifactsBeforeRunTaskHelperImpl(Project project) {
    myProject = project;
  }

  @Override
  public void setBuildArtifactBeforeRunOption(@Nonnull DataContext dataContext, @Nonnull Artifact artifact, boolean enable) {
    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRunOption(dataContext, myProject, artifact, enable);
  }

  @Override
  public void setBuildArtifactBeforeRun(@Nonnull RunConfiguration configuration, @Nonnull Artifact artifact) {
    BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(myProject, configuration, artifact);
  }
}
