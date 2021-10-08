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

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.SeparatorWithText;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
import consulo.externalService.impl.action.LoginAction;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18/09/2021
 */
public class CustomizeAuthOrScratchStep extends AbstractCustomizeWizardStep {
  public CustomizeAuthOrScratchStep(@Nonnull Runnable nextAction) {
    setLayout(new BorderLayout(10, 10));

    JPanel verticalGroup = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 10, 25, true, false));

    JLabel textLabel = new JBLabel("<html><body>I'm new user and want setup <b>Consulo</b> from scratch</html></body>", SwingConstants.CENTER);
    textLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));

    verticalGroup.add(textLabel);
    verticalGroup.add(new SeparatorWithText("or"));

    JPanel hubPanel = new JPanel(new HorizontalLayout(10, SwingConstants.CENTER));
    JLabel hubLabel = new JLabel("<html><body>Already use Hub, and want setup <b>Consulo</b> from it</html></body>");
    hubLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));

    hubPanel.add(hubLabel, HorizontalLayout.CENTER);
    JButton loginButton = new JButton("Login");
    loginButton.addActionListener(e -> LoginAction.callLogin());
    hubPanel.add(loginButton, HorizontalLayout.CENTER);
    verticalGroup.add(hubPanel);

    add(verticalGroup, BorderLayout.CENTER);
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
