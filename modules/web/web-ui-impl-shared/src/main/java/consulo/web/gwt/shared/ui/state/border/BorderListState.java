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
package consulo.web.gwt.shared.ui.state.border;

import consulo.web.gwt.shared.ui.state.RGBColorShared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class BorderListState implements Serializable {

  public static class BorderState implements Serializable {
    // please keep sync with consulo.ui.border.BorderPosition
    public static final byte TOP = 0;
    public static final byte BOTTOM = 1;
    public static final byte LEFT = 2;
    public static final byte RIGHT = 3;

    // please keep sync with consulo.ui.border.BorderStyle
    public static final byte LINE = 0;

    public byte myPosition;
    public byte myStyle;
    public RGBColorShared myColor;
    public int myWidth;
  }

  public List<BorderState> myBorders = new ArrayList<>();
}
