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
package com.intellij.ide.plugins;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.plugins.InstalledPluginsState;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author stathik
 * @since 3:51:58 PM Dec 26, 2003
 */
public class InstalledPluginsTableModel extends PluginTableModel {
  private final Map<PluginId, Boolean> myEnabled = new HashMap<>();
  private final Map<PluginId, Set<PluginId>> myDependentToRequiredListMap = new HashMap<>();

  private static final String ENABLED_DISABLED = "All plugins";
  private static final String ENABLED = "Enabled plugins";
  private static final String DISABLED = "Disabled plugins";
  public static final String[] ENABLED_VALUES = new String[]{ENABLED_DISABLED, ENABLED, DISABLED};
  private String myEnabledFilter = ENABLED_DISABLED;

  public InstalledPluginsTableModel() {
    super.columns = new ColumnInfo[]{new MyPluginManagerColumnInfo(), new EnabledPluginInfo()};
    view = new ArrayList<>(consulo.container.plugin.PluginManager.getPlugins());
    view.addAll(InstalledPluginsState.getInstance().getAllPlugins());
    reset(view);

    for (Iterator<PluginDescriptor> iterator = view.iterator(); iterator.hasNext(); ) {
      final PluginId pluginId = iterator.next().getPluginId();
      if (PluginManagerCore.isSystemPlugin(pluginId)) {
        iterator.remove();
      }
    }

    setSortKey(new RowSorter.SortKey(getNameColumn(), SortOrder.ASCENDING));
  }

  public boolean appendOrUpdateDescriptor(PluginDescriptor descriptor) {
    final PluginId descrId = descriptor.getPluginId();
    final PluginDescriptor installed = PluginManager.getPlugin(descrId);
    InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

    if (installed != null) {
      pluginsState.updateExistingPlugin(descriptor, installed);
      return true;
    }
    else if (!pluginsState.getAllPlugins().contains(descriptor)) {
      pluginsState.getAllPlugins().add(descriptor);
      view.add(descriptor);
      setEnabled(descriptor, true);
      fireTableDataChanged();
      return true;
    }
    return false;
  }

  public static int getCheckboxColumn() {
    return 1;
  }

  @Override
  public int getNameColumn() {
    return 0;
  }

  private void reset(final List<PluginDescriptor> list) {
    for (PluginDescriptor ideaPluginDescriptor : list) {
      setEnabled(ideaPluginDescriptor);
    }

    updatePluginDependencies();
  }


  private void setEnabled(PluginDescriptor ideaPluginDescriptor) {
    setEnabled(ideaPluginDescriptor, ideaPluginDescriptor.isEnabled());
  }

  private void setEnabled(PluginDescriptor ideaPluginDescriptor, final boolean enabled) {
    final Collection<String> disabledPlugins = PluginManager.getDisabledPlugins();
    final PluginId pluginId = ideaPluginDescriptor.getPluginId();
    if (!enabled && !disabledPlugins.contains(pluginId.toString())) {
      myEnabled.put(pluginId, null);
    }
    else {
      myEnabled.put(pluginId, enabled);
    }
  }

  public Map<PluginId, Set<PluginId>> getDependentToRequiredListMap() {
    return myDependentToRequiredListMap;
  }

  public boolean isLoaded(PluginId pluginId) {
    return myEnabled.get(pluginId) != null;
  }

  public boolean hasProblematicDependencies(PluginId pluginId) {
    final Set<PluginId> ids = myDependentToRequiredListMap.get(pluginId);
    return ids != null && !ids.isEmpty();
  }

  @Nullable
  public Set<PluginId> getRequiredPlugins(PluginId pluginId) {
    return myDependentToRequiredListMap.get(pluginId);
  }

  protected void updatePluginDependencies() {
    myDependentToRequiredListMap.clear();

    InstalledPluginsState pluginsState = InstalledPluginsState.getInstance();

    Set<PluginId> updatedPlugins = pluginsState.getUpdatedPlugins();

    final int rowCount = getRowCount();
    for (int i = 0; i < rowCount; i++) {
      final PluginDescriptor descriptor = getObjectAt(i);
      final PluginId pluginId = descriptor.getPluginId();
      myDependentToRequiredListMap.remove(pluginId);
      if (descriptor.isDeleted()) continue;
      final Boolean enabled = myEnabled.get(pluginId);
      if (enabled == null || enabled) {
        consulo.container.plugin.PluginManager.checkDependants(descriptor, PluginManager::getPlugin, dependantPluginId -> {
          final Boolean enabled1 = myEnabled.get(dependantPluginId);
          if ((enabled1 == null && !updatedPlugins.contains(dependantPluginId)) || (enabled1 != null && !enabled1)) {
            Set<PluginId> required = myDependentToRequiredListMap.get(pluginId);
            if (required == null) {
              required = new HashSet<>();
              myDependentToRequiredListMap.put(pluginId, required);
            }

            required.add(dependantPluginId);
            //return false;
          }

          return true;
        });
        if (enabled == null && !myDependentToRequiredListMap.containsKey(pluginId) && !PluginManager.isIncompatible(descriptor)) {
          myEnabled.put(pluginId, true);
        }
      }
    }
  }

  @Override
  public void updatePluginsList(List<PluginDescriptor> list) {
    //  For each downloadable plugin we need to know whether its counterpart
    //  is already installed, and if yes compare the difference in versions:
    //  availability of newer versions will be indicated separately.
    for (PluginDescriptor descr : list) {
      PluginId descrId = descr.getPluginId();
      PluginDescriptor existing = PluginManager.getPlugin(descrId);
      if (existing != null) {
        if (descr instanceof PluginNode) {
          InstalledPluginsState.getInstance().updateExistingPluginInfo(descr, existing);
        }
        else {
          view.add(descr);
          setEnabled(descr);
        }
      }
    }
    for (PluginDescriptor descriptor : InstalledPluginsState.getInstance().getAllPlugins()) {
      if (!view.contains(descriptor)) {
        view.add(descriptor);
      }
    }
    fireTableDataChanged();
  }

  @Override
  protected ArrayList<PluginDescriptor> toProcess() {
    ArrayList<PluginDescriptor> toProcess = super.toProcess();
    for (PluginDescriptor descriptor : InstalledPluginsState.getInstance().getAllPlugins()) {
      if (!toProcess.contains(descriptor)) {
        toProcess.add(descriptor);
      }
    }
    return toProcess;
  }

  @Override
  public void filter(final List<PluginDescriptor> filtered) {
    view.clear();
    for (PluginDescriptor descriptor : filtered) {
      view.add(descriptor);
    }

    super.filter(filtered);
  }

  public void enableRows(PluginDescriptor[] ideaPluginDescriptors, Boolean value) {
    for (PluginDescriptor ideaPluginDescriptor : ideaPluginDescriptors) {
      final PluginId currentPluginId = ideaPluginDescriptor.getPluginId();
      final Boolean enabled = myEnabled.get(currentPluginId) == null ? Boolean.FALSE : value;
      myEnabled.put(currentPluginId, enabled);
    }
    updatePluginDependencies();
    warnAboutMissedDependencies(value, ideaPluginDescriptors);
    hideNotApplicablePlugins(value, ideaPluginDescriptors);
  }

  private void hideNotApplicablePlugins(Boolean value, final PluginDescriptor... ideaPluginDescriptors) {
    if (!value && ENABLED.equals(myEnabledFilter) || (value && DISABLED.equals(myEnabledFilter))) {
      SwingUtilities.invokeLater(() -> {
        for (PluginDescriptor ideaPluginDescriptor : ideaPluginDescriptors) {
          view.remove(ideaPluginDescriptor);
          filtered.add(ideaPluginDescriptor);
        }
        fireTableDataChanged();
      });
    }
  }


  public static boolean hasNewerVersion(PluginId descr) {
    return InstalledPluginsState.getInstance().hasNewerVersion(descr);
  }

  public static boolean wasUpdated(PluginId descr) {
    return InstalledPluginsState.getInstance().wasUpdated(descr);
  }

  public boolean isEnabled(final PluginId pluginId) {
    final Boolean enabled = myEnabled.get(pluginId);
    return enabled != null && enabled;
  }

  public boolean isDisabled(final PluginId pluginId) {
    final Boolean enabled = myEnabled.get(pluginId);
    return enabled != null && !enabled;
  }

  public Map<PluginId, Boolean> getEnabledMap() {
    return myEnabled;
  }

  public String getEnabledFilter() {
    return myEnabledFilter;
  }

  public void setEnabledFilter(String enabledFilter, String filter) {
    myEnabledFilter = enabledFilter;
    filter(filter);
  }

  @Override
  public boolean isPluginDescriptorAccepted(PluginDescriptor descriptor) {
    if (!myEnabledFilter.equals(ENABLED_DISABLED)) {
      final boolean enabled = isEnabled(descriptor.getPluginId());
      if (enabled && myEnabledFilter.equals(DISABLED)) return false;
      if (!enabled && myEnabledFilter.equals(ENABLED)) return false;
    }
    return true;
  }

  private class EnabledPluginInfo extends ColumnInfo<PluginDescriptor, Boolean> {

    public EnabledPluginInfo() {
      super("");
    }

    @Override
    public Boolean valueOf(PluginDescriptor ideaPluginDescriptor) {
      return myEnabled.get(ideaPluginDescriptor.getPluginId());
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
      final PluginId currentPluginId = ideaPluginDescriptor.getPluginId();
      final Boolean enabled = myEnabled.get(currentPluginId) == null ? Boolean.FALSE : value;
      myEnabled.put(currentPluginId, enabled);
      updatePluginDependencies();
      warnAboutMissedDependencies(enabled, ideaPluginDescriptor);
      hideNotApplicablePlugins(value, ideaPluginDescriptor);
    }

    @Override
    public Comparator<PluginDescriptor> getComparator() {
      return (o1, o2) -> {
        final Boolean enabled1 = myEnabled.get(o1.getPluginId());
        final Boolean enabled2 = myEnabled.get(o2.getPluginId());
        if (enabled1 != null && enabled1) {
          if (enabled2 != null && enabled2) {
            return 0;
          }

          return 1;
        }
        else {
          if (enabled2 == null || !enabled2) {
            return 0;
          }
          return -1;
        }
      };
    }

    @Override
    public int getWidth(JTable table) {
      return new JCheckBox().getPreferredSize().width;
    }
  }

  private void warnAboutMissedDependencies(final Boolean newVal, final PluginDescriptor... ideaPluginDescriptors) {
    final Set<PluginId> deps = new HashSet<>();
    final List<PluginDescriptor> descriptorsToCheckDependencies = new ArrayList<>();
    if (newVal) {
      Collections.addAll(descriptorsToCheckDependencies, ideaPluginDescriptors);
    }
    else {
      descriptorsToCheckDependencies.addAll(getAllPlugins());
      descriptorsToCheckDependencies.removeAll(Arrays.asList(ideaPluginDescriptors));

      for (Iterator<PluginDescriptor> iterator = descriptorsToCheckDependencies.iterator(); iterator.hasNext(); ) {
        PluginDescriptor descriptor = iterator.next();
        final Boolean enabled = myEnabled.get(descriptor.getPluginId());
        if (enabled == null || !enabled) {
          iterator.remove();
        }
      }
    }

    for (final PluginDescriptor ideaPluginDescriptor : descriptorsToCheckDependencies) {
      consulo.container.plugin.PluginManager.checkDependants(ideaPluginDescriptor, PluginManager::getPlugin, pluginId -> {
        Boolean enabled = myEnabled.get(pluginId);
        if (enabled == null) {
          return false;
        }
        if (newVal && !enabled) {
          deps.add(pluginId);
        }

        if (!newVal) {
          if (ideaPluginDescriptor.isDeleted()) {
            return true;
          }
          final PluginId pluginDescriptorId = ideaPluginDescriptor.getPluginId();
          for (PluginDescriptor descriptor : ideaPluginDescriptors) {
            if (pluginId.equals(descriptor.getPluginId())) {
              deps.add(pluginDescriptorId);
              break;
            }
          }
        }
        return true;
      });
    }
    if (!deps.isEmpty()) {
      final String listOfSelectedPlugins = StringUtil.join(ideaPluginDescriptors, PluginDescriptor::getName, ", ");
      final Set<PluginDescriptor> pluginDependencies = new HashSet<>();
      final String listOfDependencies = StringUtil.join(deps, pluginId -> {
        final PluginDescriptor pluginDescriptor = PluginManager.getPlugin(pluginId);
        assert pluginDescriptor != null;
        pluginDependencies.add(pluginDescriptor);
        return pluginDescriptor.getName();
      }, "<br>");
      final String message = !newVal
                             ? "<html>The following plugins <br>" +
                               listOfDependencies +
                               "<br>are enabled and depend" +
                               (deps.size() == 1 ? "s" : "") +
                               " on selected plugins. " +
                               "<br>Would you like to disable them too?</html>"
                             : "<html>The following plugins on which " +
                               listOfSelectedPlugins +
                               " depend" +
                               (ideaPluginDescriptors.length == 1 ? "s" : "") +
                               " are disabled:<br>" +
                               listOfDependencies +
                               "<br>Would you like to enable them?</html>";
      if (Messages.showOkCancelDialog(message, newVal ? "Enable Dependant Plugins" : "Disable Plugins with Dependency on this", Messages.getQuestionIcon()) == Messages.OK) {
        for (PluginId pluginId : deps) {
          myEnabled.put(pluginId, newVal);
        }

        updatePluginDependencies();
        hideNotApplicablePlugins(newVal, pluginDependencies.toArray(new PluginDescriptor[pluginDependencies.size()]));
      }
    }
  }

  private class MyPluginManagerColumnInfo extends PluginManagerColumnInfo {
    public MyPluginManagerColumnInfo() {
      super(PluginManagerColumnInfo.COLUMN_NAME, InstalledPluginsTableModel.this);
    }

    @Override
    public TableCellRenderer getRenderer(final PluginDescriptor pluginDescriptor) {
      return new PluginsTableRenderer(pluginDescriptor, false);
    }

    @Override
    protected boolean isSortByName() {
      return true;
    }

    @Override
    public Comparator<PluginDescriptor> getComparator() {
      final Comparator<PluginDescriptor> comparator = super.getColumnComparator();
      return (o1, o2) -> {
        if (isSortByStatus()) {
          final boolean incompatible1 = PluginManager.isIncompatible(o1);
          final boolean incompatible2 = PluginManager.isIncompatible(o2);
          if (incompatible1) {
            if (incompatible2) return comparator.compare(o1, o2);
            return -1;
          }
          if (incompatible2) return 1;

          final boolean hasNewerVersion1 = hasNewerVersion(o1.getPluginId());
          final boolean hasNewerVersion2 = hasNewerVersion(o2.getPluginId());
          if (hasNewerVersion1) {
            if (hasNewerVersion2) return comparator.compare(o1, o2);
            return -1;
          }
          if (hasNewerVersion2) return 1;


          final boolean wasUpdated1 = wasUpdated(o1.getPluginId());
          final boolean wasUpdated2 = wasUpdated(o2.getPluginId());
          if (wasUpdated1) {
            if (wasUpdated2) return comparator.compare(o1, o2);
            return -1;
          }
          if (wasUpdated2) return 1;


          if (o1 instanceof PluginNode) {
            if (o2 instanceof PluginNode) return comparator.compare(o1, o2);
            return -1;
          }
          if (o2 instanceof PluginNode) return 1;


          final boolean deleted1 = o1.isDeleted();
          final boolean deleted2 = o2.isDeleted();
          if (deleted1) {
            if (deleted2) return comparator.compare(o1, o2);
            return -1;
          }
          if (deleted2) return 1;

          final boolean enabled1 = isEnabled(o1.getPluginId());
          final boolean enabled2 = isEnabled(o2.getPluginId());
          if (enabled1 && !enabled2) return -1;
          if (enabled2 && !enabled1) return 1;
        }
        return comparator.compare(o1, o2);
      };
    }

    @Override
    public int getWidth(JTable table) {
      return super.getWidth(table);
    }
  }
}
