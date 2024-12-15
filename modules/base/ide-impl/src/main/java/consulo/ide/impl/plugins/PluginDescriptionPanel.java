/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.plugins;

import consulo.container.plugin.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.webBrowser.BrowserUtil;
import consulo.ide.impl.idea.ide.plugins.PluginHeaderPanel;
import consulo.ide.impl.idea.ide.plugins.PluginManagerColumnInfo;
import consulo.ide.impl.idea.ide.plugins.PluginManagerMain;
import consulo.ide.impl.idea.ide.plugins.PluginNode;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.impl.localize.PluginLocalize;
import consulo.ide.setting.Settings;
import consulo.ui.ex.awt.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.*;

import static consulo.util.lang.StringUtil.isEmptyOrSpaces;

/**
 * @author VISTALL
 * @since 2021-11-08
 */
public class PluginDescriptionPanel {
    // Repository does not support rating. Disable stars for now.
    public static final boolean ENABLED_STARS = false;

    private static class MyHyperlinkListener implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane)e.getSource();
                if (e instanceof HTMLFrameHyperlinkEvent evt) {
                    HTMLDocument doc = (HTMLDocument)pane.getDocument();
                    doc.processHTMLFrameHyperlinkEvent(evt);
                }
                else {
                    String description = e.getDescription();
                    if (description.startsWith(PLUGIN_PREFIX)) {
                        String pluginId = description.substring(PLUGIN_PREFIX.length(), description.length());
                        select(pane, PluginId.getId(pluginId));
                        return;
                    }

                    URL url = e.getURL();
                    if (url != null) {
                        BrowserUtil.browse(url);
                    }
                }
            }
        }

        private void select(JEditorPane pane, PluginId pluginId) {
            DataContext dataContext = DataManager.getInstance().getDataContext(pane);

            Settings data = dataContext.getData(Settings.KEY);
            if (data == null) {
                return;
            }

            data.select(PluginsConfigurable.class).doWhenDone((pluginConfigurable) -> pluginConfigurable.select(pluginId));
        }
    }

    private static final String PLUGIN_PREFIX = "plugin://";
    private static final String TEXT_SUFFIX = "</body></html>";
    private static final String HTML_PREFIX = "<a href=\"";
    private static final String HTML_SUFFIX = "</a>";

    private final PluginHeaderPanel myPluginHeaderPanel;
    private final JEditorPane myDescriptionTextArea;

    private final JPanel myPanel;

    public PluginDescriptionPanel() {
        myPanel = new JPanel(new BorderLayout());
        myPanel.setBackground(MorphColor.of(UIUtil::getTextFieldBackground));

        myPluginHeaderPanel = new PluginHeaderPanel();
        myPluginHeaderPanel.getPanel().setOpaque(false);
        myPluginHeaderPanel.getPanel().setBorder(JBUI.Borders.empty(5, 5, 0, 5));

        myPanel.add(myPluginHeaderPanel.getPanel(), BorderLayout.NORTH);

        myDescriptionTextArea = new JEditorPane("text/html", "");
        myDescriptionTextArea.setEditorKit(JBHtmlEditorKit.create());
        myDescriptionTextArea.setEditable(false);
        myDescriptionTextArea.addHyperlinkListener(new MyHyperlinkListener());
        myDescriptionTextArea.setOpaque(false);

        myPanel.add(ScrollPaneFactory.createScrollPane(myDescriptionTextArea, true), BorderLayout.CENTER);
    }

    public void update(
        @Nullable PluginDescriptor plugin,
        @Nullable PluginManagerMain manager,
        @Nonnull List<PluginDescriptor> allPlugins,
        @Nullable String filter
    ) {
        if (plugin == null) {
            setTextValue(null, filter, myDescriptionTextArea);
            myPluginHeaderPanel.getPanel().setVisible(false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        myPluginHeaderPanel.update(plugin, manager);

        sb.append("<h3>")
            .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionVersionLabel().get()))
            .append("</h3>")
            .append("&nbsp;&nbsp;").append(StringUtil.notNullize(plugin.getVersion(), "N/A"));

        if (PluginIds.isPlatformPlugin(plugin.getPluginId())) {
            setTextValue(sb, filter, myDescriptionTextArea);
            return;
        }

        sb.append("<h3>")
            .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionPermissionsLabel().get()))
            .append("</h3>");
        boolean noPermissions = true;
        for (PluginPermissionType type : PluginPermissionType.values()) {
            PluginPermissionDescriptor pluginPermissionDescriptor = plugin.getPermissionDescriptor(type);
            if (pluginPermissionDescriptor != null) {
                noPermissions = false;

                sb.append("&nbsp;&nbsp;").append(type.name()).append("<br>");
            }
        }

        if (noPermissions) {
            sb.append("&nbsp;&nbsp;<span style=\"color: gray\">")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionPermissionsNotSet().get()))
                .append("</span><br>");
        }

        sb.append("<br>");

        String description = plugin.getDescription();
        if (!isEmptyOrSpaces(description)) {
            sb.append(description);
        }
        else {
            sb.append("<span style=\"color: gray\">")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionDescriptionNotSet().get()))
                .append("</span>");
        }

        String changeNotes = plugin.getChangeNotes();
        if (!isEmptyOrSpaces(changeNotes)) {
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionChangeNotesLabel().get()))
                .append("</h3>")
                .append(changeNotes);
        }

        String vendor = plugin.getVendor();
        String vendorEmail = plugin.getVendorEmail();
        String vendorUrl = plugin.getVendorUrl();
        if (!isEmptyOrSpaces(vendor) || !isEmptyOrSpaces(vendorEmail) || !isEmptyOrSpaces(vendorUrl)) {
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionVendorLabel().get()))
                .append("</h3>");

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
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionHomepageLabel().get()))
                .append("</h3>")
                .append(composeHref(pluginDescriptorUrl));
        }

        String size = plugin instanceof PluginNode pluginNode ? pluginNode.getSize() : null;
        if (!isEmptyOrSpaces(size)) {
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionSizeLabel().get()))
                .append("</h3>")
                .append(PluginManagerColumnInfo.getFormattedSize(size));
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
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionDependsOnLabel().get()))
                .append("</h3>");

            for (Map.Entry<PluginDescriptor, Boolean> entry : depends.entrySet()) {
                PluginDescriptor key = entry.getKey();
                Boolean optional = entry.getValue();

                sb.append("&nbsp;&nbsp;");
                sb.append("<a href=\"").append(PLUGIN_PREFIX).append(key.getPluginId()).append("\">").append(key.getName());
                if (optional) {
                    sb.append("&nbsp;").append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionDependsOnOptional().get()));
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
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionDependentLabel().get()))
                .append("</h3>");

            for (PluginDescriptor pluginDescriptor : dependentPlugins.values()) {
                sb.append("&nbsp;&nbsp;");
                sb.append("<a href=\"")
                    .append("plugin://")
                    .append(pluginDescriptor.getPluginId())
                    .append("\">")
                    .append(XmlStringUtil.escapeString(pluginDescriptor.getName()));
                sb.append("</a>");
                sb.append("<br>");
            }
        }

        Set<String> tags = plugin.getTags();
        if (!tags.isEmpty()) {
            sb.append("<h3>")
                .append(XmlStringUtil.escapeString(PluginLocalize.pluginDescriptionTagsLabel().get()))
                .append("</h3>");
            for (String tag : tags) {
                sb.append("&nbsp;&nbsp;").append(PluginManagerMain.getTagLocalizeValue(tag).get()).append("<br>");
            }
        }

        setTextValue(sb, filter, myDescriptionTextArea);
    }

    @Nullable
    private static PluginDescriptor findPlugin(@Nonnull List<PluginDescriptor> allPlugins, @Nonnull PluginId pluginId) {
        return ContainerUtil.find(allPlugins, it -> it.getPluginId() == pluginId);
    }

    private static void setTextValue(@Nullable StringBuilder text, @Nullable String filter, JEditorPane pane) {
        if (text != null) {
            text.insert(0, getTextPrefix());
            text.append(TEXT_SUFFIX);
            pane.setText(SearchUtil.markup(text.toString(), filter).trim());
            pane.setCaretPosition(0);
        }
        else {
            pane.setText(getTextPrefix() + TEXT_SUFFIX);
        }
    }

    private static String composeHref(String vendorUrl) {
        return HTML_PREFIX + vendorUrl + "\">" + vendorUrl + HTML_SUFFIX;
    }

    private static String getTextPrefix() {
        String string = "<html><head>\n" +
            "    <style type=\"text/css\">\n" +
            "        p {\n font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx\n" +
            "        }\n" +
            "    </style>\n" +
            "</head><body style=\"font-family: Arial,serif; font-size: %dpt; margin: %dpx %dpx;\">";
        int font = JBUI.scale(12);
        int margin5 = JBUI.scale(5);
        int margin2 = JBUI.scale(2);
        return String.format(string, font, margin2, 0, font, 0, margin5);
    }

    @Nonnull
    public JPanel getPanel() {
        return myPanel;
    }
}
