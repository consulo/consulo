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

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.ide.plugins.PluginDescriptionPanel;
import consulo.ide.plugins.PluginIconHolder;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public class PluginsTableRenderer extends DefaultTableCellRenderer {
  protected SimpleColoredComponent myName;
  private JBLabel myStatus;
  protected RatesPanel myRating;
  protected JLabel myDownloads;
  private JLabel myLastUpdated;
  private BorderLayoutPanel myPanel;

  protected JLabel myCategory;
  private final PluginDescriptor myPluginDescriptor;

  public PluginsTableRenderer(PluginDescriptor pluginDescriptor, boolean availableRender) {
    myPluginDescriptor = pluginDescriptor;

    myPanel = new BorderLayoutPanel();

    myStatus = new JBLabel();
    myStatus.setOpaque(false);

    myPanel.addToLeft(myStatus);

    JPanel nameAndCategory = new JPanel(new VerticalLayout(JBUI.scale(5)));
    nameAndCategory.setBorder(JBUI.Borders.empty(0, 5));
    nameAndCategory.setOpaque(false);
    nameAndCategory.add(myName = new SimpleColoredComponent());
    nameAndCategory.add(myCategory = new JBLabel());

    myName.setOpaque(false);
    myCategory.setOpaque(false);

    myPanel.addToCenter(nameAndCategory);

    BorderLayoutPanel rightPanel = new BorderLayoutPanel();
    rightPanel.setOpaque(false);
    myPanel.addToRight(rightPanel);

    myLastUpdated = new JBLabel();
    myLastUpdated.setOpaque(false);

    myDownloads = new JBLabel();
    myRating = new RatesPanel();
    myRating.setVisible(PluginDescriptionPanel.ENABLED_STARS);

    rightPanel.addToTop(myRating);

    myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
    myName.setIpad(JBUI.emptyInsets());
    myName.setMyBorder(null);

    JPanel rightBottom = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    rightBottom.setOpaque(false);
    rightPanel.addToBottom(rightBottom);

    rightBottom.add(myLastUpdated);
    rightBottom.add(myDownloads);

    final Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.MINI);
    myCategory.setFont(smallFont);
    myDownloads.setFont(smallFont);
    myStatus.setText("");
    myCategory.setText("");
    myLastUpdated.setFont(smallFont);
    if (!availableRender || !(pluginDescriptor instanceof PluginNode)) {
      myPanel.remove(rightPanel);
    }

    myPanel.setBorder(JBUI.Borders.empty(5));
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    if (myPluginDescriptor == null) {
      return myPanel;
    }

    myName.append(myPluginDescriptor.getName() + "  ");

    final Color fg = UIUtil.getTableForeground(isSelected);
    final Color bg = UIUtil.getTableBackground(isSelected);
    final Color grayedFg = isSelected ? fg : new JBColor(Gray._130, Gray._120);
    myName.setForeground(fg);
    myStatus.setForeground(grayedFg);
    myStatus.setIcon(PluginIconHolder.get(myPluginDescriptor));
    myCategory.setForeground(grayedFg);

    Collection<LocalizeValue> tags = PluginManagerMain.getLocalizedTags(myPluginDescriptor);
    myCategory.setText(StringUtil.join(tags, LocalizeValue::get, ", ").toUpperCase() + " ");
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

  protected void updatePresentation(boolean isSelected, @Nonnull PluginDescriptor pluginNode, TableModel model) {
    final PluginDescriptor installed = PluginManager.getPlugin(myPluginDescriptor.getPluginId());
    if (PluginManagerColumnInfo.isDownloaded(pluginNode) || installed != null && InstalledPluginsTableModel.wasUpdated(installed.getPluginId())) {
      if (!isSelected) {
        myName.setForeground(TargetAWT.to(FileStatus.ADDED.getColor()));
      }
    }
    else if (pluginNode instanceof PluginNode && ((PluginNode)pluginNode).getStatus() == PluginNode.STATUS_INSTALLED || installed != null) {
      PluginId pluginId = pluginNode.getPluginId();
      final boolean hasNewerVersion = InstalledPluginsTableModel.hasNewerVersion(pluginId);
      if (hasNewerVersion) {
        if (!isSelected) {
          myName.setForeground(TargetAWT.to(FileStatus.MODIFIED.getColor()));
        }
        //myStatus.setIcon(TargetAWT.to(AllIcons.Nodes.Pluginobsolete));
      }
    }

    if (isIncompatible(myPluginDescriptor, model)) {
      myPanel.setToolTipText(whyIncompatible(myPluginDescriptor, model));
      if (!isSelected) {
        myName.setForeground(JBColor.RED);
      }
    }
  }

  private static boolean isIncompatible(PluginDescriptor descriptor, TableModel model) {
    return PluginManagerCore.isIncompatible(descriptor) ||
           model instanceof InstalledPluginsTableModel && ((InstalledPluginsTableModel)model).hasProblematicDependencies(descriptor.getPluginId());
  }

  private static String whyIncompatible(PluginDescriptor descriptor, TableModel model) {
    if (model instanceof InstalledPluginsTableModel) {
      InstalledPluginsTableModel installedModel = (InstalledPluginsTableModel)model;
      Set<PluginId> required = installedModel.getRequiredPlugins(descriptor.getPluginId());

      if (required != null && required.size() > 0) {
        StringBuilder sb = new StringBuilder();

        if (!installedModel.isLoaded(descriptor.getPluginId())) {
          sb.append(IdeBundle.message("plugin.manager.incompatible.not.loaded.tooltip")).append('\n');
        }

        String deps = StringUtil.join(required, id -> {
          PluginDescriptor plugin = PluginManager.getPlugin(id);
          return plugin != null ? plugin.getName() : id.getIdString();
        }, ", ");
        sb.append(IdeBundle.message("plugin.manager.incompatible.deps.tooltip", required.size(), deps));

        return sb.toString();
      }
    }

    return IdeBundle.message("plugin.manager.incompatible.tooltip", ApplicationNamesInfo.getInstance().getFullProductName());
  }
}
