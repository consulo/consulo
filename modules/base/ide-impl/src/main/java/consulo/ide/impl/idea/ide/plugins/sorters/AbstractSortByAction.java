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
package consulo.ide.impl.idea.ide.plugins.sorters;

import consulo.ide.impl.idea.ide.plugins.PluginTable;
import consulo.ide.impl.idea.ide.plugins.PluginTableModel;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.container.plugin.PluginDescriptor;

/**
 * @author Konstantin Bulenkov
 */
public abstract class AbstractSortByAction extends ToggleAction {
  protected final PluginTable myTable;
  protected final PluginTableModel myModel;

  public AbstractSortByAction(String name, PluginTable table, PluginTableModel model) {
    super(name, name, null);
    myTable = table;
    myModel = model;
  }

  public abstract boolean isSelected();

  protected abstract void setSelected(boolean state);

  @Override
  public final boolean isSelected(AnActionEvent e) {
    return isSelected();
  }

  @Override
  public final void setSelected(AnActionEvent e, boolean state) {
    PluginDescriptor selected = myTable.getSelectedObject();
    setSelected(state);
    myModel.sort();
    if (selected != null) {
      myTable.select(selected.getPluginId());
    }
  }
}
