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

import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.transport.GwtColor;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtTextRange;
import consulo.web.gwt.client.util.BitUtil;
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
      private int flag;

      public StyleInfo(String key, int flag) {
        this.key = key;
        this.flag = flag;
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

      if(severity != 0) {
        if (mySeverityMap == null) {
          mySeverityMap = new HashMap<String, Integer>();
        }

        mySeverityMap.put(key, severity);
      }

      if (myStyles == null) {
        myStyles = new ArrayList<StyleInfo>();
      }
      myStyles.add(new StyleInfo(key, flag));

      widget.getElement().getStyle().setProperty(key, value);
    }

    public void removeByFlag(int flag) {
      if (BitUtil.isSet(highlightFlags, flag)) {
        highlightFlags = BitUtil.set(highlightFlags, flag, false);

        if (myStyles == null) {
          return;
        }

        StyleInfo[] styleInfos = myStyles.toArray(new StyleInfo[myStyles.size()]);
        for (StyleInfo styleInfo : styleInfos) {
          if(mySeverityMap != null) {
            mySeverityMap.remove(styleInfo.key);
          }

          if (styleInfo.flag == flag) {
            widget.getElement().getStyle().setProperty(styleInfo.key, null);

            myStyles.remove(styleInfo);
          }
        }
      }
    }
  }

  private List<Fragment> myFragments = new ArrayList<Fragment>();
  private int myLineCount;

  public EditorSegmentBuilder(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      String labelText = mapChar(c);

      Fragment fragment = new Fragment();

      final int startIndex = i;
      if (c == ' ' || c == '\t') {
        for (int k = startIndex + 1; k < text.length(); k++) {
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

      fragment.range = new GwtTextRange(startIndex, i + 1);
      fragment.widget = new InlineHTML(labelText);
      fragment.widget.setStyleName(null);
      fragment.widget.getElement().setPropertyObject("range", fragment.range);

      fragment.lineWrap = labelText.isEmpty();

      if (fragment.lineWrap) {
        myLineCount++;
      }

      myFragments.add(fragment);
    }
  }

  public void addHighlights(List<GwtHighlightInfo> result, int flag) {
    for (Fragment fragment : myFragments) {
      fragment.removeByFlag(flag);

      for (GwtHighlightInfo highlightInfo : result) {
        if (highlightInfo.getTextRange().containsRange(fragment.range)) {
          add(fragment, highlightInfo, highlightInfo.getSeverity(), flag);
        }
      }
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

    if (highlightInfo.isBold()) {
      fragment.add("fontWeight", "bold", severity, flag);
    }

    if (highlightInfo.isItalic()) {
      fragment.add("fontStyle", "italic", severity, flag);
    }
  }

  public int getLineCount() {
    return myLineCount;
  }

  public List<Fragment> getFragments() {
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
