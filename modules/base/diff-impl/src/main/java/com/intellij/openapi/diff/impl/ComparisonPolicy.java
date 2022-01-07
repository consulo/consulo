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
package com.intellij.openapi.diff.impl;

import com.intellij.CommonBundle;
import com.intellij.openapi.diff.impl.string.DiffString;
import com.intellij.openapi.diff.ex.DiffFragment;
import com.intellij.openapi.diff.impl.highlighting.FragmentSide;
import com.intellij.openapi.diff.impl.highlighting.Util;
import com.intellij.openapi.diff.impl.processing.DiffCorrection;
import com.intellij.openapi.diff.impl.processing.Formatting;
import com.intellij.openapi.diff.impl.processing.Word;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

public abstract class ComparisonPolicy {
  public static final ComparisonPolicy DEFAULT = new DefaultPolicy();
  public static final ComparisonPolicy TRIM_SPACE = new TrimSpacePolicy();
  public static final ComparisonPolicy IGNORE_SPACE = new IgnoreSpacePolicy();
  public static final ComparisonPolicy[] COMPARISON_POLICIES = new ComparisonPolicy[]{DEFAULT, IGNORE_SPACE, TRIM_SPACE};

  private final String myName;

  protected ComparisonPolicy(final String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  @Nonnull
  public DiffFragment[] buildFragments(@Nonnull DiffString[] strings1, @Nonnull DiffString[] strings2) throws FilesTooBigForDiffException {
    DiffFragmentBuilder builder = new DiffFragmentBuilder(strings1, strings2);
    Object[] wrappers1 = getWrappers(strings1);
    Object[] wrappers2 = getWrappers(strings2);
    Diff.Change change = Diff.buildChanges(wrappers1, wrappers2);
    return builder.buildFragments(Util.concatEquals(change, wrappers1, wrappers2));
  }

  @Nonnull
  public DiffFragment[] buildDiffFragmentsFromLines(@Nonnull DiffString[] lines1, @Nonnull DiffString[] lines2)
          throws FilesTooBigForDiffException {
    DiffFragmentBuilder builder = new DiffFragmentBuilder(lines1, lines2);
    Object[] wrappers1 = getLineWrappers(lines1);
    Object[] wrappers2 = getLineWrappers(lines2);
    Diff.Change change = Diff.buildChanges(wrappers1, wrappers2);
    return builder.buildFragments(change);
  }

  @Nonnull
  public DiffFragment createFragment(@Nullable DiffString text1, @Nullable DiffString text2) {
    text1 = toNull(text1);
    text2 = toNull(text2);
    if (text1 == null && text2 == null) return new DiffFragment(DiffString.EMPTY, DiffString.EMPTY);
    DiffFragment result = new DiffFragment(text1, text2);
    if (text1 != null && text2 != null) {
      result.setModified(!getWrapper(text1).equals(getWrapper(text2)));
    }
    return result;
  }

  @Nonnull
  public abstract DiffFragment createFragment(@Nonnull Word word1, @Nonnull Word word2);

  @Nonnull
  protected abstract Object[] getWrappers(@Nonnull DiffString[] strings);

  @Nonnull
  protected abstract Object[] getLineWrappers(@Nonnull DiffString[] lines);

  @Nonnull
  private Object getWrapper(@Nonnull DiffString text) {
    return getWrappers(new DiffString[]{text})[0];
  }

  private static class DefaultPolicy extends ComparisonPolicy {
    public DefaultPolicy() {
      super(CommonBundle.message("comparison.policy.default.name"));
    }

    @Nonnull
    @Override
    protected Object[] getWrappers(@Nonnull DiffString[] strings) {
      return strings;
    }

    @Nonnull
    @Override
    protected Object[] getLineWrappers(@Nonnull DiffString[] lines) {
      return lines;
    }

    @Nonnull
    @Override
    public DiffFragment createFragment(@Nonnull Word word1, @Nonnull Word word2) {
      return createFragment(word1.getText(), word2.getText());
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "DEFAULT";
    }
  }

  private static class TrimSpacePolicy extends ComparisonPolicy {
    public TrimSpacePolicy() {
      super(CommonBundle.message("comparison.policy.trim.space.name"));
    }

    @Nonnull
    @Override
    protected Object[] getLineWrappers(@Nonnull DiffString[] lines) {
      return trimStrings(lines);
    }

    @Nonnull
    @Override
    public DiffFragment createFragment(@Nonnull Word word1, @Nonnull Word word2) {
      DiffString text1 = word1.getText();
      DiffString text2 = word2.getText();
      if (word1.isWhitespace() && word2.isWhitespace() &&
          word1.atEndOfLine() && word2.atEndOfLine()) {
        return DiffFragment.unchanged(text1, text2);
      }
      return createFragment(text1, text2);
    }

    @Nonnull
    @Override
    protected Object[] getWrappers(@Nonnull DiffString[] strings) {
      Object[] result = new Object[strings.length];
      boolean atBeginning = true;
      for (int i = 0; i < strings.length; i++) {
        DiffString string = strings[i];
        DiffString wrapper = atBeginning ? string.trimLeading() : string;
        if (wrapper.endsWith('\n')) {
          atBeginning = true;
          wrapper = wrapper.trimTrailing();
        }
        else {
          atBeginning = false;
        }
        result[i] = wrapper;
      }
      return result;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "TRIM";
    }
  }

  private static class IgnoreSpacePolicy extends ComparisonPolicy
          implements DiffCorrection.FragmentProcessor<DiffCorrection.FragmentsCollector> {
    public IgnoreSpacePolicy() {
      super(CommonBundle.message("comparison.policy.ignore.spaces.name"));
    }

    @Nonnull
    @Override
    protected Object[] getLineWrappers(@Nonnull DiffString[] lines) {
      Object[] result = new Object[lines.length];
      for (int i = 0; i < lines.length; i++) {
        DiffString line = lines[i];
        result[i] = getWrapper(line);
      }
      return result;
    }

    @Nonnull
    @Override
    public DiffFragment[] buildFragments(@Nonnull DiffString[] strings1, @Nonnull DiffString[] strings2)
            throws FilesTooBigForDiffException {
      DiffFragment[] fragments = super.buildFragments(strings1, strings2);
      DiffCorrection.FragmentsCollector collector = new DiffCorrection.FragmentsCollector();
      collector.processAll(fragments, this);
      return collector.toArray();
    }

    @Nonnull
    private static Object getWrapper(@Nonnull DiffString line) {
      return line.skipSpaces();
    }

    @Nonnull
    @Override
    public DiffFragment createFragment(@Nonnull Word word1, @Nonnull Word word2) {
      DiffString text1 = word1.getText();
      DiffString text2 = word2.getText();
      return word1.isWhitespace() && word2.isWhitespace() ? DiffFragment.unchanged(text1, text2) : createFragment(text1, text2);
    }

    @Nonnull
    @Override
    public DiffFragment createFragment(DiffString text1, DiffString text2) {
      DiffString toCompare1 = toNotNull(text1);
      DiffString toCompare2 = toNotNull(text2);
      if (getWrapper(toCompare1).equals(getWrapper(toCompare2))) {
        return DiffFragment.unchanged(toCompare1, toCompare2);
      }
      return new DiffFragment(text1, text2);
    }

    @Nonnull
    @Override
    protected Object[] getWrappers(@Nonnull DiffString[] strings) {
      return trimStrings(strings);
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
      return "IGNORE";
    }

    @Override
    public void process(@Nonnull DiffFragment fragment, @Nonnull DiffCorrection.FragmentsCollector collector) {
      if (fragment.isEqual()) {
        collector.add(fragment);
        return;
      }
      if (fragment.isOneSide()) {
        FragmentSide side = FragmentSide.chooseSide(fragment);
        DiffString text = side.getText(fragment);
        DiffString trimed = text.trim();
        if (trimed.isEmpty()) {
          collector.add(side.createFragment(text, DiffString.EMPTY, false));
          return;
        }
      }
      collector.add(fragment);
    }
  }

  @Nullable
  private static DiffString toNull(@Nullable DiffString text1) {
    return text1 == null || text1.isEmpty() ? null : text1;
  }

  @Nonnull
  private static DiffString toNotNull(@Nullable DiffString text) {
    return text == null ? DiffString.EMPTY : text;
  }

  @Nonnull
  protected Object[] trimStrings(@Nonnull DiffString[] strings) {
    Object[] result = new Object[strings.length];
    for (int i = 0; i < strings.length; i++) {
      DiffString string = strings[i];
      result[i] = string.trim();
    }
    return result;
  }

  public boolean isEqual(@Nonnull DiffFragment fragment) {
    if (fragment.isOneSide()) return false;
    Object[] wrappers = getLineWrappers(new DiffString[]{fragment.getText1(), fragment.getText2()});
    return Comparing.equal(wrappers[0], wrappers[1]);
  }

  @Nonnull
  public Word createFormatting(@Nonnull DiffString text, @Nonnull TextRange textRange) {
    return new Formatting(text, textRange);
  }

  public static ComparisonPolicy[] getAllInstances() {
    return COMPARISON_POLICIES;
  }

  @Nonnull
  @TestOnly
  protected Object[] getWrappers(@Nonnull String[] lines) {
    DiffString[] unsafeStrings = new DiffString[lines.length];
    for (int i = 0; i < lines.length; i++) {
      unsafeStrings[i] = DiffString.createNullable(lines[i]);
    }
    return getWrappers(unsafeStrings);
  }

  @Nonnull
  @TestOnly
  protected Object[] getLineWrappers(@Nonnull String[] lines) {
    DiffString[] unsafeStrings = new DiffString[lines.length];
    for (int i = 0; i < lines.length; i++) {
      unsafeStrings[i] = DiffString.createNullable(lines[i]);
    }
    return getLineWrappers(unsafeStrings);
  }
}
