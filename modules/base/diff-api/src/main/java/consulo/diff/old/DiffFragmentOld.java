/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.diff.old;

import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nullable;

@Deprecated
public class DiffFragmentOld {
  public static DiffFragmentOld[] EMPTY_ARRAY = new DiffFragmentOld[0];

  @Nullable private CharSequence myText1;
  @Nullable
  private CharSequence myText2;
  private boolean myIsModified;

  @TestOnly
  public DiffFragmentOld(@Nullable String text1, @Nullable String text2) {
    this(DiffString.createNullable(text1), DiffString.createNullable(text2));
  }

  public DiffFragmentOld(@Nullable DiffString text1, @Nullable DiffString text2) {
    myText1 = text1;
    myText2 = text2;
    myIsModified = (text1 == null || text2 == null || !text1.equals(text2));
  }

  public boolean isEmpty() {
    return StringUtil.isEmpty(myText1) && StringUtil.isEmpty(myText2);
  }

  /**
   * Makes sense if both texts are not null
   * @return true if both texts are considered modified, false otherwise
   */
  public boolean isModified() {
    return myIsModified;
  }

  public void setModified(boolean modified) {
    myIsModified = modified;
  }

  public void appendText1(@Nullable DiffString str) {
    if (str == null) return;
    if (myText1 instanceof DiffStringBuilder diffStringBuilder) {
      diffStringBuilder.append(str);
      return;
    }

    if (myText1 instanceof DiffString diffString) {
      if (DiffString.canInplaceConcatenate(diffString, str)) {
        myText1 = DiffString.concatenate(diffString, str);
      }
      else {
        DiffStringBuilder builder = new DiffStringBuilder(diffString.length() + str.length());
        builder.append(diffString);
        builder.append(str);
        myText1 = builder;
      }
      return;
    }

    throw new IllegalStateException("Bad DiffFragment: " + (myText1 != null ? myText1.getClass() : "null"));
  }

  public void appendText2(@Nullable DiffString str) {
    if (str == null) return;
    if (myText2 instanceof DiffStringBuilder diffStringBuilder) {
      diffStringBuilder.append(str);
      return;
    }

    if (myText2 instanceof DiffString diffString) {
      if (DiffString.canInplaceConcatenate(diffString, str)) {
        myText2 = DiffString.concatenate(diffString, str);
      }
      else {
        DiffStringBuilder builder = new DiffStringBuilder(diffString.length() + str.length());
        builder.append(diffString);
        builder.append(str);
        myText2 = builder;
      }
      return;
    }

    throw new IllegalStateException("Bad DiffFragment: " + (myText2 != null ? myText2.getClass() : "null"));
  }

  @Nullable
  public DiffString getText1() {
    if (myText1 == null) return null;
    if (myText1 instanceof DiffString diffString) return diffString;
    if (myText1 instanceof DiffStringBuilder diffStringBuilder) return diffStringBuilder.toDiffString();

    throw new IllegalStateException("Bad DiffFragment: " + myText1.getClass());
  }

  @Nullable
  public DiffString getText2() {
    if (myText2 == null) return null;
    if (myText2 instanceof DiffString diffString) return diffString;
    if (myText2 instanceof DiffStringBuilder diffStringBuilder) return diffStringBuilder.toDiffString();

    throw new IllegalStateException("Bad DiffFragment: " + myText2.getClass());
  }

  /**
   * Same as {@link #isModified()}, but doesn't require texts checked for null.
   * @return true iff both texts are present and {@link #isModified()}
   */
  public boolean isChange() {
    return (myText1 != null) && (myText2 != null) && isModified();
  }

  /**
   * @return true iff both texts are present and not {@link #isModified()}
   */
  public boolean isEqual() {
    return (myText1 != null) && (myText2 != null) && !isModified();
  }

  @TestOnly
  public static DiffFragmentOld unchanged(@Nullable String text1, @Nullable String text2) {
    return unchanged(DiffString.createNullable(text1), DiffString.createNullable(text2));
  }

  public static DiffFragmentOld unchanged(@Nullable DiffString text1, @Nullable DiffString text2) {
    if (text1 == null) text1 = DiffString.EMPTY;
    if (text2 == null) text2 = DiffString.EMPTY;
    DiffFragmentOld result = new DiffFragmentOld(text1, text2);
    result.setModified(false);
    return result;
  }

  public boolean isOneSide() {
    return (myText1 == null) || (myText2 == null);
  }
}
