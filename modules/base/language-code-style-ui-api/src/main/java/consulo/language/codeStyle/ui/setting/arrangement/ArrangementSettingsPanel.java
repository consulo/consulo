/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.setting.arrangement;

import consulo.application.localize.ApplicationLocalize;
import consulo.codeEditor.EditorHighlighter;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.Rearranger;
import consulo.language.codeStyle.arrangement.group.ArrangementGroupingRule;
import consulo.language.codeStyle.arrangement.match.ArrangementSectionRule;
import consulo.language.codeStyle.arrangement.std.*;
import consulo.language.codeStyle.ui.internal.arrangement.*;
import consulo.language.codeStyle.ui.setting.CodeStyleAbstractPanel;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.GridBag;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Denis Zhdanov
 * @since 10/30/12 5:17 PM
 */
public abstract class ArrangementSettingsPanel extends CodeStyleAbstractPanel {

  @Nonnull
  private final JPanel myContent = new JPanel(new GridBagLayout());

  @Nonnull
  private final Language                         myLanguage;
  @Nonnull
  private final ArrangementStandardSettingsAware mySettingsAware;
  @Nonnull
  private final ArrangementGroupingRulesPanel myGroupingRulesPanel;
  @Nonnull
  private final ArrangementMatchingRulesPanel    myMatchingRulesPanel;
  @Nullable private final ForceArrangementPanel myForceArrangementPanel;

  public ArrangementSettingsPanel(@Nonnull CodeStyleSettings settings, @Nonnull Language language) {
    super(settings);
    myLanguage = language;
    Rearranger<?> rearranger = Rearranger.forLanguage(language);

    assert rearranger instanceof ArrangementStandardSettingsAware;
    mySettingsAware = (ArrangementStandardSettingsAware)rearranger;

    ArrangementColorsProvider colorsProvider;
    if (rearranger instanceof ArrangementColorsAware) {
      colorsProvider = new ArrangementColorsProviderImpl((ArrangementColorsAware)rearranger);
    }
    else {
      colorsProvider = new ArrangementColorsProviderImpl(null);
    }

    ArrangementStandardSettingsManager settingsManager = new ArrangementStandardSettingsManagerImpl(mySettingsAware, colorsProvider);

    myGroupingRulesPanel = new ArrangementGroupingRulesPanel(settingsManager, colorsProvider);
    myMatchingRulesPanel = new ArrangementMatchingRulesPanel(myLanguage, settingsManager, colorsProvider);

    myContent.add(myGroupingRulesPanel, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    myContent.add(myMatchingRulesPanel, new GridBag().fillCell().weightx(1).weighty(1).coverLine());



    if (settings.getCommonSettings(myLanguage).isForceArrangeMenuAvailable()) {
      myForceArrangementPanel = new ForceArrangementPanel();
      myForceArrangementPanel.setSelectedMode(settings.getCommonSettings(language).FORCE_REARRANGE_MODE);
      myContent.add(myForceArrangementPanel.getPanel(), new GridBag().anchor(GridBagConstraints.WEST).coverLine().fillCellHorizontally());
    }
    else {
      myForceArrangementPanel = null;
    }

    List<CompositeArrangementSettingsToken> groupingTokens = settingsManager.getSupportedGroupingTokens();
    myGroupingRulesPanel.setVisible(groupingTokens != null && !groupingTokens.isEmpty());

    registerShortcut(ArrangementConstants.MATCHING_RULE_ADD, CommonShortcuts.getNew(), myMatchingRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_RULE_REMOVE, CommonShortcuts.getDelete(), myMatchingRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_RULE_MOVE_UP, CommonShortcuts.MOVE_UP, myMatchingRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_RULE_MOVE_DOWN, CommonShortcuts.MOVE_DOWN, myMatchingRulesPanel);
    CustomShortcutSet edit = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
    registerShortcut(ArrangementConstants.MATCHING_RULE_EDIT, edit, myMatchingRulesPanel);

    registerShortcut(ArrangementConstants.GROUPING_RULE_MOVE_UP, CommonShortcuts.MOVE_UP, myGroupingRulesPanel);
    registerShortcut(ArrangementConstants.GROUPING_RULE_MOVE_DOWN, CommonShortcuts.MOVE_DOWN, myGroupingRulesPanel);
  }

  private void registerShortcut(@Nonnull String actionId, @Nonnull ShortcutSet shortcut, @Nonnull JComponent component) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action != null) {
      action.registerCustomShortcutSet(shortcut, component, this);
    }
  }

  @Nullable
  @Override
  public JComponent getPanel() {
    return myContent;
  }

  @Nullable
  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private StdArrangementSettings getSettings(@Nonnull CodeStyleSettings settings) {
    StdArrangementSettings result = (StdArrangementSettings)settings.getCommonSettings(myLanguage).getArrangementSettings();
    if (result == null) {
      result = mySettingsAware.getDefaultSettings();
    }
    return result;
  }

  @Override
  public void apply(CodeStyleSettings settings) {
    CommonCodeStyleSettings commonSettings = settings.getCommonSettings(myLanguage);
    commonSettings.setArrangementSettings(createSettings());
    if (myForceArrangementPanel != null) {
      commonSettings.FORCE_REARRANGE_MODE = myForceArrangementPanel.getRearrangeMode();
    }
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    StdArrangementSettings s = createSettings();
    return !Comparing.equal(getSettings(settings), s)
           || myForceArrangementPanel != null && settings.getCommonSettings(myLanguage).FORCE_REARRANGE_MODE != myForceArrangementPanel.getRearrangeMode();
  }

  private StdArrangementSettings createSettings() {
    List<ArrangementGroupingRule> groupingRules = myGroupingRulesPanel.getRules();
    return new StdArrangementSettings(groupingRules, myMatchingRulesPanel.getSections());
  }

  @Override
  protected void resetImpl(CodeStyleSettings settings) {
    StdArrangementSettings s = getSettings(settings);
    if (s == null) {
      myGroupingRulesPanel.setRules(null);
      myMatchingRulesPanel.setSections(null);
    }
    else {
      List<ArrangementGroupingRule> groupings = s.getGroupings();
      myGroupingRulesPanel.setRules(new ArrayList<>(groupings));
      myMatchingRulesPanel.setSections(copy(s.getSections()));
      if (myForceArrangementPanel != null) {
        myForceArrangementPanel.setSelectedMode(settings.getCommonSettings(myLanguage).FORCE_REARRANGE_MODE);
      }
    }
  }

  @Nonnull
  private static List<ArrangementSectionRule> copy(@Nonnull List<ArrangementSectionRule> rules) {
    List<ArrangementSectionRule> result = new ArrayList<>();
    for (ArrangementSectionRule rule : rules) {
      result.add(rule.clone());
    }
    return result;
  }

  @Nonnull
  @Override
  protected LocalizeValue getTabTitle() {
    return ApplicationLocalize.arrangementTitleSettingsTab();
  }
}
