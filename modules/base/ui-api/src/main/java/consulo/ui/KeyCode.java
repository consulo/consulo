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
package consulo.ui;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-05-14
 */
@Deprecated
public enum KeyCode {
  A,
  B,
  C,
  D,
  E,
  F,
  G,
  H,
  I,
  J,
  K,
  L,
  M,
  N,
  O,
  P,
  Q,
  R,
  S,
  T,
  U,
  V,
  W,
  X,
  Y,
  Z;

  private static final KeyCode[] VALUES = values();

  @Nonnull
  public static KeyCode from(char ch) {
    if (ch >= 'a' && ch <= 'z') {
      int diff = (int)ch - (int)'a';
      return VALUES[A.ordinal() + diff];
    }
    else if (ch >= 'A' && ch <= 'Z') {
      int diff = (int)ch - (int)'A';
      return VALUES[A.ordinal() + diff];
    }
    else {
      throw new IllegalArgumentException("Illegal char: " + String.valueOf(ch));
    }
  }
}
