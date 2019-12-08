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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 11-Dec-16.
 */
class Alphabet {
  final static Map<Character, AlphabetDraw> validCharacters = new HashMap<>();
  final static Character[] alphabet;

  static {
    validCharacters.put('A', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 4);
        horizonalTop(data, offset);
        horizontal(data, 3, offset);
      }
    });

    validCharacters.put('B', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        data[offset + 1][1] = 1;
        data[offset + 2][1] = 1;
        data[offset + 3][1] = 1;
        data[offset + 3][2] = 1;
        data[offset + 4][4] = 1;
        horizontal(data, 3, offset);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('C', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalTop(data, offset);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('D', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 4, 2, 5);
        horizontal(data, 1, offset, 3);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('E', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalTop(data, offset);
        horizontal(data, 3, offset);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('F', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalTop(data, offset);
        horizontal(data, 3, offset);
      }
    });

    validCharacters.put('O', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 4);

        horizonalTop(data, offset);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('N', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        data[offset + 1][2] = 1;
        data[offset + 2][3] = 1;
        data[offset + 3][4] = 1;
        vertical(data, offset + 4);
      }
    });

    validCharacters.put('K', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        data[offset + 3][1] = 1;
        data[offset + 2][2] = 1;
        data[offset + 1][3] = 1;
        data[offset + 2][4] = 1;
        data[offset + 3][5] = 1;
      }
    });

    validCharacters.put('M', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        data[offset + 1][2] = 1;
        data[offset + 2][3] = 1;
        data[offset + 3][2] = 1;
        vertical(data, offset + 4);
      }
    });

    validCharacters.put('P', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalTop(data, offset);
        horizontal(data, 3, offset);
        data[offset + 4][2] = 1;
      }
    });

    validCharacters.put('R', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalTop(data, offset);
        horizontal(data, 3, offset);
        data[offset + 4][2] = 1;
        data[offset + 3][4] = 1;
        data[offset + 3][5] = 1;
        data[offset + 4][5] = 1;
      }
    });

    validCharacters.put('H', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizontal(data, 3, offset);
        vertical(data, offset + 4);
      }
    });

    validCharacters.put('S', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalTop(data, offset);
        horizonalDown(data, offset);
        horizontal(data, 3, offset);
        data[offset][2] = 1;
        data[offset + 4][4] = 1;
      }
    });

    validCharacters.put('T', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalTop(data, offset);
        vertical(data, offset + 2);
      }
    });

    validCharacters.put('I', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        data[offset + 1][1] = 1;
        data[offset + 3][1] = 1;
        vertical(data, offset + 2);
        data[offset + 1][5] = 1;
        data[offset + 3][5] = 1;
      }
    });
    validCharacters.put('J', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalTop(data, offset);
        vertical(data, offset + 3);

        data[offset][4] = 1;
        data[offset][5] = 1;
        data[offset + 1][5] = 1;
        data[offset + 2][5] = 1;
        data[offset + 3][5] = 1;
      }
    });

    validCharacters.put('U', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalDown(data, offset);
        vertical(data, offset);
        vertical(data, offset + 4);
      }
    });

    validCharacters.put('L', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('Q', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 4);

        horizonalTop(data, offset);
        horizonalDown(data, offset);

        data[offset + 3][4] = 1;
        data[offset + 4][5] = 0;
      }
    });

    validCharacters.put('G', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 4);

        horizonalTop(data, offset);
        horizonalDown(data, offset);

        data[offset + 4][2] = 0;
        data[offset + 2][3] = 1;
        data[offset + 3][3] = 1;
      }
    });

    validCharacters.put('Z', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalTop(data, offset);
        data[offset + 3][2] = 1;
        data[offset + 2][3] = 1;
        data[offset + 1][4] = 1;
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('Y', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        data[offset][1] = 1;
        data[offset][2] = 1;
        data[offset + 4][1] = 1;
        data[offset + 4][2] = 1;
        horizontal(data, 3, offset);
        data[offset + 2][4] = 1;
        data[offset + 2][5] = 1;
      }
    });

    validCharacters.put('X', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        data[offset][1] = 1;
        data[offset + 1][2] = 1;
        data[offset + 2][3] = 1;
        data[offset + 3][2] = 1;
        data[offset + 4][1] = 1;
        data[offset][5] = 1;
        data[offset + 1][4] = 1;
        data[offset + 3][4] = 1;
        data[offset + 4][5] = 1;
      }
    });

    validCharacters.put('V', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalDown(data, offset);
        vertical(data, offset);
        vertical(data, offset + 4);

        data[offset][4] = 0;
        data[offset + 1][4] = 1;
        data[offset][5] = 0;
        data[offset + 3][4] = 1;
        data[offset + 4][4] = 0;
        data[offset + 4][5] = 0;
      }
    });

    validCharacters.put('W', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        data[offset + 1][4] = 1;
        data[offset + 2][3] = 1;
        data[offset + 3][4] = 1;
        vertical(data, offset + 4);
      }
    });

    validCharacters.put('_', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        horizonalDown(data, offset);
      }
    });

    validCharacters.put('.', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        data[offset + 2][5] = 1;
      }
    });

    // dummy
    validCharacters.put(' ', new AlphabetDraw() {
      @Override
      void draw(int offset, int[][] data) {
        vertical(data, offset);
        vertical(data, offset + 1);
        vertical(data, offset + 2);
        vertical(data, offset + 3);
        vertical(data, offset + 4);
      }
    });

    Set<Character> list = new TreeSet<>(validCharacters.keySet());
    list.remove('_');
    list.remove('.');
    list.remove(' ');
    alphabet = list.toArray(new Character[list.size()]);
  }
}
