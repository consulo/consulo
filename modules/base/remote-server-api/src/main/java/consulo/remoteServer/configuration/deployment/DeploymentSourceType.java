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
package consulo.remoteServer.configuration.deployment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataContext;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import org.jdom.Element;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DeploymentSourceType<S extends DeploymentSource> {
  public static final ExtensionPointName<DeploymentSourceType> EP_NAME = ExtensionPointName.create(DeploymentSourceType.class);
  private final String myId;

  protected DeploymentSourceType(@Nonnull String id) {
    myId = id;
  }

  public final String getId() {
    return myId;
  }

  @Nonnull
  public abstract S load(@Nonnull Element tag, @Nonnull Project project);

  public abstract void save(@Nonnull S s, @Nonnull Element tag);


  public void setBuildBeforeRunTask(@Nonnull RunConfiguration configuration, @Nonnull S source) {
  }

  public void updateBuildBeforeRunOption(@Nonnull DataContext dataContext, @Nonnull Project project, @Nonnull S source, boolean select) {
  }
}
