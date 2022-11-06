/*
 * Copyright 2013-2022 consulo.io
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
import java.util.Objects;

/**
 * @author VISTALL
 * @since 06/11/2022
 */
public final class Position2D implements Serializable {
  public static final Position2D OUT_OF_RANGE = new Position2D(Integer.MAX_VALUE, Integer.MAX_VALUE);

  private final int myX;
  private final int myY;

  public Position2D(int x, int y) {
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
    Position2D that = (Position2D)o;
    return myX == that.myX && myY == that.myY;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myX, myY);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Position2D{");
    sb.append("myX=").append(myX);
    sb.append(", myY=").append(myY);
    sb.append('}');
    return sb.toString();
  }
}
