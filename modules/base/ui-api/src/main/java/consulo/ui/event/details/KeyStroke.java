/*
 * Copyright 2013-2026 consulo.io
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

import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One key stroke of a shortcut: a key pressed or released with modifiers held down, or a character typed.
 * <p/>
 * The text form is the one keymaps are written in: the modifiers ({@code shift}, {@code ctrl}, {@code meta}, {@code alt},
 * {@code altGraph}), then {@code typed} and the character, or an optional {@code pressed} or {@code released} and the
 * name of the key, such as {@code ctrl shift released F1}. Words may come in any case, and a key may also be given by its
 * code, as in {@code #10b}.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class KeyStroke {
    public static final char CHAR_UNDEFINED = '￿';

    private static final Map<String, Modifier> MODIFIER_NAMES = Map.of(
        "shift", Modifier.SHIFT,
        "ctrl", Modifier.CTRL,
        "control", Modifier.CTRL,
        "meta", Modifier.META,
        "alt", Modifier.ALT,
        "altgraph", Modifier.ALT_GRAPH,
        "altgr", Modifier.ALT_GRAPH
    );

    private final KeyCode myKeyCode;
    private final char myKeyChar;
    private final Set<Modifier> myModifiers;
    private final boolean myOnKeyRelease;

    private KeyStroke(KeyCode keyCode, char keyChar, Set<Modifier> modifiers, boolean onKeyRelease) {
        myKeyCode = keyCode;
        myKeyChar = keyChar;
        myModifiers = modifiers.isEmpty()
            ? Collections.emptySet()
            : Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
        myOnKeyRelease = onKeyRelease;
    }

    public static KeyStroke pressed(KeyCode keyCode, Modifier... modifiers) {
        return of(keyCode, toSet(modifiers), false);
    }

    public static KeyStroke released(KeyCode keyCode, Modifier... modifiers) {
        return of(keyCode, toSet(modifiers), true);
    }

    public static KeyStroke of(KeyCode keyCode, Set<Modifier> modifiers, boolean onKeyRelease) {
        if (keyCode == KeyCode.UNDEFINED) {
            throw new IllegalArgumentException("A key stroke of a key needs a defined key");
        }
        return new KeyStroke(keyCode, CHAR_UNDEFINED, modifiers, onKeyRelease);
    }

    public static KeyStroke typed(char keyChar, Modifier... modifiers) {
        return typed(keyChar, toSet(modifiers));
    }

    public static KeyStroke typed(char keyChar, Set<Modifier> modifiers) {
        if (keyChar == CHAR_UNDEFINED) {
            throw new IllegalArgumentException("A typed key stroke needs a defined character");
        }
        return new KeyStroke(KeyCode.UNDEFINED, keyChar, modifiers, false);
    }

    /**
     * @return the key stroke the text describes, or {@code null} when it describes none
     */
    public static @Nullable KeyStroke parse(String text) {
        String[] tokens = text.trim().split("\\s+");
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        boolean typed = false;
        boolean pressed = false;
        boolean released = false;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            boolean last = i == tokens.length - 1;
            if (token.isEmpty()) {
                return null;
            }

            if (typed) {
                return last && token.length() == 1 ? typed(token.charAt(0), modifiers) : null;
            }

            String lowerCase = token.toLowerCase(Locale.ENGLISH);
            if (pressed || released || last) {
                if (!last) {
                    return null;
                }
                KeyCode keyCode = keyOf(token);
                return keyCode == null ? null : of(keyCode, modifiers, released);
            }

            switch (lowerCase) {
                case "typed" -> typed = true;
                case "pressed" -> pressed = true;
                case "released" -> released = true;
                default -> {
                    Modifier modifier = MODIFIER_NAMES.get(lowerCase);
                    if (modifier == null) {
                        return null;
                    }
                    modifiers.add(modifier);
                }
            }
        }
        return null;
    }

    private static @Nullable KeyCode keyOf(String token) {
        KeyCode keyCode = KeyCode.byName(token);
        if (keyCode != null) {
            return keyCode == KeyCode.UNDEFINED ? null : keyCode;
        }

        try {
            int code = Integer.decode(token);
            return code == KeyCode.UNDEFINED.key() ? null : KeyCode.of(code);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static Set<Modifier> toSet(Modifier[] modifiers) {
        Set<Modifier> set = EnumSet.noneOf(Modifier.class);
        Collections.addAll(set, modifiers);
        return set;
    }

    /**
     * @return the key, {@link KeyCode#UNDEFINED} for a typed key stroke
     */
    public KeyCode getKeyCode() {
        return myKeyCode;
    }

    /**
     * @return the character of a typed key stroke, {@link #CHAR_UNDEFINED} for any other
     */
    public char getKeyChar() {
        return myKeyChar;
    }

    public boolean isTyped() {
        return myKeyChar != CHAR_UNDEFINED;
    }

    public boolean isOnKeyRelease() {
        return myOnKeyRelease;
    }

    public Set<Modifier> getModifiers() {
        return myModifiers;
    }

    public boolean hasModifier(Modifier modifier) {
        return myModifiers.contains(modifier);
    }

    /**
     * @return the text form, which {@link #parse(String)} reads back to an equal key stroke
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        append(builder, "shift", Modifier.SHIFT);
        append(builder, "ctrl", Modifier.CTRL);
        append(builder, "meta", Modifier.META);
        append(builder, "alt", Modifier.ALT);
        append(builder, "altGraph", Modifier.ALT_GRAPH);
        if (isTyped()) {
            return builder.append("typed ").append(myKeyChar).toString();
        }
        if (myOnKeyRelease) {
            builder.append("released ");
        }
        return builder.append(myKeyCode.name()).toString();
    }

    private void append(StringBuilder builder, String name, Modifier modifier) {
        if (myModifiers.contains(modifier)) {
            builder.append(name).append(' ');
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof KeyStroke other
            && myKeyCode.equals(other.myKeyCode)
            && myKeyChar == other.myKeyChar
            && myOnKeyRelease == other.myOnKeyRelease
            && myModifiers.equals(other.myModifiers);
    }

    @Override
    public int hashCode() {
        int result = myKeyCode.hashCode();
        result = 31 * result + myKeyChar;
        result = 31 * result + myModifiers.hashCode();
        return 31 * result + (myOnKeyRelease ? 1 : 0);
    }
}
