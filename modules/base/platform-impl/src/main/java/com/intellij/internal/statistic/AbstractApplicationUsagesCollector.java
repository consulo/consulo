/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.internal.statistic;

import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.persistence.ApplicationStatisticsPersistence;
import com.intellij.internal.statistic.persistence.ApplicationStatisticsPersistenceComponent;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractApplicationUsagesCollector extends UsagesCollector {
  private static final Logger LOG = Logger.getInstance(AbstractApplicationUsagesCollector.class);

  public void persistProjectUsages(@Nonnull Project project) {
    try {
      final Set<UsageDescriptor> projectUsages = getProjectUsages(project);
      persistProjectUsages(project, projectUsages);
    }
    catch (CollectUsagesException e) {
      LOG.info(e);
    }
  }

  public void persistProjectUsages(@Nonnull Project project, @Nonnull Set<UsageDescriptor> usages) {
    persistProjectUsages(project, usages, ApplicationStatisticsPersistenceComponent.getInstance());
  }

  public void persistProjectUsages(@Nonnull Project project, @Nonnull Set<UsageDescriptor> usages, @Nonnull ApplicationStatisticsPersistence persistence) {
    persistence.persistUsages(getGroupId(), project, usages);
  }

  @Nonnull
  public Set<UsageDescriptor> getApplicationUsages() {
    return getApplicationUsages(ApplicationStatisticsPersistenceComponent.getInstance());
  }

  @Nonnull
  public Set<UsageDescriptor> getApplicationUsages(@Nonnull final ApplicationStatisticsPersistence persistence) {
    final Map<String, Integer> result = new HashMap<String, Integer>();

    for (Set<UsageDescriptor> usageDescriptors : persistence.getApplicationData(getGroupId()).values()) {
      for (UsageDescriptor usageDescriptor : usageDescriptors) {
        final String key = usageDescriptor.getKey();
        final Integer count = result.get(key);
        result.put(key, count == null ? 1 : count.intValue() + 1);
      }
    }

    return ContainerUtil.map2Set(result.entrySet(), entry -> new UsageDescriptor(entry.getKey(), entry.getValue()));
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
    if (project != null) {
      final Set<UsageDescriptor> projectUsages = getProjectUsages(project);
      persistProjectUsages(project, projectUsages);
    }

    return getApplicationUsages();
  }

  @Nonnull
  public abstract Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) throws CollectUsagesException;
}
