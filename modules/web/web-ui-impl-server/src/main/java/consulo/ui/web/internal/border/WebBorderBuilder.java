/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.web.internal.border;

import consulo.ui.Component;
import consulo.ui.border.BorderPosition;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.BorderInfo;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.web.internal.base.DataObjectHolder;
import consulo.ui.web.internal.util.Mappers;
import consulo.web.gwt.shared.ui.state.border.BorderListState;

import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebBorderBuilder {
  public static void fill(Component component, BorderListState borderListState) {
    DataObjectHolder dataObjectHolder = (DataObjectHolder)component;

    borderListState.myBorders.clear();

    Map<BorderPosition, BorderInfo> borders = dataObjectHolder.dataObject().getBorders();
    if(borders.isEmpty()) {
      return;
    }
    
    for (BorderInfo info : borders.values()) {
      BorderListState.BorderState borderState = new BorderListState.BorderState();
      borderState.myPosition = Mappers.map(info.getBorderPosition());
      borderState.myStyle = Mappers.map(info.getBorderStyle());

      ColorValue colorValue = info.getColorValue() == null ? null : info.getColorValue();

      borderState.myColor = colorValue == null ? null : Mappers.map(colorValue.toRGB());
      borderState.myWidth = info.getWidth();

      borderListState.myBorders.add(borderState);
    }
  }
}
