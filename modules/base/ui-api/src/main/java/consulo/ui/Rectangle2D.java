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

import javax.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public final class Rectangle2D implements Serializable, Cloneable {
  private static final long serialVersionUID = 4140523038283686399L;

  private Coordinate2D myCoordinate;
  private Size mySize;

  private Rectangle2D() {
  }

  public Rectangle2D(int width, int height) {
    this(0, 0, width, height);
  }

  public Rectangle2D(int x, int y, int width, int height) {
    this(new Coordinate2D(x, y), new Size(width, height));
  }

  public Rectangle2D(@Nonnull Coordinate2D coordinate, @Nonnull Size size) {
    myCoordinate = coordinate;
    mySize = size;
  }

  public int getHeight() {
    return mySize.getHeight();
  }

  public int getWidth() {
    return mySize.getWidth();
  }

  public int getX() {
    return myCoordinate.getX();
  }

  public int getY() {
    return myCoordinate.getY();
  }

  @Nonnull
  public Coordinate2D getCoordinate() {
    return myCoordinate;
  }

  @Nonnull
  public Size getSize() {
    return mySize;
  }

  @Override
  public Rectangle2D clone() {
    return new Rectangle2D(myCoordinate, mySize);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Rectangle2D that = (Rectangle2D)o;

    if (!myCoordinate.equals(that.myCoordinate)) return false;
    if (!mySize.equals(that.mySize)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myCoordinate.hashCode();
    result = 31 * result + (mySize.hashCode());
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Rectangle2D{");
    sb.append("myCoordinate=").append(myCoordinate);
    sb.append(", mySize=").append(mySize);
    sb.append('}');
    return sb.toString();
  }
}
