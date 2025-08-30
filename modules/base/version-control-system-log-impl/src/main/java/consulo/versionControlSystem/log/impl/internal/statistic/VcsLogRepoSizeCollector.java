/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.statistic;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.AbstractApplicationUsagesCollector;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.StatisticsUtil;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLog;
import consulo.versionControlSystem.log.impl.internal.data.DataPack;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

@ExtensionImpl
public class VcsLogRepoSizeCollector extends AbstractApplicationUsagesCollector {
  @Nonnull
  @Override
  public Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) throws CollectUsagesException {
    VcsProjectLog projectLog = VcsProjectLog.getInstance(project);
    VcsLogDataImpl logData = projectLog.getDataManager();
    if (logData != null) {
      DataPack dataPack = logData.getDataPack();
      if (dataPack.isFull()) {
        PermanentGraph<Integer> permanentGraph = dataPack.getPermanentGraph();
        MultiMap<VcsKey, VirtualFile> groupedRoots = groupRootsByVcs(dataPack.getLogProviders());

        Set<UsageDescriptor> usages = new HashSet<>();
        usages.add(StatisticsUtil.getCountingUsage("data.commit.count", permanentGraph.getAllCommits().size(), asList(0, 1, 100, 1000, 10 * 1000, 100 * 1000, 500 * 1000)));
        for (VcsKey vcs : groupedRoots.keySet()) {
          usages.add(StatisticsUtil.getCountingUsage("data." + vcs.getName().toLowerCase() + ".root.count", groupedRoots.get(vcs).size(), asList(0, 1, 2, 5, 8, 15, 30, 50, 100, 500, 1000)));
        }
        return usages;
      }
    }
    return Collections.emptySet();
  }

  @Nonnull
  private static MultiMap<VcsKey, VirtualFile> groupRootsByVcs(@Nonnull Map<VirtualFile, VcsLogProvider> providers) {
    MultiMap<VcsKey, VirtualFile> result = MultiMap.create();
    for (Map.Entry<VirtualFile, VcsLogProvider> entry : providers.entrySet()) {
      VirtualFile root = entry.getKey();
      VcsKey vcs = entry.getValue().getSupportedVcs();
      result.putValue(vcs, root);
    }
    return result;
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:vcs.log";
  }
}
