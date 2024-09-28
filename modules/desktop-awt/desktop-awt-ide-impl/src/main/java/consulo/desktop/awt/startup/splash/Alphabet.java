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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 2016-12-11
 */
class Alphabet {
    final static Map<Character, AlphabetDraw> validCharacters = new HashMap<>();
    final static Character[] alphabet;

    static {
        validCharacters.put('A', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10001,
            0b10001
        ));

        validCharacters.put('B', new AlphabetDraw(
            0b11110,
            0b10010,
            0b11111,
            0b10001,
            0b11111
        ));

        validCharacters.put('C', new AlphabetDraw(
            0b11111,
            0b10000,
            0b10000,
            0b10000,
            0b11111
        ));

        validCharacters.put('D', new AlphabetDraw(
            0b11110,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        validCharacters.put('E', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b10000,
            0b11111
        ));

        validCharacters.put('F', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b10000,
            0b10000
        ));

        validCharacters.put('O', new AlphabetDraw(
            0b11111,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        validCharacters.put('N', new AlphabetDraw(
            0b10001,
            0b11001,
            0b10101,
            0b10011,
            0b10001
        ));

        validCharacters.put('K', new AlphabetDraw(
            0b10010,
            0b10100,
            0b11000,
            0b10100,
            0b10010
        ));

        validCharacters.put('M', new AlphabetDraw(
            0b10001,
            0b11011,
            0b10101,
            0b10001,
            0b10001
        ));

        validCharacters.put('P', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10000,
            0b10000
        ));

        validCharacters.put('R', new AlphabetDraw(
            0b11111,
            0b10001,
            0b11111,
            0b10010,
            0b10011
        ));

        validCharacters.put('H', new AlphabetDraw(
            0b10001,
            0b10001,
            0b11111,
            0b10001,
            0b10001
        ));

        validCharacters.put('S', new AlphabetDraw(
            0b11111,
            0b10000,
            0b11111,
            0b00001,
            0b11111
        ));

        validCharacters.put('T', new AlphabetDraw(
            0b11111,
            0b00100,
            0b00100,
            0b00100,
            0b00100
        ));

        validCharacters.put('I', new AlphabetDraw(
            0b01110,
            0b00100,
            0b00100,
            0b00100,
            0b01110
        ));
        validCharacters.put('J', new AlphabetDraw(
            0b11111,
            0b00010,
            0b00010,
            0b10010,
            0b11110
        ));

        validCharacters.put('U', new AlphabetDraw(
            0b10001,
            0b10001,
            0b10001,
            0b10001,
            0b11111
        ));

        validCharacters.put('L', new AlphabetDraw(
            0b10000,
            0b10000,
            0b10000,
            0b10000,
            0b11111
        ));

        validCharacters.put('Q', new AlphabetDraw(
            0b11111,
            0b10001,
            0b10001,
            0b10011,
            0b11110
        ));

        validCharacters.put('G', new AlphabetDraw(
            0b11111,
            0b10000,
            0b10111,
            0b10001,
            0b11111
        ));

        validCharacters.put('Z', new AlphabetDraw(
            0b11111,
            0b00010,
            0b00100,
            0b01000,
            0b11111
        ));

        validCharacters.put('Y', new AlphabetDraw(
            0b10001,
            0b10001,
            0b11111,
            0b00100,
            0b00100

        ));

        validCharacters.put('X', new AlphabetDraw(
            0b10001,
            0b01010,
            0b00100,
            0b01010,
            0b10001
        ));

        validCharacters.put('V', new AlphabetDraw(
            0b10001,
            0b10001,
            0b01010,
            0b01010,
            0b01110
        ));

        validCharacters.put('W', new AlphabetDraw(
            0b10001,
            0b10001,
            0b10101,
            0b11011,
            0b10001
        ));

        validCharacters.put('_', new AlphabetDraw(
            0b00000,
            0b00000,
            0b00000,
            0b00000,
            0b11111
        ));

        validCharacters.put('.', new AlphabetDraw(
            0b00000,
            0b00000,
            0b00000,
            0b00000,
            0b00100
        ));

        // dummy
        validCharacters.put(' ', new AlphabetDraw(
            0b11111,
            0b11111,
            0b11111,
            0b11111,
            0b11111
        ));

        Set<Character> list = new TreeSet<>(validCharacters.keySet());
        list.remove('_');
        list.remove('.');
        list.remove(' ');
        alphabet = list.toArray(new Character[list.size()]);
    }
}
