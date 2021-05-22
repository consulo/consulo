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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.match.tokens.ArrangementRuleAliasDialog;
import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.ArrangementUtil;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementSectionRule;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchCondition;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.intellij.application.options.codeStyle.arrangement.match.ArrangementSectionRuleManager.ArrangementSectionRuleData;

/**
 * @author Denis Zhdanov
 * @since 10/31/12 1:23 PM
 */
public class ArrangementSectionRulesControl extends ArrangementMatchingRulesControl {
  @Nonnull
  public static final Key<ArrangementSectionRulesControl> KEY = Key.create("Arrangement.Rule.Match.Control");
  @Nonnull
  private static final Logger LOG = Logger.getInstance(ArrangementSectionRulesControl.class);
  @Nonnull
  private final ArrangementColorsProvider myColorsProvider;
  @Nonnull
  private final ArrangementStandardSettingsManager mySettingsManager;

  @Nullable
  private final ArrangementSectionRuleManager mySectionRuleManager;
  @Nullable
  private ArrangementStandardSettingsManager myExtendedSettingsManager;

  public ArrangementSectionRulesControl(@Nonnull Language language,
                                        @Nonnull ArrangementStandardSettingsManager settingsManager,
                                        @Nonnull ArrangementColorsProvider colorsProvider,
                                        @Nonnull RepresentationCallback callback) {
    super(settingsManager, colorsProvider, callback);
    mySectionRuleManager = ArrangementSectionRuleManager.getInstance(language, settingsManager, colorsProvider, this);
    mySettingsManager = settingsManager;
    myColorsProvider = colorsProvider;
  }

  private static void appendBufferedSectionRules(@Nonnull List<ArrangementSectionRule> result,
                                                 @Nonnull List<StdArrangementMatchRule> buffer,
                                                 @Nullable String currentSectionStart) {
    if (currentSectionStart == null) {
      return;
    }

    if (buffer.isEmpty()) {
      result.add(ArrangementSectionRule.create(currentSectionStart, null));
    }
    else {
      result.add(ArrangementSectionRule.create(currentSectionStart, null, buffer.get(0)));
      for (int j = 1; j < buffer.size(); j++) {
        result.add(ArrangementSectionRule.create(buffer.get(j)));
      }
      buffer.clear();
    }
  }

  @Override
  protected MatchingRulesRendererBase createRender() {
    return new MatchingRulesRenderer();
  }

  @Nonnull
  @Override
  protected ArrangementMatchingRulesValidator createValidator() {
    return new ArrangementSectionRulesValidator(getModel(), mySectionRuleManager);
  }

  @Nullable
  public ArrangementSectionRuleManager getSectionRuleManager() {
    return mySectionRuleManager;
  }

  public List<ArrangementSectionRule> getSections() {
    if (getModel().getSize() <= 0) {
      return Collections.emptyList();
    }

    final List<ArrangementSectionRule> result = ContainerUtil.newArrayList();
    final List<StdArrangementMatchRule> buffer = ContainerUtil.newArrayList();
    String currentSectionStart = null;
    for (int i = 0; i < getModel().getSize(); i++) {
      final Object element = getModel().getElementAt(i);
      if (element instanceof StdArrangementMatchRule) {
        final ArrangementSectionRuleData sectionRule =
                mySectionRuleManager == null ? null : mySectionRuleManager.getSectionRuleData((StdArrangementMatchRule)element);
        if (sectionRule != null) {
          if (sectionRule.isSectionStart()) {
            appendBufferedSectionRules(result, buffer, currentSectionStart);
            currentSectionStart = sectionRule.getText();
          }
          else {
            result.add(ArrangementSectionRule.create(StringUtil.notNullize(currentSectionStart), sectionRule.getText(), buffer));
            buffer.clear();
            currentSectionStart = null;
          }
        }
        else {
          if (currentSectionStart == null) {
            result.add(ArrangementSectionRule.create((StdArrangementMatchRule)element));
          }
          else {
            buffer.add((StdArrangementMatchRule)element);
          }
        }
      }
    }

    appendBufferedSectionRules(result, buffer, currentSectionStart);
    return result;
  }

  public void setSections(@Nullable List<ArrangementSectionRule> sections) {
    final List<StdArrangementMatchRule> rules = sections == null ? null : ArrangementUtil.collectMatchRules(sections);
    myComponents.clear();
    getModel().clear();

    if (rules == null) {
      return;
    }

    for (StdArrangementMatchRule rule : rules) {
      getModel().add(rule);
    }

    if (ArrangementConstants.LOG_RULE_MODIFICATION) {
      LOG.info("Arrangement matching rules list is refreshed. Given rules:");
      for (StdArrangementMatchRule rule : rules) {
        LOG.info("  " + rule.toString());
      }
    }
  }

  @Nullable
  public Collection<StdArrangementRuleAliasToken> getRulesAliases() {
    return myExtendedSettingsManager == null ? null : myExtendedSettingsManager.getRuleAliases();
  }

  public void setRulesAliases(@Nullable Collection<StdArrangementRuleAliasToken> aliases) {
    if (aliases != null) {
      myExtendedSettingsManager = new ArrangementStandardSettingsManager(mySettingsManager.getDelegate(), myColorsProvider, aliases);
      myEditor = new ArrangementMatchingRuleEditor(myExtendedSettingsManager, myColorsProvider, this);
    }
  }

  public void showEditor(int rowToEdit) {
    if (mySectionRuleManager != null && mySectionRuleManager.isSectionRule(getModel().getElementAt(rowToEdit))) {
      mySectionRuleManager.showEditor(rowToEdit);
    }
    else {
      super.showEditor(rowToEdit);
    }
  }

  @Nonnull
  public ArrangementRuleAliasDialog createRuleAliasEditDialog() {
    final Set<String> tokenIds = new HashSet<String>();
    final List<ArrangementSectionRule> sections = getSections();
    for (ArrangementSectionRule section : sections) {
      for (StdArrangementMatchRule rule : section.getMatchRules()) {
        rule.getMatcher().getCondition().invite(new ArrangementMatchConditionVisitor() {
          @Override
          public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
            if (ArrangementUtil.isAliasedCondition(condition)) {
              tokenIds.add(condition.getType().getId());
            }
          }

          @Override
          public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
            for (ArrangementMatchCondition operand : condition.getOperands()) {
              operand.invite(this);
            }
          }
        });
      }
    }

    final Collection<StdArrangementRuleAliasToken> aliases = getRulesAliases();
    assert aliases != null;
    return new ArrangementRuleAliasDialog(null, mySettingsManager, myColorsProvider, aliases, tokenIds);
  }

  private class MatchingRulesRenderer extends MatchingRulesRendererBase {
    @Override
    public boolean allowModifications(StdArrangementMatchRule rule) {
      return !(mySectionRuleManager != null && mySectionRuleManager.isSectionRule(rule));
    }
  }
}
