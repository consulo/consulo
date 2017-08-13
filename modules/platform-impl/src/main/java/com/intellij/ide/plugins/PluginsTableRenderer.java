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

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class PluginsTableRenderer extends DefaultTableCellRenderer {
  protected JLabel myName;
  private JLabel myStatus;
  protected RatesPanel myRating;
  protected JLabel myDownloads;
  private JLabel myLastUpdated;
  private JPanel myPanel;

  protected JLabel myCategory;
  private JPanel myRightPanel;
  private JPanel myBottomPanel;
  private JPanel myInfoPanel;
  private final IdeaPluginDescriptor myPluginDescriptor;

  public PluginsTableRenderer(IdeaPluginDescriptor pluginDescriptor, boolean availableRender) {
    myPluginDescriptor = pluginDescriptor;

    final Font smallFont;
    if (SystemInfo.isMac) {
      smallFont = UIUtil.getLabelFont(UIUtil.FontSize.MINI);
    }
    else {
      smallFont = UIUtil.getLabelFont().deriveFont(Math.max(UISettings.getInstance().getFontSize() - JBUI.scale(3), JBUI.scaleFontSize(10)));
    }
    myName.setFont(UIUtil.getLabelFont().deriveFont(UISettings.getInstance().getFontSize()));
    myStatus.setFont(smallFont);
    myCategory.setFont(smallFont);
    myDownloads.setFont(smallFont);
    myStatus.setText("");
    myCategory.setText("");
    myLastUpdated.setFont(smallFont);
    if (!availableRender || !(pluginDescriptor instanceof PluginNode)) {
      myPanel.remove(myRightPanel);
    }

    myPanel.setBorder(UIUtil.isRetina() ? JBUI.Borders.empty(4, 3, 4, 3) : JBUI.Borders.empty(2, 3, 2, 3));
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (myPluginDescriptor == null) {
      return myPanel;
    }

    myName.setText(myPluginDescriptor.getName() + "  ");

    final Color fg = UIUtil.getTableForeground(isSelected);
    final Color bg = UIUtil.getTableBackground(isSelected);
    final Color grayedFg = isSelected ? fg : new JBColor(Gray._130, Gray._120);
    myName.setForeground(fg);
    myStatus.setForeground(grayedFg);
    myStatus.setIcon(AllIcons.Nodes.Plugin);
    String category = myPluginDescriptor.getCategory();
    myCategory.setForeground(grayedFg);
    if (category != null) {
      myCategory.setText(category.toUpperCase() + " ");
    }

    myPanel.setBackground(bg);
    myLastUpdated.setForeground(grayedFg);
    myLastUpdated.setText("");
    myDownloads.setForeground(grayedFg);
    myDownloads.setText("");

    final PluginNode pluginNode = myPluginDescriptor instanceof PluginNode ? (PluginNode)myPluginDescriptor : null;
    if (pluginNode != null) {
      String downloads = pluginNode.getDownloads();
      if (downloads == null) downloads = "";
      if (downloads.length() > 3) {
        downloads = new DecimalFormat("#,###").format(Integer.parseInt(downloads));
      }
      myDownloads.setText(downloads);

      myRating.setRate(pluginNode.getRating());
      myLastUpdated.setText(DateFormatUtil.formatBetweenDates(pluginNode.getDate(), System.currentTimeMillis()));
    }

    updatePresentation(isSelected, myPluginDescriptor, table.getModel());

    return myPanel;
  }

  protected void updatePresentation(boolean isSelected, @NotNull IdeaPluginDescriptor pluginNode, TableModel model) {
    final IdeaPluginDescriptor installed = PluginManager.getPlugin(myPluginDescriptor.getPluginId());
    if (PluginManagerColumnInfo.isDownloaded(pluginNode) || installed != null && InstalledPluginsTableModel.wasUpdated(installed.getPluginId())) {
      if (!isSelected) {
        myName.setForeground(FileStatus.ADDED.getColor());
      }
    }
    else if (pluginNode instanceof PluginNode && ((PluginNode)pluginNode).getStatus() == PluginNode.STATUS_INSTALLED || installed != null) {
      PluginId pluginId = pluginNode.getPluginId();
      final boolean hasNewerVersion = InstalledPluginsTableModel.hasNewerVersion(pluginId);
      if (hasNewerVersion) {
        if (!isSelected) {
          myName.setForeground(FileStatus.MODIFIED.getColor());
        }
        myStatus.setIcon(AllIcons.Nodes.Pluginobsolete);
      }
    }

    if (isIncompatible(myPluginDescriptor, model)) {
      myPanel.setToolTipText(whyIncompatible(myPluginDescriptor, model));
      if (!isSelected) {
        myName.setForeground(JBColor.RED);
      }
    }
  }

  private static boolean isIncompatible(IdeaPluginDescriptor descriptor, TableModel model) {
    return PluginManagerCore.isIncompatible(descriptor) ||
           model instanceof InstalledPluginsTableModel && ((InstalledPluginsTableModel)model).hasProblematicDependencies(descriptor.getPluginId());
  }

  private static String whyIncompatible(IdeaPluginDescriptor descriptor, TableModel model) {
    if (model instanceof InstalledPluginsTableModel) {
      InstalledPluginsTableModel installedModel = (InstalledPluginsTableModel)model;
      Set<PluginId> required = installedModel.getRequiredPlugins(descriptor.getPluginId());

      if (required != null && required.size() > 0) {
        StringBuilder sb = new StringBuilder();

        if (!installedModel.isLoaded(descriptor.getPluginId())) {
          sb.append(IdeBundle.message("plugin.manager.incompatible.not.loaded.tooltip")).append('\n');
        }

        String deps = StringUtil.join(required, id -> {
          IdeaPluginDescriptor plugin = PluginManager.getPlugin(id);
          return plugin != null ? plugin.getName() : id.getIdString();
        }, ", ");
        sb.append(IdeBundle.message("plugin.manager.incompatible.deps.tooltip", required.size(), deps));

        return sb.toString();
      }
    }

    return IdeBundle.message("plugin.manager.incompatible.tooltip", ApplicationNamesInfo.getInstance().getFullProductName());
  }

  private void createUIComponents() {
    myRating = new RatesPanel();
  }
}
