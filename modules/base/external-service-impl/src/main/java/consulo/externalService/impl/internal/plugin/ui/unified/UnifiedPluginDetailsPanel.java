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

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginPermissionType;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.impl.internal.plugin.ui.PluginDescriptionMarkup;
import consulo.externalService.impl.internal.plugin.ui.PluginTagUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.FlowLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * The details column of the plugins page: version, links, permissions, dependencies and tags.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class UnifiedPluginDetailsPanel {
    private final VerticalLayout myContent;
    private final ScrollableLayout myRoot;
    private final Consumer<PluginId> myPluginSelector;
    private final Consumer<String> myTagSelector;

    /**
     * @param tagSelector filters the list by a tag clicked here
     */
    @RequiredUIAccess
    public UnifiedPluginDetailsPanel(@RequiredUIAccess Consumer<PluginId> pluginSelector, @RequiredUIAccess Consumer<String> tagSelector) {
        myPluginSelector = pluginSelector;
        myTagSelector = tagSelector;

        myContent = VerticalLayout.create(Space.LARGE);
        myContent.paddingBuilder().allSet(Space.LARGE).apply();
        myContent.setBackgroundColor(ComponentColors.LAYOUT);

        // the flows of links and badges wrap at the column width rather than growing a horizontal scroll
        myRoot = ScrollableLayout.create(
            myContent,
            ScrollableLayoutOptions.builder().horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER).build()
        );
        myRoot.setBackgroundColor(ComponentColors.LAYOUT);
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    public void update(@Nullable PluginDescriptor plugin, List<PluginDescriptor> allPlugins) {
        myContent.removeAll();
        if (plugin == null) {
            return;
        }

        String version = StringUtil.notNullize(plugin.getVersion(), "N/A");
        String size = plugin instanceof PluginNode node ? node.getSize() : null;
        if (!StringUtil.isEmptyOrSpaces(size)) {
            version += "  ·  " + PluginDescriptionMarkup.getFormattedSize(size);
        }
        section(LocalizeValue.localizeTODO("Version"), Label.create(LocalizeValue.of(version)));

        List<Component> links = new ArrayList<>();
        String url = plugin.getUrl();
        if (!StringUtil.isEmptyOrSpaces(url)) {
            links.add(browserLink(LocalizeValue.localizeTODO("Plugin page"), url));
        }
        String vendor = plugin.getVendor();
        String vendorUrl = plugin.getVendorUrl();
        String vendorEmail = plugin.getVendorEmail();
        if (!StringUtil.isEmptyOrSpaces(vendor) || !StringUtil.isEmptyOrSpaces(vendorUrl)) {
            LocalizeValue vendorText = LocalizeValue.of(StringUtil.isEmptyOrSpaces(vendor) ? vendorUrl : vendor);
            links.add(StringUtil.isEmptyOrSpaces(vendorUrl) ? Label.create(vendorText) : browserLink(vendorText, vendorUrl));
        }
        if (!StringUtil.isEmptyOrSpaces(vendorEmail)) {
            links.add(browserLink(LocalizeValue.of(vendorEmail), "mailto:" + vendorEmail));
        }
        flowSection(LocalizeValue.localizeTODO("Links"), links);

        if (PluginIds.isPlatformPlugin(plugin.getPluginId())) {
            return;
        }

        FlowLayout permissions = FlowLayout.create(Space.SMALL);
        boolean anyPermission = false;
        for (PluginPermissionType type : PluginPermissionType.values()) {
            if (plugin.getPermissionDescriptor(type) != null) {
                Badge badge = Badge.create(LocalizeValue.of(type.name()));
                badge.addStyle(BadgeStyle.WARNING);
                permissions.add(badge);
                anyPermission = true;
            }
        }
        if (!anyPermission) {
            Label none = Label.create(LocalizeValue.localizeTODO("No special permissions"));
            none.setForegroundColor(ComponentColors.INFO_FOREGROUND);
            permissions.add(none);
        }
        section(LocalizeValue.localizeTODO("Permissions"), permissions);

        List<Component> dependencies = new ArrayList<>();
        for (PluginId pluginId : plugin.getDependentPluginIds()) {
            PluginDescriptor dependency = PluginIds.isPlatformPlugin(pluginId) ? null : find(allPlugins, pluginId);
            if (dependency != null) {
                boolean optional = ArrayUtil.contains(pluginId, plugin.getOptionalDependentPluginIds());
                dependencies.add(pluginLink(dependency, optional));
            }
        }
        flowSection(LocalizeValue.localizeTODO("Depends on"), dependencies);

        Map<String, PluginDescriptor> dependents = new TreeMap<>();
        for (PluginDescriptor descriptor : allPlugins) {
            if (ArrayUtil.contains(plugin.getPluginId(), descriptor.getDependentPluginIds())) {
                dependents.put(StringUtil.notNullize(descriptor.getName()), descriptor);
            }
        }
        List<Component> dependentLinks = new ArrayList<>();
        for (PluginDescriptor dependent : dependents.values()) {
            dependentLinks.add(pluginLink(dependent, false));
        }
        flowSection(LocalizeValue.localizeTODO("Used by"), dependentLinks);

        List<Component> tags = new ArrayList<>();
        for (String tag : plugin.getTags()) {
            Badge badge = Badge.create(PluginTagUtil.getTagLocalizeValue(tag));
            badge.addStyle(BadgeStyle.NEUTRAL);
            badge.setToolTipText(LocalizeValue.localizeTODO("Show plugins with this tag"));
            badge.addClickListener(event -> myTagSelector.accept(tag));
            tags.add(badge);
        }
        flowSection(LocalizeValue.localizeTODO("Tags"), tags);
    }

    @RequiredUIAccess
    private void flowSection(LocalizeValue title, List<Component> components) {
        if (components.isEmpty()) {
            return;
        }
        FlowLayout flow = FlowLayout.create(Space.SMALL);
        components.forEach(flow::add);
        section(title, flow);
    }

    @RequiredUIAccess
    private void section(LocalizeValue title, Component body) {
        Label titleLabel = Label.create(title);
        titleLabel.addStyle(LabelStyle.FONT_SMALL);
        titleLabel.setForegroundColor(ComponentColors.INFO_FOREGROUND);

        VerticalLayout section = VerticalLayout.create(Space.SMALL);
        section.add(titleLabel);
        section.add(body);
        myContent.add(section);
    }

    @RequiredUIAccess
    private Hyperlink pluginLink(PluginDescriptor plugin, boolean optional) {
        LocalizeValue name = LocalizeValue.of(StringUtil.notNullize(plugin.getName(), plugin.getPluginId().getIdString()));
        PluginId pluginId = plugin.getPluginId();
        return Hyperlink.create(
            optional ? LocalizeValue.join(name, LocalizeValue.localizeTODO(" (optional)")) : name,
            event -> myPluginSelector.accept(pluginId)
        );
    }

    @RequiredUIAccess
    private static Hyperlink browserLink(LocalizeValue text, String url) {
        return Hyperlink.create(text, event -> Platform.current().openInBrowser(url));
    }

    private static @Nullable PluginDescriptor find(List<PluginDescriptor> allPlugins, PluginId pluginId) {
        for (PluginDescriptor descriptor : allPlugins) {
            if (descriptor.getPluginId().equals(pluginId)) {
                return descriptor;
            }
        }
        return null;
    }
}
