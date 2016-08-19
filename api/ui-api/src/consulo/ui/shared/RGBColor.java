/*
 * Copyright 2013-2016 must-be.org
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
package consulo.ui.shared;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 21-Jun-16
 */
public class RGBColor implements Serializable {
  private int myRed;
  private int myGreed;
  private int myBlue;

  private RGBColor() {
  }

  public RGBColor(int red, int greed, int blue) {
    myRed = red;
    myGreed = greed;
    myBlue = blue;
  }

  public int getRed() {
    return myRed;
  }

  public int getGreed() {
    return myGreed;
  }

  public int getBlue() {
    return myBlue;
  }
}
