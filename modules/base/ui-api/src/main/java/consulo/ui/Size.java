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
package consulo.ui;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public final class Size implements Serializable {
  public static final Size UNDEFINED = new Size(-1, -1);
  public static final Size ZERO = new Size(0, 0);

  private static final long serialVersionUID = 3195037735722861866L;

  private int myWidth;
  private int myHeight;

  private Size() {
  }

  public Size(int widthAndHeight) {
    this(widthAndHeight, widthAndHeight);
  }

  public Size(int width, int height) {
    myWidth = width;
    myHeight = height;
  }

  public int getWidth() {
    return myWidth;
  }

  public int getHeight() {
    return myHeight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Size size = (Size)o;

    if (myWidth != size.myWidth) return false;
    if (myHeight != size.myHeight) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myWidth;
    result = 31 * result + myHeight;
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Size{");
    sb.append("myWidth=").append(myWidth);
    sb.append(", myHeight=").append(myHeight);
    sb.append('}');
    return sb.toString();
  }
}
