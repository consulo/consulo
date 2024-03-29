/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.event;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.EventObject;

public class CaretEvent extends EventObject {
  private final Caret myCaret;
  private final LogicalPosition myOldPosition;
  private final LogicalPosition myNewPosition;

  /**
   * @deprecated Use {@link #CaretEvent(Caret, LogicalPosition, LogicalPosition)} instead.
   */
  @Deprecated
  public CaretEvent(@Nonnull Editor editor, @Nonnull LogicalPosition oldPosition, @Nonnull LogicalPosition newPosition) {
    this(editor, null, oldPosition, newPosition);
  }

  /**
   * @deprecated Use {@link #CaretEvent(Caret, LogicalPosition, LogicalPosition)} instead.
   */
  @Deprecated
  public CaretEvent(@Nonnull Editor editor, @Nullable Caret caret, @Nonnull LogicalPosition oldPosition, @Nonnull LogicalPosition newPosition) {
    super(editor);
    myCaret = caret;
    myOldPosition = oldPosition;
    myNewPosition = newPosition;
  }

  public CaretEvent(@Nonnull Caret caret, @Nonnull LogicalPosition oldPosition, @Nonnull LogicalPosition newPosition) {
    super(caret.getEditor());
    myCaret = caret;
    myOldPosition = oldPosition;
    myNewPosition = newPosition;
  }

  @Nonnull
  public Editor getEditor() {
    return (Editor)getSource();
  }

  @Nullable
  public Caret getCaret() {
    return myCaret;
  }

  @Nonnull
  public LogicalPosition getOldPosition() {
    return myOldPosition;
  }

  @Nonnull
  public LogicalPosition getNewPosition() {
    return myNewPosition;
  }
}
