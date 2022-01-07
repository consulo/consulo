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
import com.intellij.openapi.ui.VerticalFlowLayout;
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
import consulo.ide.plugins.PluginDescriptionPanel;
import consulo.ide.plugins.PluginIconHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author Konstantin Bulenkov
 */
public class PluginHeaderPanel {
  private JTextArea myName;
  private JBLabel myDownloads;
  private RatesPanel myRating;
  private JBLabel myUpdated;
  private JButton myInstallButton;
  private JPanel myRoot;
  private JPanel myDownloadsPanel;
  private JLabel myExperimentalLabel;
  private JLabel myIconLabel;

  enum ACTION_ID {
    INSTALL,
    UNINSTALL,
    RESTART
  }

  private ACTION_ID myActionId = ACTION_ID.INSTALL;

  private ActionListener myActionListener;

  public PluginHeaderPanel() {
    initComponents();
  }

  public void update(@Nonnull PluginDescriptor plugin, @Nullable PluginManagerMain manager) {
    myRoot.setVisible(true);
    myDownloadsPanel.setVisible(true);
    myInstallButton.setVisible(true);
    myUpdated.setVisible(true);

    myName.setText(plugin.getName());
    myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));

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
      if (myActionId == ACTION_ID.RESTART && manager != null && !manager.isRequireShutdown()) {
        myActionId = null;
      }
    }

    if (manager == null || myActionId == null) {
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
    if (myActionListener != null) {
      myInstallButton.removeActionListener(myActionListener);
      myActionListener = null;
    }

    myActionListener = e -> {
      switch (myActionId) {
        case INSTALL:
          new InstallPluginAction(manager).install(null, () -> UIUtil.invokeLaterIfNeeded(() -> update(plugin, manager)));
          break;
        case UNINSTALL:
          UninstallPluginAction.uninstall(manager.getInstalled(), plugin);
          break;
        case RESTART:
          if (manager != null) {
            manager.apply();
          }
          final DialogWrapper dialog = DialogWrapper.findInstance(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
          if (dialog != null) {
            dialog.doOKActionPublic();

            ApplicationManager.getApplication().restart(true);
          }
          break;
      }
      update(plugin, manager);
    };
    myInstallButton.addActionListener(myActionListener);

    myExperimentalLabel.setText("Experimental");
    myExperimentalLabel.setVisible(plugin.isExperimental());
    if (plugin.isExperimental()) {
      myExperimentalLabel.setIcon(TargetAWT.to(AllIcons.General.BalloonWarning));
      myExperimentalLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));
      myExperimentalLabel.setForeground(JBColor.RED);
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

    JPanel nameWrapper = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, true, true));
    nameWrapper.setOpaque(false);
    nameWrapper.add(myName);

    myRoot.add(new BorderLayoutPanel().addToLeft(myIconLabel).addToCenter(nameWrapper).andTransparent());
    myRoot.add(new BorderLayoutPanel().addToRight(myInstallButton).andTransparent());

    myExperimentalLabel = new JBLabel();
    myRoot.add(new BorderLayoutPanel().addToRight(myExperimentalLabel).andTransparent());

    myDownloadsPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    myDownloadsPanel.setOpaque(false);
    myDownloadsPanel.add(myDownloads = new JBLabel());
    myDownloadsPanel.add(myRating = new RatesPanel());
    myRating.setVisible(PluginDescriptionPanel.ENABLED_STARS);
    myRoot.add(new BorderLayoutPanel().andTransparent().addToRight(myDownloadsPanel));

    myUpdated = new JBLabel();

    myDownloads.setForeground(JBColor.GRAY);
    myUpdated.setForeground(JBColor.GRAY);
    final Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
    myDownloads.setFont(smallFont);
    myUpdated.setFont(smallFont);
    myRoot.setVisible(false);
  }

  public JPanel getPanel() {
    return myRoot;
  }
}
