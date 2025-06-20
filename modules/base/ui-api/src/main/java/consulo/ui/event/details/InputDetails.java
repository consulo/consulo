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

import consulo.ui.Point2D;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2021-08-17
 */
public class InputDetails {
  /**
   * Position inside component - relative
   */
  private final Point2D myPosition;
  /**
   * Position XY on screen - absolute
   */
  private final Point2D myPositionOnScreen;

  public InputDetails(@Nonnull Point2D position, @Nonnull Point2D positionOnScreen) {
    myPosition = position;
    myPositionOnScreen = positionOnScreen;
  }

  public int getX() {
    return myPosition.x();
  }

  public int getY() {
    return myPosition.y();
  }

  public int getXOnScreen() {
    return myPositionOnScreen.x();
  }

  public int getYOnScreen() {
    return myPositionOnScreen.y();
  }

  public Point2D getPosition() {
    return myPosition;
  }

  public Point2D getPositionOnScreen() {
    return myPositionOnScreen;
  }
}
