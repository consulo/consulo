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
package consulo.language.codeStyle.arrangement.match;

import consulo.language.codeStyle.arrangement.ArrangementUtil;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokenType;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * Arrangement rule which uses {@link StdArrangementEntryMatcher standard settings-based matcher}.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/28/12 2:59 PM
 */
public class StdArrangementMatchRule extends ArrangementMatchRule implements Cloneable, Comparable<StdArrangementMatchRule> {

  public StdArrangementMatchRule(@Nonnull StdArrangementEntryMatcher matcher) {
    super(matcher);
  }

  public StdArrangementMatchRule(@Nonnull StdArrangementEntryMatcher matcher, @Nonnull ArrangementSettingsToken orderType) {
    super(matcher, orderType);
  }

  @Nonnull
  @Override
  public StdArrangementEntryMatcher getMatcher() {
    return (StdArrangementEntryMatcher)super.getMatcher();
  }

  @Override
  public StdArrangementMatchRule clone() {
    return new StdArrangementMatchRule(new StdArrangementEntryMatcher(getMatcher().getCondition().clone()), getOrderType());
  }

  @Override
  public int compareTo(@Nonnull StdArrangementMatchRule o) {
    final Set<ArrangementSettingsToken> tokens = ArrangementUtil.extractTokens(getMatcher().getCondition()).keySet();
    final Set<ArrangementSettingsToken> tokens1 = ArrangementUtil.extractTokens(o.getMatcher().getCondition()).keySet();
    if (tokens1.containsAll(tokens)) {
      return tokens.containsAll(tokens1) ? 0 : 1;
    }
    else {
      if (tokens.containsAll(tokens1)) {
        return -1;
      }

      final String entryType = getEntryType(tokens);
      final String entryType1 = getEntryType(tokens1);
      final int compare = StringUtil.compare(entryType, entryType1, false);
      if (compare != 0 || tokens.size() == tokens1.size()) {
        return compare;
      }
      return tokens.size() < tokens1.size() ? 1 : -1;
    }
  }

  @Nullable
  private static String getEntryType(@Nonnull Set<ArrangementSettingsToken> tokens) {
    for (ArrangementSettingsToken token : tokens) {
      if (StdArrangementTokenType.ENTRY_TYPE.is(token)) {
        return token.getId();
      }
    }
    return null;
  }
}
