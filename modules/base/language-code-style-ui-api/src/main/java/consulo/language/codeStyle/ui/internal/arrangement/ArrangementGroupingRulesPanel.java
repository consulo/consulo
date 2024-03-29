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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.application.ApplicationBundle;
import consulo.dataContext.DataProvider;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.group.ArrangementGroupingRule;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.ui.ex.awt.GridBag;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 11/13/12 7:25 PM
 */
public class ArrangementGroupingRulesPanel extends JPanel implements DataProvider {

  @Nonnull
  private final ArrangementGroupingRulesControl myControl;

  public ArrangementGroupingRulesPanel(@Nonnull ArrangementStandardSettingsManager settingsManager, @Nonnull ArrangementColorsProvider colorsProvider) {
    super(new GridBagLayout());

    myControl = new ArrangementGroupingRulesControl(settingsManager, colorsProvider);

    TitleWithToolbar top = new TitleWithToolbar(ApplicationBundle.message("arrangement.settings.section.groups"), ArrangementConstants.ACTION_GROUP_GROUPING_RULES_CONTROL_TOOLBAR,
                                                ArrangementConstants.GROUPING_RULES_CONTROL_TOOLBAR_PLACE, myControl);

    add(top, new GridBag().coverLine().fillCellHorizontally().weightx(1));
    add(myControl, new GridBag().fillCell().weightx(1).weighty(1).insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
  }

  public void setRules(@Nullable List<ArrangementGroupingRule> rules) {
    myControl.setRules(rules);
  }

  @Nonnull
  public List<ArrangementGroupingRule> getRules() {
    return myControl.getRules();
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key dataId) {
    if (ArrangementGroupingRulesControl.KEY == dataId) {
      return myControl;
    }
    return null;
  }
}
