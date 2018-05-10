/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client.ui.ex;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.InlineHTML;
import consulo.web.gwt.client.util.BitUtil;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.shared.transport.GwtColor;
import consulo.web.gwt.shared.transport.GwtHighlightInfo;
import consulo.web.gwt.shared.transport.GwtTextAttributes;
import consulo.web.gwt.shared.transport.GwtTextRange;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public class GwtEditorSegmentBuilder {
  public static class CharSpan extends InlineHTML {
    public static class StyleInfo {
      private String key;
      private String value;
      private String tooltip;
      private int flag;

      public StyleInfo(String key, String value, String tooltip,int flag) {
        this.key = key;
        this.flag = flag;
        this.value = value;
        this.tooltip = tooltip;
      }
    }

    public GwtEditorTextRange range;
    public boolean lineWrap;
    private int highlightFlags;

    @Nullable
    private List<StyleInfo> myStyles;
    @Nullable
    private Map<String, Integer> mySeverityMap;

    public CharSpan(String html) {
      super(html);
    }

    public String getToolTip() {
      if(myStyles == null) {
        return null;
      }
      for (StyleInfo style : myStyles) {
        if(style.flag == GwtEditorImpl.ourLexerFlag) {
          continue;
        }

        String tooltip = style.tooltip;
        if(tooltip != null) {
          return tooltip;
        }
      }
        return null;
    }

    public void add(String key, String value, String tooltip, int severity, int flag) {
      highlightFlags = BitUtil.set(highlightFlags, flag, true);

      Integer oldSeverity = mySeverityMap == null ? null : mySeverityMap.get(key);
      if (oldSeverity != null && severity <= oldSeverity) {
        return;
      }

      if (severity != 0) {
        if (mySeverityMap == null) {
          mySeverityMap = new HashMap<String, Integer>();
        }

        mySeverityMap.put(key, severity);
      }

      if (myStyles == null) {
        myStyles = new ArrayList<StyleInfo>();
      }
      myStyles.add(new StyleInfo(key, value, tooltip, flag));

      Style style = getElement().getStyle();
      if (key.equals("textDecoration")) {
        String oldValue = style.getProperty(key);
        if (oldValue != null) {
          style.setProperty(key, oldValue + " " + value);
          return;
        }
      }

      style.setProperty(key, value);
    }

    public void removeByFlag(int flag) {
      if (BitUtil.isSet(highlightFlags, flag)) {
        highlightFlags = BitUtil.set(highlightFlags, flag, false);

        if (myStyles == null) {
          return;
        }

        StyleInfo[] styleInfos = myStyles.toArray(new StyleInfo[myStyles.size()]);
        for (StyleInfo styleInfo : styleInfos) {
          if (mySeverityMap != null) {
            mySeverityMap.remove(styleInfo.key);
          }

          if (styleInfo.flag == flag) {
            myStyles.remove(styleInfo);

            Style style = getElement().getStyle();
            if (styleInfo.key.equals("textDecoration")) {
              String oldValue = style.getProperty(styleInfo.key);
              if (oldValue == null) {
                continue;
              }

              // it mixin - need removed only our value
              if (oldValue.contains(" ")) {
                oldValue = oldValue.replace(" " + styleInfo.value, "");
                style.setProperty(styleInfo.key, oldValue);
                continue;
              }
            }

            style.setProperty(styleInfo.key, null);
          }
        }
      }
    }
  }

  private CharSpan[] myFragments;
  private int myLineCount;

  public GwtEditorSegmentBuilder(String text) {
    myFragments = new CharSpan[text.length()];

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String labelText = mapChar(c);


      final int startOffset = i;
      if (c == ' ' || c == '\t') {
        for (int k = startOffset + 1; k < text.length(); k++) {
          char nextChar = text.charAt(k);

          if (nextChar == c) {
            labelText += mapChar(nextChar);
            i++;
          }
          else {
            break;
          }
        }
      }

      int endOffset = i + 1;

      CharSpan charSpan = new CharSpan(labelText.isEmpty() ? "&#8205;" : labelText);
      charSpan.range = new GwtEditorTextRange(startOffset, endOffset);
      charSpan.setStyleName(null);
      charSpan.getElement().setPropertyObject("range", charSpan.range);
      charSpan.getElement().setPropertyObject("widget", charSpan);

      charSpan.lineWrap = labelText.isEmpty();

      if (charSpan.lineWrap) {
        myLineCount++;
      }

      for (int k = startOffset; k < endOffset; k++) {
        myFragments[k] = charSpan;
      }
    }

    if(!text.isEmpty() && myLineCount == 0) {
      myLineCount = 1;
    }
  }

  public void addHighlights(List<GwtHighlightInfo> result, int flag) {
    for (CharSpan fragment : myFragments) {
      fragment.removeByFlag(flag);
    }

    for (GwtHighlightInfo highlightInfo : result) {
      GwtTextRange textRange = highlightInfo.getTextRange();
      for (int i = textRange.getStartOffset(); i < textRange.getEndOffset(); i++) {
        CharSpan fragment = myFragments[i];

        GwtTextAttributes textAttributes = highlightInfo.getTextAttributes();
        if(textAttributes != null) {
          add(fragment, textAttributes, highlightInfo.getTooltip(), highlightInfo.getSeverity(), flag);
        }
      }
    }
  }

  public void removeHighlightByRange(GwtEditorTextRange textRange, int flag) {
    for (int i = textRange.getStartOffset(); i < textRange.getEndOffset(); i++) {
      CharSpan fragment = myFragments[i];

      fragment.removeByFlag(flag);
    }
  }

  private void add(CharSpan fragment, GwtTextAttributes textAttributes, String tooltip, int severity, int flag) {
    GwtColor foreground = textAttributes.getForeground();
    if (foreground != null) {
      fragment.add("color", GwtStyleUtil.toString(foreground), tooltip, severity, flag);
    }

    GwtColor background = textAttributes.getBackground();
    if (background != null) {
      fragment.add("backgroundColor", GwtStyleUtil.toString(background), tooltip, severity, flag);
    }

    if (BitUtil.isSet(textAttributes.getFlags(), GwtTextAttributes.BOLD)) {
      fragment.add("fontWeight", "bold", tooltip, severity, flag);
    }

    if (BitUtil.isSet(textAttributes.getFlags(), GwtTextAttributes.ITALIC)) {
      fragment.add("fontStyle", "italic", tooltip, severity, flag);
    }

    if (BitUtil.isSet(textAttributes.getFlags(), GwtTextAttributes.UNDERLINE)) {
      fragment.add("textDecoration", "underline", tooltip, severity, flag);
    }

    if (BitUtil.isSet(textAttributes.getFlags(), GwtTextAttributes.LINE_THROUGH)) {
      fragment.add("textDecoration", "line-through", tooltip, severity, flag);
    }
  }

  public int getLineCount() {
    return myLineCount;
  }

  public CharSpan[] getFragments() {
    return myFragments;
  }

  private static String mapChar(char c) {
    switch (c) {
      case '&':
        return "&amp;";
      case '<':
        return "&lt;";
      case '>':
        return "&gt;";
      case '\"':
        return "&quot;";
      case '\t':
        return "&emsp;";
      case ' ':
        return "&nbsp;";
      case '\n':
        return ""; // hack
    }
    return String.valueOf(c);
  }
}
