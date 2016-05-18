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

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public abstract class StyledSegment extends Segment {
  private Map<String, String> myStyles = new HashMap<String, String>();

  public StyledSegment(GwtTextRange textRange) {
    super(textRange);
  }


  public void add(String key, String value) {
    myStyles.put(key, value);
  }

  public abstract String getTextInner();

  @Override
  public String getText() {
    if (myStyles.isEmpty()) {
      return getTextInner();
    }

    StringBuilder builder = new StringBuilder();

    builder.append("<span style=\"");
    for (Map.Entry<String, String> entry : myStyles.entrySet()) {
      builder.append(entry.getKey()).append(":").append(entry.getValue());
    }
    builder.append("\">");
    builder.append(getTextInner());
    builder.append("</span>");
    return builder.toString();
  }
}