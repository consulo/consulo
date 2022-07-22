/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.codeStyle.arrangement.std;

import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 12-Mar-22
 */
public interface ArrangementStandardSettingsManager {
  public boolean isSectionRulesSupported();

  @Nullable
  public List<CompositeArrangementSettingsToken> getSupportedMatchingTokens();

  public boolean isEnabled(@Nonnull ArrangementSettingsToken token, @Nullable ArrangementMatchCondition current);

  @Nonnull
  public Collection<Set<ArrangementSettingsToken>> getMutexes();

  public List<ArrangementSettingsToken> sort(@Nonnull Collection<ArrangementSettingsToken> tokens);

  @Nullable
  public List<CompositeArrangementSettingsToken> getSupportedGroupingTokens();

  @Nonnull
  public Collection<StdArrangementRuleAliasToken> getRuleAliases();

  @Nonnull
  public ArrangementStandardSettingsAware getDelegate();

  public int getWidth(@Nonnull ArrangementSettingsToken token);
}
