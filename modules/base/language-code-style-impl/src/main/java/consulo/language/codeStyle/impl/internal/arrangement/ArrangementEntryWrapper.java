/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.codeStyle.impl.internal.arrangement;

import consulo.document.Document;
import consulo.language.codeStyle.arrangement.ArrangementEntry;
import consulo.language.psi.PsiFile;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Auxiliary data structure used {@link ArrangementEngine#arrange(PsiFile, Collection) arrangement}.
 * <p/>
 * The general idea is to provide the following:
 * <pre>
 * <ul>
 *   <li>'parent-child' and 'sibling' relations between the {@link ArrangementEntry entries};</li>
 *   <li>ability to reflect actual entry range (after its arrangement and/or blank lines addition/removal);</li>
 * </ul>
 * </pre>
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/31/12 12:06 PM
 */
public class ArrangementEntryWrapper<E extends ArrangementEntry> {

  @Nonnull
  private final List<ArrangementEntryWrapper<E>> myChildren = new ArrayList<ArrangementEntryWrapper<E>>();
  @Nonnull
  private final E myEntry;

  @Nullable private ArrangementEntryWrapper<E> myParent;
  @Nullable private ArrangementEntryWrapper<E> myPrevious;
  @Nullable
  private ArrangementEntryWrapper<E> myNext;

  private int myStartOffset;
  private int myEndOffset;
  private int myBlankLinesBefore;

  @SuppressWarnings("unchecked")
  public ArrangementEntryWrapper(@Nonnull E entry) {
    myEntry = entry;
    myStartOffset = entry.getStartOffset();
    myEndOffset = entry.getEndOffset();
    ArrangementEntryWrapper<E> previous = null;
    for (ArrangementEntry child : entry.getChildren()) {
      ArrangementEntryWrapper<E> childWrapper = new ArrangementEntryWrapper<E>((E)child);
      childWrapper.setParent(this);
      if (previous != null) {
        previous.setNext(childWrapper);
        childWrapper.setPrevious(previous);
      }
      previous = childWrapper;
      myChildren.add(childWrapper);
    }
  }

  @Nonnull
  public E getEntry() {
    return myEntry;
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  public int getEndOffset() {
    return myEndOffset;
  }

  public void setEndOffset(int endOffset) {
    myEndOffset = endOffset;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getParent() {
    return myParent;
  }

  public void setParent(@Nullable ArrangementEntryWrapper<E> parent) {
    myParent = parent;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getPrevious() {
    return myPrevious;
  }

  public void setPrevious(@Nullable ArrangementEntryWrapper<E> previous) {
    myPrevious = previous;
  }

  @Nullable
  public ArrangementEntryWrapper<E> getNext() {
    return myNext;
  }

  public int getBlankLinesBefore() {
    return myBlankLinesBefore;
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  public void updateBlankLines(@Nonnull Document document) {
    int startLine = document.getLineNumber(getStartOffset());
    myBlankLinesBefore = 0;
    if (startLine <= 0) {
      return;
    }
    
    CharSequence text = document.getCharsSequence();
    int lastLineFeed = document.getLineStartOffset(startLine) - 1;
    for (int i = lastLineFeed - 1; i >= 0; i--) {
      i = CharArrayUtil.shiftBackward(text, i, " \t");
      if (text.charAt(i) == '\n') {
        ++myBlankLinesBefore;
      }
      else {
        break;
      }
    }
  }
  
  public void setNext(@Nullable ArrangementEntryWrapper<E> next) {
    myNext = next;
  }

  @Nonnull
  public List<ArrangementEntryWrapper<E>> getChildren() {
    return myChildren;
  }

  public void applyShift(int shift) {
    myStartOffset += shift;
    myEndOffset += shift;
    for (ArrangementEntryWrapper<E> child : myChildren) {
      child.applyShift(shift);
    }
  }

  @Override
  public int hashCode() {
    return myEntry.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArrangementEntryWrapper wrapper = (ArrangementEntryWrapper)o;
    return myEntry.equals(wrapper.myEntry);
  }

  @Override
  public String toString() {
    return String.format("range: [%d; %d), entry: %s", myStartOffset, myEndOffset, myEntry.toString());
  }
}
