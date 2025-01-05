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
package consulo.ide.impl.idea.ide.plugins.ui;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.util.DateFormatUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.ide.impl.idea.ide.plugins.PluginNode;
import consulo.ide.impl.idea.ide.plugins.ui.action.InstallPluginAction;
import consulo.ide.impl.idea.ide.plugins.ui.action.UninstallPluginAction;
import consulo.ide.impl.plugins.PluginIconHolder;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @author Konstantin Bulenkov
 */
public class PluginHeaderPanel {
    @Nullable
    private final PluginsPanel myPluginsPanel;

    private JTextArea myName;
    private JBLabel myDownloads;
    private JBLabel myUpdated;
    private JButton myInstallButton;
    private JPanel myRoot;
    private JPanel myDownloadsPanel;
    private JLabel myExperimentalLabel;
    private JLabel myIconLabel;

    enum PluginAction {
        INSTALL,
        UNINSTALL,
        RESTART
    }

    private ActionListener myActionListener;
    private ActionListener myEnableDisableListener;

    public PluginHeaderPanel(@Nullable PluginsPanel pluginsPanel) {
        myPluginsPanel = pluginsPanel;
        initComponents();
    }

    public void update(@Nonnull PluginDescriptor plugin, @Nullable PluginTab manager) {
        PluginAction action = PluginAction.INSTALL;

        myRoot.setVisible(true);
        myDownloadsPanel.setVisible(true);
        myInstallButton.setVisible(true);
        myUpdated.setVisible(true);

        myName.setText(plugin.getName());
        myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER).deriveFont(Font.BOLD));

        if (plugin instanceof PluginNode) {
            final PluginNode node = (PluginNode) plugin;

            myDownloads.setText(node.getDownloads() + " downloads");
            myUpdated.setText("Updated " + DateFormatUtil.formatDate(node.getDate()));
            switch (node.getInstallStatus()) {
                case PluginNode.STATUS_INSTALLED:
                    action = PluginAction.UNINSTALL;
                    break;
                case PluginNode.STATUS_DOWNLOADED:
                    action = PluginAction.RESTART;
                    break;
            }
        }
        else {
            action = null;
            myDownloadsPanel.setVisible(false);
            myUpdated.setVisible(false);

            if (!PluginIds.isPlatformPlugin(plugin.getPluginId())) {
                if (plugin.isDeleted()) {
                    action = PluginAction.RESTART;
                }
                else {
                    action = PluginAction.UNINSTALL;
                }
            }

            if (action == PluginAction.RESTART && manager != null && !manager.isRequireShutdown()) {
                action = null;
            }
        }

        if (manager == null || action == null) {
            action = PluginAction.INSTALL;
            myInstallButton.setVisible(false);
        }

        myIconLabel.setOpaque(false);
        myIconLabel.setIcon(TargetAWT.to(PluginIconHolder.get(plugin)));

        switch (action) {
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

        switch (action) {
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

        if (myEnableDisableListener != null) {
            myEnableDisableListener = null;
        }

        myRoot.revalidate();
        myInstallButton.getParent().revalidate();
        myInstallButton.revalidate();

        if (myActionListener != null) {
            myInstallButton.removeActionListener(myActionListener);
            myActionListener = null;
        }

        final PluginAction finalAction = action;
        myActionListener = e -> {
            switch (finalAction) {
                case INSTALL:
                    InstallPluginAction.install(myPluginsPanel, manager, null, () -> UIUtil.invokeLaterIfNeeded(() -> update(plugin, manager)));
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
        myRoot.add(new BorderLayoutPanel().andTransparent().addToRight(myDownloadsPanel));

        myUpdated = new JBLabel();

        final Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
        myDownloads.setFont(smallFont);
        myUpdated.setFont(smallFont);
        myRoot.setVisible(false);
    }

    public JPanel getPanel() {
        return myRoot;
    }
}
