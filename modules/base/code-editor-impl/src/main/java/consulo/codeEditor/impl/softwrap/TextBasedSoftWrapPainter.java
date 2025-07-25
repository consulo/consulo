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
package consulo.codeEditor.impl.softwrap;

import consulo.codeEditor.Editor;
import consulo.codeEditor.SoftWrapDrawingType;
import consulo.codeEditor.TextDrawingCallback;
import consulo.codeEditor.impl.ColorProvider;
import consulo.codeEditor.impl.FontInfo;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@link SoftWrapPainter} implementation that uses target unicode symbols as soft wrap drawings.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @author 2010-07-01
 */
public class TextBasedSoftWrapPainter implements SoftWrapPainter {

  private final Map<SoftWrapDrawingType, char[]> mySymbols = new EnumMap<SoftWrapDrawingType, char[]>(SoftWrapDrawingType.class);
  private final Map<SoftWrapDrawingType, FontInfo> myFonts = new EnumMap<SoftWrapDrawingType, FontInfo>(SoftWrapDrawingType.class);

  /**
   * Use array here because profiling indicates that using EnumMap here gives significant performance degradation.
   */
  private final int[] myWidths = new int[SoftWrapDrawingType.values().length];
  private final Map<SoftWrapDrawingType, Integer> myVGaps = new EnumMap<SoftWrapDrawingType, Integer>(SoftWrapDrawingType.class);

  private final TextDrawingCallback myDrawingCallback;
  private final ColorProvider myColorHolder;
  private boolean myCanUse;
  private final Editor myEditor;

  public TextBasedSoftWrapPainter(Map<SoftWrapDrawingType, Character> symbols, Editor editor, TextDrawingCallback drawingCallback, ColorProvider colorHolder) throws IllegalArgumentException {
    if (symbols.size() != SoftWrapDrawingType.values().length) {
      throw new IllegalArgumentException(
              String.format("Can't create text-based soft wrap painter. Reason: given 'drawing type -> symbol' mappings " + "are incomplete - expected size %d but got %d (%s)",
                            SoftWrapDrawingType.values().length, symbols.size(), symbols));
    }
    myEditor = editor;
    myDrawingCallback = drawingCallback;
    myColorHolder = colorHolder;
    for (Map.Entry<SoftWrapDrawingType, Character> entry : symbols.entrySet()) {
      mySymbols.put(entry.getKey(), new char[]{entry.getValue()});
    }
    reinit();
  }

  @Override
  public int paint(@Nonnull Graphics g, @Nonnull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    FontInfo fontInfo = myFonts.get(drawingType);
    if (fontInfo != null) {
      char[] buffer = mySymbols.get(drawingType);
      int vGap = myVGaps.get(drawingType);
      myDrawingCallback.drawChars(g, buffer, 0, buffer.length, x, y + lineHeight - vGap, TargetAWT.to(myColorHolder.getColor()), fontInfo);
    }
    return getMinDrawingWidth(drawingType);
  }

  @Override
  public int getDrawingHorizontalOffset(@Nonnull Graphics g, @Nonnull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    return getMinDrawingWidth(drawingType);
  }

  @Override
  public int getMinDrawingWidth(@Nonnull SoftWrapDrawingType drawingType) {
    return myWidths[drawingType.ordinal()];
  }

  @Override
  public boolean canUse() {
    return myCanUse;
  }

  /**
   * Tries to find fonts that are capable to display all unicode symbols used by the current painter.
   */
  @Override
  public void reinit() {
    // We use dummy component here in order to being able to work with font metrics.
    JLabel component = new JLabel();

    myCanUse = true;
    for (Map.Entry<SoftWrapDrawingType, char[]> entry : mySymbols.entrySet()) {
      SoftWrapDrawingType type = entry.getKey();
      char c = entry.getValue()[0];
      FontInfo fontInfo = EditorImplUtil.fontForChar(c, Font.PLAIN, myEditor);
      if (!fontInfo.canDisplay(c)) {
        myCanUse = false;
        myFonts.put(type, null);
        myVGaps.put(type, null);
        myWidths[type.ordinal()] = 0;
      }
      else {
        myFonts.put(type, fontInfo);
        FontMetrics metrics = component.getFontMetrics(fontInfo.getFont());
        myWidths[type.ordinal()] = metrics.charWidth(c);
        int vGap = metrics.getDescent();
        myVGaps.put(type, vGap);
      }
    }
  }
}
