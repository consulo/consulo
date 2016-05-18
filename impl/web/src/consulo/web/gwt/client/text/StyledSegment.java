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
package consulo.web.gwt.client.text;

import consulo.web.gwt.client.transport.GwtTextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public abstract class StyledSegment extends Segment {
  public static class StyleInfo {
    private String key;
    private String value;
    private int flag;

    public StyleInfo(String key, String value, int flag) {
      this.key = key;
      this.value = value;
      this.flag = flag;
    }
  }

  private List<StyleInfo> myStyles = new ArrayList<StyleInfo>();

  public StyledSegment(GwtTextRange textRange) {
    super(textRange);
  }

  public void add(String key, String value, int flag) {
    myStyles.add(new StyleInfo(key, value, flag));
  }

  public void copy(StyledSegment to) {
    for (StyleInfo style : myStyles) {
      to.add(style.key, style.value, style.flag);
    }
  }

  public void removeByFlag(int flag) {
    StyleInfo[] styleInfos = myStyles.toArray(new StyleInfo[myStyles.size()]);
    for (StyleInfo styleInfo : styleInfos) {
      if (styleInfo.flag == flag) {

        myStyles.remove(styleInfo);
      }
    }
  }

  public abstract String getTextInner();

  @Override
  public String getText() {
    StringBuilder builder = new StringBuilder();

    GwtTextRange textRange = getTextRange();
    builder.append("<span class=\"gen_TextRage_").append(textRange.getStartOffset()).append("_").append(textRange.getEndOffset()).append("\" style=\"");
    for (StyleInfo entry : myStyles) {
      builder.append(entry.key).append(":").append(entry.value);
    }
    builder.append("\">");
    builder.append(getTextInner());
    builder.append("</span>");
    return builder.toString();
  }
}