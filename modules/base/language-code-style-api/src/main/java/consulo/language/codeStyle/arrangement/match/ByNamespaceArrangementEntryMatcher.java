/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/**
 * @author Denis Zhdanov
 * @since 3/14/13 1:01 PM
 */
public class ByNamespaceArrangementEntryMatcher extends AbstractRegexpArrangementMatcher {

  public ByNamespaceArrangementEntryMatcher(@Nonnull String pattern) {
    super(pattern);
  }

  @jakarta.annotation.Nullable
  @Override
  protected String getTextToMatch(@Nonnull ArrangementEntry entry) {
    if (entry instanceof NamespaceAwareArrangementEntry) {
      return ((NamespaceAwareArrangementEntry)entry).getNamespace();
    }
    else {
      return null;
    }
  }
}
