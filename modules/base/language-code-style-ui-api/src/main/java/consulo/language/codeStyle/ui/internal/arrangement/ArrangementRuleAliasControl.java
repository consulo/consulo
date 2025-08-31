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

import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasControl extends ArrangementMatchingRulesControl {
  @Nonnull
  public static final Key<ArrangementRuleAliasControl> KEY = Key.create("Arrangement.Alias.Rule.Control");

  public ArrangementRuleAliasControl(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                     @Nonnull ArrangementColorsProvider colorsProvider,
                                     @Nonnull RepresentationCallback callback) {
    super(settingsManager, colorsProvider, callback);
  }

  public List<StdArrangementMatchRule> getRuleSequences() {
    List<StdArrangementMatchRule> rulesSequences = new ArrayList<StdArrangementMatchRule>();
    for (int i = 0; i < getModel().getSize(); i++) {
      Object element = getModel().getElementAt(i);
      if (element instanceof StdArrangementMatchRule) {
        rulesSequences.add((StdArrangementMatchRule)element);
      }
    }
    return rulesSequences;
  }

  public void setRuleSequences(Collection<StdArrangementMatchRule> sequences) {
    myComponents.clear();
    getModel().clear();

    if (sequences == null) {
      return;
    }

    for (StdArrangementMatchRule rule : sequences) {
      getModel().add(rule);
    }
  }
}
