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

import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.StdArrangementTokens;
import jakarta.annotation.Nonnull;

/**
 * Container for matching strategies to be used during file entries arrangement. 
 * <p/>
 * Example: we can define a rule like 'private final non-static fields' or 'public static methods' etc.
 * <p/>
 * Not thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 7/17/12 11:07 AM
 */
public class ArrangementMatchRule {

  @Nonnull
  public static final ArrangementSettingsToken DEFAULT_ORDER_TYPE = StdArrangementTokens.Order.KEEP;

  @Nonnull
  private final ArrangementEntryMatcher  myMatcher;
  @Nonnull
  private final ArrangementSettingsToken myOrderType;

  public ArrangementMatchRule(@Nonnull ArrangementEntryMatcher matcher) {
    this(matcher, DEFAULT_ORDER_TYPE);
  }

  public ArrangementMatchRule(@Nonnull ArrangementEntryMatcher matcher, @Nonnull ArrangementSettingsToken orderType) {
    myMatcher = matcher;
    myOrderType = orderType;
  }

  @Nonnull
  public ArrangementEntryMatcher getMatcher() {
    return myMatcher;
  }

  @Nonnull
  public ArrangementSettingsToken getOrderType() {
    return myOrderType;
  }

  @Override
  public int hashCode() {
    int result = myMatcher.hashCode();
    result = 31 * result + myOrderType.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArrangementMatchRule that = (ArrangementMatchRule)o;
    return myOrderType == that.myOrderType && myMatcher.equals(that.myMatcher);
  }

  @Override
  public String toString() {
    return String.format("matcher: %s, sort type: %s", myMatcher, myOrderType);
  }
}
