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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class PluginHeaderPanel {
  private PluginDescriptor myPlugin;

  @Nullable
  private final PluginManagerMain myManager;
  private final JTable myPluginTable;
  private JBLabel myCategory;
  private JBLabel myName;
  private JBLabel myDownloads;
  private RatesPanel myRating;
  private JBLabel myUpdated;
  private JButton myInstallButton;
  private JBLabel myVersion;
  private JPanel myRoot;
  private JPanel myDownloadsPanel;
  private JPanel myVersionInfoPanel;
  private JLabel myExperimentalLabel;
  private JLabel myIconLabel;

  enum ACTION_ID {
    INSTALL,
    UNINSTALL,
    RESTART
  }

  private ACTION_ID myActionId = ACTION_ID.INSTALL;

  public PluginHeaderPanel(@Nullable PluginManagerMain manager, JTable pluginTable) {
    myManager = manager;
    myPluginTable = pluginTable;
    final Font font = myName.getFont();
    myName.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() + 2));
    final JBColor greyed = new JBColor(Gray._130, Gray._200);
    myCategory.setForeground(greyed);
    myDownloads.setForeground(greyed);
    myUpdated.setForeground(greyed);
    myVersion.setForeground(greyed);
    final Font smallFont = new Font(font.getFontName(), font.getStyle(), font.getSize() - 1);
    myCategory.setFont(smallFont);
    myVersion.setFont(smallFont);
    myDownloads.setFont(smallFont);
    myUpdated.setFont(smallFont);
    myRoot.setVisible(false);
  }

  public void setPlugin(PluginDescriptor plugin) {
    myPlugin = plugin;
    myRoot.setVisible(true);
    myCategory.setVisible(true);
    myDownloadsPanel.setVisible(true);
    myInstallButton.setVisible(true);
    myUpdated.setVisible(true);

    myName.setText(plugin.getName());
    myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));
    myCategory.setText(plugin.getCategory().toUpperCase());
    if (plugin instanceof PluginNode) {
      final PluginNode node = (PluginNode)plugin;

      myRating.setRate(node.getRating());
      myDownloads.setText(node.getDownloads() + " downloads");
      myVersion.setText(" ver " + node.getVersion());
      myUpdated.setText("Updated " + DateFormatUtil.formatDate(node.getDate()));
      switch (node.getStatus()) {
        case PluginNode.STATUS_INSTALLED:
          myActionId = ACTION_ID.UNINSTALL;
          break;
        case PluginNode.STATUS_DOWNLOADED:
          myActionId = ACTION_ID.RESTART;
          break;
        default:
          myActionId = ACTION_ID.INSTALL;
      }
    }
    else {
      myActionId = null;
      myVersionInfoPanel.remove(myUpdated);
      myCategory.setVisible(false);
      myDownloadsPanel.setVisible(false);
      final String version = plugin.getVersion();
      myVersion.setText("Version: " + (version == null ? "N/A" : version));
      myUpdated.setVisible(false);
      if (!PluginIds.isPlatformPlugin(plugin.getPluginId())) {
        if (plugin.isDeleted()) {
          myActionId = ACTION_ID.RESTART;
        }
        else {
          myActionId = ACTION_ID.UNINSTALL;
        }
      }
      if (myActionId == ACTION_ID.RESTART && myManager != null && !myManager.isRequireShutdown()) {
        myActionId = null;
      }
    }

    if (myManager == null || myActionId == null) {
      myActionId = ACTION_ID.INSTALL;
      myInstallButton.setVisible(false);
    }

    myIconLabel.setOpaque(false);
    myIconLabel.setIcon(TargetAWT.to(PlatformIconGroup.nodesPlugin()));

    switch (myActionId) {
      case INSTALL:
        myInstallButton.setIcon(TargetAWT.to(AllIcons.Actions.Install));
        break;
      case UNINSTALL:
        myInstallButton.setIcon(TargetAWT.to(AllIcons.Actions.Cancel));
        break;
      case RESTART:
        myInstallButton.setIcon(TargetAWT.to(AllIcons.Actions.Restart));
        break;
    }

    switch (myActionId) {
      case INSTALL:
        myInstallButton.setText("Install plugin");
        break;
      case UNINSTALL:
        myInstallButton.setText("Uninstall plugin");
        break;
      case RESTART:
        myInstallButton.setText("Restart " + ApplicationNamesInfo.getInstance().getFullProductName());
        break;
    }

    myRoot.revalidate();
    myInstallButton.getParent().revalidate();
    myInstallButton.revalidate();
    myVersion.getParent().revalidate();
    myVersion.revalidate();

    myExperimentalLabel.setVisible(plugin.isExperimental());
    if (plugin.isExperimental()) {
      myExperimentalLabel.setIcon(TargetAWT.to(AllIcons.General.BalloonWarning));
      myExperimentalLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));
      myExperimentalLabel.setForeground(JBColor.RED);
    }
  }

  private void createUIComponents() {
    myInstallButton = new JButton();

    myInstallButton.addActionListener(e -> {
      switch (myActionId) {
        case INSTALL:
          new InstallPluginAction(myManager).install(null, () -> UIUtil.invokeLaterIfNeeded(() -> setPlugin(myPlugin)));
          break;
        case UNINSTALL:
          UninstallPluginAction.uninstall(myManager.getInstalled(), myPlugin);
          break;
        case RESTART:
          if (myManager != null) {
            myManager.apply();
          }
          final DialogWrapper dialog = DialogWrapper.findInstance(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
          if (dialog != null) {
            dialog.doOKActionPublic();

            ((ApplicationEx)ApplicationManager.getApplication()).restart(true);
          }
          break;
      }
      setPlugin(myPlugin);
    });
  }

  public JBLabel getCategory() {
    return myCategory;
  }

  public JBLabel getName() {
    return myName;
  }

  public JBLabel getDownloads() {
    return myDownloads;
  }

  public RatesPanel getRating() {
    return myRating;
  }

  public JBLabel getUpdated() {
    return myUpdated;
  }

  public JButton getInstallButton() {
    return myInstallButton;
  }

  public JPanel getPanel() {
    return myRoot;
  }
}
