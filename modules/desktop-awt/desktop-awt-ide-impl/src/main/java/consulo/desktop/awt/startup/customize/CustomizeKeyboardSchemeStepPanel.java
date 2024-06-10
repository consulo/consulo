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

import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.ide.impl.idea.openapi.keymap.impl.DefaultKeymap;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CustomizeKeyboardSchemeStepPanel extends AbstractCustomizeWizardStep {
  private boolean myInitial = true;

  public CustomizeKeyboardSchemeStepPanel() {
    setLayout(new GridLayout(1, 2, GAP, GAP));
    final JRadioButton macRadioButton =
            new JRadioButton(LocalizeValue.localizeTODO("I've never used " + Application.get().getName()).get());
    macRadioButton.setOpaque(false);
    JPanel macPanel = createBigButtonPanel(new VerticalFlowLayout(), macRadioButton, () -> applyKeymap(KeymapManager.MAC_OS_X_10_5_PLUS_KEYMAP));
    String style = "<style type=\"text/css\">" +
                   "body {margin-left:"+ GAP +"px; border:none;padding:0px;}"+
                   "table {margin:0px; cell-padding:0px; border:none;}"+
                   "</style>";

    macPanel.add(macRadioButton);
    macPanel.add(new JLabel("<html><head>" + style + "</head><body><h3>" + KeymapManager.MAC_OS_X_10_5_PLUS_KEYMAP + " keymap</h3>" +
                            "Adapted for Mac<br><br><table><tr><td align=\"left\" colspan=\"2\">EXAMPLES</td></tr>" +
                            "<tr><td style=\"text-align:right;\">&#8984;N</td><td style=\"text-align:left;\">Generate</td></tr>" +
                            "<tr><td style=\"text-align:right;\">&#8984;O</td><td style=\"text-align:left;\">Go to class</td></tr>" +
                            "<tr><td style=\"text-align:right;\">&#8984;&#9003;</td><td style=\"text-align:left;\">Delete line</td></tr>" +
                            "</table></body></html>"));

    add(macPanel);
    final JRadioButton defaultRadioButton =
            new JRadioButton(LocalizeValue.localizeTODO("I used " + Application.get().getName()) + " before");
    defaultRadioButton.setOpaque(false);
    JPanel defaultPanel = createBigButtonPanel(new VerticalFlowLayout(), defaultRadioButton, () -> applyKeymap(KeymapManager.MAC_OS_X_KEYMAP));
    defaultPanel.add(defaultRadioButton);
    defaultPanel.add(new JLabel("<html><head>" + style + "</head><body><h3>" + KeymapManager.MAC_OS_X_KEYMAP + " keymap</h3>" +
                                "Default for all platforms<br><br><table><tr><td align=\"left\" colspan=\"2\">EXAMPLES</td></tr>" +
                                "<tr><td style=\"text-align:right;\">^N</td><td style=\"text-align:left;\">Generate</td></tr>" +
                                "<tr><td style=\"text-align:right;\">&#8984;N</td><td style=\"text-align:left;\">Go to class</td></tr>" +
                                "<tr><td style=\"text-align:right;\">&#8984;Y</td><td style=\"text-align:left;\">Delete line</td></tr>" +
                                "</table></body></html>"));

    add(macPanel);
    add(defaultPanel);
    ButtonGroup group = new ButtonGroup();
    group.add(macRadioButton);
    group.add(defaultRadioButton);
    defaultRadioButton.setSelected(true);
    myInitial = false;
  }

  private void applyKeymap(@Nonnull String keymapName) {
    if(myInitial) {
      return;
    }

    KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    DefaultKeymap defaultKeymap = DefaultKeymap.getInstance();
    List<Keymap> keymaps = defaultKeymap.getKeymaps();
    for (Keymap keymap : keymaps) {
      if (keymapName.equals(keymap.getName())) {
        keymapManager.setActiveKeymap(keymap);
      }
    }
  }

  @Override
  public String getTitle() {
    return "Keymaps";
  }

  @Override
  public String getHTMLHeader() {
    return "<html><body><h2>Select keymap scheme</h2>&nbsp;</body></html>";
  }

  @Override
  public String getHTMLFooter() {
    return "Keymap scheme can be later changed in " + CommonBundle.settingsTitle() + " | Keymap";
  }
}
