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
package consulo.codeEditor.impl.softwrap.mapping;

import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.LogicalPosition;

import jakarta.annotation.Nonnull;

public class EditorPosition implements Cloneable {
  public int logicalLine;
  public int offset;
  public int x;

  private final Editor myEditor;

  public EditorPosition(@Nonnull Editor editor) {
    myEditor = editor;
  }

  public EditorPosition(@Nonnull LogicalPosition logical, int offset, @Nonnull Editor editor) {
    myEditor = editor;
    logicalLine = logical.line;
    this.offset = offset;
  }

  public void onNewLine() {
    logicalLine++;
    x = 0;
    offset++;
  }

  /**
   * Updates state of the current processing position in order to point it to the end offset of the given fold region.
   *
   * @param foldRegion                        fold region which end offset should be pointed by the current position
   */
  public void advance(@Nonnull FoldRegion foldRegion) {
    offset = foldRegion.getEndOffset();

    Document document = myEditor.getDocument();
    int endOffsetLogicalLine = document.getLineNumber(foldRegion.getEndOffset());
    if (logicalLine != endOffsetLogicalLine) {
      int linesDiff = endOffsetLogicalLine - logicalLine;
      logicalLine += linesDiff;
    }
  }

  public void from(@Nonnull EditorPosition position) {
    logicalLine = position.logicalLine;
    offset = position.offset;
    x = position.x;
  }

  @Override
  public EditorPosition clone() {
    EditorPosition result = new EditorPosition(myEditor);
    result.logicalLine = logicalLine;
    result.offset = offset;
    result.x = x;
    return result;
  }

  @Override
  public String toString() {
    return String.format("logical line: %d; offset: %d", logicalLine, offset);
  }
}
