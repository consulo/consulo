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
    public static final String TITLE_CLASS = "title";
    public static final String DESCRIPTION_CLASS = "description";

    private static final String SECTION_START = "<div>";
    private static final String SECTION_END = "</div>";

    private static final float mgByte = 1024.0f * 1024.0f;
    private static final float kByte = 1024.0f;

    private PluginDescriptionMarkup() {
    }

    /**
     * The description and the change notes of the plugin.
     */
    public static StringBuilder buildDescription(PluginDescriptor plugin) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"").append(DESCRIPTION_CLASS).append("\">");
        String description = plugin.getDescription();
        if (!isEmptyOrSpaces(description)) {
            sb.append(description);
        }
        else {
            sb.append("<span style=\"color: gray\">");
            XmlStringUtil.escapeText("<description not provided>", sb);
            sb.append("</span>");
        }
        sb.append("</div>");

        String changeNotes = plugin.getChangeNotes();
        if (!isEmptyOrSpaces(changeNotes)) {
            title(sb, "Change Notes");
            sb.append(SECTION_START).append(changeNotes).append(SECTION_END);
        }
        return sb;
    }

    private static void title(StringBuilder sb, String title) {
        sb.append("<div class=\"").append(TITLE_CLASS).append("\">").append(title).append("</div>");
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

}
