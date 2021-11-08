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

/**
 * @author cdr
 */
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.DataManager;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TitledSeparator;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.ide.plugins.PluginsConfigurable;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class IntentionDescriptionPanel {
  private static final Logger LOG = Logger.getInstance(IntentionDescriptionPanel.class);
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;
  private TitledSeparator myBeforeSeparator;
  private TitledSeparator myAfterSeparator;
  private JPanel myPoweredByPanel;
  private final List<IntentionUsagePanel> myBeforeUsagePanels = new ArrayList<IntentionUsagePanel>();
  private final List<IntentionUsagePanel> myAfterUsagePanels = new ArrayList<IntentionUsagePanel>();

  public void reset(IntentionActionMetaData actionMetaData, String filter) {
    try {
      final TextDescriptor url = actionMetaData.getDescription();
      final String description = url == null ? CodeInsightBundle.message("under.construction.string") : SearchUtil.markup(url.getText(), filter);
      myDescriptionBrowser.setText(description);
      setupPoweredByPanel(actionMetaData);

      showUsages(myBeforePanel, myBeforeSeparator, myBeforeUsagePanels, actionMetaData.getExampleUsagesBefore());
      showUsages(myAfterPanel, myAfterSeparator, myAfterUsagePanels, actionMetaData.getExampleUsagesAfter());

      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myPanel.revalidate();
        }
      });

    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private void setupPoweredByPanel(final IntentionActionMetaData actionMetaData) {
    myPoweredByPanel.removeAll();
    
    PluginId pluginId = actionMetaData == null ? null : actionMetaData.getPluginId();
    if (pluginId == null || PluginIds.isPlatformPlugin(pluginId)) {
      myPoweredByPanel.setVisible(false);
      return;
    }

    JComponent owner;
    final PluginDescriptor pluginDescriptor = PluginManager.findPlugin(pluginId);
    HyperlinkLabel label = new HyperlinkLabel(CodeInsightBundle.message("powered.by.plugin", pluginDescriptor.getName()));
    label.addHyperlinkListener(new HyperlinkListener() {
      @RequiredUIAccess
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        DataContext dataContext = DataManager.getInstance().getDataContext(myPoweredByPanel);

        Settings data = dataContext.getData(Settings.KEY);
        if(data == null) {
          return;
        }

        data.select(PluginsConfigurable.class).doWhenDone((pluginConfigurable) -> pluginConfigurable.selectInstalled(pluginId));
      }
    });
    owner = label;
    myPoweredByPanel.setVisible(true);
    myPoweredByPanel.add(owner, BorderLayout.CENTER);
  }


  public void reset(String intentionCategory) {
    try {
      String text = CodeInsightBundle.message("intention.settings.category.text", intentionCategory);

      myDescriptionBrowser.setText(text);
      setupPoweredByPanel(null);

      URL beforeURL = getClass().getClassLoader().getResource("intentionDescriptions/TemplateGroup/before.txt.template");
      assert beforeURL != null : "no template file. resources are not copied?";
      showUsages(myBeforePanel, myBeforeSeparator, myBeforeUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(beforeURL)});
      URL afterURL = getClass().getClassLoader().getResource("intentionDescriptions/TemplateGroup/after.txt.template");
      assert afterURL != null : "no template file. resources are not copied?";
      showUsages(myAfterPanel, myAfterSeparator, myAfterUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(afterURL)});

      SwingUtilities.invokeLater(() -> myPanel.revalidate());
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void showUsages(final JPanel panel, final TitledSeparator separator, final List<IntentionUsagePanel> usagePanels, @Nullable final TextDescriptor[] exampleUsages) throws IOException {
    GridBagConstraints gb = null;
    boolean reuse = exampleUsages != null && panel.getComponents().length == exampleUsages.length;
    if (!reuse) {
      disposeUsagePanels(usagePanels);
      panel.setLayout(new GridBagLayout());
      panel.removeAll();
      gb = new GridBagConstraints();
      gb.anchor = GridBagConstraints.NORTHWEST;
      gb.fill = GridBagConstraints.BOTH;
      gb.gridheight = GridBagConstraints.REMAINDER;
      gb.gridwidth = 1;
      gb.gridx = 0;
      gb.gridy = 0;
      gb.insets = new Insets(0, 0, 0, 0);
      gb.ipadx = 5;
      gb.ipady = 5;
      gb.weightx = 1;
      gb.weighty = 1;
    }

    if (exampleUsages != null) {
      for (int i = 0; i < exampleUsages.length; i++) {
        final TextDescriptor exampleUsage = exampleUsages[i];
        final String name = exampleUsage.getFileName();
        final FileTypeManagerEx fileTypeManager = FileTypeManagerEx.getInstanceEx();
        final String extension = fileTypeManager.getExtension(name);
        final FileType fileType = fileTypeManager.getFileTypeByExtension(extension);

        IntentionUsagePanel usagePanel;
        if (reuse) {
          usagePanel = (IntentionUsagePanel)panel.getComponent(i);
        }
        else {
          usagePanel = new IntentionUsagePanel();
          usagePanels.add(usagePanel);
        }
        usagePanel.reset(exampleUsage.getText(), fileType);

        if (!reuse) {
          if (i == exampleUsages.length) {
            gb.gridwidth = GridBagConstraints.REMAINDER;
          }
          panel.add(usagePanel, gb);
          gb.gridx++;
        }
      }
    }
    panel.revalidate();
    panel.repaint();
  }

  public JPanel getComponent() {
    return myPanel;
  }

  public void dispose() {
    disposeUsagePanels(myBeforeUsagePanels);
    disposeUsagePanels(myAfterUsagePanels);
  }

  private static void disposeUsagePanels(List<IntentionUsagePanel> usagePanels) {
    for (final IntentionUsagePanel usagePanel : usagePanels) {
      usagePanel.dispose();
    }
    usagePanels.clear();
  }

  public void init(final int preferredWidth) {
    //adjust vertical dimension to be equal for all three panels
    double height = (myDescriptionBrowser.getSize().getHeight() + myBeforePanel.getSize().getHeight() + myAfterPanel.getSize().getHeight()) / 3;
    final Dimension newd = new Dimension(preferredWidth, (int)height);
    myDescriptionBrowser.setSize(newd);
    myDescriptionBrowser.setPreferredSize(newd);
    myDescriptionBrowser.setMaximumSize(newd);
    myDescriptionBrowser.setMinimumSize(newd);

    myBeforePanel.setSize(newd);
    myBeforePanel.setPreferredSize(newd);
    myBeforePanel.setMaximumSize(newd);
    myBeforePanel.setMinimumSize(newd);

    myAfterPanel.setSize(newd);
    myAfterPanel.setPreferredSize(newd);
    myAfterPanel.setMaximumSize(newd);
    myAfterPanel.setMinimumSize(newd);
  }
}  