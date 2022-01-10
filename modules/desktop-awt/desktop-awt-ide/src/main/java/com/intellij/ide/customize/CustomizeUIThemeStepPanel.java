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
package com.intellij.ide.customize;

import com.intellij.CommonBundle;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import consulo.awt.TargetAWT;
import consulo.ide.ui.laf.LafWithColorScheme;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.migration.impl.AWTIconLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomizeUIThemeStepPanel extends AbstractCustomizeWizardStep {
  private static final String LIGHT = "Light";
  private static final String DARK = "Dark";
  private boolean myColumnMode;
  private Map<String, Image> myLafNames = new LinkedHashMap<>();

  public CustomizeUIThemeStepPanel() {
    setLayout(new BorderLayout(10, 10));

    myLafNames.put(LIGHT, AWTIconLoader.INSTANCE.getIcon("/icon/_light/consulo.platform.base.PlatformIconGroup/lafs/LightPreview.png", PlatformIconGroup.class));
    myLafNames.put(DARK, AWTIconLoader.INSTANCE.getIcon("/icon/_light/consulo.platform.base.PlatformIconGroup/lafs/DarkPreview.png", PlatformIconGroup.class));

    myColumnMode = true;
    JPanel buttonsPanel = new JPanel(new GridLayout(myLafNames.size(), 1, 5, 5));
    ButtonGroup group = new ButtonGroup();
    String defaultLafName = null;

    for (Map.Entry<String, Image> entry : myLafNames.entrySet()) {
      final String lafName = entry.getKey();
      Image icon = entry.getValue();
      final JRadioButton radioButton = new JRadioButton(lafName, defaultLafName == null);
      radioButton.setOpaque(false);
      if (defaultLafName == null) {
        radioButton.setSelected(true);
        defaultLafName = lafName;
      }
      final JPanel panel = createBigButtonPanel(new BorderLayout(10, 10), radioButton, () -> applyLaf(lafName, CustomizeUIThemeStepPanel.this));
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(radioButton, BorderLayout.NORTH);
      final JLabel label = new JLabel(TargetAWT.to(icon)) {
        @Override
        public Dimension getPreferredSize() {
          Dimension size = super.getPreferredSize();
          if (myColumnMode) size.width *=2;
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

  private void applyLaf(String lafName, Component component) {
    UIManager.LookAndFeelInfo info = getLookAndFeelInfo(lafName);
    if (info == null) return;

    LafManager.getInstance().setCurrentLookAndFeel(info);

    LafManager.getInstance().setCurrentLookAndFeel(info);
    if (info instanceof LafWithColorScheme) {
      EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
      EditorColorsScheme scheme = editorColorsManager.getScheme(((LafWithColorScheme)info).getColorSchemeName());
      if (scheme != null) {
        editorColorsManager.setGlobalScheme(scheme);
      }
    }
  }

  @Nullable
  private static UIManager.LookAndFeelInfo getLookAndFeelInfo(@Nonnull String name) {
    for (UIManager.LookAndFeelInfo lookAndFeelInfo : LafManager.getInstance().getInstalledLookAndFeels()) {
      if(name.equals(lookAndFeelInfo.getName())) {
        return lookAndFeelInfo;
      }
    }
    return null;
  }
}
