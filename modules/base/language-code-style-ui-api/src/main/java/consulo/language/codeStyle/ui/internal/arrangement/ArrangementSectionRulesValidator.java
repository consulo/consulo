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

import consulo.application.ApplicationBundle;
import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementSectionRulesValidator extends ArrangementMatchingRulesValidator {
  private ArrangementSectionRuleManager mySectionRuleManager;

  public ArrangementSectionRulesValidator(ArrangementMatchingRulesModel model, ArrangementSectionRuleManager sectionRuleManager) {
    super(model);
    mySectionRuleManager = sectionRuleManager;
  }

  @Override
  @Nullable
  protected String validate(int index) {
    if (myRulesModel.getSize() < index) {
      return null;
    }

    if (mySectionRuleManager != null) {
      final ArrangementSectionRuleManager.ArrangementSectionRuleData data = extractSectionText(index);
      if (data != null) {
        return validateSectionRule(data, index);
      }
    }
    return super.validate(index);
  }

  @Nullable
  private String validateSectionRule(@Nonnull ArrangementSectionRuleManager.ArrangementSectionRuleData data, int index) {
    int startSectionIndex = -1;
    final Set<String> sectionRules = new HashSet<>();
    for (int i = 0; i < index; i++) {
      final ArrangementSectionRuleManager.ArrangementSectionRuleData section = extractSectionText(i);
      if (section != null) {
        startSectionIndex = section.isSectionStart() ? i : -1;
        if (StringUtil.isNotEmpty(section.getText())) {
          sectionRules.add(section.getText());
        }
      }
    }
    if (StringUtil.isNotEmpty(data.getText()) && sectionRules.contains(data.getText())) {
      return ApplicationBundle.message("arrangement.settings.validation.duplicate.section.text");
    }

    if (!data.isSectionStart()) {
      if (startSectionIndex == -1) {
        return ApplicationBundle.message("arrangement.settings.validation.end.section.rule.without.start");
      }
      else if (startSectionIndex == index - 1) {
        return ApplicationBundle.message("arrangement.settings.validation.empty.section.rule");
      }
    }
    return null;
  }

  @Nullable
  private ArrangementSectionRuleManager.ArrangementSectionRuleData extractSectionText(int i) {
    Object element = myRulesModel.getElementAt(i);
    if (element instanceof StdArrangementMatchRule) {
      assert mySectionRuleManager != null;
      return mySectionRuleManager.getSectionRuleData((StdArrangementMatchRule)element);
    }
    return null;
  }
}
