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
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.options.newEditor.DesktopSettingsDialog;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;

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
  private JPanel myButtonPanel;
  private JPanel myDownloadsPanel;
  private JPanel myVersionInfoPanel;

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
    myButtonPanel.setVisible(true);
    myUpdated.setVisible(true);

    //data
    myName.setText("<html><body>" + plugin.getName() + "</body></html>");
    myCategory.setText(plugin.getCategory() == null ? "UNKNOWN" : plugin.getCategory().toUpperCase());
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
    if (myManager == null || myActionId == null || (myManager.getInstalled() != myManager.getAvailable() && myActionId == ACTION_ID.UNINSTALL)) {
      myActionId = ACTION_ID.INSTALL;
      myButtonPanel.setVisible(false);
    }
    myRoot.revalidate();
    ((JComponent)myInstallButton.getParent()).revalidate();
    myInstallButton.revalidate();
    ((JComponent)myVersion.getParent()).revalidate();
    myVersion.revalidate();
  }

  private void createUIComponents() {
    myInstallButton = new JButton() {
      {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public Dimension getPreferredSize() {
        final FontMetrics metrics = getFontMetrics(getFont());
        final int textWidth = metrics.stringWidth(getText());
        final int width = JBUI.scale(8 + 16 + 4 + 8) + textWidth;
        final int height = JBUI.scale(2) + Math.max(JBUI.scale(16), metrics.getHeight()) + JBUI.scale(2);
        return new Dimension(width, height);
      }

      @Override
      public void paint(Graphics g2) {
        final Graphics2D g = (Graphics2D)g2;
        final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
        final int w = g.getClipBounds().width;
        final int h = g.getClipBounds().height;

        g.setPaint(getBackgroundBorderPaint());
        g.fillRoundRect(0, 0, w, h, JBUI.scale(7), JBUI.scale(7));

        g.setPaint(getBackgroundPaint());
        g.fillRoundRect(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(2), h - JBUI.scale(2), JBUI.scale(6), JBUI.scale(6));
        g.setColor(getButtonForeground());
        g.drawString(getText(), JBUI.scale(8 + 16 + 4), getBaseline(w, h));
        getIcon().paintIcon(this, g, JBUI.scale(8), (getHeight() - getIcon().getIconHeight()) / 2);
        config.restore();
      }

      private Color getButtonForeground() {
        return UIUtil.getLabelForeground();
      }

      private Paint getBackgroundPaint() {
        switch (myActionId) {
          case INSTALL:
            return new JBColor(new Color(0x4DA864), new Color(49, 98, 49));
          case UNINSTALL:
            return LightColors.RED;
          case RESTART:
            break;
        }
        return Gray._238;
      }

      private Paint getBackgroundBorderPaint() {
        return UIUtil.getBorderColor();
      }


      @Override
      public String getText() {
        switch (myActionId) {
          case INSTALL:
            return "Install plugin";
          case UNINSTALL:
            return "Uninstall plugin";
          case RESTART:
            return "Restart " + ApplicationNamesInfo.getInstance().getFullProductName();
        }
        return super.getText();
      }

      @Override
      public Icon getIcon() {
        switch (myActionId) {
          case INSTALL:
            return AllIcons.General.DownloadPlugin;
          case UNINSTALL:
            return AllIcons.Actions.Delete;
          case RESTART:
            return AllIcons.Actions.Restart;
        }
        return super.getIcon();

      }
    };
    myInstallButton.addActionListener(e -> {
      switch (myActionId) {
        case INSTALL:
          new InstallPluginAction(myManager.getAvailable(), myManager.getInstalled())
                  .install(null, () -> UIUtil.invokeLaterIfNeeded(() -> setPlugin(myPlugin)));
          break;
        case UNINSTALL:
          UninstallPluginAction.uninstall(myManager.getInstalled(), myPlugin);
          break;
        case RESTART:
          if (myManager != null) {
            myManager.apply();
          }
          final DialogWrapper dialog = DialogWrapper.findInstance(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
          if (dialog != null && dialog.isModal()) {
            dialog.close(DialogWrapper.OK_EXIT_CODE);
          }
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
            DialogWrapper settings = DialogWrapper.findInstance(IdeFocusManager.findInstance().getFocusOwner());
            if (settings instanceof DesktopSettingsDialog) {
              ((DesktopSettingsDialog)settings).doOKAction();
            }
            ((ApplicationEx)ApplicationManager.getApplication()).restart(true);
          }, ModalityState.current());
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
