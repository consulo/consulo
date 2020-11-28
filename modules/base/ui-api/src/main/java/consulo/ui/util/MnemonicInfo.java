/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-14
 */
public class MnemonicInfo {
  @Nullable
  public static MnemonicInfo parse(@Nonnull String text) {
    final StringBuilder realText = new StringBuilder();
    char mnemonic = '\0';
    int index = -1;
    for (int i = 0; i < text.length(); i++) {
      final char ch = text.charAt(i);
      if (ch != '&') {
        realText.append(ch);
      }
      else if (i + 1 < text.length()) {
        mnemonic = text.charAt(i + 1);
        index = realText.length();
      }
    }

    return mnemonic == '\0' ? null : new MnemonicInfo(mnemonic, index, realText.toString());
  }

  private final char myKeyCode;
  private final int myIndex;
  private final String myText;

  public MnemonicInfo(char keyCode, int index, String text) {
    myKeyCode = keyCode;
    myIndex = index;
    myText = text;
  }

  public char getKeyCode() {
    return myKeyCode;
  }

  public int getIndex() {
    return myIndex;
  }

  public String getText() {
    return myText;
  }
}
