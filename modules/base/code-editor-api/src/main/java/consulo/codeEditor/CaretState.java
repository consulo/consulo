/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.codeEditor;

import org.jspecify.annotations.Nullable;

public class CaretState {
  private final @Nullable LogicalPosition myCaretPosition;
  private final int myVisualColumnAdjustment;
  private final @Nullable LogicalPosition mySelectionStart;
  private final @Nullable LogicalPosition mySelectionEnd;

  public CaretState(
    @Nullable LogicalPosition caretPosition,
    @Nullable LogicalPosition selectionStart,
    @Nullable LogicalPosition selectionEnd
  ) {
    this(caretPosition, 0, selectionStart, selectionEnd);
  }

  /**
   * @param visualColumnAdjustment see {@link #getVisualColumnAdjustment()}
   */
  public CaretState(
    @Nullable LogicalPosition caretPosition,
    int visualColumnAdjustment,
    @Nullable LogicalPosition selectionStart,
    @Nullable LogicalPosition selectionEnd
  ) {
    this.myCaretPosition = caretPosition;
    this.myVisualColumnAdjustment = visualColumnAdjustment;
    this.mySelectionStart = selectionStart;
    this.mySelectionEnd = selectionEnd;
  }

  public @Nullable LogicalPosition getCaretPosition() {
    return myCaretPosition;
  }

  /**
   * Sometimes logical caret position is not fully determining its visual position (e.g. around inlays). This value should be added to the
   * result of {@code editor.logicalToVisualPosition(caretState.getCaretPosition())}'s column,
   * if one needs to calculate caret's visual position.
   */
  public int getVisualColumnAdjustment() {
    return myVisualColumnAdjustment;
  }

  public @Nullable LogicalPosition getSelectionStart() {
    return mySelectionStart;
  }

  public @Nullable LogicalPosition getSelectionEnd() {
    return mySelectionEnd;
  }

  @Override
  public String toString() {
    return "CaretState{" +
      "caretPosition=" + myCaretPosition +
      (myVisualColumnAdjustment == 0 ? "" : (", visualColumnAdjustment=" + myVisualColumnAdjustment)) +
      ", selectionStart=" + mySelectionStart +
      ", selectionEnd=" + mySelectionEnd +
      '}';
  }
}
