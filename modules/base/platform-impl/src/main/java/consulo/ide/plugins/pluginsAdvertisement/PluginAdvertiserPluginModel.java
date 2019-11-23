/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.plugins.pluginsAdvertisement;

import com.intellij.ide.plugins.PluginManagerColumnInfo;
import com.intellij.ide.plugins.PluginTableModel;
import com.intellij.ide.plugins.PluginsTableRenderer;
import consulo.container.plugin.PluginId;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import consulo.container.plugin.PluginDescriptor;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22-Jun-17
 */
public class PluginAdvertiserPluginModel extends PluginTableModel {
  private static class MyPluginManagerColumnInfo extends PluginManagerColumnInfo {
    public MyPluginManagerColumnInfo(PluginTableModel model) {
      super(PluginManagerColumnInfo.COLUMN_NAME, model);
    }

    @Override
    public TableCellRenderer getRenderer(final PluginDescriptor pluginDescriptor) {
      return new PluginsTableRenderer(pluginDescriptor, false);
    }

    @Override
    protected boolean isSortByName() {
      return true;
    }
  }

  private static class DownloadCheckboxColumnInfo extends ColumnInfo<PluginDescriptor, Boolean> {
    private final Map<PluginId, Boolean> myDownloadMap;

    public DownloadCheckboxColumnInfo(Map<PluginId, Boolean> downloadMap) {
      super("");
      myDownloadMap = downloadMap;
    }

    @Override
    public Boolean valueOf(PluginDescriptor ideaPluginDescriptor) {
      return myDownloadMap.get(ideaPluginDescriptor.getPluginId());
    }

    @Override
    public boolean isCellEditable(final PluginDescriptor ideaPluginDescriptor) {
      return true;
    }

    @Override
    public Class getColumnClass() {
      return Boolean.class;
    }

    @Override
    public TableCellEditor getEditor(final PluginDescriptor o) {
      return new BooleanTableCellEditor();
    }

    @Override
    public TableCellRenderer getRenderer(final PluginDescriptor ideaPluginDescriptor) {
      return new BooleanTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
          return super.getTableCellRendererComponent(table, value == null ? Boolean.TRUE : value, isSelected, hasFocus, row, column);
        }
      };
    }

    @Override
    public void setValue(final PluginDescriptor ideaPluginDescriptor, Boolean value) {
      myDownloadMap.put(ideaPluginDescriptor.getPluginId(), value);
    }

    @Override
    public int getWidth(JTable table) {
      return new JCheckBox().getPreferredSize().width;
    }
  }

  public PluginAdvertiserPluginModel(Map<PluginId, Boolean> downloadState, @Nonnull List<PluginDescriptor> pluginDescriptors) {
    columns = new ColumnInfo[]{new MyPluginManagerColumnInfo(this), new DownloadCheckboxColumnInfo(downloadState)};
    view = new ArrayList<>();
    updatePluginsList(pluginDescriptors);
  }

  @Override
  public void updatePluginsList(List<PluginDescriptor> list) {
    view.clear();
    filtered.clear();
    view.addAll(list);
    fireTableDataChanged();
  }

  @Override
  public int getNameColumn() {
    return 0;
  }

  @Override
  public boolean isPluginDescriptorAccepted(PluginDescriptor descriptor) {
    return false;
  }
}
