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
package consulo.ide.impl.idea.openapi.diff.impl.processing;

import consulo.ide.impl.idea.openapi.diff.impl.string.DiffString;
import consulo.ide.impl.idea.openapi.diff.ex.DiffFragment;
import consulo.ide.impl.idea.openapi.diff.impl.fragments.LineFragment;
import consulo.ide.impl.idea.openapi.diff.impl.util.TextDiffTypeEnum;
import consulo.document.util.TextRange;
import consulo.util.lang.StringUtil;
import consulo.annotation.DeprecationInfo;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

@Deprecated(forRemoval = true)
@DeprecationInfo("Old diff impl, must be removed")
class LineFragmentsCollector {
  private final ArrayList<LineFragment> myLineFragments = new ArrayList<LineFragment>();
  private int myLine1 = 0;
  private int myLine2 = 0;
  private int myOffset1 = 0;
  private int myOffset2 = 0;

  @Nonnull
  private LineFragment addFragment(@Nullable TextDiffTypeEnum type, @Nullable DiffString text1, @Nullable DiffString text2) {
    int lines1 = countLines(text1);
    int lines2 = countLines(text2);
    int endOffset1 = myOffset1 + getLength(text1);
    int endOffset2 = myOffset2 + getLength(text2);
    LineFragment lineFragment =
            new LineFragment(myLine1, lines1, myLine2, lines2, type, new TextRange(myOffset1, endOffset1), new TextRange(myOffset2, endOffset2));
    myLine1 += lines1;
    myLine2 += lines2;
    myOffset1 = endOffset1;
    myOffset2 = endOffset2;
    myLineFragments.add(lineFragment);
    return lineFragment;
  }

  @Nonnull
  public LineFragment addDiffFragment(@Nonnull DiffFragment fragment) {
    return addFragment(getType(fragment), fragment.getText1(), fragment.getText2());
  }

  static int getLength(@Nullable DiffString text) {
    return text == null ? 0 : text.length();
  }

  private static int countLines(@Nullable DiffString text) {
    if (text == null || text.isEmpty()) return 0;
    int count = StringUtil.countNewLines(text);
    if (text.charAt(text.length() - 1) != '\n') count++;
    return count;
  }

  public ArrayList<LineFragment> getFragments() {
    return myLineFragments;
  }

  @Nullable
  static TextDiffTypeEnum getType(@Nonnull DiffFragment fragment) {
    TextDiffTypeEnum type;
    if (fragment.getText1() == null) type = TextDiffTypeEnum.INSERT;
    else if (fragment.getText2() == null) type = TextDiffTypeEnum.DELETED;
    else if (fragment.isModified()) type = TextDiffTypeEnum.CHANGED;
    else type = null;
    return type;
  }
}
