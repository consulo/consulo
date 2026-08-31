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
package consulo.externalService.impl.internal.plugin.ui;

import consulo.application.Application;
import consulo.configurable.Settings;
import consulo.container.plugin.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.impl.internal.plugin.PluginNode;
import consulo.externalService.internal.ExternalServiceHelper;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.platform.Platform;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

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
    private static class MyHyperlinkListener implements HyperlinkListener {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                JEditorPane pane = (JEditorPane) e.getSource();
                if (e instanceof HTMLFrameHyperlinkEvent evt) {
                    HTMLDocument doc = (HTMLDocument) pane.getDocument();
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
                        Platform.current().openInBrowser(url);
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

            data.select(PluginsConfigurableImpl.class).whenComplete((pluginConfigurable, error) -> {
                if (error != null) {
                    return;
                }

                pluginConfigurable.select(pluginId);
            });
        }
    }

    private static final String PLUGIN_PREFIX = PluginDescriptionMarkup.PLUGIN_PREFIX;
    private static final String TEXT_SUFFIX = "</body></html>";

    private final PluginHeaderPanel myPluginHeaderPanel;
    private final JEditorPane myDescriptionTextArea;

    private final JPanel myPanel;

    public PluginDescriptionPanel(@Nullable PluginsPanel pluginsPanel) {
        myPanel = new JPanel(new BorderLayout());

        myPluginHeaderPanel = new PluginHeaderPanel(pluginsPanel);
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
        @Nullable PluginTab installedTab,
        List<PluginDescriptor> allPlugins,
        @Nullable String filter,
        boolean forceInstall
    ) {
        if (plugin == null) {
            setTextValue(null, filter, myDescriptionTextArea);
            myPluginHeaderPanel.getPanel().setVisible(false);
            return;
        }

        myPluginHeaderPanel.update(plugin, installedTab, allPlugins, forceInstall);

        setTextValue(PluginDescriptionMarkup.buildBody(plugin, allPlugins), filter, myDescriptionTextArea);
    }

    private static void setTextValue(@Nullable StringBuilder text, @Nullable String filter, JEditorPane pane) {
        if (text != null) {
            text.insert(0, getTextPrefix());
            text.append(TEXT_SUFFIX);

            ExternalServiceHelper helper = Application.get().getInstance(ExternalServiceHelper.class);
            pane.setText(helper.markup(text.toString(), filter).trim());
            pane.setCaretPosition(0);
        }
        else {
            pane.setText(getTextPrefix() + TEXT_SUFFIX);
        }
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

    
    public JPanel getPanel() {
        return myPanel;
    }
}
