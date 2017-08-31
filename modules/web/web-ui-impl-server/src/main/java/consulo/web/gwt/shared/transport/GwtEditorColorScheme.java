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
package consulo.web.gwt.shared.transport;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class GwtEditorColorScheme implements IsSerializable {
  public static final String LINE_NUMBERS_COLOR = "LINE_NUMBERS_COLOR";
  public static final String GUTTER_BACKGROUND = "GUTTER_BACKGROUND";
  public static final String CARET_ROW_COLOR = "CARET_ROW_COLOR";
  public static final String TEARLINE_COLOR = "TEARLINE_COLOR";

  public static final String TEXT = "TEXT";
  public static final String CTRL_CLICKABLE = "CTRL_CLICKABLE";

  public static String[] fetchColors = new String[]{LINE_NUMBERS_COLOR, GUTTER_BACKGROUND, CARET_ROW_COLOR, TEARLINE_COLOR};
  public static String[] fetchAttributes = new String[]{TEXT, CTRL_CLICKABLE};

  private String myName;
  private Map<String, GwtColor> myColors = new HashMap<String, GwtColor>();
  private Map<String, GwtTextAttributes> myAttributes = new HashMap<String, GwtTextAttributes>();

  public GwtEditorColorScheme() {
  }

  public GwtEditorColorScheme(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public GwtColor getColor(String colorKey) {
    return myColors.get(colorKey);
  }

  public void putColor(String key, GwtColor colorKey) {
    myColors.put(key, colorKey);
  }

  public GwtTextAttributes getAttributes(String colorKey) {
    return myAttributes.get(colorKey);
  }

  public void putAttributes(String key, GwtTextAttributes attributes) {
    myAttributes.put(key, attributes);
  }
}
