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
package consulo.language.codeStyle.arrangement.match;

import consulo.language.codeStyle.arrangement.ArrangementEntry;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ByTextArrangementEntryMatcher implements ArrangementEntryMatcher {
  @Nonnull
  private final String myText;

  public ByTextArrangementEntryMatcher(@Nonnull String text) {
    myText = text;
  }

  @Override
  public boolean isMatched(@Nonnull ArrangementEntry entry) {
    if (entry instanceof TextAwareArrangementEntry) {
      return StringUtil.equals(((TextAwareArrangementEntry)entry).getText(), myText);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return myText.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ByTextArrangementEntryMatcher)) {
      return false;
    }

    ByTextArrangementEntryMatcher matcher = (ByTextArrangementEntryMatcher)o;
    if (!myText.equals(matcher.myText)) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "with text " + myText;
  }
}
