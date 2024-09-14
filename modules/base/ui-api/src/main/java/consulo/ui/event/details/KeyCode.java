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
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-09-12
 */
public interface KeyCode {
    KeyCode ENTER = KeyCode.of('\n', "ENTER");

    KeyCode UP = KeyCode.of(0x26, "VK_UP");

    KeyCode DOWN = KeyCode.of(0x28, "VK_DOWN");

    int key();

    String name();

    @Nonnull
    static KeyCode of(int key) {
        return KeyCodeImpl.ourMap.computeIfAbsent(key, it -> new KeyCodeImpl(it, Character.toString(it)));
    }

    @Nonnull
    static KeyCode of(int key, String name) {
        return KeyCodeImpl.ourMap.computeIfAbsent(key, it -> new KeyCodeImpl(it, name));
    }
}
