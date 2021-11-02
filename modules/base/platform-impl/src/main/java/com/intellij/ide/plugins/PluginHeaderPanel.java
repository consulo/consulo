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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginPermissionDescriptor;
import consulo.container.plugin.PluginPermissionType;
import consulo.ide.plugins.PluginIconHolder;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;

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
  private JTextArea myName;
  private JBLabel myDownloads;
  private RatesPanel myRating;
  private JBLabel myUpdated;
  private JButton myInstallButton;
  private JBLabel myVersion;
  private JPanel myRoot;
  private JPanel myDownloadsPanel;
  private JLabel myExperimentalLabel;
  private JLabel myIconLabel;

  private JPanel myTagsPanel;
  private JPanel myPermissionsPanel;

  enum ACTION_ID {
    INSTALL,
    UNINSTALL,
    RESTART
  }

  private ACTION_ID myActionId = ACTION_ID.INSTALL;

  public PluginHeaderPanel(@Nullable PluginManagerMain manager) {
    initComponents();

    myManager = manager;
  }

  public void setPlugin(PluginDescriptor plugin) {
    myPlugin = plugin;
    myRoot.setVisible(true);
    myDownloadsPanel.setVisible(true);
    myInstallButton.setVisible(true);
    myUpdated.setVisible(true);

    myName.setText(plugin.getName());
    myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));
    myVersion.setText(StringUtil.notNullize(plugin.getVersion(), "N/A"));

    if (plugin instanceof PluginNode) {
      final PluginNode node = (PluginNode)plugin;

      myRating.setRate(node.getRating());
      myDownloads.setText(node.getDownloads() + " downloads");
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
      myDownloadsPanel.setVisible(false);
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
    myIconLabel.setIcon(TargetAWT.to(PluginIconHolder.get(plugin)));

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
        myInstallButton.setText("Install");
        break;
      case UNINSTALL:
        myInstallButton.setText("Uninstall");
        break;
      case RESTART:
        myInstallButton.setText("Restart");
        break;
    }

    myRoot.revalidate();
    myInstallButton.getParent().revalidate();
    myInstallButton.revalidate();

    myExperimentalLabel.setText("Experimental");
    myExperimentalLabel.setVisible(plugin.isExperimental());
    if (plugin.isExperimental()) {
      myExperimentalLabel.setIcon(TargetAWT.to(AllIcons.General.BalloonWarning));
      myExperimentalLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));
      myExperimentalLabel.setForeground(JBColor.RED);
    }

    myPermissionsPanel.removeAll();

    boolean noPermission = true;
    for (PluginPermissionType permissionType : PluginPermissionType.values()) {
      PluginPermissionDescriptor permissionDescriptor = plugin.getPermissionDescriptor(permissionType);
      if (permissionDescriptor != null) {
        JBLabel label = new JBLabel("- " + permissionType.name());
        myPermissionsPanel.add(label);
        noPermission = false;
      }
    }

    if (noPermission) {
      JBLabel noPermissionsLabel = new JBLabel("<no special permissions>");
      noPermissionsLabel.setForeground(JBColor.GRAY);
      noPermissionsLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
      myPermissionsPanel.add(noPermissionsLabel);
    }

    myTagsPanel.removeAll();

    for (LocalizeValue tagValue : PluginManagerMain.getLocalizedTags(plugin)) {
      myTagsPanel.add(new JBLabel(tagValue.get()));
    }
  }

  private void initComponents() {
    myRoot = new JPanel(new VerticalLayout(JBUI.scale(5)));
    myRoot.setOpaque(false);

    myIconLabel = new JBLabel();
    myName = new JBTextArea();
    myName.setBorder(JBUI.Borders.empty());
    myName.setOpaque(false);
    myName.setLineWrap(true);
    myName.setWrapStyleWord(true);
    myName.setEditable(false);
    myName.setBorder(JBUI.Borders.empty(0, 5));
    myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));

    myInstallButton = new JButton();
    myInstallButton.setOpaque(false);

    JPanel buttonPanel = new JPanel(new VerticalLayout(JBUI.scale(5)));
    buttonPanel.setOpaque(false);
    buttonPanel.add(myInstallButton);

    myRoot.add(new BorderLayoutPanel().addToLeft(myIconLabel).addToCenter(myName).addToRight(buttonPanel).andTransparent());

    myExperimentalLabel = new JBLabel();
    myRoot.add(new BorderLayoutPanel().addToRight(myExperimentalLabel).andTransparent());

    Font boldFont = UIUtil.getLabelFont(UIUtil.FontSize.NORMAL).deriveFont(Font.BOLD);

    JLabel permissionLabel = new JBLabel("Permissions:");
    permissionLabel.setFont(boldFont);
    myPermissionsPanel = new JPanel(new VerticalLayout(0));
    myPermissionsPanel.setOpaque(false);
    myPermissionsPanel.setBorder(JBUI.Borders.empty(0, 8, 0, 0));

    JLabel tagsLabel = new JBLabel("Tags:");
    tagsLabel.setFont(boldFont);
    myTagsPanel = new JPanel(new VerticalLayout(0));
    myTagsPanel.setOpaque(false);
    myTagsPanel.setBorder(JBUI.Borders.empty(0, 8, 0, 0));

    myDownloadsPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    myDownloadsPanel.setOpaque(false);
    myDownloadsPanel.add(myDownloads = new JBLabel());
    myDownloadsPanel.add(myRating = new RatesPanel());
    myRoot.add(new BorderLayoutPanel().andTransparent().addToRight(myDownloadsPanel));

    myUpdated = new JBLabel();
    myVersion = new JBLabel();

    JPanel versionInfoPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    versionInfoPanel.setOpaque(false);
    JBLabel verLabel = new JBLabel("Version:");
    verLabel.setFont(boldFont);
    versionInfoPanel.add(verLabel);
    versionInfoPanel.add(myVersion);
    myRoot.add(versionInfoPanel);

    myRoot.add(permissionLabel);
    myRoot.add(myPermissionsPanel);

    myRoot.add(tagsLabel);
    myRoot.add(myTagsPanel);

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

            ApplicationManager.getApplication().restart(true);
          }
          break;
      }
      setPlugin(myPlugin);
    });

    myDownloads.setForeground(JBColor.GRAY);
    myUpdated.setForeground(JBColor.GRAY);
    final Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
    myDownloads.setFont(smallFont);
    myUpdated.setFont(smallFont);
    myRoot.setVisible(false);
  }

  public JTextArea getName() {
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
