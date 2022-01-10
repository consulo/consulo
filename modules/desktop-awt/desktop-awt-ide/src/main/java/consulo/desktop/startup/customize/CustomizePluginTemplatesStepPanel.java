/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.startup.customize;

import com.intellij.ide.customize.AbstractCustomizeWizardStep;
import com.intellij.openapi.ui.ex.MultiLineLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * @author VISTALL
 * @since 29.11.14
 */
public class CustomizePluginTemplatesStepPanel extends AbstractCustomizeWizardStep {
  @Nonnull
  private final Map<String, PluginTemplate> myPredefinedTemplates;
  @Nonnull
  private Map<String, JCheckBox> mySetBoxes = new HashMap<>();

  public CustomizePluginTemplatesStepPanel(Map<String, PluginTemplate> predefinedTemplates) {
    myPredefinedTemplates = predefinedTemplates;
    setLayout(new BorderLayout());

    JPanel panel = new JPanel(new GridBagLayout());

    for (Map.Entry<String, PluginTemplate> entry : predefinedTemplates.entrySet()) {
      JCheckBox checkBox = new JCheckBox(entry.getKey());
      checkBox.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
      checkBox.setOpaque(false);

      mySetBoxes.put(entry.getKey(), checkBox);

      JPanel buttonPanel = createBigButtonPanel(new GridBagLayout(), checkBox, true, () -> {
      });

      buttonPanel.setBorder(JBUI.Borders.empty(10));

      PluginTemplate pluginTemplate = entry.getValue();
      Image icon = pluginTemplate.getImage();

      GridBagConstraints constraints = new GridBagConstraints();
      constraints.gridy = 0;
      constraints.insets = JBUI.insets(10, 0);
      buttonPanel.add(TargetAWT.to(ImageBox.create(icon)), constraints);

      constraints.gridy = 1;
      buttonPanel.add(checkBox, constraints);

      JLabel descriptionLabel = new MultiLineLabel(StringUtil.notNullize(pluginTemplate.getDescription()));
      descriptionLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
      descriptionLabel.setForeground(JBColor.gray);

      constraints.gridy = 2;
      buttonPanel.add(descriptionLabel, constraints);

      GridBagConstraints buttonConstraints = new GridBagConstraints();
      buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
      buttonConstraints.gridx = pluginTemplate.getCol();
      buttonConstraints.gridy = pluginTemplate.getRow();
      buttonConstraints.weightx = 1;
      buttonConstraints.weighty = 1;
      buttonConstraints.anchor = GridBagConstraints.NORTHWEST;

      panel.add(buttonPanel, buttonConstraints);
    }

    JScrollPane pane = ScrollPaneFactory.createScrollPane(panel);
    pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    add(pane, BorderLayout.CENTER);
  }

  @Nonnull
  public Set<String> getEnablePluginSet() {
    Set<String> set = new HashSet<>();
    for (Map.Entry<String, JCheckBox> entry : mySetBoxes.entrySet()) {
        if(entry.getValue().isSelected()) {
          PluginTemplate template = myPredefinedTemplates.get(entry.getKey());

          set.addAll(template.getPluginIds());
        }
    }
    return set;
  }

  @Override
  protected String getTitle() {
    return "Predefined Plugin Sets";
  }

  @Override
  protected String getHTMLHeader() {
    return "<html><body><h2>Select predefined plugin sets</h2></body></html>";
  }

  @Override
  protected String getHTMLFooter() {
    return null;
  }
}
