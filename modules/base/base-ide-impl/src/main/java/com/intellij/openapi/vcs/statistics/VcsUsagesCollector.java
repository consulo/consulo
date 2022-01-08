/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.statistics;

import com.intellij.internal.statistic.AbstractApplicationUsagesCollector;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.Set;

public class VcsUsagesCollector extends AbstractApplicationUsagesCollector {
  @Override
  @Nonnull
  public String getGroupId() {
    return "consulo.platform.base:vcs";
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) {
    return ContainerUtil.map2Set(ProjectLevelVcsManager.getInstance(project).getAllActiveVcss(), vcs -> new UsageDescriptor(vcs.getName(), 1));
  }
}

