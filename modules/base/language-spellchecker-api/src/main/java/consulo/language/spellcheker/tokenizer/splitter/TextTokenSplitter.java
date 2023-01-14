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
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextTokenSplitter extends BaseTokenSplitter {
  private static final TextTokenSplitter INSTANCE = new TextTokenSplitter();

  public static TextTokenSplitter getInstance() {
    return INSTANCE;
  }

  private static final Pattern EXTENDED_WORD_AND_SPECIAL = Pattern.compile("([&#]|0x[0-9]*)?\\p{L}+'?\\p{L}[_\\p{L}]*");

  @Override
  public void split(@Nullable String text, @Nonnull TextRange range, Consumer<TextRange> consumer) {
    if (text == null || StringUtil.isEmpty(text)) {
      return;
    }
    doSplit(text, range, consumer);
  }

  protected void doSplit(@Nonnull String text, @Nonnull TextRange range, Consumer<TextRange> consumer) {
    final WordTokenSplitter ws = WordTokenSplitter.getInstance();
    Matcher matcher = EXTENDED_WORD_AND_SPECIAL.matcher(text);
    matcher.region(range.getStartOffset(), range.getEndOffset());
    while (matcher.find()) {
      TextRange found = new TextRange(matcher.start(), matcher.end());
      ws.split(text, found, consumer);
    }
  }
}