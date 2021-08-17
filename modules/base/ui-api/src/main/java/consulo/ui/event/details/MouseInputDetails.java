/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.event.details;

import javax.annotation.Nonnull;
import java.util.EnumSet;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
public class MouseInputDetails extends ModifiedInputDetails {
  public static enum MouseButton {
    LEFT,
    MIDDLE,
    RIGHT
  }

  private final MouseButton myButton;

  public MouseInputDetails(int x, int y, @Nonnull EnumSet<Modifier> modifiers, @Nonnull MouseButton button) {
    super(x, y, modifiers);
    myButton = button;
  }

  @Nonnull
  public MouseButton getButton() {
    return myButton;
  }
}
