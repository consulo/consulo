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
package consulo.ui.internal;

import consulo.ui.event.details.KeyCode;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every known key is registered once, by its {@link KeyCode} constant, so a code always resolves to the same key with
 * the same name. A code nobody registered still gets a key of its own, named by its hexadecimal value.
 *
 * @author VISTALL
 * @since 2024-09-12
 */
public final class KeyCodeImpl implements KeyCode {
    private static final Map<Integer, KeyCode> ourByCode = new ConcurrentHashMap<>();
    private static final Map<String, KeyCode> ourByName = new ConcurrentHashMap<>();

    public static KeyCode register(int key, String name, String text) {
        KeyCodeImpl keyCode = new KeyCodeImpl(key, name, text);
        ourByCode.putIfAbsent(key, keyCode);
        ourByName.putIfAbsent(name.toLowerCase(Locale.ENGLISH), keyCode);
        return keyCode;
    }

    public static KeyCode of(int key) {
        ensureRegistered();
        KeyCode known = ourByCode.get(key);
        if (known != null) {
            return known;
        }
        String name = "#" + Integer.toHexString(key);
        return ourByCode.computeIfAbsent(key, it -> new KeyCodeImpl(it, name, name));
    }

    public static @Nullable KeyCode byName(String name) {
        ensureRegistered();
        String key = name.toLowerCase(Locale.ENGLISH);
        // an old spelling the toolkits kept for the same key
        return ourByName.get(key.equals("separater") ? "separator" : key);
    }

    /**
     * A lookup before the constants are made would give a code a key of its own, which the constant can no longer replace.
     */
    private static void ensureRegistered() {
        Objects.requireNonNull(KeyCode.UNDEFINED);
    }

    private final int myKey;
    private final String myName;
    private final String myText;

    private KeyCodeImpl(int key, String name, String text) {
        myKey = key;
        myName = name;
        myText = text;
    }

    @Override
    public int key() {
        return myKey;
    }

    @Override
    public String name() {
        return myName;
    }

    @Override
    public String getText() {
        return myText;
    }

    @Override
    public String toString() {
        return myName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof KeyCodeImpl other && myKey == other.myKey;
    }

    @Override
    public int hashCode() {
        return myKey;
    }
}
