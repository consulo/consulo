/*
 * Copyright 2013-2019 consulo.io
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
package consulo.awt;

import com.intellij.util.ui.JBUI;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.cursor.Cursor;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public interface TargetAWTFacade {
  @Nonnull
  java.awt.Dimension to(@Nonnull Size size);

  @Nonnull
  java.awt.Color to(@Nonnull RGBColor color);

  @Contract("null -> null")
  java.awt.Color to(@Nullable ColorValue colorValue);

  @Contract("null -> null")
  java.awt.Rectangle to(@Nullable Rectangle2D rectangle2D);

  @Contract("null -> null")
  java.awt.Component to(@Nullable Component component);

  @Contract("null -> null")
  Component from(@Nullable java.awt.Component component);

  @Nonnull
  default Component wrap(@Nonnull java.awt.Component component) {
    throw new UnsupportedOperationException();
  }

  @Contract("null -> null")
  java.awt.Window to(@Nullable Window component);

  @Contract("null -> null")
  Window from(@Nullable java.awt.Window component);

  @Contract("null -> null")
  Rectangle2D from(@Nullable java.awt.Rectangle rectangle);

  @Contract("null -> null")
  ColorValue from(@Nullable java.awt.Color color);

  @Contract("null -> null")
  javax.swing.Icon to(@Nullable Image image);

  @Contract("null -> null")
  Image from(@Nullable Icon icon);

  @Nonnull
  java.awt.Font to(@Nonnull Font font);

  java.awt.Image toImage(@Nonnull ImageKey key, @Nullable JBUI.ScaleContext ctx);

  default java.awt.Cursor to(Cursor cursor) {
    throw new AbstractMethodError();
  }

  default Cursor from(java.awt.Cursor cursor) {
    throw new AbstractMethodError();
  }
}
