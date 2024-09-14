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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2024-09-12
 */
public class KeyCodeImpl implements KeyCode {
    public static final Map<Integer, KeyCode> ourMap = new ConcurrentHashMap<>();

    private final int myKey;
    private final String myName;

    public KeyCodeImpl(int key, String name) {
        myKey = key;
        myName = name;
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
    public String toString() {
        return myKey + ":" + myName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KeyCodeImpl keyChar = (KeyCodeImpl) o;
        return myKey == keyChar.myKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myKey);
    }
}
