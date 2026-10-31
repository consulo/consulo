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
package consulo.language.editor.impl.internal.richcopy.view;

import consulo.language.editor.impl.internal.richcopy.model.ColorRegistry;
import consulo.language.editor.impl.internal.richcopy.model.MarkupHandler;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.language.editor.impl.internal.richcopy.model.TypefaceRegistry;
import consulo.platform.Platform;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.font.Font;
import consulo.ui.font.Typeface;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Locale;

/**
 * @author Denis Zhdanov
 * @since 3/28/13 1:06 PM
 */
public class HtmlSyntaxInfoReader extends AbstractSyntaxAwareReader implements MarkupHandler {
  private final int myTabSize;
  protected StringBuilder myResultBuffer;
  private ColorRegistry myColorRegistry;
  private TypefaceRegistry myTypefaceRegistry;
  private int myMaxLength;

  private int myDefaultForeground;
  private int myDefaultBackground;
  private int myDefaultFontFamily;
  private int myForeground;
  private int myBackground;
  private int myFontFamily;
  private boolean myBold;
  private boolean myItalic;
  private int myCurrentColumn;

  private final Int2ObjectMap<String> myColors = new Int2ObjectOpenHashMap<>();

  public HtmlSyntaxInfoReader(SyntaxInfo syntaxInfo, int tabSize) {
    super(syntaxInfo);
    myTabSize = tabSize;
  }

  @Override
  protected void build(StringBuilder holder, int maxLength) {
    myResultBuffer = holder;
    myColorRegistry = mySyntaxInfo.getColorRegistry();
    myTypefaceRegistry = mySyntaxInfo.getTypefaceRegistry();
    myDefaultForeground = myForeground = mySyntaxInfo.getDefaultForeground();
    myDefaultBackground = myBackground = mySyntaxInfo.getDefaultBackground();
    myBold = myItalic = false;
    myCurrentColumn = 0;
    myMaxLength = maxLength;
    try {
      buildColorMap();
      appendStartTags();

      mySyntaxInfo.processOutputInfo(this);

      appendCloseTags();
    }
    finally {
      myResultBuffer = null;
      myColorRegistry = null;
      myTypefaceRegistry = null;
      myColors.clear();
    }
  }

  protected void appendCloseTags() {
    myResultBuffer.append("</pre></div>");
    generateCloseBodyHtmlTags();
  }

  protected void generateCloseBodyHtmlTags() {
    myResultBuffer.append("</body></html>");
  }

  protected void appendStartTags() {
    appendOpenBodyHtmlTags();

    // the background color is applied to a div block in order to be displayed in Google Docs correctly
    myResultBuffer.append("<div style=\"background-color:");
    appendColor(myResultBuffer, myDefaultBackground);
    myResultBuffer.append(";color:");
    appendColor(myResultBuffer, myDefaultForeground);
    myResultBuffer.append("\">");

    // the pre tag is used instead of `white-space:pre;`, as some applications drop the style
    myResultBuffer.append("<pre style=\"");
    int[] fontIds = myTypefaceRegistry.getAllIds();
    if (fontIds.length > 0) {
      myFontFamily = myDefaultFontFamily = fontIds[0];
      appendFontFamilyRule(myResultBuffer, myDefaultFontFamily);
    }
    else {
      myFontFamily = myDefaultFontFamily = -1;
    }
    appendFontSizeRule();
    myResultBuffer.append("\">");
  }

  protected void appendOpenBodyHtmlTags() {
    myResultBuffer.append("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></head><body>");
  }

  protected void appendFontSizeRule() {
    float fontSize = mySyntaxInfo.getFontSize();
    // on macOS font size in points declared in HTML doesn't mean the same value as when declared e.g. in TextEdit (and in Java),
    // this is the correction factor
    if (Platform.current().os().isMac()) {
      fontSize *= 0.75f;
    }
    myResultBuffer.append(String.format(Locale.US, "font-size:%.1fpt;", fontSize));
  }

  protected void appendFontFamilyRule(StringBuilder styleBuffer, int fontFamilyId) {
    Typeface typeface = myTypefaceRegistry.dataById(fontFamilyId);
    styleBuffer.append("font-family:'").append(typeface.getName()).append('\'');
    if (typeface.isMonospaced()) {
      styleBuffer.append(",monospace");
    }
    styleBuffer.append(';');
  }

  protected void appendLineBreak() {
    myResultBuffer.append("<br>");
  }

  private static void defineBold(StringBuilder styleBuffer) {
    styleBuffer.append("font-weight:bold;");
  }

  private static void defineItalic(StringBuilder styleBuffer) {
    styleBuffer.append("font-style:italic;");
  }

  private void defineForeground(int id, StringBuilder styleBuffer) {
    styleBuffer.append("color:");
    appendColor(styleBuffer, id);
    styleBuffer.append(";");
  }

  protected void defineBackground(int id, StringBuilder styleBuffer) {
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

  private static void appendColor(ColorValue color, StringBuilder sb) {
    RGBColor rgb = color.toRGB();

    if (rgb.getRed() < 16) {
      sb.append('0');
    }
    sb.append(Integer.toHexString(rgb.getRed()));
    if (rgb.getGreen() < 16) {
      sb.append('0');
    }
    sb.append(Integer.toHexString(rgb.getGreen()));
    if (rgb.getBlue() < 16) {
      sb.append('0');
    }
    sb.append(Integer.toHexString(rgb.getBlue()));
  }

  @Override
  public void handleText(int startOffset, int endOffset) {
    boolean formattedText =
      myForeground != myDefaultForeground || myBackground != myDefaultBackground || myFontFamily != myDefaultFontFamily || myBold || myItalic;
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
        case '<' -> myResultBuffer.append("&lt;");
        case '>' -> myResultBuffer.append("&gt;");
        case '&' -> myResultBuffer.append("&amp;");
        case ' ' -> myResultBuffer.append("&#32;");
        case '\n' -> {
          appendLineBreak();
          // -1 as the column is incremented right after the switch
          myCurrentColumn = -1;
        }
        case '\t' -> {
          int newColumn = (myCurrentColumn / myTabSize + 1) * myTabSize;
          for (; myCurrentColumn < newColumn; myCurrentColumn++) {
            myResultBuffer.append("&#32;");
          }
        }
        default -> myResultBuffer.append(c);
      }
      myCurrentColumn++;
    }
  }

  @Override
  public void handleForeground(int foregroundId) {
    myForeground = foregroundId;
  }

  @Override
  public void handleBackground(int backgroundId) {
    myBackground = backgroundId;
  }

  @Override
  public void handleFont(int fontNameId) {
    myFontFamily = fontNameId;
  }

  @Override
  public void handleStyle(int style) {
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
