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
package consulo.language.spellcheker.tokenizer.splitter;

import consulo.document.util.TextRange;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTokenSplitter extends BaseTokenSplitter {
  private static final WordTokenSplitter INSTANCE = new WordTokenSplitter();

  public static WordTokenSplitter getInstance() {
    return INSTANCE;
  }

  private static final Pattern SPECIAL = Pattern.compile("&\\p{Alnum}{4};?|#\\p{Alnum}{3,6}|0x\\p{Alnum}?");

  @Override
  public void split(@Nullable String text, @Nonnull TextRange range, Consumer<TextRange> consumer) {
    if (text == null || range.getLength() <= 1) {
      return;
    }
    Matcher specialMatcher = SPECIAL.matcher(text);
    specialMatcher.region(range.getStartOffset(), range.getEndOffset());
    if (specialMatcher.find()) {
      TextRange found = new TextRange(specialMatcher.start(), specialMatcher.end());
      addWord(consumer, true, found);
    }
    else {
      IdentifierTokenSplitter.getInstance().split(text, range, consumer);
    }
  }
}
