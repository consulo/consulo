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
import consulo.ide.ui.laf.LafWithColorScheme;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomizeUIThemeStepPanel extends AbstractCustomizeWizardStep {

  private static final String DARCULA = "Darcula";
  private static final String INTELLIJ = "IntelliJ";
  private boolean myInitial = true;
  private boolean myColumnMode;
  private JLabel myPreviewLabel;
  private Map<String, Icon> myLafNames = new LinkedHashMap<String, Icon>();

  public CustomizeUIThemeStepPanel() {
    setLayout(new BorderLayout(10, 10));
    IconLoader.activate();

    if (SystemInfo.isMac) {
      myLafNames.put(INTELLIJ, IconLoader.getIcon("/lafs/OSXAqua.png"));
      myLafNames.put(DARCULA, IconLoader.getIcon("/lafs/OSXDarcula.png"));
    }
    else if (SystemInfo.isWindows) {
      myLafNames.put(INTELLIJ, IconLoader.getIcon("/lafs/WindowsIntelliJ.png"));
      myLafNames.put(DARCULA, IconLoader.getIcon("/lafs/WindowsDarcula.png"));
    }
    else {
      myLafNames.put(INTELLIJ, IconLoader.getIcon("/lafs/LinuxIntelliJ.png"));
      myLafNames.put(DARCULA, IconLoader.getIcon("/lafs/LinuxDarcula.png"));
    }
    myColumnMode = myLafNames.size() > 2;
    JPanel buttonsPanel = new JPanel(new GridLayout(myColumnMode ? myLafNames.size() : 1, myColumnMode ? 1 : myLafNames.size(), 5, 5));
    ButtonGroup group = new ButtonGroup();
    String defaultLafName = null;

    for (Map.Entry<String, Icon> entry : myLafNames.entrySet()) {
      final String lafName = entry.getKey();
      Icon icon = entry.getValue();
      final JRadioButton radioButton = new JRadioButton(lafName, defaultLafName == null);
      radioButton.setOpaque(false);
      if (defaultLafName == null) {
        radioButton.setSelected(true);
        defaultLafName = lafName;
      }
      final JPanel panel = createBigButtonPanel(new BorderLayout(10, 10), radioButton, new Runnable() {
        @Override
        public void run() {
          applyLaf(lafName, CustomizeUIThemeStepPanel.this);
        }
      });
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(radioButton, BorderLayout.NORTH);
      final JLabel label = new JLabel(myColumnMode ? IconUtil.scale(icon, .2) : icon) {
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
    myPreviewLabel = new JLabel();
    myPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
    myPreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
    if (myColumnMode) {
      add(buttonsPanel, BorderLayout.WEST);
      add(myPreviewLabel, BorderLayout.CENTER);
    }
    applyLaf(defaultLafName, this);
    myInitial = false;
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
    try {
      UIManager.setLookAndFeel(info.getClassName());

      if (!myInitial) {
        LafManager.getInstance().setCurrentLookAndFeel(info);
        if(info instanceof LafWithColorScheme) {
          EditorColorsManager editorColorsManager = EditorColorsManager.getInstance();
          EditorColorsScheme scheme = editorColorsManager.getScheme(((LafWithColorScheme)info).getColorSchemeName());
          if(scheme != null) {
            editorColorsManager.setGlobalScheme(scheme);
          }
        }
      }
      Window window = SwingUtilities.getWindowAncestor(component);
      if (window != null) {
        window.setBackground(new Color(UIUtil.getPanelBackground().getRGB()));
        SwingUtilities.updateComponentTreeUI(window);
      }
      if (myColumnMode) {
        myPreviewLabel.setIcon(myLafNames.get(lafName));
        myPreviewLabel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Label.foreground")));
      }
    }
    catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
      e.printStackTrace();
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
