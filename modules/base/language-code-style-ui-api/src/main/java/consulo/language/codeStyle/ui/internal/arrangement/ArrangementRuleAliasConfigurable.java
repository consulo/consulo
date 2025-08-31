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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.language.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasConfigurable implements UnnamedConfigurable {
  private StdArrangementRuleAliasToken myToken;
  private ArrangementRuleAliasesPanel myTokenRulesPanel;

  public ArrangementRuleAliasConfigurable(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                          @Nonnull ArrangementColorsProvider colorsProvider,
                                          @Nonnull StdArrangementRuleAliasToken token) {
    myToken = token;
    myTokenRulesPanel = new ArrangementRuleAliasesPanel(settingsManager, colorsProvider);
    myTokenRulesPanel.setRuleSequences(token.getDefinitionRules());

    registerShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_ADD, CommonShortcuts.getNew(), myTokenRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_REMOVE, CommonShortcuts.getDelete(), myTokenRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_MOVE_UP, CommonShortcuts.MOVE_UP, myTokenRulesPanel);
    registerShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_MOVE_DOWN, CommonShortcuts.MOVE_DOWN, myTokenRulesPanel);
    CustomShortcutSet edit = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
    registerShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_EDIT, edit, myTokenRulesPanel);
  }

  private static void registerShortcut(@Nonnull String actionId,
                                       @Nonnull ShortcutSet shortcut,
                                       @Nonnull JComponent component) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action != null) {
      action.registerCustomShortcutSet(shortcut, component);
    }
  }

  private static void unregisterShortcut(@Nonnull String actionId, @Nonnull JComponent component) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action != null) {
      action.unregisterCustomShortcutSet(component);
    }
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myTokenRulesPanel;
  }

  @Override
  public boolean isModified() {
    List<StdArrangementMatchRule> newRules = myTokenRulesPanel.getRuleSequences();
    return !newRules.equals(myToken.getDefinitionRules());
  }

  @Override
  public void apply() throws ConfigurationException {
    myToken.setDefinitionRules(myTokenRulesPanel.getRuleSequences());
  }

  @Override
  public void reset() {
    myTokenRulesPanel.setRuleSequences(myToken.getDefinitionRules());
  }

  @Override
  public void disposeUIResources() {
    Disposer.dispose(new Disposable() {
      @Override
      public void dispose() {
        unregisterShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_ADD, myTokenRulesPanel);
        unregisterShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_REMOVE, myTokenRulesPanel);
        unregisterShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_MOVE_UP, myTokenRulesPanel);
        unregisterShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_MOVE_DOWN, myTokenRulesPanel);
        unregisterShortcut(ArrangementConstants.MATCHING_ALIAS_RULE_EDIT, myTokenRulesPanel);
      }
    });
  }
}
