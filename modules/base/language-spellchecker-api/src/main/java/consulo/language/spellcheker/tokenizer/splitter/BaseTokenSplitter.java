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

import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.BombedStringUtil;
import consulo.component.ProcessCanceledException;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import consulo.util.collection.SmartList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseTokenSplitter implements TokenSplitter {

  static final Logger LOG = Logger.getInstance(BaseTokenSplitter.class);

  public static final int MIN_RANGE_LENGTH = 3;


  protected static void addWord(@Nonnull Consumer<TextRange> consumer, boolean ignore, @Nullable TextRange found) {
    if (found == null || ignore) {
      return;
    }
    boolean tooShort = (found.getEndOffset() - found.getStartOffset()) <= MIN_RANGE_LENGTH;
    if (tooShort) {
      return;
    }
    consumer.accept(found);
  }

  protected static boolean isAllWordsAreUpperCased(@Nonnull String text, @Nonnull List<TextRange> words) {
    for (TextRange word : words) {
      CharacterIterator it = new StringCharacterIterator(text, word.getStartOffset(), word.getEndOffset(), word.getStartOffset());
      for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
        if (!Character.isUpperCase(c)) {
          return false;
        }
      }
    }
    return true;

  }

  protected static boolean containsShortWord(@Nonnull List<TextRange> words) {
    for (TextRange word : words) {
      if (word.getLength() < MIN_RANGE_LENGTH) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  protected static TextRange matcherRange(@Nonnull TextRange range, @Nonnull Matcher matcher) {
    return subRange(range, matcher.start(), matcher.end());
  }

  @Nonnull
  protected static TextRange matcherRange(@Nonnull TextRange range, @Nonnull Matcher matcher, int group) {
    return subRange(range, matcher.start(group), matcher.end(group));
  }

  @Nonnull
  protected static TextRange subRange(@Nonnull TextRange range, int start, int end) {
    return TextRange.from(range.getStartOffset() + start, end - start);
  }

  protected static boolean badSize(int from, int till) {
    int l = till - from;
    return l <= MIN_RANGE_LENGTH;
  }

  @Nonnull
  static protected List<TextRange> excludeByPattern(String text, TextRange range, @Nonnull Pattern toExclude, int groupToInclude) {
    List<TextRange> toCheck = new SmartList<TextRange>();
    int from = range.getStartOffset();
    int till;
    boolean addLast = true;
    Matcher matcher = toExclude.matcher(BombedStringUtil.newBombedCharSequence(range.substring(text), 500));
    try {
      while (matcher.find()) {
        checkCancelled();
        TextRange found = matcherRange(range, matcher);
        till = found.getStartOffset();
        if (range.getEndOffset() - found.getEndOffset() < MIN_RANGE_LENGTH) {
          addLast = false;
        }
        if (!badSize(from, till)) {
          toCheck.add(new TextRange(from, till));
        }
        if (groupToInclude > 0) {
          TextRange contentFound = matcherRange(range, matcher, groupToInclude);
          if (badSize(contentFound.getEndOffset(), contentFound.getStartOffset())) {
            toCheck.add(TextRange.create(contentFound));
          }
        }
        from = found.getEndOffset();
      }
      till = range.getEndOffset();
      if (badSize(from, till)) {
        return toCheck;
      }
      if (addLast) {
        toCheck.add(new TextRange(from, till));
      }
      return toCheck;
    }
    catch (ProcessCanceledException e) {
      //LOG.warn("Matching took too long: >>>" + range.substring(text) + "<<< " + toExclude);
      return Collections.singletonList(range);
      //return Collections.emptyList();
    }
  }

  public static void checkCancelled() {
    ProgressIndicatorProvider.checkCanceled();
  }


}
