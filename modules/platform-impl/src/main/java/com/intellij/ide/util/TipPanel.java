/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.ide.util;

import com.intellij.featureStatistics.FeatureDescriptor;
import com.intellij.featureStatistics.ProductivityFeaturesProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.GeneralSettings;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.UIUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;

public class TipPanel extends JPanel {
  private static final int DEFAULT_WIDTH = 400;
  private static final int DEFAULT_HEIGHT = 200;
  private final JCheckBox myCheckBox;
  private final JEditorPane browser;
  private final ArrayList<String> myTipPaths = new ArrayList<String>();
  private final HashMap<String, Class< ? extends ProductivityFeaturesProvider>> myPathsToProviderMap = new HashMap<String, Class<? extends ProductivityFeaturesProvider>>();
  @NonNls
  private static final String ELEMENT_TIP = "tip";
  @NonNls
  private static final String ATTRIBUTE_FILE = "file";

  public TipPanel() {
    setLayout(new BorderLayout());
    JLabel jlabel = new JLabel(AllIcons.General.Tip);
    jlabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    JLabel jlabel1 = new JLabel(IdeBundle.message("label.did.you.know"));
    Font font = jlabel1.getFont();
    jlabel1.setFont(font.deriveFont(Font.PLAIN, font.getSize() + 4));
    JPanel jpanel = new JPanel();
    jpanel.setLayout(new BorderLayout());
    jpanel.add(jlabel, BorderLayout.WEST);
    jpanel.add(jlabel1, BorderLayout.CENTER);
    jpanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    add(jpanel, BorderLayout.NORTH);
    browser = new JEditorPane();
    browser.setEditable(false);
    browser.setEditorKit(new HTMLEditorKit());
    browser.setBackground(UIUtil.getTextFieldBackground());
    browser.addHyperlinkListener(
      new HyperlinkListener() {
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            //TODO: Open url in browser
          }
        }
      }
    );
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(browser);
    add(scrollPane, BorderLayout.CENTER);
    myCheckBox = new JCheckBox(IdeBundle.message("checkbox.show.tips.on.startup"), true);
    myCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    final GeneralSettings settings = GeneralSettings.getInstance();
    myCheckBox.setSelected(settings.showTipsOnStartup());
    myCheckBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        settings.setShowTipsOnStartup(e.getStateChange() == ItemEvent.SELECTED);
      }
    });
    add(myCheckBox, BorderLayout.SOUTH);
    try {
      readTips("/tips/tips.xml");
      readTips("/tips/IdeSpecificTips.xml");
    }
    catch (Exception exception) {//
    }

    for (ProductivityFeaturesProvider provider : ProductivityFeaturesProvider.EP_NAME.getExtensions()) {
      final FeatureDescriptor[] descriptors = provider.getFeatureDescriptors();
      for (int j = 0; descriptors != null && j < descriptors.length; j++) {
        FeatureDescriptor descriptor = descriptors[j];
        myPathsToProviderMap.put(descriptor.getTipFileName(), descriptor.getProvider());
      }
    }
  }

  public Dimension getPreferredSize() {
    return new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public void prevTip() {
    if (myTipPaths.size() == 0) {
      browser.setText(IdeBundle.message("error.tips.not.found", ApplicationNamesInfo.getInstance().getFullProductName()));
      return;
    }
    final GeneralSettings settings = GeneralSettings.getInstance();
    int lastTip = settings.getLastTip();

    final String path;
    lastTip--;
    if (lastTip <= 0) {
      path = myTipPaths.get(myTipPaths.size() - 1);
      lastTip = myTipPaths.size();
    }
    else {
      path = myTipPaths.get(lastTip - 1);
    }

    setTip(path, lastTip, browser, settings);
  }

  private void setTip (String path, int lastTip, JEditorPane browser, GeneralSettings settings) {
    TipUIUtil.openTipInBrowser(path, browser, myPathsToProviderMap.get(path));
    settings.setLastTip(lastTip);
  }

  public void nextTip() {
    if (myTipPaths.size() == 0) {
      browser.setText(IdeBundle.message("error.tips.not.found", ApplicationNamesInfo.getInstance().getFullProductName()));
      return;
    }
    GeneralSettings settings = GeneralSettings.getInstance();
    int lastTip = settings.getLastTip();
    String path;
    lastTip++;
    if (lastTip - 1 >= myTipPaths.size()) {
      path = myTipPaths.get(0);
      lastTip = 1;
    }
    else {
      path = myTipPaths.get(lastTip - 1);
    }

    setTip(path, lastTip, browser, settings);
  }

  private void readTips(String tipsURL) throws Exception {
    final Document document = JDOMUtil.loadDocument(getClass().getResource(tipsURL).openStream());

    for (Object o : document.getRootElement().getChildren(ELEMENT_TIP)) {
      Element tip = (Element)o;
      myTipPaths.add(tip.getAttributeValue(ATTRIBUTE_FILE));
    }
  }
}
