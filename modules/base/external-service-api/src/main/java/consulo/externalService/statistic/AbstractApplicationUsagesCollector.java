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
package consulo.externalService.statistic;

import consulo.externalService.internal.ApplicationStatisticsPersistenceComponent;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractApplicationUsagesCollector extends UsagesCollector {
  private static final Logger LOG = Logger.getInstance(AbstractApplicationUsagesCollector.class);

  public void persistProjectUsages(@Nonnull Project project) {
    try {
      Set<UsageDescriptor> projectUsages = getProjectUsages(project);
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
  public Set<UsageDescriptor> getApplicationUsages(@Nonnull ApplicationStatisticsPersistence persistence) {
    Map<String, Integer> result = new HashMap<String, Integer>();

    for (Set<UsageDescriptor> usageDescriptors : persistence.getApplicationData(getGroupId()).values()) {
      for (UsageDescriptor usageDescriptor : usageDescriptors) {
        String key = usageDescriptor.getKey();
        Integer count = result.get(key);
        result.put(key, count == null ? 1 : count.intValue() + 1);
      }
    }

    return ContainerUtil.map2Set(result.entrySet(), entry -> new UsageDescriptor(entry.getKey(), entry.getValue()));
  }

  @Override
  @Nonnull
  public Set<UsageDescriptor> getUsages(@Nullable Project project) throws CollectUsagesException {
    if (project != null) {
      Set<UsageDescriptor> projectUsages = getProjectUsages(project);
      persistProjectUsages(project, projectUsages);
    }

    return getApplicationUsages();
  }

  @Nonnull
  public abstract Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) throws CollectUsagesException;
}
