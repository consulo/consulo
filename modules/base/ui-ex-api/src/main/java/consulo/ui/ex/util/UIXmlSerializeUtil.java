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
package consulo.ui.ex.util;

import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 2022-12-30
 */
public final class UIXmlSerializeUtil {
  private static final String X = "x";
  private static final String Y = "y";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  
  @Nullable
  public static Point2D getLocation(@Nullable Element element) {
    return element == null ? null : getLocation(element, X, Y);
  }

  @Nullable
  public static Point2D getLocation(@Nonnull Element element, @Nonnull String x, @Nonnull String y) {
    String sX = element.getAttributeValue(x);
    if (sX == null) return null;
    String sY = element.getAttributeValue(y);
    if (sY == null) return null;
    try {
      return new Point2D(Integer.parseInt(sX), Integer.parseInt(sY));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setLocation(@Nonnull Element element, @Nonnull Point2D location) {
    return setLocation(element, X, Y, location);
  }

  @Nonnull
  public static Element setLocation(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull Point2D location) {
    return element.setAttribute(x, Integer.toString(location.x())).setAttribute(y, Integer.toString(location.y()));
  }

  @Nullable
  public static Size2D getSize(@Nullable Element element) {
    return element == null ? null : getSize(element, WIDTH, HEIGHT);
  }

  @Nullable
  public static Size2D getSize(@Nonnull Element element, @Nonnull String width, @Nonnull String height) {
    String sWidth = element.getAttributeValue(width);
    if (sWidth == null) return null;
    String sHeight = element.getAttributeValue(height);
    if (sHeight == null) return null;
    try {
      int iWidth = Integer.parseInt(sWidth);
      if (iWidth <= 0) return null;
      int iHeight = Integer.parseInt(sHeight);
      if (iHeight <= 0) return null;
      return new Size2D(iWidth, iHeight);
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setSize(@Nonnull Element element, @Nonnull Size2D size) {
    return setSize(element, WIDTH, HEIGHT, size);
  }

  @Nonnull
  public static Element setSize(@Nonnull Element element, @Nonnull String width, @Nonnull String height, @Nonnull Size2D size) {
    return element.setAttribute(width, Integer.toString(size.width())).setAttribute(height, Integer.toString(size.height()));
  }


  @Nullable
  public static Rectangle2D getBounds(@Nullable Element element) {
    return element == null ? null : getBounds(element, X, Y, WIDTH, HEIGHT);
  }

  @Nullable
  public static Rectangle2D getBounds(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull String width, @Nonnull String height) {
    String sX = element.getAttributeValue(x);
    if (sX == null) return null;
    String sY = element.getAttributeValue(y);
    if (sY == null) return null;
    String sWidth = element.getAttributeValue(width);
    if (sWidth == null) return null;
    String sHeight = element.getAttributeValue(height);
    if (sHeight == null) return null;
    try {
      int iWidth = Integer.parseInt(sWidth);
      if (iWidth <= 0) return null;
      int iHeight = Integer.parseInt(sHeight);
      if (iHeight <= 0) return null;
      return new Rectangle2D(Integer.parseInt(sX), Integer.parseInt(sY), iWidth, iHeight);
    }
    catch (NumberFormatException ignored) {
      return null;
    }
  }

  @Nonnull
  public static Element setBounds(@Nonnull Element element, @Nonnull Rectangle2D bounds) {
    return setBounds(element, X, Y, WIDTH, HEIGHT, bounds);
  }

  @Nonnull
  public static Element setBounds(@Nonnull Element element, @Nonnull String x, @Nonnull String y, @Nonnull String width, @Nonnull String height, @Nonnull Rectangle2D bounds) {
    return element.setAttribute(x, Integer.toString(bounds.minX()))
        .setAttribute(y, Integer.toString(bounds.minY()))
        .setAttribute(width, Integer.toString(bounds.width()))
        .setAttribute(height, Integer.toString(bounds.height()));
  }
}
