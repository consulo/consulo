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
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;

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

  public PluginsTableRenderer(IdeaPluginDescriptor pluginDescriptor, boolean showFullInfo) {
    myPluginDescriptor = pluginDescriptor;

    final Font smallFont;
    if (SystemInfo.isMac) {
      smallFont = UIUtil.getLabelFont(UIUtil.FontSize.MINI);
    }
    else {
      smallFont = UIUtil.getLabelFont().deriveFont(Math.max(UIUtil.getLabelFont().getSize() - 2, 11f));
    }
    myName.setFont(UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().getSize() + 1.0f));
    myStatus.setFont(smallFont);
    myCategory.setFont(smallFont);
    myDownloads.setFont(smallFont);
    myStatus.setText("");
    myCategory.setText("");
    myLastUpdated.setFont(smallFont);
    if (!showFullInfo || !(pluginDescriptor instanceof PluginNode)) {
      myPanel.remove(myRightPanel);
    }

    if (!showFullInfo) {
      myInfoPanel.remove(myBottomPanel);
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

    updatePresentation(isSelected, pluginNode);

    return myPanel;
  }

  protected void updatePresentation(boolean isSelected, PluginNode pluginNode) {
    final IdeaPluginDescriptor installed = PluginManager.getPlugin(myPluginDescriptor.getPluginId());
    if ((pluginNode != null && PluginManagerColumnInfo.isDownloaded(pluginNode)) ||
        (installed != null && InstalledPluginsTableModel.wasUpdated(installed.getPluginId()))) {
      if (!isSelected) myName.setForeground(FileStatus.ADDED.getColor());
    }
    else if (pluginNode != null && pluginNode.getStatus() == PluginNode.STATUS_INSTALLED) {
      PluginId pluginId = pluginNode.getPluginId();
      final boolean hasNewerVersion = InstalledPluginsTableModel.hasNewerVersion(pluginId);
      if (!isSelected) myName.setForeground(FileStatus.MODIFIED.getColor());
      if (hasNewerVersion) {
        if (!isSelected) {
          myName.setForeground(JBColor.RED);
        }
        myStatus.setIcon(AllIcons.Nodes.Pluginobsolete);
      }
    }
  }

  private void createUIComponents() {
    myRating = new RatesPanel();
  }
}
