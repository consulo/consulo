/*
 * Copyright 2013-2024 consulo.io
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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.application.util.DateFormatUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2024-12-23
 */
public class PluginsListRender implements ListCellRenderer<PluginDescriptor> {
    private final PluginsPanel myPluginsPanel;
    protected SimpleColoredComponent myName;
    private JBLabel myIcon;
    protected JLabel myDownloads;
    private JLabel myLastUpdated;
    private BorderLayoutPanel myPanel;

    protected JLabel myCategory;

    public PluginsListRender(@Nullable PluginsPanel pluginsPanel) {
        myPluginsPanel = pluginsPanel;
        myPanel = new BorderLayoutPanel();

        myIcon = new JBLabel();
        myIcon.setOpaque(false);
        myIcon.setBorder(JBUI.Borders.emptyRight(8));

        myPanel.addToLeft(myIcon);

        JPanel nameAndCategory = new JPanel(new VerticalLayout(JBUI.scale(8)));
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

        myName.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
        myName.setIpad(JBUI.emptyInsets());

        JPanel rightBottom = new JPanel(new HorizontalLayout(JBUI.scale(5)));
        rightBottom.setOpaque(false);
        rightPanel.addToBottom(rightBottom);

        rightBottom.add(myLastUpdated);
        rightBottom.add(myDownloads);

        final Font smallFont = UIUtil.getLabelFont(UIUtil.FontSize.MINI);
        myCategory.setFont(smallFont);
        myDownloads.setFont(smallFont);
        myIcon.setText("");
        myCategory.setText("");
        myLastUpdated.setFont(smallFont);

        myPanel.setBorder(JBUI.Borders.empty(8));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends PluginDescriptor> list, PluginDescriptor value, int index, boolean isSelected, boolean cellHasFocus) {
        myName.clear();

        if (value == null) {
            return myPanel;
        }

        myName.append(value.getName() + "  ");

        final Color fg = UIUtil.getListForeground(isSelected, cellHasFocus);
        Color backgroundColor = UIUtil.getListBackground(isSelected, cellHasFocus);

        if (!isSelected && !cellHasFocus) {
            if (value.isDeleted()) {
                backgroundColor = LightColors.RED;
            } else if (PluginsPanel.isDownloaded(value))  {
                backgroundColor = LightColors.SLIGHTLY_GREEN;
            }
        }

        final Color grayedFg = isSelected ? fg : new JBColor(Gray._130, Gray._120);
        myName.setForeground(fg);
        myIcon.setForeground(grayedFg);
        myIcon.setIcon(PluginIconHolder.get(value));
        myCategory.setForeground(grayedFg);

        Collection<LocalizeValue> tags = PluginTab.getLocalizedTags(value);
        myCategory.setText(StringUtil.join(tags, LocalizeValue::get, ", ").toUpperCase() + " ");
        myPanel.setBackground(backgroundColor);
        myLastUpdated.setForeground(grayedFg);
        myLastUpdated.setText("");
        myDownloads.setForeground(grayedFg);
        myDownloads.setText("");

        final PluginNode pluginNode = value instanceof PluginNode ? (PluginNode) value : null;
        if (pluginNode != null) {
            String downloads = pluginNode.getDownloads();
            if (downloads == null) {
                downloads = "";
            }
            if (downloads.length() > 3) {
                downloads = new DecimalFormat("#,###").format(Integer.parseInt(downloads));
            }
            myDownloads.setText(downloads);

            myLastUpdated.setText(DateFormatUtil.formatBetweenDates(pluginNode.getDate(), System.currentTimeMillis()));
        }

        updatePresentation(isSelected, value);

        return myPanel;
    }

    protected void updatePresentation(boolean isSelected, @Nonnull PluginDescriptor pluginNode) {
        final PluginDescriptor installed = PluginManager.findPlugin(pluginNode.getPluginId());
        if (PluginsPanel.isDownloaded(pluginNode) || installed != null && InstalledPluginsState.getInstance().wasUpdated(installed.getPluginId())) {
            if (!isSelected) {
                myName.setForeground(TargetAWT.to(FileStatus.ADDED.getColor()));
            }
        }
        else if (pluginNode instanceof PluginNode && ((PluginNode) pluginNode).getInstallStatus() == PluginNode.STATUS_INSTALLED || installed != null) {
            PluginId pluginId = pluginNode.getPluginId();
            final boolean hasNewerVersion = InstalledPluginsState.getInstance().hasNewerVersion(pluginId);
            if (hasNewerVersion) {
                if (!isSelected) {
                    myName.setForeground(TargetAWT.to(FileStatus.MODIFIED.getColor()));
                }
            }
        }

        if (myPluginsPanel != null && myPluginsPanel.isIncompatible(pluginNode)) {
            myPanel.setToolTipText(whyIncompatible(pluginNode));
            if (!isSelected) {
                myName.setForeground(pluginNode.getStatus() == PluginDescriptorStatus.WRONG_PLATFORM ? JBColor.GRAY : JBColor.RED);
            }
        }
    }

    @Nullable
    private String whyIncompatible(PluginDescriptor descriptor) {
        if (descriptor.getStatus() == PluginDescriptorStatus.WRONG_PLATFORM) {
            return ExternalServiceLocalize.pluginManagerWrongPlatformNotLoadedTooltip(descriptor.getName()).get();
        }

        Set<PluginId> required = myPluginsPanel.getRequiredPlugins(descriptor.getPluginId());

        if (required != null && required.size() > 0) {
            StringBuilder sb = new StringBuilder();

            sb.append(ExternalServiceLocalize.pluginManagerIncompatibleNotLoadedTooltip()).append('\n');

            String deps = StringUtil.join(required, id -> {
                PluginDescriptor plugin = PluginManager.findPlugin(id);
                return plugin != null ? plugin.getName() : id.getIdString();
            }, ", ");
            sb.append(ExternalServiceLocalize.pluginManagerIncompatibleDepsTooltip(required.size(), deps));

            return sb.toString();
        }

        return null;
    }
}
