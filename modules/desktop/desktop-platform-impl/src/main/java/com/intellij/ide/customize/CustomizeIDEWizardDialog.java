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

import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.JBCardLayout;
import com.intellij.util.containers.MultiMap;
import consulo.container.plugin.PluginDescriptor;
import consulo.desktop.startup.customize.CustomizeDownloadAndStartStepPanel;
import consulo.desktop.startup.customize.CustomizePluginTemplatesStepPanel;
import consulo.desktop.startup.customize.PluginTemplate;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomizeIDEWizardDialog extends DialogWrapper implements ActionListener {
  private static final String BUTTONS = "BUTTONS";
  private static final String NOBUTTONS = "NOBUTTONS";
  private final JButton mySkipButton = new JButton("Skip All");
  private final JButton myBackButton = new JButton("Back");
  private final JButton myNextButton = new JButton("Next");

  private final JBCardLayout myCardLayout = new JBCardLayout();
  protected final List<AbstractCustomizeWizardStep> mySteps = new ArrayList<AbstractCustomizeWizardStep>();
  private final MultiMap<String, PluginDescriptor> myPluginDescriptors;
  private final Map<String, PluginTemplate> myPredefinedTemplateSets;
  private int myIndex = 0;
  private final JLabel myNavigationLabel = new JLabel();
  private final JLabel myHeaderLabel = new JLabel();
  private final JLabel myFooterLabel = new JLabel();
  private final CardLayout myButtonWrapperLayout = new CardLayout();
  private final JPanel myButtonWrapper = new JPanel(myButtonWrapperLayout);
  private JPanel myContentPanel;

  public CustomizeIDEWizardDialog(MultiMap<String, PluginDescriptor> pluginDescriptors, Map<String, PluginTemplate> predefinedTemplateSets) {
    super(null);
    myPluginDescriptors = pluginDescriptors;
    myPredefinedTemplateSets = predefinedTemplateSets;
    setTitle("Customize " + ApplicationNamesInfo.getInstance().getProductName());
    initSteps();
    mySkipButton.addActionListener(this);
    myBackButton.addActionListener(this);
    myNextButton.addActionListener(this);
    myNavigationLabel.setEnabled(false);
    myFooterLabel.setEnabled(false);
    init();
    initCurrentStep(true);
    setScalableSize(400, 300);
    System.setProperty(StartupActionScriptManager.STARTUP_WIZARD_MODE, "true");
  }

  @Override
  protected void dispose() {
    System.clearProperty(StartupActionScriptManager.STARTUP_WIZARD_MODE);
    super.dispose();
  }

  @Nullable
  @Override
  protected ActionListener createCancelAction() {
    return null;//Prevent closing by <Esc>
  }

  protected void initSteps() {
    mySteps.add(new CustomizeUIThemeStepPanel());
    if (SystemInfo.isMac) {
      mySteps.add(new CustomizeKeyboardSchemeStepPanel());
    }

    CustomizePluginTemplatesStepPanel templateStepPanel = myPredefinedTemplateSets.isEmpty() ? null : new CustomizePluginTemplatesStepPanel(myPredefinedTemplateSets);
    CustomizePluginsStepPanel pluginsStepPanel = null;
    if (!myPluginDescriptors.isEmpty()) {

      if (templateStepPanel != null) {
        mySteps.add(templateStepPanel);
      }
      pluginsStepPanel = new CustomizePluginsStepPanel(myPluginDescriptors, templateStepPanel);
      mySteps.add(pluginsStepPanel);

    }
    mySteps.add(new CustomizeDownloadAndStartStepPanel(this, pluginsStepPanel));
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel result = new JPanel(new BorderLayout(5, 5));
    myContentPanel = new JPanel(myCardLayout);
    for (AbstractCustomizeWizardStep step : mySteps) {
      myContentPanel.add(step, step.getTitle());
    }
    JPanel topPanel = new JPanel(new BorderLayout(5, 5));
    topPanel.add(myNavigationLabel, BorderLayout.NORTH);
    topPanel.add(myHeaderLabel, BorderLayout.CENTER);
    result.add(topPanel, BorderLayout.NORTH);
    result.add(myContentPanel, BorderLayout.CENTER);
    result.add(myFooterLabel, BorderLayout.SOUTH);
    result.setPreferredSize(new Dimension(700, 600));
    result.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    return result;
  }

  @Override
  protected JComponent createSouthPanel() {
    final JPanel buttonPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets.right = 5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = 0;
    buttonPanel.add(mySkipButton, gbc);
    gbc.gridx++;
    buttonPanel.add(myBackButton, gbc);
    gbc.gridx++;
    gbc.weightx = 1;
    buttonPanel.add(Box.createHorizontalGlue(), gbc);
    gbc.gridx++;
    gbc.weightx = 0;
    buttonPanel.add(myNextButton, gbc);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    myButtonWrapper.add(buttonPanel, BUTTONS);
    myButtonWrapper.add(new JLabel(), NOBUTTONS);
    myButtonWrapperLayout.show(myButtonWrapper, BUTTONS);
    return myButtonWrapper;
  }

  void setButtonsVisible(boolean visible) {
    myButtonWrapperLayout.show(myButtonWrapper, visible ? BUTTONS : NOBUTTONS);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == mySkipButton) {
      dispose();
      return;
    }
    if (e.getSource() == myBackButton) {
      myIndex--;
      initCurrentStep(false);
      return;
    }
    if (e.getSource() == myNextButton) {
      if (myIndex >= mySteps.size() - 1) {
        doOKAction();
        return;
      }
      myIndex++;
      initCurrentStep(true);
    }
  }

  @Override
  public void doCancelAction() {
    dispose();
  }

  private void initCurrentStep(boolean forward) {
    final AbstractCustomizeWizardStep myCurrentStep = mySteps.get(myIndex);
    boolean disableBack = myCurrentStep.beforeShown(forward);
    myCardLayout.swipe(myContentPanel, myCurrentStep.getTitle(), JBCardLayout.SwipeDirection.AUTO, new Runnable() {
      @Override
      public void run() {
        Component component = myCurrentStep.getDefaultFocusedComponent();
        if (component != null) {
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(component);
        }
      }
    });

    myBackButton.setVisible(myIndex > 0);
    if (disableBack) {
      myBackButton.setVisible(false);
      mySkipButton.setVisible(false);
    }
    if (myIndex > 0) {
      myBackButton.setText("Back to " + mySteps.get(myIndex - 1).getTitle());
    }
    mySkipButton.setText("Skip " + (myIndex > 0 ? "Remaining" : "All"));

    boolean nextButton = myIndex < mySteps.size() - 1;
    if (nextButton) {
      myNextButton.setText("Next: " + mySteps.get(myIndex + 1).getTitle());
    }
    else {
      myNextButton.setVisible(false);
    }
    myHeaderLabel.setText(myCurrentStep.getHTMLHeader());
    myFooterLabel.setText(myCurrentStep.getHTMLFooter());
    StringBuilder navHTML = new StringBuilder("<html><body>");
    for (int i = 0; i < mySteps.size(); i++) {
      if (i > 0) navHTML.append("&nbsp;&#8594;&nbsp;");
      if (i == myIndex) navHTML.append("<b>");
      navHTML.append(mySteps.get(i).getTitle());
      if (i == myIndex) navHTML.append("</b>");
    }
    myNavigationLabel.setText(navHTML.toString());
  }

  public void updateHeader() {
    final AbstractCustomizeWizardStep myCurrentStep = mySteps.get(myIndex);

    myHeaderLabel.setText(myCurrentStep.getHTMLHeader());
    myFooterLabel.setText(myCurrentStep.getHTMLFooter());
  }
}
