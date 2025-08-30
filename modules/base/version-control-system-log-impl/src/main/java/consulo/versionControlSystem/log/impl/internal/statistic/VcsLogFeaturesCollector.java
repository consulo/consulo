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
package consulo.versionControlSystem.log.impl.internal.statistic;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.statistic.*;
import consulo.project.Project;
import consulo.versionControlSystem.log.VcsLogHighlighterFactory;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLog;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties.*;

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

                Set<UsageDescriptor> usages = new HashSet<>();
                usages.add(StatisticsUtil.getBooleanUsage("ui.details", properties.get(SHOW_DETAILS)));
                usages.add(StatisticsUtil.getBooleanUsage("ui.long.edges", properties.get(SHOW_LONG_EDGES)));

                PermanentGraph.SortType sortType = properties.get(BEK_SORT_TYPE);
                usages.add(StatisticsUtil.getBooleanUsage("ui.sort.linear.bek", sortType.equals(PermanentGraph.SortType.LinearBek)));
                usages.add(StatisticsUtil.getBooleanUsage("ui.sort.bek", sortType.equals(PermanentGraph.SortType.Bek)));
                usages.add(StatisticsUtil.getBooleanUsage("ui.sort.normal", sortType.equals(PermanentGraph.SortType.Normal)));

                if (ui.isMultipleRoots()) {
                    usages.add(StatisticsUtil.getBooleanUsage("ui.roots", properties.get(SHOW_ROOT_NAMES)));
                }

                usages.add(StatisticsUtil.getBooleanUsage("ui.labels.compact", properties.get(COMPACT_REFERENCES_VIEW)));
                usages.add(StatisticsUtil.getBooleanUsage("ui.labels.showTagNames", properties.get(SHOW_TAG_NAMES)));

                usages.add(StatisticsUtil.getBooleanUsage("ui.textFilter.regex", properties.get(TEXT_FILTER_REGEX)));
                usages.add(StatisticsUtil.getBooleanUsage("ui.textFilter.matchCase", properties.get(TEXT_FILTER_MATCH_CASE)));

                project.getExtensionPoint(VcsLogHighlighterFactory.class).forEach(factory -> {
                    if (factory.showMenuItem()) {
                        VcsLogHighlighterProperty property = VcsLogHighlighterProperty.get(factory.getId());
                        usages.add(StatisticsUtil.getBooleanUsage(
                            "ui.highlighter." + ConvertUsagesUtil.ensureProperKey(factory.getId()),
                            properties.exists(property) && properties.get(property)
                        ));
                    }
                });

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
