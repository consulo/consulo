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

import consulo.application.util.DateFormatUtil;
import consulo.container.internal.PluginValidator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.PluginIconHolder;
import consulo.externalService.impl.internal.plugin.InstalledPluginsState;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginTagUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A row of a plugin list - the icon, the name in the colour of its status and the tags under it.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public class UnifiedPluginRowRender {
    public static final Length ROW_HEIGHT = Length.ofFont(2).plusPixel(20);

    private static final Key<Label> ICON_KEY = Key.create("UnifiedPluginRowRender.icon");
    private static final Key<Label> NAME_KEY = Key.create("UnifiedPluginRowRender.name");
    private static final Key<Label> TAGS_KEY = Key.create("UnifiedPluginRowRender.tags");
    private static final Key<Label> UPDATED_KEY = Key.create("UnifiedPluginRowRender.updated");

    public static ComponentItemRender<PluginDescriptor> reusable() {
        return ComponentItemRender.reusable(UnifiedPluginRowRender::createRow, (row, item) -> bindRow(row, item, null, null));
    }

    @RequiredUIAccess
    public static Component render(RenderItem<PluginDescriptor> item) {
        return render(item, null, null);
    }

    /**
     * @param nameColor  the colour of the name, for a list which tells its rows apart by a status of its own
     *                   rather than by the one of the plugin in the platform
     * @param secondLine what is drawn under the name in place of the tags
     */
    @RequiredUIAccess
    public static Component render(
        RenderItem<PluginDescriptor> item,
        @Nullable ColorValue nameColor,
        @Nullable LocalizeValue secondLine
    ) {
        DockLayout row = createRow();
        bindRow(row, item, nameColor, secondLine);
        return row;
    }

    @RequiredUIAccess
    private static DockLayout createRow() {
        Label iconLabel = Label.create(LocalizeValue.empty());
        iconLabel.paddingBuilder().rightSet(Space.LARGE).apply();

        Label nameLabel = Label.create(LocalizeValue.empty());
        nameLabel.addStyle(LabelStyle.FONT_LARGE);

        Label tagsLabel = Label.create(LocalizeValue.empty());
        tagsLabel.addStyle(LabelStyle.FONT_XX_SMALL);
        tagsLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        VerticalLayout text = VerticalLayout.create(Space.LARGE);
        text.add(nameLabel);
        text.add(tagsLabel);
        text.paddingBuilder().horizontalSet(Space.MEDIUM).apply();

        Label updatedLabel = Label.create(LocalizeValue.empty());
        updatedLabel.addStyle(LabelStyle.FONT_XX_SMALL);
        updatedLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        DockLayout right = DockLayout.create(Space.NONE);
        right.bottom(updatedLabel);

        DockLayout row = DockLayout.create(Space.NONE);
        row.left(iconLabel);
        row.center(text);
        row.right(right);
        row.paddingBuilder().allSet(Space.LARGE).apply();

        row.putUserData(ICON_KEY, iconLabel);
        row.putUserData(NAME_KEY, nameLabel);
        row.putUserData(TAGS_KEY, tagsLabel);
        row.putUserData(UPDATED_KEY, updatedLabel);
        return row;
    }

    @RequiredUIAccess
    private static void bindRow(
        Component row,
        RenderItem<PluginDescriptor> item,
        @Nullable ColorValue nameColor,
        @Nullable LocalizeValue secondLine
    ) {
        Label iconLabel = Objects.requireNonNull(row.getUserData(ICON_KEY));
        Label nameLabel = Objects.requireNonNull(row.getUserData(NAME_KEY));
        Label tagsLabel = Objects.requireNonNull(row.getUserData(TAGS_KEY));
        Label updatedLabel = Objects.requireNonNull(row.getUserData(UPDATED_KEY));

        PluginDescriptor descriptor = item.getValue();
        if (descriptor == null) {
            iconLabel.setImage(null);
            nameLabel.setText(LocalizeValue.empty());
            tagsLabel.setText(LocalizeValue.empty());
            updatedLabel.setText(LocalizeValue.empty());
            row.setBackgroundColor(null);
            return;
        }

        iconLabel.setImage(PluginIconHolder.get(descriptor));
        nameLabel.setText(LocalizeValue.of(StringUtil.notNullize(descriptor.getName())));

        String under = secondLine != null
            ? secondLine.get()
            : descriptor.getTags()
                .stream()
                .map(tag -> PluginTagUtil.getTagLocalizeValue(tag).get().toUpperCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
        tagsLabel.setText(LocalizeValue.of(under));

        updatedLabel.setText(descriptor instanceof PluginNode node ? updatedText(node) : LocalizeValue.empty());

        ColorValue background = null;
        ColorValue foreground = null;
        if (!item.isSelected()) {
            background = getRowBackgroundColor(descriptor);
            foreground = nameColor != null ? nameColor : getNameColor(descriptor);
        }
        row.setBackgroundColor(background);
        nameLabel.setForegroundColor(foreground);
    }

    private static LocalizeValue updatedText(PluginNode node) {
        String downloads = String.valueOf(node.getDownloads());
        if (downloads.length() > 3) {
            downloads = new DecimalFormat("#,###").format(node.getDownloads());
        }

        String updated = DateFormatUtil.formatBetweenDates(node.getDate(), System.currentTimeMillis()).get();
        return LocalizeValue.of(updated + "  " + downloads);
    }

    private static @Nullable ColorValue getRowBackgroundColor(PluginDescriptor descriptor) {
        if (descriptor.isDeleted()) {
            return StandardColors.LIGHT_RED;
        }
        if (isDownloaded(descriptor)) {
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

        if (isDownloaded(descriptor) || installed != null && state.wasUpdated(installed.getPluginId())) {
            return FileStatus.ADDED.getColor();
        }

        boolean installedNode = descriptor instanceof PluginNode node && node.getInstallStatus() == PluginNode.STATUS_INSTALLED;
        if ((installedNode || installed != null) && state.hasNewerVersion(pluginId)) {
            return FileStatus.MODIFIED.getColor();
        }

        return null;
    }

    private static boolean isDownloaded(PluginDescriptor descriptor) {
        if (descriptor instanceof PluginNode pluginNode && pluginNode.getInstallStatus() == PluginNode.STATUS_DOWNLOADED) {
            return true;
        }

        PluginId pluginId = descriptor.getPluginId();
        if (PluginManager.findPlugin(pluginId) != null) {
            return false;
        }
        return InstalledPluginsState.getInstance().getInstalledPlugins().contains(pluginId);
    }

    private static boolean isIncompatible(PluginDescriptor descriptor) {
        PluginDescriptorStatus status = descriptor.getStatus();
        if (status != PluginDescriptorStatus.OK && status != PluginDescriptorStatus.DISABLED_BY_USER) {
            return true;
        }
        return PluginValidator.isIncompatible(descriptor);
    }
}
