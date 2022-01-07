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

package com.intellij.application.options.codeStyle;

import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.ui.components.labels.SwingActionLink;
import com.intellij.util.concurrency.EdtExecutorService;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CodeStyleMainPanel extends JPanel implements TabbedLanguageCodeStylePanel.TabChangeListener  {
  private final CardLayout myLayout = new CardLayout();
  private final JPanel mySettingsPanel = new JPanel(myLayout);

  private final Map<String, NewCodeStyleSettingsPanel> mySettingsPanels = new HashMap<String, NewCodeStyleSettingsPanel>();

  private Future<?> myAlarm = CompletableFuture.completedFuture(null);
  private final CodeStyleSchemesModel myModel;
  private final CodeStyleSettingsPanelFactory myFactory;
  private final CodeStyleSchemesPanel mySchemesPanel;
  private boolean myIsDisposed = false;
  private final Action mySetFromAction = new AbstractAction("Set from...") {
    @Override
    public void actionPerformed(ActionEvent event) {
      CodeStyleAbstractPanel selectedPanel = ensureCurrentPanel().getSelectedPanel();
      if (selectedPanel instanceof TabbedLanguageCodeStylePanel) {
        ((TabbedLanguageCodeStylePanel)selectedPanel).showSetFrom((Component)event.getSource());
      }
    }
  };

  @NonNls
  private static final String WAIT_CARD = "CodeStyleSchemesConfigurable.$$$.Wait.placeholder.$$$";

  private final PropertiesComponent myProperties;

  private final static String SELECTED_TAB = "settings.code.style.selected.tab";

  public CodeStyleMainPanel(CodeStyleSchemesModel model, CodeStyleSettingsPanelFactory factory) {
    super(new BorderLayout());
    myModel = model;
    myFactory = factory;
    mySchemesPanel = new CodeStyleSchemesPanel(model);
    myProperties = PropertiesComponent.getInstance();

    model.addListener(new CodeStyleSchemesModelListener(){
      @Override
      public void currentSchemeChanged(final Object source) {
        if (source != mySchemesPanel) {
          mySchemesPanel.onSelectedSchemeChanged();
        }
        onCurrentSchemeChanged();
      }

      @Override
      public void schemeListChanged() {
        mySchemesPanel.resetSchemesCombo();
      }

      @Override
      public void currentSettingsChanged() {
        ensureCurrentPanel().onSomethingChanged();
      }

      @Override
      public void usePerProjectSettingsOptionChanged() {
        mySchemesPanel.usePerProjectSettingsOptionChanged();
      }

      @Override
      public void schemeChanged(final CodeStyleScheme scheme) {
        ensurePanel(scheme).reset();
      }
    });

    addWaitCard();

    JLabel link = new SwingActionLink(mySetFromAction);
    link.setVerticalAlignment(SwingConstants.CENTER);

    JPanel top = new JPanel(new BorderLayout());
    top.add(BorderLayout.WEST, mySchemesPanel.getPanel());
    top.add(BorderLayout.EAST, link);
    top.setBorder(new EmptyBorder(0, 0, 0, 8));
    add(top, BorderLayout.NORTH);
    add(mySettingsPanel, BorderLayout.CENTER);

    mySchemesPanel.resetSchemesCombo();
    mySchemesPanel.onSelectedSchemeChanged();
    onCurrentSchemeChanged();

  }

  private void addWaitCard() {
    JPanel waitPanel = new JPanel(new BorderLayout());
    JLabel label = new JLabel(ApplicationBundle.message("label.loading.page.please.wait"));
    label.setHorizontalAlignment(SwingConstants.CENTER);
    waitPanel.add(label, BorderLayout.CENTER);
    label.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    waitPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    mySettingsPanel.add(WAIT_CARD, waitPanel);
  }

  public void onCurrentSchemeChanged() {
    myLayout.show(mySettingsPanel, WAIT_CARD);
    final Runnable replaceLayout = new Runnable() {
      @Override
      public void run() {
        if (!myIsDisposed) {
          ensureCurrentPanel().onSomethingChanged();
          String schemeName = myModel.getSelectedScheme().getName();
          updateSetFrom();
          myLayout.show(mySettingsPanel, schemeName);
        }
      }
    };
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      replaceLayout.run();
    } else {
      myAlarm.cancel(false);
      myAlarm = EdtExecutorService.getScheduledExecutorInstance().schedule(replaceLayout, 200, TimeUnit.MILLISECONDS);
    }
  }

  private void updateSetFrom() {
    mySetFromAction.setEnabled(ensureCurrentPanel().getSelectedPanel() instanceof TabbedLanguageCodeStylePanel);
  }

  public NewCodeStyleSettingsPanel[] getPanels() {
    final Collection<NewCodeStyleSettingsPanel> panels = mySettingsPanels.values();
    return panels.toArray(new NewCodeStyleSettingsPanel[panels.size()]);
  }

  public boolean isModified() {
    final NewCodeStyleSettingsPanel[] panels = getPanels();
    for (NewCodeStyleSettingsPanel panel : panels) {
      //if (!panel.isMultiLanguage()) mySchemesPanel.setPredefinedEnabled(false);
      if (panel.isModified()) return true;
    }
    return false;
  }

  public void showTabOnCurrentPanel(String tab) {
    NewCodeStyleSettingsPanel selectedPanel = ensureCurrentPanel();
    CodeStyleAbstractPanel settingsPanel = selectedPanel.getSelectedPanel();
    if (settingsPanel instanceof TabbedLanguageCodeStylePanel) {
      TabbedLanguageCodeStylePanel tabbedPanel = (TabbedLanguageCodeStylePanel)settingsPanel;
      tabbedPanel.changeTab(tab);
    }
  }

  public void reset() {
    for (NewCodeStyleSettingsPanel panel : mySettingsPanels.values()) {
      panel.reset();
    }

    onCurrentSchemeChanged();
  }

  private void clearPanels() {
    for (NewCodeStyleSettingsPanel panel : mySettingsPanels.values()) {
      panel.dispose();
    }
    mySettingsPanels.clear();
  }

  public void apply() {
    final NewCodeStyleSettingsPanel[] panels = getPanels();
    for (NewCodeStyleSettingsPanel panel : panels) {
      if (panel.isModified()) panel.apply();
    }
  }

  @NonNls
  public String getHelpTopic() {
    NewCodeStyleSettingsPanel selectedPanel = ensureCurrentPanel();
    if (selectedPanel == null) {
      return "reference.settingsdialog.IDE.globalcodestyle";
    }
    String helpTopic = selectedPanel.getHelpTopic();
    if (helpTopic != null) {
      return helpTopic;
    }
    return "";
  }

  private NewCodeStyleSettingsPanel ensureCurrentPanel() {
    return ensurePanel(myModel.getSelectedScheme());
  }

  private NewCodeStyleSettingsPanel ensurePanel(@Nonnull CodeStyleScheme scheme) {
    String name = scheme.getName();
    if (!mySettingsPanels.containsKey(name)) {
      NewCodeStyleSettingsPanel panel = myFactory.createPanel(scheme);
      panel.reset();
      panel.setModel(myModel);
      CodeStyleAbstractPanel settingsPanel = panel.getSelectedPanel();
      if (settingsPanel instanceof TabbedLanguageCodeStylePanel) {
        TabbedLanguageCodeStylePanel tabbedPanel = (TabbedLanguageCodeStylePanel)settingsPanel;
        tabbedPanel.setListener(this);
        String currentTab = myProperties.getValue(getSelectedTabPropertyName(tabbedPanel));
        if (currentTab != null) {
          tabbedPanel.changeTab(currentTab);
        }
      }
      mySettingsPanels.put(name, panel);
      mySettingsPanel.add(scheme.getName(), panel);
    }

    return mySettingsPanels.get(name);
  }

  public String getDisplayName() {
    return myModel.getSelectedScheme().getName();
  }

  public void disposeUIResources() {
    myAlarm.cancel(false);
    clearPanels();
    myIsDisposed = true;
  }

  public boolean isModified(final CodeStyleScheme scheme) {
    if (!mySettingsPanels.containsKey(scheme.getName())) {
      return false;
    }

    return mySettingsPanels.get(scheme.getName()).isModified();
  }

  public Set<String> processListOptions() {
    final CodeStyleScheme defaultScheme = CodeStyleSchemes.getInstance().getDefaultScheme();
    final NewCodeStyleSettingsPanel panel = ensurePanel(defaultScheme);
    return panel.processListOptions();
  }

  @Override
  public void tabChanged(@Nonnull TabbedLanguageCodeStylePanel source, @Nonnull String tabTitle) {
    myProperties.setValue(getSelectedTabPropertyName(source), tabTitle);
    for (NewCodeStyleSettingsPanel panel : getPanels()) {
      panel.tabChanged(source, tabTitle);
    }
  }

  @Nonnull
  private static String getSelectedTabPropertyName(@Nonnull TabbedLanguageCodeStylePanel panel) {
    Language language = panel.getDefaultLanguage();
    return SELECTED_TAB + (language != null ? "." + language.getID() : "");
  }
}
