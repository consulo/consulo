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
package consulo.desktop.awt.startup.splash;

/**
 * @author VISTALL
 * @author UNV
 * @since 2016-12-11
 */
public class Glyph {
    private final int[] rows;

    Glyph(int row0, int row1, int row2, int row3, int row4) {
        this.rows = new int[]{row0, row1, row2, row3, row4};
    }

    void draw(int offset, int[][] data) {
        for (int y = 0; y < rows.length; y++) {
            int row = rows[y];
            for (int x = 0; x < 5; x++) {
                data[x + offset][y + 1] = (row >> (4 - x)) & 0x1;
            }
        }
    }
}
