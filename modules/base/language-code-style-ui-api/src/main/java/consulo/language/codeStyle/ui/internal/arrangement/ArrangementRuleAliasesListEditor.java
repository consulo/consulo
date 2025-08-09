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

import consulo.configurable.UnnamedConfigurable;
import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.language.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.NamedItemsListEditor;
import consulo.util.collection.HashingStrategy;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasesListEditor extends NamedItemsListEditor<StdArrangementRuleAliasToken> {
  private static final Namer<StdArrangementRuleAliasToken> NAMER = new Namer<StdArrangementRuleAliasToken>() {
    @Override
    public String getName(StdArrangementRuleAliasToken token) {
      return token.getName();
    }

    @Override
    public boolean canRename(StdArrangementRuleAliasToken item) {
      return false;
    }

    @Override
    public void setName(StdArrangementRuleAliasToken token, String name) {
      token.setTokenName(name.replaceAll("\\s+", " "));
    }
  };
  private static final Supplier<StdArrangementRuleAliasToken> FACTORY = () -> new StdArrangementRuleAliasToken("");
  private static final Cloner<StdArrangementRuleAliasToken> CLONER = new Cloner<StdArrangementRuleAliasToken>() {
    @Override
    public StdArrangementRuleAliasToken cloneOf(StdArrangementRuleAliasToken original) {
      return copyOf(original);
    }

    @Override
    public StdArrangementRuleAliasToken copyOf(StdArrangementRuleAliasToken original) {
      return new StdArrangementRuleAliasToken(original.getName(), original.getDefinitionRules());
    }
  };
  private static final HashingStrategy<StdArrangementRuleAliasToken> COMPARER = new HashingStrategy<>() {
    @Override
    public boolean equals(StdArrangementRuleAliasToken o1, StdArrangementRuleAliasToken o2) {
      return Comparing.equal(o1.getId(), o2.getId());
    }
  };

  @Nonnull
  private Set<String> myUsedTokenIds;
  @Nonnull
  private ArrangementStandardSettingsManager mySettingsManager;
  @Nonnull
  private ArrangementColorsProvider myColorsProvider;

  protected ArrangementRuleAliasesListEditor(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                             @Nonnull ArrangementColorsProvider colorsProvider,
                                             @Nonnull List<StdArrangementRuleAliasToken> items,
                                             @Nonnull Set<String> usedTokenIds) {
    super(NAMER, FACTORY, CLONER, COMPARER, items, false, () -> null);
    mySettingsManager = settingsManager;
    myColorsProvider = colorsProvider;
    myUsedTokenIds = usedTokenIds;
    reset();
    initTree();
  }

  @Override
  protected UnnamedConfigurable createConfigurable(StdArrangementRuleAliasToken item) {
    return new ArrangementRuleAliasConfigurable(mySettingsManager, myColorsProvider, item);
  }

  @Override
  protected boolean canDelete(StdArrangementRuleAliasToken item) {
    return !myUsedTokenIds.contains(item.getId());
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Custom Composite Tokens");
  }
}
