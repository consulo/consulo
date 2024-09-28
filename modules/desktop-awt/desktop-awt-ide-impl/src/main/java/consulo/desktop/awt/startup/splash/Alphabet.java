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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 2016-12-11
 */
class Alphabet {
    final static Map<Character, AlphabetDraw> VALID_CHARACTERS;
    final static Character[] ALPHABET;

    static {
        Map<Character, AlphabetDraw> font = new HashMap<>();

        font.put('A', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10001,
            0b10001
        ));

        font.put('B', new AlphabetDraw(
            0b11110,
            0b10010,
            0b11111,
            0b10001,
            0b11111
        ));

        font.put('C', new AlphabetDraw(
            0b11111,
            0b10000,
            0b10000,
            0b10000,
            0b11111
        ));

        font.put('D', new AlphabetDraw(
            0b11110,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        font.put('E', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b10000,
            0b11111
        ));

        font.put('F', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b10000,
            0b10000
        ));

        font.put('G', new AlphabetDraw(
            0b11111,
            0b10000,
            0b10111,
            0b10001,
            0b11111
        ));

        font.put('H', new AlphabetDraw(
            0b10001,
            0b10001,
            0b11111,
            0b10001,
            0b10001
        ));

        font.put('I', new AlphabetDraw(
            0b01110,
            0b00100,
            0b00100,
            0b00100,
            0b01110
        ));

        font.put('J', new AlphabetDraw(
            0b11111,
            0b00010,
            0b00010,
            0b10010,
            0b11110
        ));

        font.put('K', new AlphabetDraw(
            0b10010,
            0b10100,
            0b11000,
            0b10100,
            0b10010
        ));

        font.put('L', new AlphabetDraw(
            0b10000,
            0b10000,
            0b10000,
            0b10000,
            0b11111
        ));

        font.put('M', new AlphabetDraw(
            0b10001,
            0b11011,
            0b10101,
            0b10001,
            0b10001
        ));

        font.put('N', new AlphabetDraw(
            0b10001,
            0b11001,
            0b10101,
            0b10011,
            0b10001
        ));

        font.put('O', new AlphabetDraw(
            0b11111,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        font.put('P', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10000,
            0b10000
        ));

        font.put('Q', new AlphabetDraw(
            0b11111,
            0b10001,
            0b10001,
            0b10011,
            0b11110
        ));

        font.put('R', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10010,
            0b10011
        ));

        font.put('S', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b00001,
            0b11111
        ));

        font.put('T', new AlphabetDraw(
            0b11111,
            0b00100,
            0b00100,
            0b00100,
            0b00100
        ));

        font.put('U', new AlphabetDraw(
            0b10001,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        font.put('V', new AlphabetDraw(
            0b10001,
            0b10001,
            0b01010,
            0b01010,
            0b01110
        ));

        font.put('W', new AlphabetDraw(
            0b10001,
            0b10001,
            0b10101,
            0b11011,
            0b10001
        ));

        font.put('X', new AlphabetDraw(
            0b10001,
            0b01010,
            0b00100,
            0b01010,
            0b10001
        ));

        font.put('Y', new AlphabetDraw(
            0b10001,
            0b10001,
            0b11111,
            0b00100,
            0b00100
        ));

        font.put('Z', new AlphabetDraw(
            0b11111,
            0b00010,
            0b00100,
            0b01000,
            0b11111
        ));

        Set<Character> chars = new TreeSet<>(font.keySet());
        ALPHABET = chars.toArray(new Character[chars.size()]);

        font.put('_', new AlphabetDraw(
            0b00000,
            0b00000,
            0b00000,
            0b00000,
            0b11111
        ));

        font.put('.', new AlphabetDraw(
            0b00000,
            0b00000,
            0b00000,
            0b00000,
            0b00100
        ));

        // dummy
        font.put(' ', new AlphabetDraw(
            0b11111,
            0b11111,
            0b11111,
            0b11111,
            0b11111
        ));

        VALID_CHARACTERS = Collections.unmodifiableMap(font);
    }
}
