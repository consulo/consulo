// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.internal.custom;

import consulo.annotation.ReviewAfterIssueFix;
import consulo.util.collection.ArrayFactory;
import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class CharTrie {
  private int myAllNodesSize;
  private char @Nullable [] myAllNodesChars = null;
  private char @Nullable [] myAllNodesParents = null; // unsigned short
  private char @Nullable [] @Nullable [] myAllNodesChildren = null; // unsigned short

  public CharTrie() {
    init();
  }

  private void init() {
    myAllNodesChars = null;
    myAllNodesParents = null;
    myAllNodesChildren = null;
    myAllNodesSize = 0;
    addNode(-1, (char)0);
  }

  @ReviewAfterIssueFix(value = "github.com/uber/NullAway/issues/1521", todo = "Remove NullAway suppression")
  @SuppressWarnings("NullAway")
  private void addNode(int parentIndex, char ch) {
    if (myAllNodesSize == 0) {
      int initialCapacity = 10;
      myAllNodesChars = new char[initialCapacity];
      myAllNodesParents = new char[initialCapacity];
      myAllNodesChildren = new char[initialCapacity][];
    }
    else if (myAllNodesSize >= Objects.requireNonNull(myAllNodesChars).length) {
      int increment = Math.max(myAllNodesSize >> 2, 10);

      int newSize = myAllNodesSize + increment;
      myAllNodesChars = ArrayUtil.realloc(myAllNodesChars, newSize);
      myAllNodesParents = ArrayUtil.realloc(Objects.requireNonNull(myAllNodesParents), newSize);
      myAllNodesChildren = ArrayUtil.realloc(Objects.requireNonNull(myAllNodesChildren), newSize, FACTORY);
    }

    myAllNodesChars[myAllNodesSize] = ch;
    Objects.requireNonNull(myAllNodesParents)[myAllNodesSize] = (char)parentIndex;
    // NullAway erroneously thinks second dimension also became non-null after requireNonNull, suppressing NullAway until fix
    Objects.requireNonNull(myAllNodesChildren)[myAllNodesSize] = null;
    ++myAllNodesSize;
    assert myAllNodesSize < Character.MAX_VALUE;
  }

  public int size() {
    return myAllNodesSize;
  }

  /**
   * Returns reversed string by unique hash code.
   */
  public String getReversedString(int hashCode) {
    return new String(getReversedChars(hashCode));
  }

  public String getString(int hashCode) {
    return new String(getChars(hashCode));
  }

  public int getHashCode(char[] chars) {
    return getHashCode(chars, 0, chars.length);
  }

  public int getHashCode(char[] chars, int offset, int length) {
    int index = 0;
    for (int i = offset; i < offset + length; i++) {
      index = getSubNode(index, chars[i], true);
    }
    return index;
  }

  public int getHashCode(CharSequence seq) {
    int index = 0;
    int l = seq.length();
    for (int i = 0; i < l; i++) {
      index = getSubNode(index, seq.charAt(i), true);
    }
    return index;
  }

  public long getMaximumMatch(CharSequence seq, int offset, int length) {
    int index = 0;
    int resultingLength = 0;
    while (length-- > 0) {
      int nextIndex = findSubNode(index, seq.charAt(offset++));
      if (nextIndex == 0) {
        break;
      }
      index = nextIndex;
      resultingLength++;
    }

    return index + (((long)resultingLength) << 32);
  }

  public char[] getChars(int hashCode) {
    int length = 0;
    int run = hashCode;
    while (run > 0) {
      length++;
      run = Objects.requireNonNull(myAllNodesParents)[run];
    }

    char[] result = new char[length];
    run = hashCode;
    for (int i = 0; i < length; i++) {
      assert run > 0;
      result[length - i - 1] = Objects.requireNonNull(myAllNodesChars)[run];
      run = Objects.requireNonNull(myAllNodesParents)[run];
    }

    return result;
  }

  public int getHashCodeForReversedChars(char[] chars) {
    return getHashCodeForReversedChars(chars, chars.length - 1, chars.length);
  }

  public int getHashCodeForReversedChars(char[] chars, int offset, int length) {
    int index = 0;
    while (length-- > 0) {
      index = getSubNode(index, chars[offset--], true);
    }
    return index;
  }

  public char[] getReversedChars(int hashCode) {
    int length = 0;
    int run = hashCode;
    while (run > 0) {
      length++;
      run = Objects.requireNonNull(myAllNodesParents)[run];
    }

    char[] result = new char[length];
    run = hashCode;
    for (int i = 0; i < length; i++) {
      assert run > 0;
      result[i] = Objects.requireNonNull(myAllNodesChars)[run];
      run = Objects.requireNonNull(myAllNodesParents)[run];
    }

    return result;
  }

  public int findSubNode(int parentIndex, char c) {
    return getSubNode(parentIndex, c, false);
  }

  private static final int LENGTH_SLOT_LENGTH = 1;

  private int getSubNode(int parentIndex, char c, boolean createIfNotExists) {
    if (Objects.requireNonNull(myAllNodesChildren)[parentIndex] == null) {
      if (!createIfNotExists) {
        return 0;
      }
      char[] chars = new char[1 + LENGTH_SLOT_LENGTH];
      myAllNodesChildren[parentIndex] = chars;
    }

    char[] children = Objects.requireNonNull(myAllNodesChildren[parentIndex]);
    char childrenCount = children[children.length - LENGTH_SLOT_LENGTH];
    int left = 0;
    int right = childrenCount - 1;
    while (left <= right) {
      int middle = (left + right) >> 1;
      int index = children[middle];
      int comp = Objects.requireNonNull(myAllNodesChars)[index] - c;
      if (comp == 0) {
        return index;
      }
      if (comp < 0) {
        left = middle + 1;
      }
      else {
        right = middle - 1;
      }
    }
    if (!createIfNotExists) {
      return 0;
    }

    if (childrenCount == children.length - LENGTH_SLOT_LENGTH) {
      children = myAllNodesChildren[parentIndex] = ArrayUtil.realloc(children, (children.length * 3 / 2 + 1) + LENGTH_SLOT_LENGTH);
    }

    if (left != childrenCount) {
      System.arraycopy(children, left, children, left + 1, childrenCount - left);
    }

    int index = myAllNodesSize;
    children[left] = (char)index;
    assert childrenCount + 1 < Character.MAX_VALUE;
    children[children.length - LENGTH_SLOT_LENGTH] = (char)(childrenCount + 1);

    addNode(parentIndex, c);
    return index;
  }

  public void clear() {
    init();
  }

  private static final ArrayFactory<char[]> FACTORY = char[][]::new;
}
