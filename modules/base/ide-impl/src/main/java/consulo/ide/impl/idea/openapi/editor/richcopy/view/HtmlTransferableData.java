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
package consulo.ide.impl.idea.openapi.editor.richcopy.view;

import consulo.ide.impl.idea.openapi.editor.richcopy.model.ColorRegistry;
import consulo.ide.impl.idea.openapi.editor.richcopy.model.FontNameRegistry;
import consulo.ide.impl.idea.openapi.editor.richcopy.model.MarkupHandler;
import consulo.ide.impl.idea.openapi.editor.richcopy.model.SyntaxInfo;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import gnu.trove.TIntObjectHashMap;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 1:06 PM
 */
public class HtmlTransferableData extends AbstractSyntaxAwareReaderTransferableData implements MarkupHandler {

  @Nonnull
  public static final DataFlavor FLAVOR = new DataFlavor("text/html; class=java.io.Reader; charset=UTF-8", "HTML text");

  private final int myTabSize;
  private StringBuilder    myResultBuffer;
  private ColorRegistry    myColorRegistry;
  private FontNameRegistry myFontNameRegistry;
  private int myMaxLength;

  private int     myDefaultForeground;
  private int     myDefaultBackground;
  private int     myDefaultFontFamily;
  private int     myForeground;
  private int     myBackground;
  private int     myFontFamily;
  private boolean myBold;
  private boolean myItalic;
  private int     myCurrentColumn;

  private final TIntObjectHashMap<String> myColors = new TIntObjectHashMap<String>();

  public HtmlTransferableData(@Nonnull SyntaxInfo syntaxInfo, int tabSize) {
    super(syntaxInfo, FLAVOR);
    myTabSize = tabSize;
  }

  @Override
  protected void build(@Nonnull StringBuilder holder, int maxLength) {
    myResultBuffer = holder;
    myColorRegistry = mySyntaxInfo.getColorRegistry();
    myFontNameRegistry = mySyntaxInfo.getFontNameRegistry();
    myDefaultForeground = myForeground = mySyntaxInfo.getDefaultForeground();
    myDefaultBackground = myBackground = mySyntaxInfo.getDefaultBackground();
    myBold = myItalic = false;
    myCurrentColumn = 0;
    myMaxLength = maxLength;
    try {
      buildColorMap();
      myResultBuffer.append("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body>")
              .append("<pre style=\"background-color:");
      appendColor(myResultBuffer, myDefaultBackground);
      myResultBuffer.append(";color:");
      appendColor(myResultBuffer, myDefaultForeground);
      myResultBuffer.append(';');
      int[] fontIds = myFontNameRegistry.getAllIds();
      if (fontIds.length > 0) {
        myFontFamily = myDefaultFontFamily = fontIds[0];
        appendFontFamilyRule(myResultBuffer, myDefaultFontFamily);
      }
      else {
        myFontFamily = myDefaultFontFamily = -1;
      }
      myResultBuffer.append("font-size:").append(mySyntaxInfo.getFontSize()).append("pt;\">");

      mySyntaxInfo.processOutputInfo(this);

      myResultBuffer.append("</pre></body></html>");
    }
    finally {
      myResultBuffer = null;
      myColorRegistry = null;
      myFontNameRegistry = null;
      myColors.clear();
    }
  }

  private void appendFontFamilyRule(@Nonnull StringBuilder styleBuffer, int fontFamilyId) {
    styleBuffer.append("font-family:'").append(myFontNameRegistry.dataById(fontFamilyId)).append("';");
  }

  private static void defineBold(@Nonnull StringBuilder styleBuffer) {
    styleBuffer.append("font-weight:bold;");
  }

  private static void defineItalic(@Nonnull StringBuilder styleBuffer) {
    styleBuffer.append("font-style:italic;");
  }

  private void defineForeground(int id, @Nonnull StringBuilder styleBuffer) {
    styleBuffer.append("color:");
    appendColor(styleBuffer, id);
    styleBuffer.append(";");
  }

  private void defineBackground(int id, @Nonnull StringBuilder styleBuffer) {
    styleBuffer.append("background-color:");
    appendColor(styleBuffer, id);
    styleBuffer.append(";");
  }

  private void appendColor(StringBuilder builder, int id) {
    builder.append(myColors.get(id));
  }

  private void buildColorMap() {
    for (int id : myColorRegistry.getAllIds()) {
      StringBuilder b = new StringBuilder("#");
      appendColor(myColorRegistry.dataById(id), b);
      myColors.put(id, b.toString());
    }
  }

  private static void appendColor(final ColorValue color, final StringBuilder sb) {
    RGBColor rgb = color.toRGB();

    if (rgb.getRed() < 16) sb.append('0');
    sb.append(Integer.toHexString(rgb.getRed()));
    if (rgb.getGreen() < 16) sb.append('0');
    sb.append(Integer.toHexString(rgb.getGreen()));
    if (rgb.getBlue() < 16) sb.append('0');
    sb.append(Integer.toHexString(rgb.getBlue()));
  }

  @Override
  public void handleText(int startOffset, int endOffset) {
    boolean formattedText = myForeground != myDefaultForeground || myBackground != myDefaultBackground || myFontFamily != myDefaultFontFamily || myBold || myItalic;
    if (!formattedText) {
      escapeAndAdd(startOffset, endOffset);
      return;
    }

    myResultBuffer.append("<span style=\"");
    if (myForeground != myDefaultForeground) {
      defineForeground(myForeground, myResultBuffer);
    }
    if (myBackground != myDefaultBackground) {
      defineBackground(myBackground, myResultBuffer);
    }
    if (myBold) {
      defineBold(myResultBuffer);
    }
    if (myItalic) {
      defineItalic(myResultBuffer);
    }
    if (myFontFamily != myDefaultFontFamily) {
      appendFontFamilyRule(myResultBuffer, myFontFamily);
    }
    myResultBuffer.append("\">");
    escapeAndAdd(startOffset, endOffset);
    myResultBuffer.append("</span>");
  }

  private void escapeAndAdd(int start, int end) {
    for (int i = start; i < end; i++) {
      char c = myRawText.charAt(i);
      switch (c) {
        case '<': myResultBuffer.append("&lt;"); break;
        case '>': myResultBuffer.append("&gt;"); break;
        case '&': myResultBuffer.append("&amp;"); break;
        case ' ': myResultBuffer.append("&#32;"); break;
        case '\n': myResultBuffer.append("<br>"); myCurrentColumn = 0; break;
        case '\t':
          int newColumn = (myCurrentColumn / myTabSize + 1) * myTabSize;
          for (; myCurrentColumn < newColumn; myCurrentColumn++) myResultBuffer.append("&#32;");
          break;
        default: myResultBuffer.append(c);
      }
      myCurrentColumn++;
    }
  }

  @Override
  public void handleForeground(int foregroundId) throws Exception {
    myForeground = foregroundId;
  }

  @Override
  public void handleBackground(int backgroundId) throws Exception {
    myBackground = backgroundId;
  }

  @Override
  public void handleFont(int fontNameId) throws Exception {
    myFontFamily = fontNameId;
  }

  @Override
  public void handleStyle(int style) throws Exception {
    myBold = (Font.BOLD & style) != 0;
    myItalic = (Font.ITALIC & style) != 0;
  }

  @Override
  public boolean canHandleMore() {
    if (myResultBuffer.length() > myMaxLength) {
      myResultBuffer.append("... truncated ...");
      return false;
    }
    return true;
  }
}
