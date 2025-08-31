/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.internal.custom;

import consulo.language.ast.IElementType;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author dsl
 * @author peter
 */
public class KeywordParser {
  private static final Logger LOG = Logger.getInstance(KeywordParser.class);
  private final List<Set<String>> myKeywordSets = new ArrayList<>();
  private final CharTrie myTrie = new CharTrie();
  private final IntSet myHashCodes = IntSets.newHashSet();
  private final boolean myIgnoreCase;

  public KeywordParser(List<Set<String>> keywordSets, boolean ignoreCase) {
    myIgnoreCase = ignoreCase;
    LOG.assertTrue(keywordSets.size() == CustomHighlighterTokenType.KEYWORD_TYPE_COUNT);
    for (Set<String> keywordSet : keywordSets) {
      Set<String> normalized = normalizeKeywordSet(keywordSet);
      myKeywordSets.add(normalized);
      for (String s : normalized) {
        myHashCodes.add(myTrie.getHashCode(s));
      }
    }
  }

  private Set<String> normalizeKeywordSet(Set<String> keywordSet) {
    if (!myIgnoreCase) {
      return new HashSet<>(keywordSet);
    }

    Set<String> result = new HashSet<>();
    for (String s : keywordSet) {
      result.add(StringUtil.toUpperCase(s));
    }
    return result;
  }

  public boolean hasToken(int position, CharSequence myBuffer, TokenInfo myTokenInfo) {
    int index = 0;
    int offset = position;
    boolean found = false;
    while (offset < myBuffer.length()) {
      char c = myBuffer.charAt(offset++);
      int nextIndex = myTrie.findSubNode(index, myIgnoreCase ? Character.toUpperCase(c) : c);
      if (nextIndex == 0) {
        break;
      }
      index = nextIndex;
      if (myHashCodes.contains(index) && isWordEnd(offset, myBuffer)) {
        String keyword = myBuffer.subSequence(position, offset).toString();
        String testKeyword = myIgnoreCase ? StringUtil.toUpperCase(keyword) : keyword;
        for (int i = 0; i < CustomHighlighterTokenType.KEYWORD_TYPE_COUNT; i++) {
          if (myKeywordSets.get(i).contains(testKeyword)) {
            myTokenInfo.updateData(position, position + keyword.length(), getToken(i));
            found = true;
            break;
          }
        }
      }
    }

    return found;
  }

  private static boolean isWordEnd(int offset, CharSequence myBuffer) {
    if (offset == myBuffer.length()) {
      return true;
    }

    char ch = myBuffer.charAt(offset);
    return ch != '-' && !Character.isLetterOrDigit(ch);
  }

  private static IElementType getToken(int keywordSetIndex) {
    switch(keywordSetIndex) {
      case 0: return CustomHighlighterTokenType.KEYWORD_1;
      case 1: return CustomHighlighterTokenType.KEYWORD_2;
      case 2: return CustomHighlighterTokenType.KEYWORD_3;
      case 3: return CustomHighlighterTokenType.KEYWORD_4;
    }
    throw new AssertionError(keywordSetIndex);
  }
}
