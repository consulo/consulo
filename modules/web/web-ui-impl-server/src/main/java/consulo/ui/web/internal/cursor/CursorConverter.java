/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.web.internal.cursor;

import com.vaadin.ui.Component;
import consulo.ui.cursor.Cursor;
import consulo.ui.cursor.StandardCursors;

/**
 * @author VISTALL
 * @since 10/08/2021
 */
public class CursorConverter {
  public static final String UI_CURSOR_AUTO = "ui-cursor-auto";
  public static final String UI_CURSOR_DEFAULT = "ui-cursor-default";
  public static final String UI_CURSOR_CROSSHAIR = "ui-cursor-crosshair";
  public static final String UI_CURSOR_TEXT = "ui-cursor-text";
  public static final String UI_CURSOR_WAIT = "ui-cursor-wait";
  public static final String UI_CURSOR_HAND = "ui-cursor-hand";

  private static String[] ourStyles = {UI_CURSOR_HAND, UI_CURSOR_WAIT, UI_CURSOR_AUTO, UI_CURSOR_DEFAULT, UI_CURSOR_TEXT, UI_CURSOR_CROSSHAIR};

  public static void setCursor(Component component, Cursor cursor) {
    component.removeStyleNames(ourStyles);

    if (cursor == null) {
      component.addStyleName(UI_CURSOR_AUTO);
    }
    else if (cursor instanceof StandardCursors standardCursor) {
      switch (standardCursor) {
        case ARROW:
          component.addStyleName(UI_CURSOR_DEFAULT);
          break;
        case CROSSHAIR:
          component.addStyleName(UI_CURSOR_CROSSHAIR);
          break;
        case TEXT:
          component.addStyleName(UI_CURSOR_TEXT);
          break;
        case WAIT:
          component.addStyleName(UI_CURSOR_WAIT);
          break;
        case HAND:
          component.addStyleName(UI_CURSOR_HAND);
          break;
        default:
          throw new UnsupportedOperationException(cursor.toString());
      }
    }
    else {
      throw new UnsupportedOperationException(cursor.toString());
    }
  }
}
