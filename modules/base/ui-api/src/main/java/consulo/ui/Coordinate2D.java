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
package consulo.ui;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public final class Coordinate2D implements Serializable {
  private static final long serialVersionUID = -8454212049522017852L;

  private int myX;
  private int myY;

  private Coordinate2D() {
  }

  public Coordinate2D(int x, int y) {
    myX = x;
    myY = y;
  }

  public int getX() {
    return myX;
  }

  public int getY() {
    return myY;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Coordinate2D that = (Coordinate2D)o;

    if (myX != that.myX) return false;
    if (myY != that.myY) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myX;
    result = 31 * result + myY;
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Coordinate2D{");
    sb.append("myX=").append(myX);
    sb.append(", myY=").append(myY);
    sb.append('}');
    return sb.toString();
  }
}
