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
package consulo.ide.plugins;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.PluginHeaderPanel;
import com.intellij.ide.plugins.PluginManagerColumnInfo;
import com.intellij.ide.plugins.PluginNode;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import java.awt.*;
import java.net.URL;

import static consulo.util.lang.StringUtil.isEmptyOrSpaces;

/**
 * @author VISTALL
 * @since 08/11/2021
 */
public class PluginDescriptionPanel {
  private static class MyHyperlinkListener implements HyperlinkListener {
    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        JEditorPane pane = (JEditorPane)e.getSource();
        if (e instanceof HTMLFrameHyperlinkEvent) {
          HTMLFrameHyperlinkEvent evt = (HTMLFrameHyperlinkEvent)e;
          HTMLDocument doc = (HTMLDocument)pane.getDocument();
          doc.processHTMLFrameHyperlinkEvent(evt);
        }
        else {
          URL url = e.getURL();
          if (url != null) {
            BrowserUtil.browse(url);
          }
        }
      }
    }
  }

  private static final String TEXT_SUFFIX = "</body></html>";
  private static final String HTML_PREFIX = "<a href=\"";
  private static final String HTML_SUFFIX = "</a>";

  private final PluginHeaderPanel myPluginHeaderPanel;
  private final JEditorPane myDescriptionTextArea;

  private final JPanel myPanel;

  public PluginDescriptionPanel() {
    myPanel = new JPanel(new BorderLayout());
    myPanel.setBackground(UIUtil.getTextFieldBackground());

    myPluginHeaderPanel = new PluginHeaderPanel(null);
    myPluginHeaderPanel.getPanel().setBackground(UIUtil.getTextFieldBackground());
    myPluginHeaderPanel.getPanel().setOpaque(true);
    myPluginHeaderPanel.getPanel().setBorder(JBUI.Borders.empty(5));

    myPanel.add(myPluginHeaderPanel.getPanel(), BorderLayout.NORTH);

    myDescriptionTextArea = new JEditorPane("text/html", "");
    myDescriptionTextArea.setEditorKit(UIUtil.getHTMLEditorKit());
    myDescriptionTextArea.setEditable(false);
    myDescriptionTextArea.addHyperlinkListener(new MyHyperlinkListener());
    myDescriptionTextArea.setBackground(UIUtil.getTextFieldBackground());

    myPanel.add(ScrollPaneFactory.createScrollPane(myDescriptionTextArea, true), BorderLayout.CENTER);
  }

  public void setPlugin(@Nullable PluginDescriptor plugin, @Nullable String filter) {
    pluginInfoUpdate(plugin, filter, myDescriptionTextArea, myPluginHeaderPanel);
  }

  private static void pluginInfoUpdate(PluginDescriptor plugin, @Nullable String filter, @Nonnull JEditorPane descriptionTextArea, @Nonnull PluginHeaderPanel header) {
    if (plugin == null) {
      setTextValue(null, filter, descriptionTextArea);
      header.getPanel().setVisible(false);
      return;
    }
    StringBuilder sb = new StringBuilder();
    header.setPlugin(plugin);
    String description = plugin.getDescription();
    if (!isEmptyOrSpaces(description)) {
      sb.append(description);
    }

    String changeNotes = plugin.getChangeNotes();
    if (!isEmptyOrSpaces(changeNotes)) {
      sb.append("<h4>Change Notes</h4>");
      sb.append(changeNotes);
    }

    if (!PluginIds.isPlatformPlugin(plugin.getPluginId())) {
      String vendor = plugin.getVendor();
      String vendorEmail = plugin.getVendorEmail();
      String vendorUrl = plugin.getVendorUrl();
      if (!isEmptyOrSpaces(vendor) || !isEmptyOrSpaces(vendorEmail) || !isEmptyOrSpaces(vendorUrl)) {
        sb.append("<h4>Vendor</h4>");

        if (!isEmptyOrSpaces(vendor)) {
          sb.append(vendor);
        }
        if (!isEmptyOrSpaces(vendorUrl)) {
          sb.append("<br>").append(composeHref(vendorUrl));
        }
        if (!isEmptyOrSpaces(vendorEmail)) {
          sb.append("<br>").append(HTML_PREFIX).append("mailto:").append(vendorEmail).append("\">").append(vendorEmail).append(HTML_SUFFIX);
        }
      }

      String pluginDescriptorUrl = plugin.getUrl();
      if (!isEmptyOrSpaces(pluginDescriptorUrl)) {
        sb.append("<h4>Plugin homepage</h4>").append(composeHref(pluginDescriptorUrl));
      }

      String size = plugin instanceof PluginNode ? ((PluginNode)plugin).getSize() : null;
      if (!isEmptyOrSpaces(size)) {
        sb.append("<h4>Size</h4>").append(PluginManagerColumnInfo.getFormattedSize(size));
      }
    }

    setTextValue(sb, filter, descriptionTextArea);
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
    return String.format(string, font, margin2, margin2, font, margin5, margin5);
  }

  @Nonnull
  public JPanel getPanel() {
    return myPanel;
  }
}
