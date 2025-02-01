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
import consulo.externalService.impl.internal.plugin.ui.PluginSorter;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginSortFilterGroup extends DefaultActionGroup implements DumbAware {
    public PluginSortFilterGroup() {
        super("Sort & Filters", true);
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.generalFilter();
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        PluginTab tab = e.getRequiredData(PluginTab.KEY);

        Presentation presentation = e.getPresentation();
        if (tab.getSorter() != PluginSorter.DEFAULT_SORTER || tab.getPluginList().getTag() != null) {
            presentation.setIcon(ImageEffects.layered(getTemplateIcon(), PlatformIconGroup.greenbadge()));
        } else {
            presentation.setIcon(getTemplateIcon());
        }
    }
}
