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
package consulo.language.editor.impl.highlight;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.document.util.ProperTextRange;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author max
 */
public abstract class VisibleHighlightingPassFactory  {
  @Nonnull
  public static ProperTextRange calculateVisibleRange(@Nonnull Editor editor) {
    Rectangle rect = editor.getScrollingModel().getVisibleArea();
    LogicalPosition startPosition = editor.xyToLogicalPosition(new Point(rect.x, rect.y));

    int visibleStart = editor.logicalPositionToOffset(startPosition);
    LogicalPosition endPosition = editor.xyToLogicalPosition(new Point(rect.x + rect.width, rect.y + rect.height));

    int visibleEnd = editor.logicalPositionToOffset(new LogicalPosition(endPosition.line + 1, 0));

    return new ProperTextRange(visibleStart, Math.max(visibleEnd, visibleStart));
  }
}
