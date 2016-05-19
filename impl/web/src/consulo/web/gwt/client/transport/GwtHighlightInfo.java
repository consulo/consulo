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
package consulo.web.gwt.client.transport;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class GwtHighlightInfo implements IsSerializable {
  public static int BOLD = 1 << 1;
  public static int ITALIC = 1 << 2;
  public static int UNDERLINE = 1 << 3;
  public static int LINE_THROUGH = 1 << 4;

  private GwtColor myForeground;
  private GwtColor myBackground;
  private int myFlags;
  private GwtTextRange myTextRange;
  private String myTooltip;
  private int mySeverity;

  public GwtHighlightInfo() {
  }

  public GwtHighlightInfo(GwtColor foreground, GwtColor background, int flags, GwtTextRange textRange, int severity) {
    myForeground = foreground;
    myBackground = background;
    myFlags = flags;
    myTextRange = textRange;
    mySeverity = severity;
  }

  public boolean isEmpty() {
    return myFlags == 0 && myForeground == null && myBackground == null && myTooltip == null;
  }

  public int getSeverity() {
    return mySeverity;
  }

  public String getTooltip() {
    return myTooltip;
  }

  public void setTooltip(String tooltip) {
    myTooltip = tooltip;
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

  public GwtTextRange getTextRange() {
    return myTextRange;
  }
}
