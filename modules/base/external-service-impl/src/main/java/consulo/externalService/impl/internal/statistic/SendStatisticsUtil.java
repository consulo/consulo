/*
 * Copyright 2013-2020 consulo.io
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
package consulo.externalService.impl.internal.statistic;

import consulo.externalService.impl.internal.PermanentInstallationID;
import consulo.externalService.impl.internal.repository.api.StatisticsBean;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
public class SendStatisticsUtil {
  private static final Logger LOG = Logger.getInstance(SendStatisticsUtil.class);

  @Nonnull
  public static StatisticsBean getBean(UsageStatisticsPersistenceComponent component) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

    final Map<String, Set<PatchedUsage>> map = getPatchedUsages(Collections.emptySet(), openProjects);

    List<StatisticsBean.UsageGroup> groups = new ArrayList<>();
    for (Map.Entry<String, Set<PatchedUsage>> entry : map.entrySet()) {
      StatisticsBean.UsageGroup usageGroup = new StatisticsBean.UsageGroup();

      List<StatisticsBean.UsageGroupValue> values = new ArrayList<>();
      for (PatchedUsage patchedUsage : entry.getValue()) {
        values.add(new StatisticsBean.UsageGroupValue(patchedUsage.getKey(), patchedUsage.getValue()));
      }

      usageGroup.id = entry.getKey();
      usageGroup.values = ContainerUtil.toArray(values, StatisticsBean.UsageGroupValue[]::new);

      groups.add(usageGroup);
    }

    StatisticsBean bean = new StatisticsBean();
    bean.key = component.getSecretKey();
    bean.installationID = PermanentInstallationID.get();
    bean.groups = ContainerUtil.toArray(groups, StatisticsBean.UsageGroup[]::new);
    return bean;
  }

  @Nonnull
  private static Map<String, Set<PatchedUsage>> getPatchedUsages(@Nonnull Set<String> disabledGroups, @Nonnull Project[] projects) {
    Map<String, Set<PatchedUsage>> usages = new LinkedHashMap<>();

    for (Project project : projects) {
      final Map<String, Set<UsageDescriptor>> allUsages = StatisticsUploadAssistant.getAllUsages(project, disabledGroups);

      usages.putAll(StatisticsUploadAssistant.getPatchedUsages(allUsages, Collections.emptyMap()));
    }
    return usages;
  }
}
