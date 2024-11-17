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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.dumb.DumbAware;
import consulo.container.plugin.PluginDescriptor;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class SortByAction extends ToggleAction implements DumbAware {
    protected final PluginTable myTable;
    protected final PluginTableModel myModel;
    protected final SortBy mySortBy;

    public SortByAction(SortBy sortBy, PluginTable table, PluginTableModel model) {
        super(sortBy.getTitle(), sortBy.getTitle(), null);
        mySortBy = sortBy;
        myTable = table;
        myModel = model;
    }

    public boolean isSelected() {
        return myModel.getSortBy() == mySortBy;
    }

    protected void setSelected(boolean state) {
        myModel.setSortBy(state ? mySortBy : SortBy.NAME);
    }

    @Override
    public final boolean isSelected(@Nonnull AnActionEvent e) {
        return isSelected();
    }

    @Override
    public final void setSelected(@Nonnull AnActionEvent e, boolean state) {
        PluginDescriptor selected = myTable.getSelectedObject();
        setSelected(state);
        myModel.sort();
        if (selected != null) {
            myTable.select(selected.getPluginId());
        }
    }
}
