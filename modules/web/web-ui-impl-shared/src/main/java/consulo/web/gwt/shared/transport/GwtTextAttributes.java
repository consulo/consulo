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

import consulo.annotation.DeprecationInfo;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 20-May-16
 */
@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public class GwtTextAttributes implements Serializable {
  public static int BOLD = 1 << 1;
  public static int ITALIC = 1 << 2;
  public static int UNDERLINE = 1 << 3;
  public static int LINE_THROUGH = 1 << 4;

  private GwtColor myForeground;
  private GwtColor myBackground;
  private int myFlags;


  public GwtTextAttributes() {
  }

  public GwtTextAttributes(GwtColor foreground, GwtColor background, int flags) {
    myForeground = foreground;
    myBackground = background;
    myFlags = flags;
  }

  public boolean isEmpty() {
    return myFlags == 0 && myForeground == null && myBackground == null;
  }

  public GwtColor getBackground() {
    return myBackground;
  }

  public GwtColor getForeground() {
    return myForeground;
  }

  public int getFlags() {
    return myFlags;
  }
}
