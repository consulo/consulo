/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.startup.customize;

import consulo.container.plugin.PluginId;
import consulo.ui.ImageBox;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MultiLineLabel;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2014-11-29
 */
public class CustomizePluginTemplatesStepPanel extends AbstractCustomizeWizardStep {
    private static final int COLUMN_COUNT = 2;

    private static class Rows {
        private int nextIndex = 0;

        public int[] next() {
            int idx = nextIndex++;
            return new int[]{idx / COLUMN_COUNT, idx % COLUMN_COUNT};
        }
    }

    @Nonnull
    private final Map<PluginId, PluginTemplate> myPredefinedTemplates;
    @Nonnull
    private Map<PluginId, JCheckBox> mySetBoxes = new HashMap<>();

    public CustomizePluginTemplatesStepPanel(Map<PluginId, PluginTemplate> predefinedTemplates) {
        myPredefinedTemplates = predefinedTemplates;
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());

        int imageIndex = StyleManager.get().getCurrentStyle().isDark() ? 1 : 0;

        Rows rows = new Rows();

        predefinedTemplates
            .values()
            .stream()
            .sorted((o1, o2) -> Integer.compareUnsigned(o2.downloadsAll(), o1.downloadsAll()))
            .forEach(pluginTemplate -> {
                JCheckBox checkBox = new JCheckBox(pluginTemplate.name());
                checkBox.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));
                checkBox.setOpaque(false);

                mySetBoxes.put(pluginTemplate.id(), checkBox);

                JPanel buttonPanel = createBigButtonPanel(new GridBagLayout(), checkBox, true, () -> {
                });

                buttonPanel.setBorder(JBUI.Borders.empty(10));

                Image icon = pluginTemplate.images()[imageIndex];

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridy = 0;
                constraints.insets = JBUI.insets(10, 0);
                buttonPanel.add(TargetAWT.to(ImageBox.create(icon)), constraints);

                constraints.gridy = 1;
                buttonPanel.add(checkBox, constraints);

                JLabel descriptionLabel = new MultiLineLabel(StringUtil.notNullize(pluginTemplate.description()));
                descriptionLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
                descriptionLabel.setForeground(JBColor.gray);

                constraints.gridy = 2;
                buttonPanel.add(descriptionLabel, constraints);

                int[] info = rows.next();

                GridBagConstraints buttonConstraints = new GridBagConstraints();
                buttonConstraints.fill = GridBagConstraints.HORIZONTAL;
                buttonConstraints.gridx = info[1];
                buttonConstraints.gridy = info[0];
                buttonConstraints.weightx = 1;
                buttonConstraints.weighty = 1;
                buttonConstraints.anchor = GridBagConstraints.NORTHWEST;

                panel.add(buttonPanel, buttonConstraints);
            });

        JScrollPane pane = ScrollPaneFactory.createScrollPane(panel, true);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(pane, BorderLayout.CENTER);
    }

    @Nonnull
    public Set<PluginId> getEnablePluginSet() {
        Set<PluginId> set = new HashSet<>();
        for (Map.Entry<PluginId, JCheckBox> entry : mySetBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                set.add(entry.getKey());

                PluginTemplate template = myPredefinedTemplates.get(entry.getKey());

                set.addAll(template.pluginIds());
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
