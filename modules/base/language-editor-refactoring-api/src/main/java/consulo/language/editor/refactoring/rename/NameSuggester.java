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

package consulo.language.editor.refactoring.rename;

import consulo.application.util.matcher.NameUtil;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntIntMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nullable;
import java.util.*;

/**
 * @author dsl
 */
public class NameSuggester {
  private static final Logger LOG = Logger.getInstance(NameSuggester.class);
  private final String[] myOldClassName;
  private final String[] myNewClassName;
  private final List<OriginalToNewChange> myChanges; // sorted from right to left
  private final String myOldClassNameAsGiven;
  private final String myNewClassNameAsGiven;


  public NameSuggester(String oldClassName, String newClassName) {
    myOldClassNameAsGiven = oldClassName;
    myNewClassNameAsGiven = newClassName;
    myOldClassName = NameUtil.splitNameIntoWords(oldClassName);
    myNewClassName = NameUtil.splitNameIntoWords(newClassName);

    myChanges = new ArrayList<>();
    int oldIndex = myOldClassName.length - 1;
    int oldLastMatch = myOldClassName.length;
    int newLastMatch = myNewClassName.length;

    while(oldIndex >= 0) {
      String patternWord = myOldClassName[oldIndex];
      int matchingWordIndex = findInNewBackwardsFromIndex(patternWord, newLastMatch - 1);
      if (matchingWordIndex < 0) { // no matching word
        oldIndex--;
      }
      else { // matching word found
        if (oldIndex + 1 <= oldLastMatch - 1 || matchingWordIndex + 1 <= newLastMatch - 1) {
          OriginalToNewChange change = new OriginalToNewChange(
            oldIndex + 1, oldLastMatch - 1, matchingWordIndex + 1, newLastMatch - 1);
          myChanges.add(change);
        }
        oldLastMatch = oldIndex;
        newLastMatch = matchingWordIndex;
        oldIndex--;
      }
    }
    if (0 <= oldLastMatch - 1 || 0 <= newLastMatch - 1) {
      myChanges.add(new OriginalToNewChange(0, oldLastMatch - 1, 0, newLastMatch - 1));
    }
  }

  private int findInNewBackwardsFromIndex(String patternWord, int newIndex) {
    for (int i = newIndex; i >= 0; i--) {
      String s = myNewClassName[i];
      if (s.equals(patternWord)) return i;
    }
    return -1;
  }

  List<Pair<String,String>> getChanges() {
    ArrayList<Pair<String,String>> result = new ArrayList<>();
    for (int i = myChanges.size() - 1; i >=0; i--) {
      OriginalToNewChange change = myChanges.get(i);
      result.add(Pair.create(change.getOldString(), change.getNewString()));
    }
    return result;
  }

  public String suggestName(String propertyName) {
    if (myOldClassNameAsGiven.equals(propertyName)) return myNewClassNameAsGiven;
    String[] propertyWords = NameUtil.splitNameIntoWords(propertyName);
    IntIntMap matches = calculateMatches(propertyWords);
    if (matches.isEmpty()) return propertyName;
    TreeMap<Pair<Integer,Integer>, String> replacements = calculateReplacements(propertyWords, matches);
    if (replacements.isEmpty()) return propertyName;
    return calculateNewName(replacements, propertyWords, propertyName);
  }


  private static Pair<int[],int[]> calculateWordPositions(String s, String[] words) {
    int[] starts = new int[words.length + 1];
    int[] prevEnds = new int[words.length + 1];
    prevEnds[0] = -1;
    int pos = 0;
    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      int index = s.indexOf(word, pos);
      LOG.assertTrue(index >= 0);
      starts[i] = index;
      pos = index + word.length();
      prevEnds[i + 1] = pos - 1;
    }
    starts[words.length] = s.length();
    return Pair.create(starts, prevEnds);
  }

  private static String calculateNewName(TreeMap<Pair<Integer, Integer>, String> replacements,
                                  String[] propertyWords,
                                  String propertyName) {
    StringBuffer resultingWords = new StringBuffer();
    int currentWord = 0;
    Pair<int[],int[]> wordIndicies = calculateWordPositions(propertyName, propertyWords);
    for (Map.Entry<Pair<Integer, Integer>, String> entry : replacements.entrySet()) {
      int first = entry.getKey().getFirst().intValue();
      int last = entry.getKey().getSecond().intValue();
      for (int i = currentWord; i < first; i++) {
        resultingWords.append(calculateBetween(wordIndicies, i, propertyName));
        String propertyWord = propertyWords[i];
        appendWord(resultingWords, propertyWord);
      }
      resultingWords.append(calculateBetween(wordIndicies, first, propertyName));
      appendWord(resultingWords, entry.getValue());
      currentWord = last + 1;
    }
    for(; currentWord < propertyWords.length; currentWord++) {
      resultingWords.append(calculateBetween(wordIndicies, currentWord, propertyName));
      appendWord(resultingWords, propertyWords[currentWord]);
    }
    resultingWords.append(calculateBetween(wordIndicies, propertyWords.length, propertyName));
    if (resultingWords.length() == 0) return propertyName;
    return decapitalizeProbably(resultingWords.toString(), propertyName);
  }

  private static void appendWord(StringBuffer resultingWords, String propertyWord) {
    if (resultingWords.length() > 0) {
      char lastChar = resultingWords.charAt(resultingWords.length() - 1);
      if (Character.isLetterOrDigit(lastChar)) {
        propertyWord = StringUtil.capitalize(propertyWord);
      }
    }
    resultingWords.append(propertyWord);
  }

  private static String calculateBetween(Pair<int[], int[]> wordIndicies, int i, String propertyName) {
    int thisWordStart = wordIndicies.getFirst()[i];
    int prevWordEnd = wordIndicies.getSecond()[i];
    return propertyName.substring(prevWordEnd + 1, thisWordStart);
  }

  /**
   * Calculates a map of replacements. Result has a form:<br>
   * {&lt;first,last&gt; -&gt; replacement} <br>
   * where start and end are indices of property words range (inclusive), and replacement is a
   * string that this range must be replaced with.<br>
   * It is valid situation that <code>last == first - 1</code>: in this case replace means insertion
   * before first word. Furthermore, first may be equal to <code>propertyWords.length</code>  - in
   * that case replacements transormates to appending.
   * @param propertyWords
   * @param matches
   * @return
   */
  private TreeMap<Pair<Integer, Integer>, String> calculateReplacements(String[] propertyWords, IntIntMap matches) {
    TreeMap<Pair<Integer,Integer>, String> replacements = new TreeMap<>((Comparator<Pair<Integer, Integer>>)(pair, pair1) -> pair.getFirst().compareTo(pair1.getFirst()));
    for (OriginalToNewChange change : myChanges) {
      int first = change.oldFirst;
      int last = change.oldLast;
      if (change.getOldLength() > 0) {
        if (containsAllBetween(matches, first, last)) {
          String newString = change.getNewString();
          int propertyWordFirst = matches.getInt(first);

          if (first >= myOldClassName.length || last >= myOldClassName.length) {
            LOG.error("old class name = " + myOldClassNameAsGiven + ", new class name = " + myNewClassNameAsGiven + ", propertyWords = " +
                      Arrays.asList(propertyWords).toString());
          }

          String replacement = suggestReplacement(propertyWords[propertyWordFirst], newString);
          replacements.put(Pair.create(propertyWordFirst, matches.getInt(last)), replacement);
        }
      }
      else {
        String newString = change.getNewString();
        int propertyWordToInsertBefore;
        if (matches.containsKey(first)) {
          propertyWordToInsertBefore = matches.getInt(first);
        }
        else {
          if (matches.containsKey(last)) {
            propertyWordToInsertBefore = matches.getInt(last) + 1;
          } else {
            propertyWordToInsertBefore = propertyWords.length;
          }
        }
        replacements.put(Pair.create(propertyWordToInsertBefore, propertyWordToInsertBefore - 1), newString);
      }
    }
    return replacements;
  }

  private static String suggestReplacement(String propertyWord, String newClassNameWords) {
    return decapitalizeProbably(newClassNameWords, propertyWord);
  }

  private static String decapitalizeProbably(String word, String originalWord) {
    if (originalWord.length() == 0) return word;
    if (Character.isLowerCase(originalWord.charAt(0))) {
      return StringUtil.decapitalize(word);
    }
    return word;
  }

  private static boolean containsAllBetween(IntIntMap matches, int first, int last) {
    for (int i = first; i <= last; i++) {
      if (!matches.containsKey(i)) return false;
    }
    return true;
  }

  private IntIntMap calculateMatches(String[] propertyWords) {
    int classNameIndex = myOldClassName.length - 1;
    IntIntMap matches = IntMaps.newIntIntHashMap();
    for (int i = propertyWords.length - 1; i >= 0; i--) {
      String propertyWord = propertyWords[i];
      Match match = null;
      for (int j = classNameIndex; j >= 0 && match == null; j--) {
        match = checkMatch(j, i, propertyWord);
      }
      if (match != null) {
        matches.putInt(match.oldClassNameIndex, i);
        classNameIndex = match.oldClassNameIndex - 1;
      }
    }
    return matches;
  }

  private class OriginalToNewChange {
    final int oldFirst;
    final int oldLast;
    final int newFirst;
    final int newLast;

    public OriginalToNewChange(int firstInOld, int lastInOld, int firstInNew, int lastInNew) {
      oldFirst = firstInOld;
      oldLast = lastInOld;
      newFirst = firstInNew;
      newLast = lastInNew;
    }

    int getOldLength() {
      return oldLast - oldFirst + 1;
    }

    String getOldString() {
      StringBuilder buffer = new StringBuilder();
      for (int i = oldFirst; i <= oldLast; i++) {
        buffer.append(myOldClassName[i]);
      }
      return buffer.toString();
    }

    String getNewString() {
      StringBuilder buffer = new StringBuilder();
      for (int i = newFirst; i <= newLast; i++) {
        buffer.append(myNewClassName[i]);
      }
      return buffer.toString();
    }
  }

  private static class Match {
    final int oldClassNameIndex;
    final int propertyNameIndex;
    final String propertyWord;

    public Match(int oldClassNameIndex, int propertyNameIndex, String propertyWord) {
      this.oldClassNameIndex = oldClassNameIndex;
      this.propertyNameIndex = propertyNameIndex;
      this.propertyWord = propertyWord;
    }
  }

  @Nullable
  private Match checkMatch(int oldClassNameIndex, int propertyNameIndex, String propertyWord) {
    if (propertyWord.equalsIgnoreCase(myOldClassName[oldClassNameIndex])) {
      return new Match(oldClassNameIndex, propertyNameIndex, propertyWord);
    }
    else return null;
  }
}
