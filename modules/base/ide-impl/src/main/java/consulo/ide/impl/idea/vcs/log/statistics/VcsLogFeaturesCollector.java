/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.statistics;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.AbstractApplicationUsagesCollector;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.ide.impl.idea.internal.statistic.StatisticsUtilKt;
import consulo.externalService.statistic.ConvertUsagesUtil;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.impl.VcsProjectLog;
import consulo.versionControlSystem.log.VcsLogHighlighterFactory;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

import static consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties.*;
import static consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl.LOG_HIGHLIGHTER_FACTORY_EP;

@ExtensionImpl
public class VcsLogFeaturesCollector extends AbstractApplicationUsagesCollector {
  @Nonnull
  @Override
  public Set<UsageDescriptor> getProjectUsages(@Nonnull Project project) throws CollectUsagesException {
    VcsProjectLog projectLog = VcsProjectLog.getInstance(project);
    if (projectLog != null) {
      VcsLogUiImpl ui = projectLog.getMainLogUi();
      if (ui != null) {
        MainVcsLogUiProperties properties = ui.getProperties();

        Set<UsageDescriptor> usages = ContainerUtil.newHashSet();
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.details", properties.get(SHOW_DETAILS)));
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.long.edges", properties.get(SHOW_LONG_EDGES)));

        PermanentGraph.SortType sortType = properties.get(BEK_SORT_TYPE);
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.sort.linear.bek", sortType.equals(PermanentGraph.SortType.LinearBek)));
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.sort.bek", sortType.equals(PermanentGraph.SortType.Bek)));
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.sort.normal", sortType.equals(PermanentGraph.SortType.Normal)));

        if (ui.isMultipleRoots()) {
          usages.add(StatisticsUtilKt.getBooleanUsage("ui.roots", properties.get(SHOW_ROOT_NAMES)));
        }

        usages.add(StatisticsUtilKt.getBooleanUsage("ui.labels.compact", properties.get(COMPACT_REFERENCES_VIEW)));
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.labels.showTagNames", properties.get(SHOW_TAG_NAMES)));

        usages.add(StatisticsUtilKt.getBooleanUsage("ui.textFilter.regex", properties.get(TEXT_FILTER_REGEX)));
        usages.add(StatisticsUtilKt.getBooleanUsage("ui.textFilter.matchCase", properties.get(TEXT_FILTER_MATCH_CASE)));

        for (VcsLogHighlighterFactory factory : LOG_HIGHLIGHTER_FACTORY_EP.getExtensionList(project)) {
          if (factory.showMenuItem()) {
            VcsLogHighlighterProperty property = VcsLogHighlighterProperty.get(factory.getId());
            usages.add(StatisticsUtilKt.getBooleanUsage("ui.highlighter." + ConvertUsagesUtil.ensureProperKey(factory.getId()),
                                                        properties.exists(property) && properties.get(property)));
          }
        }

        return usages;
      }
    }
    return Collections.emptySet();
  }

  @Nonnull
  @Override
  public String getGroupId() {
    return "consulo.platform.base:vcs.log.ui.settings";
  }
}
