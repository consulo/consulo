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

import consulo.application.util.SystemInfo;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;

/**
 * @author VISTALL
 * @since 21-Aug-17
 * <p>
 * original author Nikolay Chashnikov (kotlin)
 */
class Keymaps {
  static enum KeymapKind {
    WIN("Win/Linux", "$default"),
    MAC("Mac", "Mac OS X 10.5+");

    private String myDisplayName;
    private String myDefaultKeymapName;

    KeymapKind(String displayName, String defaultKeymapName) {

      myDisplayName = displayName;
      myDefaultKeymapName = defaultKeymapName;
    }

    public Keymap getKeymap() {
      switch (this) {
        case WIN:
          return winKeymap;
        case MAC:
          return macKeymap;
        default:
          throw new UnsupportedOperationException();
      }
    }

    public KeymapKind getAlternativeKind() {
      switch (this) {
        case WIN:
          return MAC;
        case MAC:
          return WIN;
        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  public static class KeymapDescription {
    private String name;
    private String displayText;

    public KeymapDescription(String name, String displayText) {
      this.name = name;
      this.displayText = displayText;
    }

    public KeymapKind getKind() {
      return name.contains("Mac OS") ? KeymapKind.MAC : KeymapKind.WIN;
    }

    public String getName() {
      return name;
    }

    public String getDisplayText() {
      return displayText;
    }

    public Keymap getKeymap() {
      return KeymapManager.getInstance().getKeymap(name);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      KeymapDescription that = (KeymapDescription)o;

      if (name != null ? !name.equals(that.name) : that.name != null) {
        return false;
      }
      if (displayText != null ? !displayText.equals(that.displayText) : that.displayText != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = name != null ? name.hashCode() : 0;
      result = 31 * result + (displayText != null ? displayText.hashCode() : 0);
      return result;
    }
  }

  private static final Keymap winKeymap = KeymapManager.getInstance().getKeymap(KeymapKind.WIN.myDefaultKeymapName);
  private static final Keymap macKeymap = KeymapManager.getInstance().getKeymap(KeymapKind.MAC.myDefaultKeymapName);

  public static KeymapKind getCurrentOSKind() {
    if (SystemInfo.isMac) {
      return KeymapKind.MAC;
    }
    return KeymapKind.WIN;
  }

  public static KeymapDescription getDefaultMainKeymap() {
    return new KeymapDescription(getCurrentOSKind().myDefaultKeymapName, "");
  }

  public static KeymapDescription getDefaultAlternativeKeymap() {
    KeymapKind keymap = getCurrentOSKind().getAlternativeKind();
    if (keymap != null) {
      return new KeymapDescription(keymap.myDefaultKeymapName, "for " + keymap.myDisplayName);
    }
    return null;
  }
}
