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
package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.externalService.plugin.PluginsConfigurable;
import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.language.internal.FileTypeManagerEx;
import consulo.configurable.Settings;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.ResourceTextDescriptor;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.language.editor.ui.awt.AWTLanguageEditorUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class IntentionDescriptionPanel {
  private static final Logger LOG = Logger.getInstance(IntentionDescriptionPanel.class);
  private JPanel myPanel;

  private JPanel myAfterPanel;
  private JPanel myBeforePanel;
  private JEditorPane myDescriptionBrowser;

  private JPanel myPoweredByPanel;
  private final List<IntentionUsagePanel> myBeforeUsagePanels = new ArrayList<>();
  private final List<IntentionUsagePanel> myAfterUsagePanels = new ArrayList<>();

  public IntentionDescriptionPanel() {
    myPanel = new JPanel(new BorderLayout());
    myPanel.setBorder(JBUI.Borders.emptyLeft(5));
    
    myDescriptionBrowser = new JEditorPane("text/html", "");
    myDescriptionBrowser.setEditable(false);
    myDescriptionBrowser.setBorder(JBUI.Borders.customLine(JBColor.border(), 1));
    myDescriptionBrowser.setFont(AWTLanguageEditorUtil.getEditorFont());

    myPanel.add(new BorderLayoutPanel().withBorder(JBUI.Borders.empty(5)).addToTop(new TitledSeparator("Description")).addToCenter(myDescriptionBrowser), BorderLayout.CENTER);

    myBeforePanel = new JPanel();
    myAfterPanel = new JPanel();
    myPoweredByPanel = new JPanel(new BorderLayout());

    JPanel bottomPanel = new JPanel(new VerticalFlowLayout());
    bottomPanel.add(new TitledSeparator("Before"));
    bottomPanel.add(myBeforePanel);
    bottomPanel.add(new TitledSeparator("Before"));
    bottomPanel.add(myAfterPanel);
    bottomPanel.add(myPoweredByPanel);

    myPanel.add(bottomPanel, BorderLayout.SOUTH);
  }

  public void reset(IntentionActionMetaData actionMetaData, String filter) {
    try {
      TextDescriptor url = actionMetaData.getDescription();
      String description = url == null ? CodeInsightBundle.message("under.construction.string") : SearchUtil.markup(url.getText(), filter);
      myDescriptionBrowser.setText(description);
      setupPoweredByPanel(actionMetaData);

      showUsages(myBeforePanel, myBeforeUsagePanels, actionMetaData.getExampleUsagesBefore());
      showUsages(myAfterPanel, myAfterUsagePanels, actionMetaData.getExampleUsagesAfter());

      SwingUtilities.invokeLater(() -> myPanel.revalidate());
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private void setupPoweredByPanel(IntentionActionMetaData actionMetaData) {
    myPoweredByPanel.removeAll();

    PluginId pluginId = actionMetaData == null ? null : actionMetaData.getPluginId();
    if (pluginId == null || PluginIds.isPlatformPlugin(pluginId)) {
      myPoweredByPanel.setVisible(false);
      return;
    }

    JComponent owner;
    PluginDescriptor pluginDescriptor = PluginManager.findPlugin(pluginId);
    HyperlinkLabel label = new HyperlinkLabel(CodeInsightBundle.message("powered.by.plugin", pluginDescriptor.getName()));
    label.addHyperlinkListener(new HyperlinkListener() {
      @RequiredUIAccess
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        DataContext dataContext = DataManager.getInstance().getDataContext(myPoweredByPanel);

        Settings data = dataContext.getData(Settings.KEY);
        if (data == null) {
          return;
        }

        data.select(PluginsConfigurable.class).doWhenDone((pluginConfigurable) -> pluginConfigurable.selectInstalled(pluginId));
      }
    });
    owner = label;
    myPoweredByPanel.setVisible(true);
    myPoweredByPanel.add(owner, BorderLayout.WEST);
  }


  public void reset(String intentionCategory) {
    try {
      String text = CodeInsightBundle.message("intention.settings.category.text", intentionCategory);

      myDescriptionBrowser.setText(text);
      setupPoweredByPanel(null);

      URL beforeURL = getClass().getClassLoader().getResource("intentionDescriptions/TemplateGroup/before.txt.template");
      assert beforeURL != null : "no template file. resources are not copied?";
      showUsages(myBeforePanel, myBeforeUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(beforeURL)});
      URL afterURL = getClass().getClassLoader().getResource("intentionDescriptions/TemplateGroup/after.txt.template");
      assert afterURL != null : "no template file. resources are not copied?";
      showUsages(myAfterPanel, myAfterUsagePanels, new ResourceTextDescriptor[]{new ResourceTextDescriptor(afterURL)});

      SwingUtilities.invokeLater(() -> myPanel.revalidate());
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private static void showUsages(JPanel panel, List<IntentionUsagePanel> usagePanels, @Nullable TextDescriptor[] exampleUsages) throws IOException {
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
        TextDescriptor exampleUsage = exampleUsages[i];
        String name = exampleUsage.getFileName();
        FileTypeManagerEx fileTypeManager = FileTypeManagerEx.getInstanceEx();
        String extension = fileTypeManager.getExtension(name);
        FileType fileType = fileTypeManager.getFileTypeByExtension(extension);

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
    for (IntentionUsagePanel usagePanel : usagePanels) {
      usagePanel.dispose();
    }
    usagePanels.clear();
  }

  public void init(int preferredWidth) {
    //adjust vertical dimension to be equal for all three panels
    double height = (myDescriptionBrowser.getSize().getHeight() + myBeforePanel.getSize().getHeight() + myAfterPanel.getSize().getHeight()) / 3;
    Dimension newd = new Dimension(preferredWidth, (int)height);
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