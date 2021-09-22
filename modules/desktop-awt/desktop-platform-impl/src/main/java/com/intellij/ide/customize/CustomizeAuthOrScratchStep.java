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
package com.intellij.ide.customize;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.UILocalize;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18/09/2021
 */
public class CustomizeAuthOrScratchStep extends AbstractCustomizeWizardStep {
  private enum Column {
    FROM_SCRATCH(UILocalize.newUserCustomizeManager()),
    HUB_USER(UILocalize.hubUserCustomizeManager());

    private final LocalizeValue myRadioText;

    Column(LocalizeValue radioText) {
      myRadioText = radioText;
    }
  }

  public CustomizeAuthOrScratchStep(@Nonnull Runnable nextAction) {
    setLayout(new BorderLayout(10, 10));

    ButtonGroup group = new ButtonGroup();

    JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 5, 5));

    boolean firstSelected = false;
    for (Column key : Column.values()) {
      JRadioButton radioButton = new JBRadioButton(key.myRadioText.get());
      radioButton.setOpaque(false);
      if(!firstSelected) {
        radioButton.setSelected(true);
        firstSelected = true;
      }

      final JPanel panel = createBigButtonPanel(new BorderLayout(10, 10), radioButton, nextAction);

      switch (key) {
        case FROM_SCRATCH:
          JLabel textLabel = new JBLabel("<html><body>I'm new user and<br> want setup <b>Consulo</b><br> from scratch</html></body>", SwingConstants.CENTER);
          textLabel.setFont(new Font(textLabel.getFont().getFontName(), Font.PLAIN, 32));
          panel.add(textLabel, BorderLayout.CENTER);
          break;
        case HUB_USER:
          break;
      }
      panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      panel.add(radioButton, BorderLayout.NORTH);

      group.add(radioButton);
      buttonsPanel.add(panel);
    }

    add(buttonsPanel, BorderLayout.CENTER);
  }

  @Override
  protected String getTitle() {
    return "Welcome";
  }

  @Override
  protected String getHTMLHeader() {
    return "<html><body><h2>Welcome</h2>&nbsp;</body></html>";
  }

  @Override
  protected String getHTMLFooter() {
    return null;
  }
}
