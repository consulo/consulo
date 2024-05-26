/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.image.reference;

import consulo.ui.ex.awt.JBUI;
import consulo.ui.impl.image.ImageReference;
import jakarta.annotation.Nonnull;

import java.awt.*;

/**
 * @author VISTALL
 * @since 26.05.2024
 */
public abstract class DesktopAWTImageReference implements ImageReference {
  protected DesktopAWTImageReference() {
  }

  public abstract void draw(@Nonnull JBUI.ScaleContext ctx,
                            @Nonnull Graphics2D graphics,
                            int x,
                            int y,
                            int width,
                            int height);
}
