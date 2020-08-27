/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal.image;

import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageState;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-08-27
 */
public class DesktopStatedImageImpl<S> implements Icon, Image {
  private final ImageState<S> myState;
  private final Function<S, Image> myImageFunction;

  public DesktopStatedImageImpl(ImageState<S> state, Function<S, Image> imageFunction) {
    myState = state;
    myImageFunction = imageFunction;
  }

  @Override
  public int getHeight() {
    return myImageFunction.apply(myState.getState()).getHeight();
  }

  @Override
  public int getWidth() {
    return myImageFunction.apply(myState.getState()).getWidth();
  }

  @Override
  public int getIconWidth() {
    return getWidth();
  }

  @Override
  public int getIconHeight() {
    return getHeight();
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Image image = myImageFunction.apply(myState.getState());

    Icon icon = TargetAWT.to(image);

    icon.paintIcon(c, g, x, y);
  }
}
