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
import consulo.ui.ex.localize.ShortcutLocalize;
import consulo.ui.ex.localize.UnicodeShortcutLocalize;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;

import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;

/**
 * @author VISTALL
 * @since 2025-09-13
 *
 * Some some code from Swing {@link KeyEvent#getKeyText(int)}
 */
public class ShortcutLocalizeHolder {
    private record ShortcutInfo(@Nonnull LocalizeValue textShortcut, @Nonnull LocalizeValue unicodeShortcut) {
    }

    private static final Int2ObjectMap<ShortcutInfo> cache = new Int2ObjectLinkedOpenHashMap<>();

    static {
        put(VK_ENTER, UnicodeShortcutLocalize.keyboardEnter(), ShortcutLocalize.keyboardEnter());
        put(VK_BACK_SPACE, UnicodeShortcutLocalize.keyboardBackspace(), ShortcutLocalize.keyboardBackspace());
        put(VK_TAB, UnicodeShortcutLocalize.keyboardTab(), ShortcutLocalize.keyboardTab());
        put(VK_CLEAR, UnicodeShortcutLocalize.keyboardClear(), ShortcutLocalize.keyboardClear());
        put(VK_CAPS_LOCK, UnicodeShortcutLocalize.keyboardCapslock(), ShortcutLocalize.keyboardCapslock());
        put(VK_ESCAPE, UnicodeShortcutLocalize.keyboardEscape(), ShortcutLocalize.keyboardEscape());
        put(VK_SPACE, UnicodeShortcutLocalize.keyboardSpace(), ShortcutLocalize.keyboardSpace());
        put(VK_PAGE_UP, UnicodeShortcutLocalize.keyboardPgup(), ShortcutLocalize.keyboardPgup());
        put(VK_PAGE_DOWN, UnicodeShortcutLocalize.keyboardPgdn(), ShortcutLocalize.keyboardPgdn());
        put(VK_END, UnicodeShortcutLocalize.keyboardEnd(), ShortcutLocalize.keyboardEnd());
        put(VK_HOME, UnicodeShortcutLocalize.keyboardHome(), ShortcutLocalize.keyboardHome());
        put(VK_KP_LEFT, UnicodeShortcutLocalize.keyboardLeft(), ShortcutLocalize.keyboardLeft());
        put(VK_LEFT, UnicodeShortcutLocalize.keyboardLeft(), ShortcutLocalize.keyboardLeft());
        put(VK_KP_UP, UnicodeShortcutLocalize.keyboardUp(), ShortcutLocalize.keyboardUp());
        put(VK_UP, UnicodeShortcutLocalize.keyboardUp(), ShortcutLocalize.keyboardUp());
        put(VK_KP_RIGHT, UnicodeShortcutLocalize.keyboardRight(), ShortcutLocalize.keyboardRight());
        put(VK_RIGHT, UnicodeShortcutLocalize.keyboardRight(), ShortcutLocalize.keyboardRight());
        put(VK_KP_DOWN, UnicodeShortcutLocalize.keyboardDown(), ShortcutLocalize.keyboardDown());
        put(VK_DOWN, UnicodeShortcutLocalize.keyboardDown(), ShortcutLocalize.keyboardDown());
        put(VK_BACK_QUOTE, UnicodeShortcutLocalize.keyboardBackquote(), ShortcutLocalize.keyboardBackquote());
        put(VK_COMMA, UnicodeShortcutLocalize.keyboardComma(), ShortcutLocalize.keyboardComma());
        put(VK_PERIOD, UnicodeShortcutLocalize.keyboardPeriod(), ShortcutLocalize.keyboardPeriod());
        put(VK_SEMICOLON, UnicodeShortcutLocalize.keyboardSemicolon(), ShortcutLocalize.keyboardSemicolon());
        put(VK_EQUALS, UnicodeShortcutLocalize.keyboardEquals(), ShortcutLocalize.keyboardEquals());
        put(VK_SLASH, UnicodeShortcutLocalize.keyboardSlash(), ShortcutLocalize.keyboardSlash());
        put(VK_OPEN_BRACKET, UnicodeShortcutLocalize.keyboardOpenbracket(), ShortcutLocalize.keyboardOpenbracket());
        put(VK_BACK_SLASH, UnicodeShortcutLocalize.keyboardBackslash(), ShortcutLocalize.keyboardBackslash());
        put(VK_CLOSE_BRACKET, UnicodeShortcutLocalize.keyboardClosebracket(), ShortcutLocalize.keyboardClosebracket());
        put(VK_ADD, UnicodeShortcutLocalize.keyboardAdd(), ShortcutLocalize.keyboardAdd());
        put(VK_MINUS, UnicodeShortcutLocalize.keyboardMinus(), ShortcutLocalize.keyboardMinus());
        put(VK_PLUS, UnicodeShortcutLocalize.keyboardPlus(), ShortcutLocalize.keyboardPlus());

        put(VK_BEGIN, ShortcutLocalize.keyboardBegin());
        put(VK_CANCEL, ShortcutLocalize.keyboardCancel());
        put(VK_COMPOSE, ShortcutLocalize.keyboardCompose());
        put(VK_PAUSE, ShortcutLocalize.keyboardPause());

        put(VK_SHIFT, UnicodeShortcutLocalize.keyboardShift(), ShortcutLocalize.keyboardShift());
        put(VK_CONTROL, UnicodeShortcutLocalize.keyboardControl(), ShortcutLocalize.keyboardControl());
        put(VK_ALT, UnicodeShortcutLocalize.keyboardAlt(), ShortcutLocalize.keyboardAlt());
        put(VK_META, UnicodeShortcutLocalize.keyboardMeta(), ShortcutLocalize.keyboardMeta());
        put(VK_ALT_GRAPH, UnicodeShortcutLocalize.keyboardAltgraph(), ShortcutLocalize.keyboardAltgraph());

        put(VK_MULTIPLY, UnicodeShortcutLocalize.keyboardMultiply(), ShortcutLocalize.keyboardMultiply());
        put(VK_SEPARATOR, UnicodeShortcutLocalize.keyboardSeparator(), ShortcutLocalize.keyboardSeparator());
        put(VK_SUBTRACT, UnicodeShortcutLocalize.keyboardSubtract(), ShortcutLocalize.keyboardSubtract());
        put(VK_DECIMAL, UnicodeShortcutLocalize.keyboardDecimal(), ShortcutLocalize.keyboardDecimal());
        put(VK_DIVIDE, UnicodeShortcutLocalize.keyboardDivide(), ShortcutLocalize.keyboardDivide());
        put(VK_NUM_LOCK, UnicodeShortcutLocalize.keyboardNumlock(), ShortcutLocalize.keyboardNumlock());

        put(VK_DELETE, ShortcutLocalize.keyboardDelete());
        put(VK_SCROLL_LOCK, ShortcutLocalize.keyboardScrolllock());

        put(VK_WINDOWS, ShortcutLocalize.keyboardWindows());
        put(VK_CONTEXT_MENU, ShortcutLocalize.keyboardContext());
        
        put(VK_NUMPAD0, UnicodeShortcutLocalize.keyboardNumpad0(), ShortcutLocalize.keyboardNumpad0());
        put(VK_NUMPAD1, UnicodeShortcutLocalize.keyboardNumpad1(), ShortcutLocalize.keyboardNumpad1());
        put(VK_NUMPAD2, UnicodeShortcutLocalize.keyboardNumpad2(), ShortcutLocalize.keyboardNumpad2());
        put(VK_NUMPAD3, UnicodeShortcutLocalize.keyboardNumpad3(), ShortcutLocalize.keyboardNumpad3());
        put(VK_NUMPAD4, UnicodeShortcutLocalize.keyboardNumpad4(), ShortcutLocalize.keyboardNumpad4());
        put(VK_NUMPAD5, UnicodeShortcutLocalize.keyboardNumpad5(), ShortcutLocalize.keyboardNumpad5());
        put(VK_NUMPAD6, UnicodeShortcutLocalize.keyboardNumpad6(), ShortcutLocalize.keyboardNumpad6());
        put(VK_NUMPAD7, UnicodeShortcutLocalize.keyboardNumpad7(), ShortcutLocalize.keyboardNumpad7());
        put(VK_NUMPAD8, UnicodeShortcutLocalize.keyboardNumpad8(), ShortcutLocalize.keyboardNumpad8());
        put(VK_NUMPAD9, UnicodeShortcutLocalize.keyboardNumpad9(), ShortcutLocalize.keyboardNumpad9());

        put(VK_0, UnicodeShortcutLocalize.keyboard0());
        put(VK_1, UnicodeShortcutLocalize.keyboard1());
        put(VK_2, UnicodeShortcutLocalize.keyboard2());
        put(VK_3, UnicodeShortcutLocalize.keyboard3());
        put(VK_4, UnicodeShortcutLocalize.keyboard4());
        put(VK_5, UnicodeShortcutLocalize.keyboard5());
        put(VK_6, UnicodeShortcutLocalize.keyboard6());
        put(VK_7, UnicodeShortcutLocalize.keyboard7());
        put(VK_8, UnicodeShortcutLocalize.keyboard8());
        put(VK_9, UnicodeShortcutLocalize.keyboard9());

        put(VK_A, UnicodeShortcutLocalize.keyboardA());
        put(VK_B, UnicodeShortcutLocalize.keyboardB());
        put(VK_C, UnicodeShortcutLocalize.keyboardC());
        put(VK_D, UnicodeShortcutLocalize.keyboardD());
        put(VK_E, UnicodeShortcutLocalize.keyboardE());
        put(VK_F, UnicodeShortcutLocalize.keyboardF());
        put(VK_G, UnicodeShortcutLocalize.keyboardG());
        put(VK_H, UnicodeShortcutLocalize.keyboardH());
        put(VK_I, UnicodeShortcutLocalize.keyboardI());
        put(VK_J, UnicodeShortcutLocalize.keyboardJ());
        put(VK_K, UnicodeShortcutLocalize.keyboardK());
        put(VK_L, UnicodeShortcutLocalize.keyboardL());
        put(VK_M, UnicodeShortcutLocalize.keyboardM());
        put(VK_N, UnicodeShortcutLocalize.keyboardN());
        put(VK_O, UnicodeShortcutLocalize.keyboardO());
        put(VK_P, UnicodeShortcutLocalize.keyboardP());
        put(VK_Q, UnicodeShortcutLocalize.keyboardQ());
        put(VK_R, UnicodeShortcutLocalize.keyboardR());
        put(VK_S, UnicodeShortcutLocalize.keyboardS());
        put(VK_T, UnicodeShortcutLocalize.keyboardT());
        put(VK_U, UnicodeShortcutLocalize.keyboardU());
        put(VK_V, UnicodeShortcutLocalize.keyboardV());
        put(VK_W, UnicodeShortcutLocalize.keyboardW());
        put(VK_X, UnicodeShortcutLocalize.keyboardX());
        put(VK_Y, UnicodeShortcutLocalize.keyboardY());
        put(VK_Z, UnicodeShortcutLocalize.keyboardZ());

        put(VK_F1, ShortcutLocalize.keyboardF1());
        put(VK_F2, ShortcutLocalize.keyboardF2());
        put(VK_F3, ShortcutLocalize.keyboardF3());
        put(VK_F4, ShortcutLocalize.keyboardF4());
        put(VK_F5, ShortcutLocalize.keyboardF5());
        put(VK_F6, ShortcutLocalize.keyboardF6());
        put(VK_F7, ShortcutLocalize.keyboardF7());
        put(VK_F8, ShortcutLocalize.keyboardF8());
        put(VK_F9, ShortcutLocalize.keyboardF9());
        put(VK_F10, ShortcutLocalize.keyboardF10());
        put(VK_F11, ShortcutLocalize.keyboardF11());
        put(VK_F12, ShortcutLocalize.keyboardF12());
        put(VK_F13, ShortcutLocalize.keyboardF13());
        put(VK_F14, ShortcutLocalize.keyboardF14());
        put(VK_F15, ShortcutLocalize.keyboardF15());
        put(VK_F16, ShortcutLocalize.keyboardF16());
        put(VK_F17, ShortcutLocalize.keyboardF17());
        put(VK_F18, ShortcutLocalize.keyboardF18());
        put(VK_F19, ShortcutLocalize.keyboardF19());
        put(VK_F20, ShortcutLocalize.keyboardF20());
        put(VK_F21, ShortcutLocalize.keyboardF21());
        put(VK_F22, ShortcutLocalize.keyboardF22());
        put(VK_F23, ShortcutLocalize.keyboardF23());
        put(VK_F24, ShortcutLocalize.keyboardF24());

        put(VK_PRINTSCREEN, ShortcutLocalize.keyboardPrintscreen());
        put(VK_INSERT, ShortcutLocalize.keyboardInsert());
        put(VK_HELP, ShortcutLocalize.keyboardHelp());
        put(VK_QUOTE, ShortcutLocalize.keyboardQuote());

        put(VK_DEAD_GRAVE, ShortcutLocalize.keyboardDeadgrave());
        put(VK_DEAD_ACUTE, ShortcutLocalize.keyboardDeadacute());
        put(VK_DEAD_CIRCUMFLEX, ShortcutLocalize.keyboardDeadcircumflex());
        put(VK_DEAD_TILDE, ShortcutLocalize.keyboardDeadtilde());
        put(VK_DEAD_MACRON, ShortcutLocalize.keyboardDeadmacron());
        put(VK_DEAD_BREVE, ShortcutLocalize.keyboardDeadbreve());
        put(VK_DEAD_ABOVEDOT, ShortcutLocalize.keyboardDeadabovedot());
        put(VK_DEAD_DIAERESIS, ShortcutLocalize.keyboardDeaddiaeresis());
        put(VK_DEAD_ABOVERING, ShortcutLocalize.keyboardDeadabovering());
        put(VK_DEAD_DOUBLEACUTE, ShortcutLocalize.keyboardDeaddoubleacute());
        put(VK_DEAD_CARON, ShortcutLocalize.keyboardDeadcaron());
        put(VK_DEAD_CEDILLA, ShortcutLocalize.keyboardDeadcedilla());
        put(VK_DEAD_OGONEK, ShortcutLocalize.keyboardDeadogonek());
        put(VK_DEAD_IOTA, ShortcutLocalize.keyboardDeadiota());
        put(VK_DEAD_VOICED_SOUND, ShortcutLocalize.keyboardDeadvoicedsound());
        put(VK_DEAD_SEMIVOICED_SOUND, ShortcutLocalize.keyboardDeadsemivoicedsound());

        put(VK_AMPERSAND, UnicodeShortcutLocalize.keyboardAmpersand(), ShortcutLocalize.keyboardAmpersand());
        put(VK_ASTERISK, UnicodeShortcutLocalize.keyboardAsterisk(), ShortcutLocalize.keyboardAsterisk());
        put(VK_QUOTEDBL, UnicodeShortcutLocalize.keyboardQuotedbl(), ShortcutLocalize.keyboardQuotedbl());
        put(VK_LESS, UnicodeShortcutLocalize.keyboardLess(), ShortcutLocalize.keyboardLess());
        put(VK_GREATER, UnicodeShortcutLocalize.keyboardGreater(), ShortcutLocalize.keyboardGreater());
        put(VK_BRACELEFT, UnicodeShortcutLocalize.keyboardBraceleft(), ShortcutLocalize.keyboardBraceleft());
        put(VK_BRACERIGHT, UnicodeShortcutLocalize.keyboardBraceright(), ShortcutLocalize.keyboardBraceright());
        put(VK_AT, UnicodeShortcutLocalize.keyboardAt(), ShortcutLocalize.keyboardAt());
        put(VK_COLON, UnicodeShortcutLocalize.keyboardColon(), ShortcutLocalize.keyboardColon());
        put(VK_CIRCUMFLEX, ShortcutLocalize.keyboardCircumflex());
        put(VK_DOLLAR, UnicodeShortcutLocalize.keyboardDollar(), ShortcutLocalize.keyboardDollar());
        put(VK_EURO_SIGN, UnicodeShortcutLocalize.keyboardEuro(), ShortcutLocalize.keyboardEuro());
        put(VK_EXCLAMATION_MARK, UnicodeShortcutLocalize.keyboardExclamationmark(), ShortcutLocalize.keyboardExclamationmark());
        put(VK_INVERTED_EXCLAMATION_MARK, UnicodeShortcutLocalize.keyboardInvertedexclamationmark(), ShortcutLocalize.keyboardInvertedexclamationmark());
        put(VK_LEFT_PARENTHESIS, UnicodeShortcutLocalize.keyboardLeftparenthesis(), ShortcutLocalize.keyboardLeftparenthesis());
        put(VK_NUMBER_SIGN, UnicodeShortcutLocalize.keyboardNumbersign(), ShortcutLocalize.keyboardNumbersign());
        put(VK_RIGHT_PARENTHESIS, UnicodeShortcutLocalize.keyboardRightparenthesis(), ShortcutLocalize.keyboardRightparenthesis());
        put(VK_UNDERSCORE, UnicodeShortcutLocalize.keyboardUnderscore(), ShortcutLocalize.keyboardUnderscore());

        put(VK_FINAL, ShortcutLocalize.keyboardFinal());
        put(VK_CONVERT, ShortcutLocalize.keyboardConvert());
        put(VK_NONCONVERT, ShortcutLocalize.keyboardNoconvert());
        put(VK_ACCEPT, ShortcutLocalize.keyboardAccept());
        put(VK_MODECHANGE, ShortcutLocalize.keyboardModechange());
        put(VK_KANA, ShortcutLocalize.keyboardKana());
        put(VK_KANJI, ShortcutLocalize.keyboardKanji());
        put(VK_ALPHANUMERIC, ShortcutLocalize.keyboardAlphanumeric());
        put(VK_KATAKANA, ShortcutLocalize.keyboardKatakana());
        put(VK_HIRAGANA, ShortcutLocalize.keyboardHiragana());
        put(VK_FULL_WIDTH, ShortcutLocalize.keyboardFullwidth());
        put(VK_HALF_WIDTH, ShortcutLocalize.keyboardHalfwidth());
        put(VK_ROMAN_CHARACTERS, ShortcutLocalize.keyboardRomancharacters());
        put(VK_ALL_CANDIDATES, ShortcutLocalize.keyboardAllcandidates());
        put(VK_PREVIOUS_CANDIDATE, ShortcutLocalize.keyboardPreviouscandidate());
        put(VK_CODE_INPUT, ShortcutLocalize.keyboardCodeinput());
        put(VK_JAPANESE_KATAKANA, ShortcutLocalize.keyboardJapanesekatakana());
        put(VK_JAPANESE_HIRAGANA, ShortcutLocalize.keyboardJapanesehiragana());
        put(VK_JAPANESE_ROMAN, ShortcutLocalize.keyboardJapaneseroman());
        put(VK_KANA_LOCK, ShortcutLocalize.keyboardKanalock());
        put(VK_INPUT_METHOD_ON_OFF, ShortcutLocalize.keyboardInputmethodonoff());

        put(VK_AGAIN, ShortcutLocalize.keyboardAgain());
        put(VK_UNDO, ShortcutLocalize.keyboardUndo());
        put(VK_COPY, ShortcutLocalize.keyboardCopy());
        put(VK_PASTE, ShortcutLocalize.keyboardPaste());
        put(VK_CUT, ShortcutLocalize.keyboardCut());
        put(VK_FIND, ShortcutLocalize.keyboardFind());
        put(VK_PROPS, ShortcutLocalize.keyboardProps());
        put(VK_STOP, ShortcutLocalize.keyboardStop());
    }

    static void put(int keyCode, LocalizeValue textLocalize) {
        cache.put(keyCode, new ShortcutInfo(textLocalize, textLocalize));
    }

    static void put(int keyCode, LocalizeValue unicodeLocalize, LocalizeValue textLocalize) {
        cache.put(keyCode, new ShortcutInfo(textLocalize, unicodeLocalize));
    }

    @Nonnull
    public static LocalizeValue getKeyText(int keyCode, boolean unicodeShortcut) {
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
