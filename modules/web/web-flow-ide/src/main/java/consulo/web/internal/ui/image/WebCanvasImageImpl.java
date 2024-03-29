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
package consulo.web.internal.ui.image;

import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2020-10-04
 */
public class WebCanvasImageImpl implements Image, WebImageCanvasDraw {
  private final Consumer<Canvas2D> myConsumer;
  private final int myWidth;
  private final int myHeight;

  public WebCanvasImageImpl(int width, int height, Consumer<Canvas2D> consumer) {
    myWidth = width;
    myHeight = height;
    myConsumer = consumer;
  }

  @Override
  public int getHeight() {
    return myHeight;
  }

  @Override
  public int getWidth() {
    return myWidth;
  }

  @Override
  public void drawCanvas(WebCanvasRenderingContext2D context) {
    // TODO draw
  }
}
