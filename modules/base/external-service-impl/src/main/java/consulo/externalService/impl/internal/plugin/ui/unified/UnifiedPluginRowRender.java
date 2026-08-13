/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalService.impl.internal.plugin.ui.unified;

import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginTab;
import consulo.externalService.impl.internal.plugin.ui.PluginsPanel;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RenderItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A row of a plugin list - the icon, the name in the colour of its status and the tags under it.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginRowRender {
    public static final int ROW_HEIGHT = 40;

    @RequiredUIAccess
    public static Component render(RenderItem<PluginDescriptor> item) {
        PluginDescriptor descriptor = item.getValue();

        DockLayout row = DockLayout.create(0);
        if (descriptor == null) {
            return row;
        }

        Label iconLabel = Label.create(LocalizeValue.empty());
        iconLabel.setImage(PluginIconHolder.get(descriptor));
        iconLabel.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 5);

        Label nameLabel = Label.create(LocalizeValue.of(StringUtil.notNullize(descriptor.getName())));

        if (!item.isSelected()) {
            ColorValue background = getRowBackgroundColor(descriptor);
            if (background != null) {
                row.setBackgroundColor(background);
            }

            ColorValue nameColor = getNameColor(descriptor);
            if (nameColor != null) {
                nameLabel.setForegroundColor(nameColor);
            }
        }

        VerticalLayout text = VerticalLayout.create(0);
        text.add(nameLabel);

        String tags = descriptor.getTags()
            .stream()
            .map(tag -> PluginTab.getTagLocalizeValue(tag).get().toUpperCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
        if (!tags.isEmpty()) {
            Label tagsLabel = Label.create(LocalizeValue.of(tags));
            tagsLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            text.add(tagsLabel);
        }

        row.left(iconLabel);
        row.center(text);
        return row;
    }

    private static @Nullable ColorValue getRowBackgroundColor(PluginDescriptor descriptor) {
        if (descriptor.isDeleted()) {
            return StandardColors.LIGHT_RED;
        }
        if (PluginsPanel.isDownloaded(descriptor)) {
            return StandardColors.LIGHT_GREEN;
        }
        return null;
    }

    private static @Nullable ColorValue getNameColor(PluginDescriptor descriptor) {
        PluginId pluginId = descriptor.getPluginId();
        PluginDescriptor installed = PluginManager.findPlugin(pluginId);
        InstalledPluginsState state = InstalledPluginsState.getInstance();

        if (isIncompatible(descriptor)) {
            return descriptor.getStatus() == PluginDescriptorStatus.WRONG_PLATFORM
                ? StandardColors.GRAY
                : StandardColors.RED;
        }

        if (PluginsPanel.isDownloaded(descriptor) || installed != null && state.wasUpdated(installed.getPluginId())) {
            return FileStatus.ADDED.getColor();
        }

        boolean installedNode = descriptor instanceof PluginNode node && node.getInstallStatus() == PluginNode.STATUS_INSTALLED;
        if ((installedNode || installed != null) && state.hasNewerVersion(pluginId)) {
            return FileStatus.MODIFIED.getColor();
        }

        return null;
    }

    private static boolean isIncompatible(PluginDescriptor descriptor) {
        PluginDescriptorStatus status = descriptor.getStatus();
        if (status != PluginDescriptorStatus.OK && status != PluginDescriptorStatus.DISABLED_BY_USER) {
            return true;
        }
        return PluginValidator.isIncompatible(descriptor);
    }
}
