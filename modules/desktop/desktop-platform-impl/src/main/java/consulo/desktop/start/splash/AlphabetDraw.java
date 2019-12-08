/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.start.splash;

/**
 * @author VISTALL
 * @since 11-Dec-16.
 */
abstract class AlphabetDraw {
  void horizonalTop(int[][] data, int fromY) {
    horizontal(data, 1, fromY);
  }

  void horizonalDown(int[][] data, int fromY) {
    horizontal(data, 5, fromY);
  }

  void horizontal(int[][] data, int x, int fromY) {
    horizontal(data, x, fromY, 4);
  }

  void horizontal(int[][] data, int x, int fromY, int step) {
    for (int i = fromY; i <= (fromY + step); i++) {
      data[i][x] = 1;
    }
  }

  void vertical(int[][] data, int y) {
    vertical(data, y, 1, 5);
  }

  void vertical(int[][] data, int y, int fromX, int toX) {
    for (int i = fromX; i <= toX; i++) {
      data[y][i] = 1;
    }
  }

  abstract void draw(int offset, int[][] data);
}
