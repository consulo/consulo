// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.matcher;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import java.util.Arrays;

public class WordPrefixMatcher implements Matcher {
  private final String[] myPatternWords;

  public WordPrefixMatcher(String pattern) {
    myPatternWords = NameUtil.nameToWords(pattern);
  }

  @Override
  public boolean matches(@Nonnull String name) {
    String[] nameWords = NameUtil.nameToWords(name);
    return Arrays.stream(myPatternWords).allMatch(pw -> ContainerUtil.exists(nameWords, nw -> StringUtil.startsWithIgnoreCase(nw, pw)));
  }
}
