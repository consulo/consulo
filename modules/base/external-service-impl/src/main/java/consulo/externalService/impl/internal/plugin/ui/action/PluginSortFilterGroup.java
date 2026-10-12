/*
 * Copyright 2013-2024 consulo.io
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
package consulo.externalService.impl.internal.plugin.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.externalService.impl.internal.plugin.ui.PluginFilterHost;
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginSortFilterGroup extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
    public PluginSortFilterGroup() {
        super("Sort & Filters", true);
    }

    public static PluginSortFilterGroup create(List<PluginSorter> sorters) {
        PluginSortActionGroup sortBy = new PluginSortActionGroup();
        for (PluginSorter sorter : sorters) {
            sortBy.add(new PluginSorterAction(sorter));
        }

        PluginSortFilterGroup group = new PluginSortFilterGroup();
        group.add(sortBy);
        group.add(new PluginTagFilterGroup());
        return group;
    }

    @Override
    protected @Nullable Image getTemplateIcon() {
        return PlatformIconGroup.generalFilter();
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

    @Override
    public void update(AnActionEvent e) {
        PluginFilterHost host = e.getRequiredData(PluginFilterHost.KEY);

        Presentation presentation = e.getPresentation();
        if (host.getSorter() != PluginSorter.DEFAULT_SORTER || host.getTagFilter() != null) {
            presentation.setIcon(ImageEffects.layered(getTemplateIcon(), PlatformIconGroup.greenbadge()));
        } else {
            presentation.setIcon(getTemplateIcon());
        }
    }
}
