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
package consulo.ide.impl.idea.ide.plugins.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.ide.plugins.ui.PluginSorter;
import consulo.ide.impl.idea.ide.plugins.ui.PluginTab;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginSorterAction extends ToggleAction implements DumbAware {
    @Nonnull
    private final PluginSorter myPluginSorter;

    public PluginSorterAction(@Nonnull PluginSorter pluginSorter) {
        super(pluginSorter.getSortName());
        myPluginSorter = pluginSorter;
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        PluginTab tab = e.getRequiredData(PluginTab.KEY);
        return tab.getSorter() == myPluginSorter;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        PluginTab tab = e.getRequiredData(PluginTab.KEY);

        if (state) {
            tab.reSort(myPluginSorter);
        } else {
            tab.reSort(PluginSorter.DEFAULT_SORTER);
        }
    }
}
