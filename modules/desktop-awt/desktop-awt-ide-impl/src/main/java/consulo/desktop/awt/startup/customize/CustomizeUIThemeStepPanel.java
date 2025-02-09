/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.application.CommonBundle;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomizeUIThemeStepPanel extends AbstractCustomizeWizardStep {
    private boolean myColumnMode;

    public CustomizeUIThemeStepPanel(boolean darkTheme) {
        setLayout(new BorderLayout(10, 10));

        Map<String, Image> previewImages = new LinkedHashMap<>();

        previewImages.put(Style.LIGHT_ID, PlatformIconGroup.lafsLightpreview());
        previewImages.put(Style.DARK_ID, PlatformIconGroup.lafsDarkpreview());

        myColumnMode = true;

        JPanel buttonsPanel = new JPanel(new GridLayout(previewImages.size(), 1, 5, 5));
        ButtonGroup group = new ButtonGroup();
        String defaultStyleId = darkTheme ? Style.DARK_ID : Style.LIGHT_ID;

        for (Style style : StyleManager.get().getStyles()) {
            Image image = previewImages.get(style.getId());
            if (image == null) {
                continue;
            }

            final JRadioButton radioButton = new JRadioButton(style.getName(), defaultStyleId.equals(style.getId()));
            radioButton.setOpaque(false);

            final JPanel panel = createBigButtonPanel(
                new BorderLayout(10, 10),
                radioButton,
                () -> applyStyle(style)
            );
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(radioButton, BorderLayout.NORTH);
            final JLabel label = new JLabel(TargetAWT.to(image)) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    if (myColumnMode) {
                        size.width *= 2;
                    }
                    return size;
                }
            };
            label.setVerticalAlignment(SwingConstants.TOP);
            panel.add(label, BorderLayout.CENTER);

            group.add(radioButton);
            buttonsPanel.add(panel);
        }

        add(buttonsPanel, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width += 30;
        return size;
    }


    @Override
    public String getTitle() {
        return "UI Themes";
    }

    @Override
    public String getHTMLHeader() {
        return "<html><body><h2>Set UI theme</h2>&nbsp;</body></html>";
    }

    @Override
    public String getHTMLFooter() {
        return "UI theme can be changed later in " + CommonBundle.settingsTitle() + " | Appearance";
    }

    @RequiredUIAccess
    private void applyStyle(Style style) {
        StyleManager styleManager = StyleManager.get();

        UIAccess uiAccess = UIAccess.current();

        styleManager.setCurrentStyle(style);

        uiAccess.give(styleManager::refreshUI);
    }
}
