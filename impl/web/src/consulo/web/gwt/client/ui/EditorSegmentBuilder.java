/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.BitUtil;
import consulo.web.gwt.shared.transport.GwtColor;
import consulo.web.gwt.shared.transport.GwtHighlightInfo;
import consulo.web.gwt.shared.transport.GwtTextRange;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public class EditorSegmentBuilder {
  public static class Fragment {
    public static class StyleInfo {
      private String key;
      private String value;
      private int flag;

      public StyleInfo(String key, String value, int flag) {
        this.key = key;
        this.flag = flag;
        this.value = value;
      }
    }

    public Widget widget;
    public GwtTextRange range;
    public boolean lineWrap;
    private int highlightFlags;

    @Nullable
    private List<StyleInfo> myStyles;
    @Nullable
    private Map<String, Integer> mySeverityMap;

    public void add(String key, String value, int severity, int flag) {
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
      myStyles.add(new StyleInfo(key, value, flag));

      Style style = widget.getElement().getStyle();
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

            Style style = widget.getElement().getStyle();
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

  private Fragment[] myFragments;
  private int myLineCount;

  public EditorSegmentBuilder(String text) {
    myFragments = new Fragment[text.length()];

    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String labelText = mapChar(c);

      Fragment fragment = new Fragment();

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
      fragment.range = new GwtTextRange(startOffset, endOffset);
      fragment.widget = new InlineHTML(labelText);
      fragment.widget.setStyleName(null);
      fragment.widget.getElement().setPropertyObject("range", fragment.range);

      fragment.lineWrap = labelText.isEmpty();

      if (fragment.lineWrap) {
        myLineCount++;
      }

      for (int k = startOffset; k < endOffset; k++) {
        myFragments[k] = fragment;
      }
    }
  }

  public void addHighlights(List<GwtHighlightInfo> result, int flag) {
    for (Fragment fragment : myFragments) {
      fragment.removeByFlag(flag);
    }

    for (GwtHighlightInfo highlightInfo : result) {
      GwtTextRange textRange = highlightInfo.getTextRange();
      for (int i = textRange.getStartOffset(); i < textRange.getEndOffset(); i++) {
        Fragment fragment = myFragments[i];

        add(fragment, highlightInfo, highlightInfo.getSeverity(), flag);
      }
    }
  }

  public void removeHighlightByRange(GwtTextRange textRange, int flag) {
    for (int i = textRange.getStartOffset(); i < textRange.getEndOffset(); i++) {
      Fragment fragment = myFragments[i];

      fragment.removeByFlag(flag);
    }
  }

  private void add(Fragment fragment, GwtHighlightInfo highlightInfo, int severity, int flag) {
    GwtColor foreground = highlightInfo.getForeground();
    if (foreground != null) {
      fragment.add("color", "rgb(" + foreground.getRed() + ", " + foreground.getGreen() + ", " + foreground.getBlue() + ")", severity, flag);
    }

    GwtColor background = highlightInfo.getBackground();
    if (background != null) {
      fragment.add("backgroundColor", "rgb(" + background.getRed() + ", " + background.getGreen() + ", " + background.getBlue() + ")", severity, flag);
    }

    if (BitUtil.isSet(highlightInfo.getFlags(), GwtHighlightInfo.BOLD)) {
      fragment.add("fontWeight", "bold", severity, flag);
    }

    if (BitUtil.isSet(highlightInfo.getFlags(), GwtHighlightInfo.ITALIC)) {
      fragment.add("fontStyle", "italic", severity, flag);
    }

    if (BitUtil.isSet(highlightInfo.getFlags(), GwtHighlightInfo.UNDERLINE)) {
      fragment.add("textDecoration", "underline", severity, flag);
    }

    if (BitUtil.isSet(highlightInfo.getFlags(), GwtHighlightInfo.LINE_THROUGH)) {
      fragment.add("textDecoration", "line-through", severity, flag);
    }
  }

  public int getLineCount() {
    return myLineCount;
  }

  public Fragment[] getFragments() {
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
