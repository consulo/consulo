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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginPermissionDescriptor;
import consulo.container.plugin.PluginPermissionType;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static consulo.util.lang.StringUtil.isEmptyOrSpaces;

/**
 * The body of the plugin description as html. Shared by the frontends - whoever renders it only decides how the
 * text is shown and what happens on a {@code plugin://} link.
 */
public final class PluginDescriptionMarkup {
    public static final String PLUGIN_PREFIX = "plugin://";

    private static final String HTML_PREFIX = "<a href=\"";
    private static final String HTML_SUFFIX = "</a>";

    private static final float mgByte = 1024.0f * 1024.0f;
    private static final float kByte = 1024.0f;

    private PluginDescriptionMarkup() {
    }

    public static StringBuilder buildBody(PluginDescriptor plugin, List<PluginDescriptor> allPlugins) {
        StringBuilder sb = new StringBuilder();

        sb.append("<h3>Version:</h3>").append("&nbsp;&nbsp;").append(StringUtil.notNullize(plugin.getVersion(), "N/A"));

        if (PluginIds.isPlatformPlugin(plugin.getPluginId())) {
            return sb;
        }

        sb.append("<h3>Permissions:</h3>");
        boolean noPermissions = true;
        for (PluginPermissionType type : PluginPermissionType.values()) {
            PluginPermissionDescriptor pluginPermissionDescriptor = plugin.getPermissionDescriptor(type);
            if (pluginPermissionDescriptor != null) {
                noPermissions = false;

                sb.append("&nbsp;&nbsp;").append(type.name()).append("<br>");
            }
        }

        if (noPermissions) {
            sb.append("&nbsp;&nbsp;<span style=\"color: gray\">");
            XmlStringUtil.escapeText("<no special permissions>", sb);
            sb.append("</span><br>");
        }

        sb.append("<br>");

        String description = plugin.getDescription();
        if (!isEmptyOrSpaces(description)) {
            sb.append(description);
        }
        else {
            sb.append("<span style=\"color: gray\">");
            XmlStringUtil.escapeText("<description not provided>", sb);
            sb.append("</span>");
        }

        String changeNotes = plugin.getChangeNotes();
        if (!isEmptyOrSpaces(changeNotes)) {
            sb.append("<h3>Change Notes</h3>");
            sb.append(changeNotes);
        }

        String vendor = plugin.getVendor();
        String vendorEmail = plugin.getVendorEmail();
        String vendorUrl = plugin.getVendorUrl();
        if (!isEmptyOrSpaces(vendor) || !isEmptyOrSpaces(vendorEmail) || !isEmptyOrSpaces(vendorUrl)) {
            sb.append("<h3>Vendor</h3>");

            if (!isEmptyOrSpaces(vendor)) {
                sb.append("&nbsp;&nbsp;").append(vendor);
            }
            if (!isEmptyOrSpaces(vendorEmail)) {
                sb.append("&nbsp;")
                    .append(HTML_PREFIX)
                    .append("mailto:")
                    .append(vendorEmail)
                    .append("\">")
                    .append(vendorEmail)
                    .append(HTML_SUFFIX);
            }
            if (!isEmptyOrSpaces(vendorUrl)) {
                sb.append("&nbsp;").append(composeHref(vendorUrl));
            }
        }

        String pluginDescriptorUrl = plugin.getUrl();
        if (!isEmptyOrSpaces(pluginDescriptorUrl)) {
            sb.append("<h3>Plugin homepage</h3>").append(composeHref(pluginDescriptorUrl));
        }

        String size = plugin instanceof PluginNode pluginNode ? pluginNode.getSize() : null;
        if (!isEmptyOrSpaces(size)) {
            sb.append("<h3>Size</h3>").append(getFormattedSize(size));
        }

        Map<PluginDescriptor, Boolean> depends = new LinkedHashMap<>();
        for (PluginId pluginId : plugin.getDependentPluginIds()) {
            if (PluginIds.isPlatformPlugin(pluginId)) {
                continue;
            }

            PluginDescriptor temp = findPlugin(allPlugins, pluginId);
            if (temp != null) {
                depends.put(temp, Boolean.FALSE);
            }
        }

        for (PluginId pluginId : plugin.getOptionalDependentPluginIds()) {
            if (PluginIds.isPlatformPlugin(pluginId)) {
                continue;
            }
            PluginDescriptor temp = findPlugin(allPlugins, pluginId);
            if (temp != null) {
                depends.put(temp, Boolean.TRUE);
            }
        }

        if (!depends.isEmpty()) {
            sb.append("<h3>Depends on plugins:</h3>");

            for (Map.Entry<PluginDescriptor, Boolean> entry : depends.entrySet()) {
                PluginDescriptor key = entry.getKey();
                Boolean optional = entry.getValue();

                sb.append("&nbsp;&nbsp;");
                sb.append("<a href=\"").append(PLUGIN_PREFIX).append(key.getPluginId()).append("\">").append(key.getName());
                if (optional) {
                    sb.append("&nbsp;(optional)");
                }
                sb.append("</a>");

                sb.append("<br>");
            }
        }

        Map<PluginId, PluginDescriptor> dependentPlugins = new TreeMap<>();
        for (PluginDescriptor descriptor : allPlugins) {
            if (ArrayUtil.contains(plugin.getPluginId(), descriptor.getDependentPluginIds())) {
                dependentPlugins.put(descriptor.getPluginId(), descriptor);
            }

            if (ArrayUtil.contains(plugin.getPluginId(), descriptor.getOptionalDependentPluginIds())) {
                dependentPlugins.put(descriptor.getPluginId(), descriptor);
            }
        }

        if (!dependentPlugins.isEmpty()) {
            sb.append("<h3>Dependent plugins:</h3>");

            for (PluginDescriptor pluginDescriptor : dependentPlugins.values()) {
                sb.append("&nbsp;&nbsp;");
                sb.append("<a href=\"")
                    .append(PLUGIN_PREFIX)
                    .append(pluginDescriptor.getPluginId())
                    .append("\">")
                    .append(pluginDescriptor.getName());
                sb.append("</a>");
                sb.append("<br>");
            }
        }

        Set<String> tags = plugin.getTags();
        if (!tags.isEmpty()) {
            sb.append("<h3>Tags:</h3>");
            for (String tag : tags) {
                sb.append("&nbsp;&nbsp;").append(PluginTab.getTagLocalizeValue(tag).get()).append("<br>");
            }
        }

        return sb;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public static String getFormattedSize(String size) {
        if (size.equals("-1")) {
            return ExternalServiceLocalize.pluginInfoUnknown().get();
        }
        else if (size.length() >= 4) {
            if (size.length() < 7) {
                size = String.format("%.1f", (float) Integer.parseInt(size) / kByte) + " K";
            }
            else {
                size = String.format("%.1f", (float) Integer.parseInt(size) / mgByte) + " M";
            }
        }
        return size;
    }

    private static @Nullable PluginDescriptor findPlugin(List<PluginDescriptor> allPlugins, PluginId pluginId) {
        return ContainerUtil.find(allPlugins, it -> it.getPluginId() == pluginId);
    }

    private static String composeHref(String vendorUrl) {
        return HTML_PREFIX + vendorUrl + "\">" + vendorUrl + HTML_SUFFIX;
    }
}
