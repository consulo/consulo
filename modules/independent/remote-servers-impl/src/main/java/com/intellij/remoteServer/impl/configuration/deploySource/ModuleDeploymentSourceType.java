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
package com.intellij.remoteServer.impl.configuration.deploySource;

import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deploySource.impl.ModuleDeploymentSourceImpl;
import org.jdom.Element;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class ModuleDeploymentSourceType extends DeploymentSourceType<ModuleDeploymentSource> {
  private static final String NAME_ATTRIBUTE = "name";

  public ModuleDeploymentSourceType() {
    super("module");
  }

  @Nonnull
  @Override
  public ModuleDeploymentSource load(@Nonnull Element tag, @Nonnull Project project) {
    return new ModuleDeploymentSourceImpl(ModuleUtilCore.createPointer(project, tag.getAttributeValue(NAME_ATTRIBUTE)));
  }

  @Override
  public void save(@Nonnull ModuleDeploymentSource source, @Nonnull Element tag) {
    tag.setAttribute(NAME_ATTRIBUTE, source.getModulePointer().getName());
  }
}
