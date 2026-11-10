/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ui.ex.action.util;

import consulo.localize.LocalizeValue;
import consulo.ui.event.details.KeyCode;
import consulo.ui.ex.localize.ShortcutLocalize;
import consulo.ui.ex.localize.UnicodeShortcutLocalize;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;


/**
 * @author VISTALL
 * @since 2025-09-13
 *
 * Some some code from Swing {@code KeyEvent#getKeyText(int)}
 */
public class ShortcutLocalizeHolder {
    private record ShortcutInfo(LocalizeValue textShortcut, LocalizeValue unicodeShortcut) {
    }

    private static final Int2ObjectMap<ShortcutInfo> cache = new Int2ObjectLinkedOpenHashMap<>();

    static {
        put(KeyCode.ENTER, UnicodeShortcutLocalize.keyboardEnter(), ShortcutLocalize.keyboardEnter());
        put(KeyCode.BACK_SPACE, UnicodeShortcutLocalize.keyboardBackspace(), ShortcutLocalize.keyboardBackspace());
        put(KeyCode.TAB, UnicodeShortcutLocalize.keyboardTab(), ShortcutLocalize.keyboardTab());
        put(KeyCode.CLEAR, UnicodeShortcutLocalize.keyboardClear(), ShortcutLocalize.keyboardClear());
        put(KeyCode.CAPS_LOCK, UnicodeShortcutLocalize.keyboardCapslock(), ShortcutLocalize.keyboardCapslock());
        put(KeyCode.ESCAPE, UnicodeShortcutLocalize.keyboardEscape(), ShortcutLocalize.keyboardEscape());
        put(KeyCode.SPACE, UnicodeShortcutLocalize.keyboardSpace(), ShortcutLocalize.keyboardSpace());
        put(KeyCode.PAGE_UP, UnicodeShortcutLocalize.keyboardPgup(), ShortcutLocalize.keyboardPgup());
        put(KeyCode.PAGE_DOWN, UnicodeShortcutLocalize.keyboardPgdn(), ShortcutLocalize.keyboardPgdn());
        put(KeyCode.END, UnicodeShortcutLocalize.keyboardEnd(), ShortcutLocalize.keyboardEnd());
        put(KeyCode.HOME, UnicodeShortcutLocalize.keyboardHome(), ShortcutLocalize.keyboardHome());
        put(KeyCode.KP_LEFT, UnicodeShortcutLocalize.keyboardLeft(), ShortcutLocalize.keyboardLeft());
        put(KeyCode.LEFT, UnicodeShortcutLocalize.keyboardLeft(), ShortcutLocalize.keyboardLeft());
        put(KeyCode.KP_UP, UnicodeShortcutLocalize.keyboardUp(), ShortcutLocalize.keyboardUp());
        put(KeyCode.UP, UnicodeShortcutLocalize.keyboardUp(), ShortcutLocalize.keyboardUp());
        put(KeyCode.KP_RIGHT, UnicodeShortcutLocalize.keyboardRight(), ShortcutLocalize.keyboardRight());
        put(KeyCode.RIGHT, UnicodeShortcutLocalize.keyboardRight(), ShortcutLocalize.keyboardRight());
        put(KeyCode.KP_DOWN, UnicodeShortcutLocalize.keyboardDown(), ShortcutLocalize.keyboardDown());
        put(KeyCode.DOWN, UnicodeShortcutLocalize.keyboardDown(), ShortcutLocalize.keyboardDown());
        put(KeyCode.BACK_QUOTE, UnicodeShortcutLocalize.keyboardBackquote(), ShortcutLocalize.keyboardBackquote());
        put(KeyCode.COMMA, UnicodeShortcutLocalize.keyboardComma(), ShortcutLocalize.keyboardComma());
        put(KeyCode.PERIOD, UnicodeShortcutLocalize.keyboardPeriod(), ShortcutLocalize.keyboardPeriod());
        put(KeyCode.SEMICOLON, UnicodeShortcutLocalize.keyboardSemicolon(), ShortcutLocalize.keyboardSemicolon());
        put(KeyCode.EQUALS, UnicodeShortcutLocalize.keyboardEquals(), ShortcutLocalize.keyboardEquals());
        put(KeyCode.SLASH, UnicodeShortcutLocalize.keyboardSlash(), ShortcutLocalize.keyboardSlash());
        put(KeyCode.OPEN_BRACKET, UnicodeShortcutLocalize.keyboardOpenbracket(), ShortcutLocalize.keyboardOpenbracket());
        put(KeyCode.BACK_SLASH, UnicodeShortcutLocalize.keyboardBackslash(), ShortcutLocalize.keyboardBackslash());
        put(KeyCode.CLOSE_BRACKET, UnicodeShortcutLocalize.keyboardClosebracket(), ShortcutLocalize.keyboardClosebracket());
        put(KeyCode.ADD, UnicodeShortcutLocalize.keyboardAdd(), ShortcutLocalize.keyboardAdd());
        put(KeyCode.MINUS, UnicodeShortcutLocalize.keyboardMinus(), ShortcutLocalize.keyboardMinus());
        put(KeyCode.PLUS, UnicodeShortcutLocalize.keyboardPlus(), ShortcutLocalize.keyboardPlus());

        put(KeyCode.BEGIN, ShortcutLocalize.keyboardBegin());
        put(KeyCode.CANCEL, ShortcutLocalize.keyboardCancel());
        put(KeyCode.COMPOSE, ShortcutLocalize.keyboardCompose());
        put(KeyCode.PAUSE, ShortcutLocalize.keyboardPause());

        put(KeyCode.SHIFT, UnicodeShortcutLocalize.keyboardShift(), ShortcutLocalize.keyboardShift());
        put(KeyCode.CONTROL, UnicodeShortcutLocalize.keyboardControl(), ShortcutLocalize.keyboardControl());
        put(KeyCode.ALT, UnicodeShortcutLocalize.keyboardAlt(), ShortcutLocalize.keyboardAlt());
        put(KeyCode.META, UnicodeShortcutLocalize.keyboardMeta(), ShortcutLocalize.keyboardMeta());
        put(KeyCode.ALT_GRAPH, UnicodeShortcutLocalize.keyboardAltgraph(), ShortcutLocalize.keyboardAltgraph());

        put(KeyCode.MULTIPLY, UnicodeShortcutLocalize.keyboardMultiply(), ShortcutLocalize.keyboardMultiply());
        put(KeyCode.SEPARATOR, UnicodeShortcutLocalize.keyboardSeparator(), ShortcutLocalize.keyboardSeparator());
        put(KeyCode.SUBTRACT, UnicodeShortcutLocalize.keyboardSubtract(), ShortcutLocalize.keyboardSubtract());
        put(KeyCode.DECIMAL, UnicodeShortcutLocalize.keyboardDecimal(), ShortcutLocalize.keyboardDecimal());
        put(KeyCode.DIVIDE, UnicodeShortcutLocalize.keyboardDivide(), ShortcutLocalize.keyboardDivide());
        put(KeyCode.NUM_LOCK, UnicodeShortcutLocalize.keyboardNumlock(), ShortcutLocalize.keyboardNumlock());

        put(KeyCode.DELETE, ShortcutLocalize.keyboardDelete());
        put(KeyCode.SCROLL_LOCK, ShortcutLocalize.keyboardScrolllock());

        put(KeyCode.WINDOWS, ShortcutLocalize.keyboardWindows());
        put(KeyCode.CONTEXT_MENU, ShortcutLocalize.keyboardContext());
        
        put(KeyCode.NUMPAD0, UnicodeShortcutLocalize.keyboardNumpad0(), ShortcutLocalize.keyboardNumpad0());
        put(KeyCode.NUMPAD1, UnicodeShortcutLocalize.keyboardNumpad1(), ShortcutLocalize.keyboardNumpad1());
        put(KeyCode.NUMPAD2, UnicodeShortcutLocalize.keyboardNumpad2(), ShortcutLocalize.keyboardNumpad2());
        put(KeyCode.NUMPAD3, UnicodeShortcutLocalize.keyboardNumpad3(), ShortcutLocalize.keyboardNumpad3());
        put(KeyCode.NUMPAD4, UnicodeShortcutLocalize.keyboardNumpad4(), ShortcutLocalize.keyboardNumpad4());
        put(KeyCode.NUMPAD5, UnicodeShortcutLocalize.keyboardNumpad5(), ShortcutLocalize.keyboardNumpad5());
        put(KeyCode.NUMPAD6, UnicodeShortcutLocalize.keyboardNumpad6(), ShortcutLocalize.keyboardNumpad6());
        put(KeyCode.NUMPAD7, UnicodeShortcutLocalize.keyboardNumpad7(), ShortcutLocalize.keyboardNumpad7());
        put(KeyCode.NUMPAD8, UnicodeShortcutLocalize.keyboardNumpad8(), ShortcutLocalize.keyboardNumpad8());
        put(KeyCode.NUMPAD9, UnicodeShortcutLocalize.keyboardNumpad9(), ShortcutLocalize.keyboardNumpad9());

        put(KeyCode.DIGIT_0, UnicodeShortcutLocalize.keyboard0());
        put(KeyCode.DIGIT_1, UnicodeShortcutLocalize.keyboard1());
        put(KeyCode.DIGIT_2, UnicodeShortcutLocalize.keyboard2());
        put(KeyCode.DIGIT_3, UnicodeShortcutLocalize.keyboard3());
        put(KeyCode.DIGIT_4, UnicodeShortcutLocalize.keyboard4());
        put(KeyCode.DIGIT_5, UnicodeShortcutLocalize.keyboard5());
        put(KeyCode.DIGIT_6, UnicodeShortcutLocalize.keyboard6());
        put(KeyCode.DIGIT_7, UnicodeShortcutLocalize.keyboard7());
        put(KeyCode.DIGIT_8, UnicodeShortcutLocalize.keyboard8());
        put(KeyCode.DIGIT_9, UnicodeShortcutLocalize.keyboard9());

        put(KeyCode.A, UnicodeShortcutLocalize.keyboardA());
        put(KeyCode.B, UnicodeShortcutLocalize.keyboardB());
        put(KeyCode.C, UnicodeShortcutLocalize.keyboardC());
        put(KeyCode.D, UnicodeShortcutLocalize.keyboardD());
        put(KeyCode.E, UnicodeShortcutLocalize.keyboardE());
        put(KeyCode.F, UnicodeShortcutLocalize.keyboardF());
        put(KeyCode.G, UnicodeShortcutLocalize.keyboardG());
        put(KeyCode.H, UnicodeShortcutLocalize.keyboardH());
        put(KeyCode.I, UnicodeShortcutLocalize.keyboardI());
        put(KeyCode.J, UnicodeShortcutLocalize.keyboardJ());
        put(KeyCode.K, UnicodeShortcutLocalize.keyboardK());
        put(KeyCode.L, UnicodeShortcutLocalize.keyboardL());
        put(KeyCode.M, UnicodeShortcutLocalize.keyboardM());
        put(KeyCode.N, UnicodeShortcutLocalize.keyboardN());
        put(KeyCode.O, UnicodeShortcutLocalize.keyboardO());
        put(KeyCode.P, UnicodeShortcutLocalize.keyboardP());
        put(KeyCode.Q, UnicodeShortcutLocalize.keyboardQ());
        put(KeyCode.R, UnicodeShortcutLocalize.keyboardR());
        put(KeyCode.S, UnicodeShortcutLocalize.keyboardS());
        put(KeyCode.T, UnicodeShortcutLocalize.keyboardT());
        put(KeyCode.U, UnicodeShortcutLocalize.keyboardU());
        put(KeyCode.V, UnicodeShortcutLocalize.keyboardV());
        put(KeyCode.W, UnicodeShortcutLocalize.keyboardW());
        put(KeyCode.X, UnicodeShortcutLocalize.keyboardX());
        put(KeyCode.Y, UnicodeShortcutLocalize.keyboardY());
        put(KeyCode.Z, UnicodeShortcutLocalize.keyboardZ());

        put(KeyCode.F1, ShortcutLocalize.keyboardF1());
        put(KeyCode.F2, ShortcutLocalize.keyboardF2());
        put(KeyCode.F3, ShortcutLocalize.keyboardF3());
        put(KeyCode.F4, ShortcutLocalize.keyboardF4());
        put(KeyCode.F5, ShortcutLocalize.keyboardF5());
        put(KeyCode.F6, ShortcutLocalize.keyboardF6());
        put(KeyCode.F7, ShortcutLocalize.keyboardF7());
        put(KeyCode.F8, ShortcutLocalize.keyboardF8());
        put(KeyCode.F9, ShortcutLocalize.keyboardF9());
        put(KeyCode.F10, ShortcutLocalize.keyboardF10());
        put(KeyCode.F11, ShortcutLocalize.keyboardF11());
        put(KeyCode.F12, ShortcutLocalize.keyboardF12());
        put(KeyCode.F13, ShortcutLocalize.keyboardF13());
        put(KeyCode.F14, ShortcutLocalize.keyboardF14());
        put(KeyCode.F15, ShortcutLocalize.keyboardF15());
        put(KeyCode.F16, ShortcutLocalize.keyboardF16());
        put(KeyCode.F17, ShortcutLocalize.keyboardF17());
        put(KeyCode.F18, ShortcutLocalize.keyboardF18());
        put(KeyCode.F19, ShortcutLocalize.keyboardF19());
        put(KeyCode.F20, ShortcutLocalize.keyboardF20());
        put(KeyCode.F21, ShortcutLocalize.keyboardF21());
        put(KeyCode.F22, ShortcutLocalize.keyboardF22());
        put(KeyCode.F23, ShortcutLocalize.keyboardF23());
        put(KeyCode.F24, ShortcutLocalize.keyboardF24());

        put(KeyCode.PRINTSCREEN, ShortcutLocalize.keyboardPrintscreen());
        put(KeyCode.INSERT, ShortcutLocalize.keyboardInsert());
        put(KeyCode.HELP, ShortcutLocalize.keyboardHelp());
        put(KeyCode.QUOTE, ShortcutLocalize.keyboardQuote());

        put(KeyCode.DEAD_GRAVE, ShortcutLocalize.keyboardDeadgrave());
        put(KeyCode.DEAD_ACUTE, ShortcutLocalize.keyboardDeadacute());
        put(KeyCode.DEAD_CIRCUMFLEX, ShortcutLocalize.keyboardDeadcircumflex());
        put(KeyCode.DEAD_TILDE, ShortcutLocalize.keyboardDeadtilde());
        put(KeyCode.DEAD_MACRON, ShortcutLocalize.keyboardDeadmacron());
        put(KeyCode.DEAD_BREVE, ShortcutLocalize.keyboardDeadbreve());
        put(KeyCode.DEAD_ABOVEDOT, ShortcutLocalize.keyboardDeadabovedot());
        put(KeyCode.DEAD_DIAERESIS, ShortcutLocalize.keyboardDeaddiaeresis());
        put(KeyCode.DEAD_ABOVERING, ShortcutLocalize.keyboardDeadabovering());
        put(KeyCode.DEAD_DOUBLEACUTE, ShortcutLocalize.keyboardDeaddoubleacute());
        put(KeyCode.DEAD_CARON, ShortcutLocalize.keyboardDeadcaron());
        put(KeyCode.DEAD_CEDILLA, ShortcutLocalize.keyboardDeadcedilla());
        put(KeyCode.DEAD_OGONEK, ShortcutLocalize.keyboardDeadogonek());
        put(KeyCode.DEAD_IOTA, ShortcutLocalize.keyboardDeadiota());
        put(KeyCode.DEAD_VOICED_SOUND, ShortcutLocalize.keyboardDeadvoicedsound());
        put(KeyCode.DEAD_SEMIVOICED_SOUND, ShortcutLocalize.keyboardDeadsemivoicedsound());

        put(KeyCode.AMPERSAND, UnicodeShortcutLocalize.keyboardAmpersand(), ShortcutLocalize.keyboardAmpersand());
        put(KeyCode.ASTERISK, UnicodeShortcutLocalize.keyboardAsterisk(), ShortcutLocalize.keyboardAsterisk());
        put(KeyCode.QUOTEDBL, UnicodeShortcutLocalize.keyboardQuotedbl(), ShortcutLocalize.keyboardQuotedbl());
        put(KeyCode.LESS, UnicodeShortcutLocalize.keyboardLess(), ShortcutLocalize.keyboardLess());
        put(KeyCode.GREATER, UnicodeShortcutLocalize.keyboardGreater(), ShortcutLocalize.keyboardGreater());
        put(KeyCode.BRACELEFT, UnicodeShortcutLocalize.keyboardBraceleft(), ShortcutLocalize.keyboardBraceleft());
        put(KeyCode.BRACERIGHT, UnicodeShortcutLocalize.keyboardBraceright(), ShortcutLocalize.keyboardBraceright());
        put(KeyCode.AT, UnicodeShortcutLocalize.keyboardAt(), ShortcutLocalize.keyboardAt());
        put(KeyCode.COLON, UnicodeShortcutLocalize.keyboardColon(), ShortcutLocalize.keyboardColon());
        put(KeyCode.CIRCUMFLEX, ShortcutLocalize.keyboardCircumflex());
        put(KeyCode.DOLLAR, UnicodeShortcutLocalize.keyboardDollar(), ShortcutLocalize.keyboardDollar());
        put(KeyCode.EURO_SIGN, UnicodeShortcutLocalize.keyboardEuro(), ShortcutLocalize.keyboardEuro());
        put(KeyCode.EXCLAMATION_MARK, UnicodeShortcutLocalize.keyboardExclamationmark(), ShortcutLocalize.keyboardExclamationmark());
        put(KeyCode.INVERTED_EXCLAMATION_MARK, UnicodeShortcutLocalize.keyboardInvertedexclamationmark(), ShortcutLocalize.keyboardInvertedexclamationmark());
        put(KeyCode.LEFT_PARENTHESIS, UnicodeShortcutLocalize.keyboardLeftparenthesis(), ShortcutLocalize.keyboardLeftparenthesis());
        put(KeyCode.NUMBER_SIGN, UnicodeShortcutLocalize.keyboardNumbersign(), ShortcutLocalize.keyboardNumbersign());
        put(KeyCode.RIGHT_PARENTHESIS, UnicodeShortcutLocalize.keyboardRightparenthesis(), ShortcutLocalize.keyboardRightparenthesis());
        put(KeyCode.UNDERSCORE, UnicodeShortcutLocalize.keyboardUnderscore(), ShortcutLocalize.keyboardUnderscore());

        put(KeyCode.FINAL, ShortcutLocalize.keyboardFinal());
        put(KeyCode.CONVERT, ShortcutLocalize.keyboardConvert());
        put(KeyCode.NONCONVERT, ShortcutLocalize.keyboardNoconvert());
        put(KeyCode.ACCEPT, ShortcutLocalize.keyboardAccept());
        put(KeyCode.MODECHANGE, ShortcutLocalize.keyboardModechange());
        put(KeyCode.KANA, ShortcutLocalize.keyboardKana());
        put(KeyCode.KANJI, ShortcutLocalize.keyboardKanji());
        put(KeyCode.ALPHANUMERIC, ShortcutLocalize.keyboardAlphanumeric());
        put(KeyCode.KATAKANA, ShortcutLocalize.keyboardKatakana());
        put(KeyCode.HIRAGANA, ShortcutLocalize.keyboardHiragana());
        put(KeyCode.FULL_WIDTH, ShortcutLocalize.keyboardFullwidth());
        put(KeyCode.HALF_WIDTH, ShortcutLocalize.keyboardHalfwidth());
        put(KeyCode.ROMAN_CHARACTERS, ShortcutLocalize.keyboardRomancharacters());
        put(KeyCode.ALL_CANDIDATES, ShortcutLocalize.keyboardAllcandidates());
        put(KeyCode.PREVIOUS_CANDIDATE, ShortcutLocalize.keyboardPreviouscandidate());
        put(KeyCode.CODE_INPUT, ShortcutLocalize.keyboardCodeinput());
        put(KeyCode.JAPANESE_KATAKANA, ShortcutLocalize.keyboardJapanesekatakana());
        put(KeyCode.JAPANESE_HIRAGANA, ShortcutLocalize.keyboardJapanesehiragana());
        put(KeyCode.JAPANESE_ROMAN, ShortcutLocalize.keyboardJapaneseroman());
        put(KeyCode.KANA_LOCK, ShortcutLocalize.keyboardKanalock());
        put(KeyCode.INPUT_METHOD_ON_OFF, ShortcutLocalize.keyboardInputmethodonoff());

        put(KeyCode.AGAIN, ShortcutLocalize.keyboardAgain());
        put(KeyCode.UNDO, ShortcutLocalize.keyboardUndo());
        put(KeyCode.COPY, ShortcutLocalize.keyboardCopy());
        put(KeyCode.PASTE, ShortcutLocalize.keyboardPaste());
        put(KeyCode.CUT, ShortcutLocalize.keyboardCut());
        put(KeyCode.FIND, ShortcutLocalize.keyboardFind());
        put(KeyCode.PROPS, ShortcutLocalize.keyboardProps());
        put(KeyCode.STOP, ShortcutLocalize.keyboardStop());
    }

    static void put(KeyCode keyCode, LocalizeValue textLocalize) {
        cache.put(keyCode.key(), new ShortcutInfo(textLocalize, textLocalize));
    }

    static void put(KeyCode keyCode, LocalizeValue unicodeLocalize, LocalizeValue textLocalize) {
        cache.put(keyCode.key(), new ShortcutInfo(textLocalize, unicodeLocalize));
    }

    
    public static LocalizeValue getKeyText(KeyCode key, boolean unicodeShortcut) {
        int keyCode = key.key();
        ShortcutInfo info = cache.get(keyCode);
        if (info != null) {
            if (unicodeShortcut) {
                return info.unicodeShortcut();
            } else {
                return info.textShortcut();
            }
        }

        // idk what is
        if ((keyCode & 0x01000000) != 0) {
            return LocalizeValue.of(String.valueOf((char) (keyCode ^ 0x01000000)));
        }
        return ShortcutLocalize.keyboardUnknown0("keyCode: 0x" + Integer.toString(keyCode, 16));
    }
}
