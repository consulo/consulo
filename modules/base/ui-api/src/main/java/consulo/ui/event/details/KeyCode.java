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
package consulo.ui.event.details;

import consulo.ui.internal.KeyCodeImpl;
import org.jspecify.annotations.Nullable;

/**
 * A key of the keyboard. The codes are the virtual key codes every frontend translates its own keys to, and the names
 * are the ones a keymap writes keys with.
 *
 * @author VISTALL
 * @since 2024-09-12
 */
public interface KeyCode {
    KeyCode UNDEFINED = KeyCodeImpl.register(0x0, "UNDEFINED", "");

    KeyCode ENTER = KeyCodeImpl.register(0xA, "ENTER", "Enter");

    KeyCode BACK_SPACE = KeyCodeImpl.register(0x8, "BACK_SPACE", "Backspace");

    KeyCode TAB = KeyCodeImpl.register(0x9, "TAB", "Tab");

    KeyCode CANCEL = KeyCodeImpl.register(0x3, "CANCEL", "Cancel");

    KeyCode CLEAR = KeyCodeImpl.register(0xC, "CLEAR", "Clear");

    KeyCode SHIFT = KeyCodeImpl.register(0x10, "SHIFT", "Shift");

    KeyCode CONTROL = KeyCodeImpl.register(0x11, "CONTROL", "Ctrl");

    KeyCode ALT = KeyCodeImpl.register(0x12, "ALT", "Alt");

    KeyCode PAUSE = KeyCodeImpl.register(0x13, "PAUSE", "Pause");

    KeyCode CAPS_LOCK = KeyCodeImpl.register(0x14, "CAPS_LOCK", "Caps Lock");

    KeyCode ESCAPE = KeyCodeImpl.register(0x1B, "ESCAPE", "Escape");

    KeyCode SPACE = KeyCodeImpl.register(0x20, "SPACE", "Space");

    KeyCode PAGE_UP = KeyCodeImpl.register(0x21, "PAGE_UP", "Page Up");

    KeyCode PAGE_DOWN = KeyCodeImpl.register(0x22, "PAGE_DOWN", "Page Down");

    KeyCode END = KeyCodeImpl.register(0x23, "END", "End");

    KeyCode HOME = KeyCodeImpl.register(0x24, "HOME", "Home");

    KeyCode LEFT = KeyCodeImpl.register(0x25, "LEFT", "Left");

    KeyCode UP = KeyCodeImpl.register(0x26, "UP", "Up");

    KeyCode RIGHT = KeyCodeImpl.register(0x27, "RIGHT", "Right");

    KeyCode DOWN = KeyCodeImpl.register(0x28, "DOWN", "Down");

    KeyCode COMMA = KeyCodeImpl.register(0x2C, "COMMA", "Comma");

    KeyCode MINUS = KeyCodeImpl.register(0x2D, "MINUS", "Minus");

    KeyCode PERIOD = KeyCodeImpl.register(0x2E, "PERIOD", "Period");

    KeyCode SLASH = KeyCodeImpl.register(0x2F, "SLASH", "Slash");

    KeyCode DIGIT_0 = KeyCodeImpl.register(0x30, "0", "0");

    KeyCode DIGIT_1 = KeyCodeImpl.register(0x31, "1", "1");

    KeyCode DIGIT_2 = KeyCodeImpl.register(0x32, "2", "2");

    KeyCode DIGIT_3 = KeyCodeImpl.register(0x33, "3", "3");

    KeyCode DIGIT_4 = KeyCodeImpl.register(0x34, "4", "4");

    KeyCode DIGIT_5 = KeyCodeImpl.register(0x35, "5", "5");

    KeyCode DIGIT_6 = KeyCodeImpl.register(0x36, "6", "6");

    KeyCode DIGIT_7 = KeyCodeImpl.register(0x37, "7", "7");

    KeyCode DIGIT_8 = KeyCodeImpl.register(0x38, "8", "8");

    KeyCode DIGIT_9 = KeyCodeImpl.register(0x39, "9", "9");

    KeyCode SEMICOLON = KeyCodeImpl.register(0x3B, "SEMICOLON", "Semicolon");

    KeyCode EQUALS = KeyCodeImpl.register(0x3D, "EQUALS", "Equals");

    KeyCode A = KeyCodeImpl.register(0x41, "A", "A");

    KeyCode B = KeyCodeImpl.register(0x42, "B", "B");

    KeyCode C = KeyCodeImpl.register(0x43, "C", "C");

    KeyCode D = KeyCodeImpl.register(0x44, "D", "D");

    KeyCode E = KeyCodeImpl.register(0x45, "E", "E");

    KeyCode F = KeyCodeImpl.register(0x46, "F", "F");

    KeyCode G = KeyCodeImpl.register(0x47, "G", "G");

    KeyCode H = KeyCodeImpl.register(0x48, "H", "H");

    KeyCode I = KeyCodeImpl.register(0x49, "I", "I");

    KeyCode J = KeyCodeImpl.register(0x4A, "J", "J");

    KeyCode K = KeyCodeImpl.register(0x4B, "K", "K");

    KeyCode L = KeyCodeImpl.register(0x4C, "L", "L");

    KeyCode M = KeyCodeImpl.register(0x4D, "M", "M");

    KeyCode N = KeyCodeImpl.register(0x4E, "N", "N");

    KeyCode O = KeyCodeImpl.register(0x4F, "O", "O");

    KeyCode P = KeyCodeImpl.register(0x50, "P", "P");

    KeyCode Q = KeyCodeImpl.register(0x51, "Q", "Q");

    KeyCode R = KeyCodeImpl.register(0x52, "R", "R");

    KeyCode S = KeyCodeImpl.register(0x53, "S", "S");

    KeyCode T = KeyCodeImpl.register(0x54, "T", "T");

    KeyCode U = KeyCodeImpl.register(0x55, "U", "U");

    KeyCode V = KeyCodeImpl.register(0x56, "V", "V");

    KeyCode W = KeyCodeImpl.register(0x57, "W", "W");

    KeyCode X = KeyCodeImpl.register(0x58, "X", "X");

    KeyCode Y = KeyCodeImpl.register(0x59, "Y", "Y");

    KeyCode Z = KeyCodeImpl.register(0x5A, "Z", "Z");

    KeyCode OPEN_BRACKET = KeyCodeImpl.register(0x5B, "OPEN_BRACKET", "Open Bracket");

    KeyCode BACK_SLASH = KeyCodeImpl.register(0x5C, "BACK_SLASH", "Back Slash");

    KeyCode CLOSE_BRACKET = KeyCodeImpl.register(0x5D, "CLOSE_BRACKET", "Close Bracket");

    KeyCode NUMPAD0 = KeyCodeImpl.register(0x60, "NUMPAD0", "NumPad-0");

    KeyCode NUMPAD1 = KeyCodeImpl.register(0x61, "NUMPAD1", "NumPad-1");

    KeyCode NUMPAD2 = KeyCodeImpl.register(0x62, "NUMPAD2", "NumPad-2");

    KeyCode NUMPAD3 = KeyCodeImpl.register(0x63, "NUMPAD3", "NumPad-3");

    KeyCode NUMPAD4 = KeyCodeImpl.register(0x64, "NUMPAD4", "NumPad-4");

    KeyCode NUMPAD5 = KeyCodeImpl.register(0x65, "NUMPAD5", "NumPad-5");

    KeyCode NUMPAD6 = KeyCodeImpl.register(0x66, "NUMPAD6", "NumPad-6");

    KeyCode NUMPAD7 = KeyCodeImpl.register(0x67, "NUMPAD7", "NumPad-7");

    KeyCode NUMPAD8 = KeyCodeImpl.register(0x68, "NUMPAD8", "NumPad-8");

    KeyCode NUMPAD9 = KeyCodeImpl.register(0x69, "NUMPAD9", "NumPad-9");

    KeyCode MULTIPLY = KeyCodeImpl.register(0x6A, "MULTIPLY", "NumPad *");

    KeyCode ADD = KeyCodeImpl.register(0x6B, "ADD", "NumPad +");

    KeyCode SEPARATOR = KeyCodeImpl.register(0x6C, "SEPARATOR", "NumPad ,");

    KeyCode SUBTRACT = KeyCodeImpl.register(0x6D, "SUBTRACT", "NumPad -");

    KeyCode DECIMAL = KeyCodeImpl.register(0x6E, "DECIMAL", "NumPad .");

    KeyCode DIVIDE = KeyCodeImpl.register(0x6F, "DIVIDE", "NumPad /");

    KeyCode DELETE = KeyCodeImpl.register(0x7F, "DELETE", "Delete");

    KeyCode NUM_LOCK = KeyCodeImpl.register(0x90, "NUM_LOCK", "Num Lock");

    KeyCode SCROLL_LOCK = KeyCodeImpl.register(0x91, "SCROLL_LOCK", "Scroll Lock");

    KeyCode F1 = KeyCodeImpl.register(0x70, "F1", "F1");

    KeyCode F2 = KeyCodeImpl.register(0x71, "F2", "F2");

    KeyCode F3 = KeyCodeImpl.register(0x72, "F3", "F3");

    KeyCode F4 = KeyCodeImpl.register(0x73, "F4", "F4");

    KeyCode F5 = KeyCodeImpl.register(0x74, "F5", "F5");

    KeyCode F6 = KeyCodeImpl.register(0x75, "F6", "F6");

    KeyCode F7 = KeyCodeImpl.register(0x76, "F7", "F7");

    KeyCode F8 = KeyCodeImpl.register(0x77, "F8", "F8");

    KeyCode F9 = KeyCodeImpl.register(0x78, "F9", "F9");

    KeyCode F10 = KeyCodeImpl.register(0x79, "F10", "F10");

    KeyCode F11 = KeyCodeImpl.register(0x7A, "F11", "F11");

    KeyCode F12 = KeyCodeImpl.register(0x7B, "F12", "F12");

    KeyCode F13 = KeyCodeImpl.register(0xF000, "F13", "F13");

    KeyCode F14 = KeyCodeImpl.register(0xF001, "F14", "F14");

    KeyCode F15 = KeyCodeImpl.register(0xF002, "F15", "F15");

    KeyCode F16 = KeyCodeImpl.register(0xF003, "F16", "F16");

    KeyCode F17 = KeyCodeImpl.register(0xF004, "F17", "F17");

    KeyCode F18 = KeyCodeImpl.register(0xF005, "F18", "F18");

    KeyCode F19 = KeyCodeImpl.register(0xF006, "F19", "F19");

    KeyCode F20 = KeyCodeImpl.register(0xF007, "F20", "F20");

    KeyCode F21 = KeyCodeImpl.register(0xF008, "F21", "F21");

    KeyCode F22 = KeyCodeImpl.register(0xF009, "F22", "F22");

    KeyCode F23 = KeyCodeImpl.register(0xF00A, "F23", "F23");

    KeyCode F24 = KeyCodeImpl.register(0xF00B, "F24", "F24");

    KeyCode PRINTSCREEN = KeyCodeImpl.register(0x9A, "PRINTSCREEN", "Print Screen");

    KeyCode INSERT = KeyCodeImpl.register(0x9B, "INSERT", "Insert");

    KeyCode HELP = KeyCodeImpl.register(0x9C, "HELP", "Help");

    KeyCode META = KeyCodeImpl.register(0x9D, "META", "Meta");

    KeyCode BACK_QUOTE = KeyCodeImpl.register(0xC0, "BACK_QUOTE", "Back Quote");

    KeyCode QUOTE = KeyCodeImpl.register(0xDE, "QUOTE", "Quote");

    KeyCode KP_UP = KeyCodeImpl.register(0xE0, "KP_UP", "Up");

    KeyCode KP_DOWN = KeyCodeImpl.register(0xE1, "KP_DOWN", "Down");

    KeyCode KP_LEFT = KeyCodeImpl.register(0xE2, "KP_LEFT", "Left");

    KeyCode KP_RIGHT = KeyCodeImpl.register(0xE3, "KP_RIGHT", "Right");

    KeyCode DEAD_GRAVE = KeyCodeImpl.register(0x80, "DEAD_GRAVE", "Dead Grave");

    KeyCode DEAD_ACUTE = KeyCodeImpl.register(0x81, "DEAD_ACUTE", "Dead Acute");

    KeyCode DEAD_CIRCUMFLEX = KeyCodeImpl.register(0x82, "DEAD_CIRCUMFLEX", "Dead Circumflex");

    KeyCode DEAD_TILDE = KeyCodeImpl.register(0x83, "DEAD_TILDE", "Dead Tilde");

    KeyCode DEAD_MACRON = KeyCodeImpl.register(0x84, "DEAD_MACRON", "Dead Macron");

    KeyCode DEAD_BREVE = KeyCodeImpl.register(0x85, "DEAD_BREVE", "Dead Breve");

    KeyCode DEAD_ABOVEDOT = KeyCodeImpl.register(0x86, "DEAD_ABOVEDOT", "Dead Above Dot");

    KeyCode DEAD_DIAERESIS = KeyCodeImpl.register(0x87, "DEAD_DIAERESIS", "Dead Diaeresis");

    KeyCode DEAD_ABOVERING = KeyCodeImpl.register(0x88, "DEAD_ABOVERING", "Dead Above Ring");

    KeyCode DEAD_DOUBLEACUTE = KeyCodeImpl.register(0x89, "DEAD_DOUBLEACUTE", "Dead Double Acute");

    KeyCode DEAD_CARON = KeyCodeImpl.register(0x8A, "DEAD_CARON", "Dead Caron");

    KeyCode DEAD_CEDILLA = KeyCodeImpl.register(0x8B, "DEAD_CEDILLA", "Dead Cedilla");

    KeyCode DEAD_OGONEK = KeyCodeImpl.register(0x8C, "DEAD_OGONEK", "Dead Ogonek");

    KeyCode DEAD_IOTA = KeyCodeImpl.register(0x8D, "DEAD_IOTA", "Dead Iota");

    KeyCode DEAD_VOICED_SOUND = KeyCodeImpl.register(0x8E, "DEAD_VOICED_SOUND", "Dead Voiced Sound");

    KeyCode DEAD_SEMIVOICED_SOUND = KeyCodeImpl.register(0x8F, "DEAD_SEMIVOICED_SOUND", "Dead Semivoiced Sound");

    KeyCode AMPERSAND = KeyCodeImpl.register(0x96, "AMPERSAND", "Ampersand");

    KeyCode ASTERISK = KeyCodeImpl.register(0x97, "ASTERISK", "Asterisk");

    KeyCode QUOTEDBL = KeyCodeImpl.register(0x98, "QUOTEDBL", "Double Quote");

    KeyCode LESS = KeyCodeImpl.register(0x99, "LESS", "Less");

    KeyCode GREATER = KeyCodeImpl.register(0xA0, "GREATER", "Greater");

    KeyCode BRACELEFT = KeyCodeImpl.register(0xA1, "BRACELEFT", "Left Brace");

    KeyCode BRACERIGHT = KeyCodeImpl.register(0xA2, "BRACERIGHT", "Right Brace");

    KeyCode AT = KeyCodeImpl.register(0x200, "AT", "At");

    KeyCode COLON = KeyCodeImpl.register(0x201, "COLON", "Colon");

    KeyCode CIRCUMFLEX = KeyCodeImpl.register(0x202, "CIRCUMFLEX", "Circumflex");

    KeyCode DOLLAR = KeyCodeImpl.register(0x203, "DOLLAR", "Dollar");

    KeyCode EURO_SIGN = KeyCodeImpl.register(0x204, "EURO_SIGN", "Euro");

    KeyCode EXCLAMATION_MARK = KeyCodeImpl.register(0x205, "EXCLAMATION_MARK", "Exclamation Mark");

    KeyCode INVERTED_EXCLAMATION_MARK = KeyCodeImpl.register(0x206, "INVERTED_EXCLAMATION_MARK", "Inverted Exclamation Mark");

    KeyCode LEFT_PARENTHESIS = KeyCodeImpl.register(0x207, "LEFT_PARENTHESIS", "Left Parenthesis");

    KeyCode NUMBER_SIGN = KeyCodeImpl.register(0x208, "NUMBER_SIGN", "Number Sign");

    KeyCode PLUS = KeyCodeImpl.register(0x209, "PLUS", "Plus");

    KeyCode RIGHT_PARENTHESIS = KeyCodeImpl.register(0x20A, "RIGHT_PARENTHESIS", "Right Parenthesis");

    KeyCode UNDERSCORE = KeyCodeImpl.register(0x20B, "UNDERSCORE", "Underscore");

    KeyCode WINDOWS = KeyCodeImpl.register(0x20C, "WINDOWS", "Windows");

    KeyCode CONTEXT_MENU = KeyCodeImpl.register(0x20D, "CONTEXT_MENU", "Context Menu");

    KeyCode FINAL = KeyCodeImpl.register(0x18, "FINAL", "Final");

    KeyCode CONVERT = KeyCodeImpl.register(0x1C, "CONVERT", "Convert");

    KeyCode NONCONVERT = KeyCodeImpl.register(0x1D, "NONCONVERT", "No Convert");

    KeyCode ACCEPT = KeyCodeImpl.register(0x1E, "ACCEPT", "Accept");

    KeyCode MODECHANGE = KeyCodeImpl.register(0x1F, "MODECHANGE", "Mode Change");

    KeyCode KANA = KeyCodeImpl.register(0x15, "KANA", "Kana");

    KeyCode KANJI = KeyCodeImpl.register(0x19, "KANJI", "Kanji");

    KeyCode ALPHANUMERIC = KeyCodeImpl.register(0xF0, "ALPHANUMERIC", "Alphanumeric");

    KeyCode KATAKANA = KeyCodeImpl.register(0xF1, "KATAKANA", "Katakana");

    KeyCode HIRAGANA = KeyCodeImpl.register(0xF2, "HIRAGANA", "Hiragana");

    KeyCode FULL_WIDTH = KeyCodeImpl.register(0xF3, "FULL_WIDTH", "Full-Width");

    KeyCode HALF_WIDTH = KeyCodeImpl.register(0xF4, "HALF_WIDTH", "Half-Width");

    KeyCode ROMAN_CHARACTERS = KeyCodeImpl.register(0xF5, "ROMAN_CHARACTERS", "Roman Characters");

    KeyCode ALL_CANDIDATES = KeyCodeImpl.register(0x100, "ALL_CANDIDATES", "All Candidates");

    KeyCode PREVIOUS_CANDIDATE = KeyCodeImpl.register(0x101, "PREVIOUS_CANDIDATE", "Previous Candidate");

    KeyCode CODE_INPUT = KeyCodeImpl.register(0x102, "CODE_INPUT", "Code Input");

    KeyCode JAPANESE_KATAKANA = KeyCodeImpl.register(0x103, "JAPANESE_KATAKANA", "Japanese Katakana");

    KeyCode JAPANESE_HIRAGANA = KeyCodeImpl.register(0x104, "JAPANESE_HIRAGANA", "Japanese Hiragana");

    KeyCode JAPANESE_ROMAN = KeyCodeImpl.register(0x105, "JAPANESE_ROMAN", "Japanese Roman");

    KeyCode KANA_LOCK = KeyCodeImpl.register(0x106, "KANA_LOCK", "Kana Lock");

    KeyCode INPUT_METHOD_ON_OFF = KeyCodeImpl.register(0x107, "INPUT_METHOD_ON_OFF", "Input Method On/Off");

    KeyCode CUT = KeyCodeImpl.register(0xFFD1, "CUT", "Cut");

    KeyCode COPY = KeyCodeImpl.register(0xFFCD, "COPY", "Copy");

    KeyCode PASTE = KeyCodeImpl.register(0xFFCF, "PASTE", "Paste");

    KeyCode UNDO = KeyCodeImpl.register(0xFFCB, "UNDO", "Undo");

    KeyCode AGAIN = KeyCodeImpl.register(0xFFC9, "AGAIN", "Again");

    KeyCode FIND = KeyCodeImpl.register(0xFFD0, "FIND", "Find");

    KeyCode PROPS = KeyCodeImpl.register(0xFFCA, "PROPS", "Props");

    KeyCode STOP = KeyCodeImpl.register(0xFFC8, "STOP", "Stop");

    KeyCode COMPOSE = KeyCodeImpl.register(0xFF20, "COMPOSE", "Compose");

    KeyCode ALT_GRAPH = KeyCodeImpl.register(0xFF7E, "ALT_GRAPH", "Alt Graph");

    KeyCode BEGIN = KeyCodeImpl.register(0xFF58, "BEGIN", "Begin");

    /**
     * @return the virtual key code
     */
    int key();

    /**
     * @return the name a keymap writes the key with, such as {@code BACK_SPACE}
     */
    String name();

    /**
     * @return the English text a shortcut shows the key with, such as {@code Backspace}
     */
    String getText();

    static KeyCode of(int key) {
        return KeyCodeImpl.of(key);
    }

    /**
     * @param name the name of a key in any case, such as {@code back_space}
     */
    static @Nullable KeyCode byName(String name) {
        return KeyCodeImpl.byName(name);
    }
}
