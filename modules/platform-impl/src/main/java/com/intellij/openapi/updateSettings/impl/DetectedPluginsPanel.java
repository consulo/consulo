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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginHeaderPanel;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Couple;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.OrderPanel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author anna
 *         Date: 04-Dec-2007
 */
public class DetectedPluginsPanel extends OrderPanel<Couple> {
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  private JEditorPane myDescriptionPanel = new JEditorPane();
  private PluginHeaderPanel myHeader;

  public DetectedPluginsPanel() {
    super(Couple.class);
    final JTable entryTable = getEntryTable();
    myHeader = new PluginHeaderPanel(null, entryTable);
    entryTable.setTableHeader(null);
    entryTable.setDefaultRenderer(Couple.class, new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(final JTable table,
                                           final Object value,
                                           final boolean selected,
                                           final boolean hasFocus,
                                           final int row,
                                           final int column) {
        setBorder(null);
        final Couple<IdeaPluginDescriptor> targetForUpdate = (Couple<IdeaPluginDescriptor>)value;
        if (targetForUpdate != null) {
          IdeaPluginDescriptor targetPluginDescriptor = targetForUpdate.getSecond();
          final String pluginName = targetPluginDescriptor.getName();
          append(pluginName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
          final IdeaPluginDescriptor installedPluginDescriptor = targetForUpdate.getFirst();
          if (installedPluginDescriptor != null) {
            final String oldPluginName = installedPluginDescriptor.getName();
            if (!Comparing.strEqual(pluginName, oldPluginName)) {
              append(" - " + oldPluginName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
          }
          final String loadedVersion = targetPluginDescriptor.getVersion();
          if (loadedVersion != null || (installedPluginDescriptor != null && installedPluginDescriptor.getVersion() != null)) {
            final String installedVersion = installedPluginDescriptor != null && installedPluginDescriptor.getVersion() != null
                                            ? "v. " +
                                              installedPluginDescriptor.getVersion() +
                                              (loadedVersion != null ? " -> " : "")
                                            : "";
            final String availableVersion = loadedVersion != null ? loadedVersion : "";
            append(" (" + installedVersion + availableVersion + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
        }
      }
    });
    entryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      @SuppressWarnings("unchecked")
      public void valueChanged(ListSelectionEvent e) {
        final int selectedRow = entryTable.getSelectedRow();
        if (selectedRow != -1) {
          final Couple<IdeaPluginDescriptor> selection = getValueAt(selectedRow);
          final IdeaPluginDescriptor descriptor = selection.getSecond();
          if (descriptor != null) {
            PluginManagerMain.pluginInfoUpdate(descriptor, null, myDescriptionPanel, myHeader, null);
          }
        }
      }
    });
    setCheckboxColumnName("");
    myDescriptionPanel.setPreferredSize(new Dimension(400, -1));
    myDescriptionPanel.setEditable(false);
    myDescriptionPanel.setContentType(UIUtil.HTML_MIME);
    myDescriptionPanel.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());
    removeAll();

    final Splitter splitter = new Splitter(false);
    splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(entryTable));
    splitter.setSecondComponent(ScrollPaneFactory.createScrollPane(myDescriptionPanel));
    add(splitter, BorderLayout.CENTER);
  }

  @Override
  public String getCheckboxColumnName() {
    return "";
  }

  @Override
  public boolean isCheckable(final Couple downloader) {
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isChecked(final Couple temp) {
    Couple<IdeaPluginDescriptor> couple = temp;
    return !getSkippedPlugins().contains(couple.getFirst().getPluginId().getIdString());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setChecked(final Couple temp, final boolean checked) {
    for (Listener listener : myListeners) {
      listener.stateChanged();
    }
  }

  protected Set<String> getSkippedPlugins() {
    return Collections.emptySet();
  }

  public void addStateListener(Listener l) {
    myListeners.add(l);
  }

  public interface Listener {
    void stateChanged();
  }
}