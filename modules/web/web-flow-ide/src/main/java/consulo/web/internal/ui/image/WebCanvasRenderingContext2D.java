/*
 * Copyright 2013-2023 consulo.io
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

import org.vaadin.pekkam.Canvas;
import org.vaadin.pekkam.CanvasRenderingContext2D;

/**
 * @author VISTALL
 * @since 29/05/2023
 */
public class WebCanvasRenderingContext2D extends CanvasRenderingContext2D {
  public WebCanvasRenderingContext2D(Canvas canvas) {
    super(canvas);
  }

  public void setGlobalAlpha(float value) {
    setProperty("globalAlpha", String.valueOf(value));
  }
}
