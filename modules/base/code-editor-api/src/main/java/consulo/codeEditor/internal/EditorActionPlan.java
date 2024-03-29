/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.codeEditor.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.ActionPlan;
import consulo.util.lang.ImmutableCharSequence;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditorActionPlan implements ActionPlan {
  private ImmutableCharSequence myText;
  private final Editor myEditor;
  private int myCaretOffset;
  private final List<Replacement> myReplacements = new ArrayList<>();

  public EditorActionPlan(@Nonnull Editor editor) {
    myEditor = editor;
    myText = (ImmutableCharSequence)editor.getDocument().getImmutableCharSequence();
    myCaretOffset = editor.getCaretModel().getOffset();
  }

  @Nonnull
  @Override
  public ImmutableCharSequence getText() {
    return myText;
  }

  @Override
  public void replace(int begin, int end, String s) {
    myText = myText.delete(begin, end).insert(begin, s);
    myReplacements.add(new Replacement(begin, end, s));
    if (myCaretOffset == end) {
      myCaretOffset += s.length() - (end - begin);
    }
  }

  @Override
  public int getCaretOffset() {
    return myCaretOffset;
  }

  @Override
  public void setCaretOffset(int offset) {
    myCaretOffset = offset;
  }

  public List<Replacement> getReplacements() {
    return Collections.unmodifiableList(myReplacements);
  }

  public int getCaretShift() {
    return myCaretOffset - myEditor.getCaretModel().getOffset();
  }

  public static class Replacement {
    private final int myBegin;
    private final int myEnd;
    private final String myText;

    public Replacement(int begin, int end, String text) {
      myBegin = begin;
      myEnd = end;
      myText = text;
    }

    public int getBegin() {
      return myBegin;
    }

    public int getEnd() {
      return myEnd;
    }

    public String getText() {
      return myText;
    }
  }
}
