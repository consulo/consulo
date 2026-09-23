/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.presentationAssistant;

import consulo.platform.Platform;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Nikolay Chashnikov
 * @author VISTALL
 * @since 2017-08-21
 */
class Keymaps {
    static enum KeymapKind {
        WIN("Win/Linux", "$default"),
        MAC("Mac", "Mac OS X 10.5+");

        private final String myDisplayName;
        private final String myDefaultKeymapName;

        KeymapKind(String displayName, String defaultKeymapName) {
            myDisplayName = displayName;
            myDefaultKeymapName = defaultKeymapName;
        }

        public Keymap getKeymap() {
            return switch (this) {
                case WIN -> WIN_KEYMAP;
                case MAC -> MAC_KEYMAP;
            };
        }

        public KeymapKind getAlternativeKind() {
            return switch (this) {
                case WIN -> MAC;
                case MAC -> WIN;
            };
        }
    }

    public static class KeymapDescription {
        private final String myName;
        private final String myDisplayText;

        public KeymapDescription(String name, String displayText) {
            this.myName = name;
            this.myDisplayText = displayText;
        }

        public KeymapKind getKind() {
            return myName.contains("Mac OS") ? KeymapKind.MAC : KeymapKind.WIN;
        }

        public String getName() {
            return myName;
        }

        public String getDisplayText() {
            return myDisplayText;
        }

        public Keymap getKeymap() {
            return KeymapManager.getInstance().getKeymap(myName);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            KeymapDescription that = (KeymapDescription) o;

            return Objects.equals(myName, that.myName)
                && Objects.equals(myDisplayText, that.myDisplayText);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(myName) + Objects.hashCode(myDisplayText);
        }
    }

    private static final Keymap WIN_KEYMAP = KeymapManager.getInstance().getKeymap(KeymapKind.WIN.myDefaultKeymapName);
    private static final Keymap MAC_KEYMAP = KeymapManager.getInstance().getKeymap(KeymapKind.MAC.myDefaultKeymapName);

    public static KeymapKind getCurrentOSKind() {
        return Platform.current().os().isMac() ? KeymapKind.MAC : KeymapKind.WIN;
    }

    public static KeymapDescription getDefaultMainKeymap() {
        return new KeymapDescription(getCurrentOSKind().myDefaultKeymapName, "");
    }

    public static KeymapDescription getDefaultAlternativeKeymap() {
        KeymapKind keymap = getCurrentOSKind().getAlternativeKind();
        return new KeymapDescription(keymap.myDefaultKeymapName, "for " + keymap.myDisplayName);
    }
}
