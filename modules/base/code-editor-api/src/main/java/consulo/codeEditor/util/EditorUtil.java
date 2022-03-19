/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.LogicalPosition;
import consulo.colorScheme.TextAttributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Mar-22
 */
public class EditorUtil {
  public static boolean attributesImpactFontStyleOrColor(@Nullable TextAttributes attributes) {
    return attributes == TextAttributes.ERASE_MARKER || (attributes != null && (attributes.getFontType() != Font.PLAIN || attributes.getForegroundColor() != null));
  }

  public static int getTabSize(@Nonnull Editor editor) {
    return editor.getSettings().getTabSize(editor.getProject());
  }

  public static int getTotalInlaysHeight(@Nonnull List<? extends Inlay> inlays) {
    int sum = 0;
    for (Inlay inlay : inlays) {
      sum += inlay.getHeightInPixels();
    }
    return sum;
  }

  public static boolean inVirtualSpace(@Nonnull Editor editor, @Nonnull LogicalPosition logicalPosition) {
    return !editor.offsetToLogicalPosition(editor.logicalPositionToOffset(logicalPosition)).equals(logicalPosition);
  }
}
