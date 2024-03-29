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

import consulo.language.codeStyle.arrangement.ArrangementEntry;

import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Denis Zhdanov
 * @since 7/17/12 11:26 AM
 */
public class CompositeArrangementEntryMatcher implements ArrangementEntryMatcher {

  @Nonnull
  private final Set<ArrangementEntryMatcher> myMatchers = new HashSet<ArrangementEntryMatcher>();

  public CompositeArrangementEntryMatcher(@Nonnull ArrangementEntryMatcher... matchers) {
    myMatchers.addAll(Arrays.asList(matchers));
  }

  @Override
  public boolean isMatched(@Nonnull ArrangementEntry entry) {
    for (ArrangementEntryMatcher matcher : myMatchers) {
      if (!matcher.isMatched(entry)) {
        return false;
      }
    }
    return true;
  }

  public void addMatcher(@Nonnull ArrangementEntryMatcher rule) {
    myMatchers.add(rule);
  }

  @Override
  public int hashCode() {
    return myMatchers.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CompositeArrangementEntryMatcher matcher = (CompositeArrangementEntryMatcher)o;

    return myMatchers.equals(matcher.myMatchers);
  }

  @Override
  public String toString() {
    return String.format("all of those: %s", myMatchers);
  }
}
