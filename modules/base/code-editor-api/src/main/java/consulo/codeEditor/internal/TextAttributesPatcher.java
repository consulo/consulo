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
package consulo.codeEditor.internal;

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29-Apr-22
 */
public class TextAttributesPatcher {

  /**
   * Patches attributes to be visible under debugger active line
   */
  @SuppressWarnings("UseJBColor")
  public static TextAttributes patchAttributesColor(TextAttributes attributes, @Nonnull TextRange range, @Nonnull Editor editor) {
    if (attributes.getForegroundColor() == null && attributes.getEffectColor() == null) return attributes;
    MarkupModel model = DocumentMarkupModel.forDocument(editor.getDocument(), editor.getProject(), false);
    if (model != null) {
      if (!((MarkupModelEx)model).processRangeHighlightersOverlappingWith(range.getStartOffset(), range.getEndOffset(), highlighter -> {
        if (highlighter.isValid() && highlighter.getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE) {
          TextAttributes textAttributes = highlighter.getTextAttributes(editor.getColorsScheme());
          if (textAttributes != null) {
            RGBColor color = textAttributes.getBackgroundColor() == null ? null : textAttributes.getBackgroundColor().toRGB();
            return !(color != null && color.getBlue() > 128 && color.getRed() < 128 && color.getGreen() < 128);
          }
        }
        return true;
      })) {
        TextAttributes clone = attributes.clone();
        clone.setForegroundColor(StandardColors.ORANGE);
        clone.setEffectColor(StandardColors.ORANGE);
        return clone;
      }
    }
    return attributes;
  }
}
