/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.web.internal.util;

import consulo.ui.color.RGBColor;
import consulo.web.gwt.shared.ui.state.HorizontalAlignment;
import consulo.web.gwt.shared.ui.state.RGBColorShared;
import consulo.web.gwt.shared.ui.state.border.BorderPosition;
import consulo.web.gwt.shared.ui.state.border.BorderStyle;

/**
 * @author VISTALL
 * @since 2020-10-24
 */
public class Mappers {
  public static RGBColorShared map(RGBColor color) {
    RGBColorShared rgbColorShared = new RGBColorShared();
    rgbColorShared.myBlue = color.getBlue();
    rgbColorShared.myRed = color.getRed();
    rgbColorShared.myGreen = color.getGreen();
    return rgbColorShared;
  }

  public static BorderPosition map(consulo.ui.border.BorderPosition position) {
    switch (position) {
      case TOP:
        return BorderPosition.TOP;
      case BOTTOM:
        return BorderPosition.BOTTOM;
      case LEFT:
        return BorderPosition.LEFT;
      case RIGHT:
        return BorderPosition.RIGHT;
    }
    throw new UnsupportedOperationException();
  }

  public static HorizontalAlignment map(consulo.ui.HorizontalAlignment alignment) {
    switch (alignment) {

      case LEFT:
        return HorizontalAlignment.LEFT;
      case CENTER:
        return HorizontalAlignment.CENTER;
      case RIGHT:
        return HorizontalAlignment.RIGHT;
    }

    throw new UnsupportedOperationException();
  }

  public static BorderStyle map(consulo.ui.border.BorderStyle style) {
    switch (style) {
      case LINE:
      return BorderStyle.LINE;
      case EMPTY:
        return BorderStyle.EMPTY;
    }
    throw new UnsupportedOperationException();
  }
}
